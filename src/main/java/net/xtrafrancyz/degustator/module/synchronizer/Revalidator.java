package net.xtrafrancyz.degustator.module.synchronizer;

import discord4j.core.object.util.Snowflake;
import reactor.core.publisher.Mono;

import net.xtrafrancyz.degustator.Degustator;
import net.xtrafrancyz.degustator.Scheduler;
import net.xtrafrancyz.degustator.mysql.Row;
import net.xtrafrancyz.degustator.mysql.SelectResult;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

/**
 * @author xtrafrancyz
 */
public class Revalidator {
    private final Degustator degustator;
    private final Synchronizer2 synchronizer;
    
    private int revalidatorTask = -1;
    private int revalidatorSaverTask = -1;
    private Queue<UpdatingPlayer> revalidated = new ConcurrentLinkedQueue<>();
    
    public Revalidator(Degustator degustator, Synchronizer2 synchronizer) {
        this.degustator = degustator;
        this.synchronizer = synchronizer;
    }
    
    public boolean isRunning() {
        return revalidatorTask != -1;
    }
    
    public void start() {
        if (isRunning())
            return;
        revalidatorTask = Scheduler.scheduleAtFixedRate(this::revalidate, 0, 1, TimeUnit.HOURS);
        revalidatorSaverTask = Scheduler.scheduleAtFixedRate(this::saveRevalidated, 0, 30, TimeUnit.SECONDS);
    }
    
    public void stop() {
        if (!isRunning())
            return;
        Scheduler.cancelTask(revalidatorTask);
        revalidatorTask = -1;
        Scheduler.cancelTask(revalidatorSaverTask);
        revalidatorSaverTask = -1;
    }
    
    private void revalidate() {
        try {
            SelectResult select = degustator.mysql.select("SELECT id, rank, username FROM linked WHERE updated < ?", ps ->
                ps.setInt(1, (int) (System.currentTimeMillis() / 1000) - 12 * 60 * 60)
            );
            Map<String, UpdatingPlayer> players = new HashMap<>(select.getRows().size() * 2);
            List<String> nicknames = new ArrayList<>(select.getRows().size());
            for (Row row : select.getRows()) {
                Snowflake id = Snowflake.of(row.getLong("id"));
                String username = row.getString("username");
                String rank = row.getString("rank");
                synchronizer.usernames.put(id, CompletableFuture.completedFuture(username));
                players.put(username, new UpdatingPlayer(id, username, rank));
                nicknames.add(username);
            }
            synchronizer.ranks.getAll(nicknames).thenAccept(ranks -> {
                synchronizer.getVimeWorldGuild(guild -> {
                    for (Map.Entry<String, String> entry : ranks.entrySet()) {
                        UpdatingPlayer player = players.get(entry.getKey());
                        if (player != null && !Objects.equals(player.syncedRank, entry.getValue())) {
                            guild.getMemberById(player.id)
                                .onErrorResume(ignored -> Mono.empty())
                                .subscribe(member -> {
                                    if (member == null) {
                                        Scheduler.execute(() -> {
                                            try {
                                                degustator.mysql.query("UPDATE linked SET updated = ? WHERE id = ?", ps -> {
                                                    ps.setInt(1, (int) (System.currentTimeMillis() / 1000) + 60 * 24 * 31);
                                                    ps.setLong(2, player.id.asLong());
                                                });
                                            } catch (SQLException e) {
                                                e.printStackTrace();
                                            }
                                        });
                                    } else {
                                        synchronizer.update(member, player.username, true);
                                        player.newRank = entry.getValue();
                                    }
                                    //revalidated.add(player);
                                });
                        }
                    }
                });
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void saveRevalidated() {
        if (revalidated.isEmpty())
            return;
        for (UpdatingPlayer player = revalidated.poll(); player != null; player = revalidated.poll()) {
            // do nothing
        }
    }
    
    private class UpdatingPlayer {
        public Snowflake id;
        public String username;
        public String syncedRank;
        public String newRank;
        
        public UpdatingPlayer(Snowflake id, String username, String syncedRank) {
            this.id = id;
            this.username = username;
            this.syncedRank = syncedRank;
        }
    }
}
