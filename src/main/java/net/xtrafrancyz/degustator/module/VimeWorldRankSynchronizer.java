package net.xtrafrancyz.degustator.module;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import sx.blah.discord.Discord4J;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.ReadyEvent;
import sx.blah.discord.handle.impl.events.guild.member.UserJoinEvent;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IRole;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.util.DiscordException;
import sx.blah.discord.util.RateLimitException;

import net.xtrafrancyz.degustator.Degustator;
import net.xtrafrancyz.degustator.mysql.Row;
import net.xtrafrancyz.degustator.mysql.SelectResult;
import net.xtrafrancyz.degustator.util.HttpUtils;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * @author xtrafrancyz
 */
public class VimeWorldRankSynchronizer {
    private static final long VIMEWORLD_GUILD_ID = 105720432073666560L;
    private static final long VERIFIED_ROLE = 342269949852778497L;
    
    private final Map<String, Long> rankToRole = new HashMap<>();
    private final Set<Long> autoRoles = new HashSet<>();
    private final Degustator degustator;
    
    public VimeWorldRankSynchronizer(Degustator degustator) {
        this.degustator = degustator;
        rankToRole.put("WARDEN", 106123456122212352L);
        rankToRole.put("MODER", 106123456122212352L);
        rankToRole.put("BUILDER", 163050681266077696L);
        rankToRole.put("MAPLEAD", 163050681266077696L);
        rankToRole.put("YOUTUBE", 299093535007703050L);
        rankToRole.put("VIP", 342269106466324482L);
        rankToRole.put("PREMIUM", 342269249194033152L);
        rankToRole.put("HOLY", 342269451020009473L);
        rankToRole.put("IMMORTAL", 342269608541159435L);
        autoRoles.addAll(rankToRole.values());
        
        degustator.client.getDispatcher().registerListeners(this);
        
        try {
            degustator.mysql.query("CREATE TABLE IF NOT EXISTS `linked` (\n" +
                "  `id` bigint(20) NOT NULL,\n" +
                "  `username` varchar(20) NOT NULL,\n" +
                "  `updated` int(11) DEFAULT '0',\n" +
                "  PRIMARY KEY (`id`),\n" +
                "  KEY `iupdated` (`updated`)\n" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8;");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    @EventSubscriber
    public void onReady(ReadyEvent event) throws RateLimitException, DiscordException {
        Thread updater = new Thread(this::updateExpiredTask);
        updater.setDaemon(true);
        updater.start();
    }
    
    @EventSubscriber
    public void onUserJoin(UserJoinEvent event) {
        if (event.getGuild().getLongID() == VIMEWORLD_GUILD_ID)
            update(event.getUser());
    }
    
    private void updateExpiredTask() {
        Thread thread = Thread.currentThread();
        while (!thread.isInterrupted()) {
            try {
                IGuild guild = getVimeWorldGuild();
                if (guild == null)
                    continue;
                
                SelectResult select = degustator.mysql.select("SELECT id, username FROM linked WHERE updated < ?", ps ->
                    ps.setInt(1, (int) (System.currentTimeMillis() / 1000) - 12 * 60 * 60)
                );
                
                if (!select.isEmpty()) {
                    Map<Long, String> ranks = new HashMap<>();
                    Map<String, Long> usernamesAccumulator = new HashMap<>(100);
                    List<Long> updated = new ArrayList<>(select.getRowCount());
                    
                    Consumer<Map<String, Long>> rankLoader = usernames -> {
                        try {
                            JsonArray arr = new JsonParser().parse(HttpUtils.apiGet("/user/name/" + implode(",", usernamesAccumulator.keySet()))).getAsJsonArray();
                            for (JsonElement elem : arr) {
                                JsonObject object = elem.getAsJsonObject();
                                String username = object.get("username").getAsString();
                                ranks.put(usernames.get(username), object.get("rank").getAsString());
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    };
                    
                    for (Row row : select.getRows()) {
                        long id = row.getLong("id");
                        usernamesAccumulator.put(row.getString("username"), id);
                        updated.add(id);
                        if (usernamesAccumulator.size() == 50) {
                            rankLoader.accept(usernamesAccumulator);
                            usernamesAccumulator.clear();
                        }
                    }
                    
                    if (!usernamesAccumulator.isEmpty())
                        rankLoader.accept(usernamesAccumulator);
                    
                    for (Map.Entry<Long, String> entry : ranks.entrySet())
                        setRank(guild, guild.getUserByID(entry.getKey()), entry.getValue());
                    
                    degustator.mysql.query("UPDATE linked SET updated = ? WHERE id IN (" + implode(",", updated) + ")", ps -> {
                        ps.setInt(1, (int) (System.currentTimeMillis() / 1000));
                    });
                }
                
                Thread.sleep(60000L);
            } catch (InterruptedException ex) {
                return;
            } catch (RateLimitException ex) {
                try {
                    Thread.sleep(ex.getRetryDelay());
                } catch (InterruptedException ignored) {}
            } catch (Exception ex) {
                ex.printStackTrace();
                try {
                    Thread.sleep(5000L);
                } catch (InterruptedException ignored) {}
            }
        }
    }
    
    public void register(long userid, String username) throws SQLException {
        IGuild guild = getVimeWorldGuild();
        if (guild == null)
            return;
        SelectResult result = degustator.mysql.select("SELECT * FROM linked WHERE id = ?", ps -> ps.setLong(1, userid));
        if (result.isEmpty()) {
            degustator.mysql.query("INSERT INTO linked (id, username, updated) VALUES (?, ?, ?)", ps -> {
                ps.setLong(1, userid);
                ps.setString(2, username);
                ps.setInt(3, (int) (System.currentTimeMillis() / 1000));
            });
        } else {
            degustator.mysql.query("UPDATE linked SET updated = ?, username = ? WHERE id = ?", ps -> {
                ps.setInt(1, (int) (System.currentTimeMillis() / 1000));
                ps.setString(2, username);
                ps.setLong(3, userid);
            });
        }
        IUser user = guild.getUserByID(userid);
        if (user != null)
            Discord4J.LOGGER.info("[Synchronizer] Registered: " + user.getDisplayName(guild) + " to " + username);
        update(guild, user, username);
    }
    
    public void update(IUser user) {
        try {
            SelectResult result = degustator.mysql.select("SELECT * FROM linked WHERE id = ?", ps -> ps.setLong(1, user.getLongID()));
            if (!result.isEmpty())
                update(user, result.getFirst().getString("username"));
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    public void update(IUser user, String linked) {
        IGuild guild = getVimeWorldGuild();
        if (guild == null)
            return;
        update(guild, user, linked);
    }
    
    public void update(IGuild guild, IUser user, String linked) {
        if (user == null)
            return;
        if (linked == null) {
            List<IRole> roles = user.getRolesForGuild(guild);
            int oldLength = roles.size();
            Iterator<IRole> it = roles.iterator();
            while (it.hasNext()) {
                long id = it.next().getLongID();
                if (VERIFIED_ROLE == id || autoRoles.contains(id))
                    it.remove();
            }
            if (oldLength != roles.size())
                guild.editUserRoles(user, roles.toArray(new IRole[roles.size()]));
            return;
        }
        try {
            JsonArray arr = new JsonParser().parse(HttpUtils.apiGet("/user/name/" + linked)).getAsJsonArray();
            String rank = null;
            if (arr.size() == 1)
                rank = arr.get(0).getAsJsonObject().get("rank").getAsString();
            setRank(guild, user, rank);
        } catch (IOException ignored) {}
    }
    
    private void setRank(IGuild guild, IUser user, String rank) {
        if (user == null)
            return;
        addRole(guild, user, guild.getRoleByID(VERIFIED_ROLE));
        Long id = rankToRole.get(rank);
        if (id == null) {
            for (IRole role : user.getRolesForGuild(guild)) {
                long existed = role.getLongID();
                if (existed == VERIFIED_ROLE)
                    continue;
                if (autoRoles.contains(existed)) {
                    Discord4J.LOGGER.info("[Synchronizer] Remove role " + role.getName() + " from " + user.getDisplayName(guild));
                    user.removeRole(role);
                }
            }
            return;
        }
        for (IRole role : user.getRolesForGuild(guild)) {
            long existed = role.getLongID();
            if (existed == VERIFIED_ROLE)
                continue;
            if (existed == id)
                return;
            if (autoRoles.contains(existed)) {
                Discord4J.LOGGER.info("[Synchronizer] Remove role " + role.getName() + " from " + user.getDisplayName(guild));
                user.removeRole(role);
            }
        }
        addRole(guild, user, guild.getRoleByID(id));
    }
    
    private void addRole(IGuild guild, IUser user, IRole role) {
        for (IRole has : user.getRolesForGuild(guild))
            if (has.equals(role))
                return;
        Discord4J.LOGGER.info("[Synchronizer] Add role " + role.getName() + " to " + user.getDisplayName(guild));
        user.addRole(role);
    }
    
    public IGuild getVimeWorldGuild() {
        return degustator.client.getGuildByID(VIMEWORLD_GUILD_ID);
    }
    
    private static String implode(String glue, Collection collection) {
        StringBuilder joined = new StringBuilder();
        for (Object username : collection)
            joined.append(username).append(glue);
        return joined.delete(joined.length() - glue.length(), joined.length()).toString();
    }
}
