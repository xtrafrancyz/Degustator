package net.xtrafrancyz.degustator.module.synchronizer;

import discord4j.common.util.Snowflake;

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
    private final Synchronizer synchronizer;
    
    private int revalidatorTask = -1;
    private int revalidatorSaverTask = -1;
    private Queue<UpdatingPlayer> revalidated = new ConcurrentLinkedQueue<>();
    
    public Revalidator(Degustator degustator, Synchronizer synchronizer) {
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
                String lower = username.toLowerCase();
                players.put(lower, new UpdatingPlayer(id, username, rank));
                nicknames.add(lower);
            }
            synchronizer.vimeApi.getAll(nicknames).thenAccept(vimeApiPlayerMap -> {
                synchronizer.getVimeWorldGuild(guild -> {
                    for (Map.Entry<String, VimeApiPlayer> entry : vimeApiPlayerMap.entrySet()) {
                        UpdatingPlayer player = players.get(entry.getKey());
                        if (player != null &&
                            (!Objects.equals(player.syncedRank, entry.getValue().rank) ||
                                !Objects.equals(player.username, entry.getValue().username))) {
                            guild.getMemberById(player.id)
                                .subscribe(member -> {
                                    synchronizer.update("Revalidator", member, player.username, true);
                                }, error -> {
                                    // Юзер не найден
                                    Scheduler.execute(() -> {
                                        try {
                                            degustator.mysql.query("UPDATE linked SET updated = ?, rank = ? WHERE id = ?", ps -> {
                                                ps.setInt(1, (int) (System.currentTimeMillis() / 1000) + 60 * 60 * 24 * 31);
                                                ps.setString(2, entry.getValue().rank);
                                                ps.setLong(3, player.id.asLong());
                                            });
                                        } catch (SQLException e) {
                                            e.printStackTrace();
                                        }
                                    });
                                });
                        } else {
                            revalidated.add(player);
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
        StringBuilder ids = new StringBuilder(256);
        for (UpdatingPlayer player = revalidated.poll(); player != null; player = revalidated.poll()) {
            // do nothing
            if (ids.length() > 0)
                ids.append(',');
            ids.append(player.id.asString());
        }
        Scheduler.execute(() -> {
            try {
                long time = (int) (System.currentTimeMillis() / 1000) + 60 * 60 * 24;
                degustator.mysql.query("UPDATE linked SET updated = " + time + " WHERE id IN (" + ids + ")");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }
    
    private static class UpdatingPlayer {
        public Snowflake id;
        public String username;
        public String syncedRank;
        
        public UpdatingPlayer(Snowflake id, String username, String syncedRank) {
            this.id = id;
            this.username = username;
            this.syncedRank = syncedRank;
        }
    }
}
