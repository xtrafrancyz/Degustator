package net.xtrafrancyz.degustator.module;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import sx.blah.discord.Discord4J;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.ReadyEvent;
import sx.blah.discord.handle.impl.events.guild.member.UserJoinEvent;
import sx.blah.discord.handle.impl.events.user.UserUpdateEvent;
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
    public static final long VIMEWORLD_GUILD_ID = 105720432073666560L;
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
                "  `username` varchar(20) DEFAULT NULL,\n" +
                "  `updated` int(11) DEFAULT 0,\n" +
                "  PRIMARY KEY (`id`),\n" +
                "  KEY `iupdated` (`updated`),\n" +
                "  KEY `username` (`username`)\n" +
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
        if (event.getGuild().getLongID() == VIMEWORLD_GUILD_ID) {
            try {
                SelectResult result = degustator.mysql.select("SELECT * FROM linked WHERE id = ?", ps -> ps.setLong(1, event.getUser().getLongID()));
                if (!result.isEmpty()) {
                    IGuild guild = getVimeWorldGuild();
                    if (guild == null)
                        return;
                    link(guild, event.getUser(), result.getFirst().getString("username"));
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
    
    @EventSubscriber
    public void onUserUpdate(UserUpdateEvent event) {
        IGuild guild = getVimeWorldGuild();
        if (guild != null && event.getNewUser().hasRole(guild.getRoleByID(VERIFIED_ROLE))) {
            if (!event.getNewUser().getDisplayName(guild).equals(event.getOldUser().getDisplayName(guild))) {
                try {
                    String vimenick = getVimeNick(event.getUser().getLongID());
                    if (vimenick != null)
                        link(guild, event.getUser(), vimenick);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    
    private void updateExpiredTask() {
        Thread thread = Thread.currentThread();
        while (!thread.isInterrupted()) {
            try {
                IGuild guild = getVimeWorldGuild();
                if (guild == null) {
                    try {
                        Thread.sleep(50L);
                    } catch (InterruptedException ignored) {}
                    continue;
                }
                
                SelectResult select = degustator.mysql.select("SELECT id, username FROM linked WHERE updated < ?", ps ->
                    ps.setInt(1, (int) (System.currentTimeMillis() / 1000) - 12 * 60 * 60)
                );
                
                if (!select.isEmpty()) {
                    Map<Long, VimeWorldUserData> ranks = new HashMap<>();
                    Map<String, Long> usernamesAccumulator = new HashMap<>(100);
                    List<Long> updated = new ArrayList<>(select.getRowCount());
                    
                    Consumer<Map<String, Long>> rankLoader = usernames -> {
                        try {
                            JsonArray arr = new JsonParser().parse(HttpUtils.apiGet("/user/name/" + String.join(",", usernamesAccumulator.keySet()))).getAsJsonArray();
                            for (JsonElement elem : arr) {
                                JsonObject object = elem.getAsJsonObject();
                                String username = object.get("username").getAsString();
                                ranks.put(usernames.get(username), new VimeWorldUserData(username, object.get("rank").getAsString()));
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    };
                    
                    for (Row row : select.getRows()) {
                        long id = row.getLong("id");
                        String username = row.getString("username");
                        if (username == null) {
                            unlink(guild, guild.getUserByID(id));
                            degustator.mysql.query("DELETE FROM linked WHERE id = ?", ps -> ps.setLong(1, id));
                            continue;
                        }
                        usernamesAccumulator.put(username, id);
                        updated.add(id);
                        if (usernamesAccumulator.size() == 50) {
                            rankLoader.accept(usernamesAccumulator);
                            usernamesAccumulator.clear();
                        }
                    }
                    
                    if (!usernamesAccumulator.isEmpty())
                        rankLoader.accept(usernamesAccumulator);
                    
                    for (Map.Entry<Long, VimeWorldUserData> entry : ranks.entrySet())
                        updateUser(guild, guild.getUserByID(entry.getKey()), entry.getValue());
                    
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
    
    public String getVimeNick(long userid) throws SQLException {
        SelectResult result = degustator.mysql.select("SELECT username FROM linked WHERE id = ?", ps -> ps.setLong(1, userid));
        if (result.isEmpty())
            return null;
        else
            return result.getFirst().getString("username");
    }
    
    public long getDsId(String nick) throws SQLException {
        SelectResult result = degustator.mysql.select("SELECT id FROM linked WHERE username = ?", ps -> ps.setString(1, nick));
        if (result.isEmpty())
            return -1;
        else
            return result.getFirst().getLong("id");
    }
    
    public void register(long userid, String username) throws SQLException {
        IGuild guild = getVimeWorldGuild();
        if (guild == null)
            return;
        
        SelectResult result = degustator.mysql.select("SELECT id FROM linked WHERE id = ?", ps -> ps.setLong(1, userid));
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
        
        result = degustator.mysql.select("SELECT id FROM linked WHERE username = ?", ps -> ps.setString(1, username));
        for (Row row : result.getRows()) {
            long id = row.getLong("id");
            if (id == userid)
                continue;
            try {
                unlink(guild, guild.getUserByID(id));
                degustator.mysql.query("DELETE FROM linked WHERE id = ?", ps -> ps.setLong(1, id));
            } catch (Exception ex) {
                degustator.mysql.query("UPDATE linked SET username = NULL WHERE id = ?", ps -> ps.setLong(1, id));
            }
        }
        
        IUser user = guild.getUserByID(userid);
        if (user != null)
            Discord4J.LOGGER.info("[Synchronizer] Registered: " + user.getDisplayName(guild) + " to " + username);
        link(guild, user, username);
    }
    
    public void link(IGuild guild, IUser user, String linked) {
        if (user == null)
            return;
        try {
            JsonArray arr = new JsonParser().parse(HttpUtils.apiGet("/user/name/" + linked)).getAsJsonArray();
            String rank = null;
            if (arr.size() == 1) {
                rank = arr.get(0).getAsJsonObject().get("rank").getAsString();
                linked = arr.get(0).getAsJsonObject().get("username").getAsString();
            }
            updateUser(guild, user, new VimeWorldUserData(linked, rank));
        } catch (IOException ignored) {}
    }
    
    private void unlink(IGuild guild, IUser user) {
        if (user == null)
            return;
        List<IRole> roles = user.getRolesForGuild(guild);
        int oldLength = roles.size();
        Iterator<IRole> it = roles.iterator();
        while (it.hasNext()) {
            long id = it.next().getLongID();
            if (VERIFIED_ROLE == id || autoRoles.contains(id))
                it.remove();
        }
        if (oldLength != roles.size())
            guild.editUserRoles(user, roles.toArray(new IRole[0]));
        if (!user.getDisplayName(guild).equals(user.getName()))
            guild.setUserNickname(user, null);
    }
    
    private void updateUser(IGuild guild, IUser user, VimeWorldUserData data) {
        if (user == null)
            return;
        
        if (!data.username.equals(user.getDisplayName(guild))) {
            Discord4J.LOGGER.info("[Synchronizer] Set username to " + data.username + " instead of " + user.getDisplayName(guild));
            guild.setUserNickname(user, data.username);
        }
        
        addRole(guild, user, guild.getRoleByID(VERIFIED_ROLE));
        Long id = rankToRole.get(data.rank);
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
                continue;
            if (autoRoles.contains(existed)) {
                Discord4J.LOGGER.info("[Synchronizer] Remove role " + role.getName() + " from " + user.getDisplayName(guild));
                user.removeRole(role);
            }
        }
        addRole(guild, user, guild.getRoleByID(id));
    }
    
    private void addRole(IGuild guild, IUser user, IRole role) {
        if (user.hasRole(role))
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
    
    private static class VimeWorldUserData {
        String username;
        String rank;
        
        public VimeWorldUserData(String username, String rank) {
            this.username = username;
            this.rank = rank;
        }
    }
}
