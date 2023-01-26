package net.xtrafrancyz.degustator.module.synchronizer;

import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.UserUpdateEvent;
import discord4j.core.event.domain.guild.GuildCreateEvent;
import discord4j.core.event.domain.guild.GuildDeleteEvent;
import discord4j.core.event.domain.guild.MemberJoinEvent;
import discord4j.core.event.domain.guild.MemberUpdateEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.User;
import discord4j.core.spec.GuildMemberEditSpec;
import reactor.core.publisher.Mono;

import net.xtrafrancyz.degustator.Degustator;
import net.xtrafrancyz.degustator.Scheduler;
import net.xtrafrancyz.degustator.mysql.Row;
import net.xtrafrancyz.degustator.mysql.SelectResult;

import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * @author xtrafrancyz
 */
public class Synchronizer {
    public static final Snowflake VIMEWORLD_GUILD_ID = Snowflake.of(105720432073666560L);
    private static final Snowflake VERIFIED_ROLE = Snowflake.of(342269949852778497L);
    private static final Snowflake NITRO_BOOSTER_ROLE = Snowflake.of(585532209121853455L);
    private static final Snowflake DISCORD_MODER_ROLE = Snowflake.of(344881335401316356L);
    private static final Map<String, Snowflake> RANK_TO_ROLE = new HashMap<>();
    private static final Set<Snowflake> AUTOROLES = new HashSet<>();
    
    static {
        RANK_TO_ROLE.put("WARDEN", Snowflake.of(106123456122212352L));
        RANK_TO_ROLE.put("MODER", Snowflake.of(106123456122212352L));
        RANK_TO_ROLE.put("DEV", Snowflake.of(291284899263152128L));
        RANK_TO_ROLE.put("BUILDER", Snowflake.of(163050681266077696L));
        RANK_TO_ROLE.put("SRBUILDER", Snowflake.of(163050681266077696L));
        RANK_TO_ROLE.put("MAPLEAD", Snowflake.of(163050681266077696L));
        RANK_TO_ROLE.put("YOUTUBE", Snowflake.of(299093535007703050L));
        RANK_TO_ROLE.put("VIP", Snowflake.of(342269106466324482L));
        RANK_TO_ROLE.put("PREMIUM", Snowflake.of(342269249194033152L));
        RANK_TO_ROLE.put("HOLY", Snowflake.of(342269451020009473L));
        RANK_TO_ROLE.put("IMMORTAL", Snowflake.of(342269608541159435L));
        AUTOROLES.addAll(RANK_TO_ROLE.values());
    }
    
    private final Degustator degustator;
    private boolean inVimeWorldGuild;
    private Revalidator revalidator;
    
    AsyncLoadingCache<Snowflake, String> usernames;
    AsyncLoadingCache<String, VimeApiPlayer> vimeApi;
    
    public Synchronizer(Degustator degustator) {
        this.degustator = degustator;
        usernames = Caffeine.newBuilder()
            .expireAfterWrite(30, TimeUnit.MINUTES)
            .buildAsync(new UsernameLoader(degustator));
        vimeApi = Caffeine.newBuilder()
            .expireAfterWrite(30, TimeUnit.MINUTES)
            .buildAsync(new RankLoader());
        revalidator = new Revalidator(degustator, this);
        
        degustator.gateway.on(GuildCreateEvent.class).subscribe(event -> {
            if (event.getGuild().getId().equals(VIMEWORLD_GUILD_ID)) {
                if (!revalidator.isRunning()) {
                    try {
                        degustator.mysql.query("CREATE TABLE IF NOT EXISTS `linked` (\n" +
                            "  `id` bigint(20) NOT NULL,\n" +
                            "  `username` varchar(20) DEFAULT NULL,\n" +
                            "  `rank` varchar(20) DEFAULT NULL,\n" +
                            "  `updated` int(11) DEFAULT 0,\n" +
                            "  PRIMARY KEY (`id`),\n" +
                            "  KEY `iupdated` (`updated`),\n" +
                            "  KEY `username` (`username`)\n" +
                            ") ENGINE=InnoDB DEFAULT CHARSET=utf8;");
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                    revalidator.start();
                }
                
                inVimeWorldGuild = true;
            }
        });
        
        degustator.gateway.on(GuildDeleteEvent.class).subscribe(event -> {
            if (event.getGuildId().equals(VIMEWORLD_GUILD_ID)) {
                revalidator.stop();
                inVimeWorldGuild = false;
            }
        });
        
        degustator.gateway.on(MemberJoinEvent.class).subscribe(event -> {
            if (event.getGuildId().equals(VIMEWORLD_GUILD_ID)) {
                try {
                    SelectResult result = degustator.mysql.select("SELECT * FROM linked WHERE id = ?", ps ->
                        ps.setLong(1, event.getMember().getId().asLong())
                    );
                    if (!result.isEmpty())
                        update("MemberJoinEvent", event.getMember(), result.getFirst().getString("username"), true);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        });
        
        degustator.gateway.on(UserUpdateEvent.class).subscribe(event -> {
            if (event.getCurrent().isBot() || !inVimeWorldGuild)
                return;
            User old = event.getOld().orElse(null);
            if (old != null && !old.getUsername().equals(event.getCurrent().getUsername())) {
                getVimeWorldGuild(guild -> {
                    guild.getMemberById(event.getCurrent().getId())
                        .subscribe(member -> {
                            usernames.get(member.getId()).thenAccept(username -> {
                                update("UserUpdateEvent", member, username, true);
                            });
                        }, error -> {
                        });
                });
            }
        });
        
        // Меняются ник в гильдии или роли
        degustator.gateway.on(MemberUpdateEvent.class).subscribe(event -> {
            if (!event.getGuildId().equals(VIMEWORLD_GUILD_ID))
                return;
            event.getMember().subscribe(member -> {
                if (member.isBot())
                    return;
                usernames.get(member.getId()).thenAccept(username -> {
                    if (username.equals("") || member.getDisplayName().equals(username))
                        return;
                    update("MemberUpdateEvent", member, username, true);
                });
            }, error -> {
            });
        });
    }
    
    public void update(String caller, Member member, String username, boolean writeToDb) {
        Degustator.log.info("Check (" + caller + "): " + username + " => " + member.getDisplayName() + "#" + member.getDiscriminator());
        vimeApi.get(username.toLowerCase()).whenComplete((vimeApiPlayer, ex) -> {
            if (ex != null) {
                Degustator.log.warn("Load vime api user error", ex);
                return;
            }
            Set<Snowflake> roles = member.getRoleIds();
            Set<Snowflake> originalRoles = new HashSet<>(roles);
            List<Consumer<GuildMemberEditSpec.Builder>> modifiers = new ArrayList<>();
            
            roles.removeIf(AUTOROLES::contains);
            roles.add(VERIFIED_ROLE);
            
            if (!roles.contains(NITRO_BOOSTER_ROLE) &&
                !roles.contains(DISCORD_MODER_ROLE) &&
                !vimeApiPlayer.username.equals(member.getDisplayName())) {
                modifiers.add(spec ->
                    spec.nicknameOrNull(vimeApiPlayer.username)
                );
            }
            
            Snowflake role = RANK_TO_ROLE.get(vimeApiPlayer.rank);
            if (role != null)
                roles.add(role);
            if (!originalRoles.equals(roles))
                modifiers.add(spec ->
                    spec.roles(roles)
                );
            
            Runnable dbWrite = !writeToDb ? null : () -> {
                usernames.put(member.getId(), CompletableFuture.completedFuture(vimeApiPlayer.username));
                Scheduler.execute(() -> {
                    try {
                        degustator.mysql.query("UPDATE linked SET updated = ?, username = ?, rank = ? WHERE id = ?", ps -> {
                            ps.setInt(1, (int) (System.currentTimeMillis() / 1000));
                            ps.setString(2, vimeApiPlayer.username);
                            ps.setString(3, vimeApiPlayer.rank);
                            ps.setLong(4, member.getId().asLong());
                        });
                    } catch (SQLException e) {
                        Degustator.log.warn("Could not update user", e);
                    }
                });
            };
            
            if (!modifiers.isEmpty()) {
                GuildMemberEditSpec.Builder builder = GuildMemberEditSpec.builder();
                for (Consumer<GuildMemberEditSpec.Builder> modifier : modifiers)
                    modifier.accept(builder);
                Degustator.log.info("User " + member.getUsername() + "#" + member.getDiscriminator() + " updated (" + vimeApiPlayer.username + ")");
                member.edit(builder.build()).subscribe();
            }
            if (dbWrite != null)
                dbWrite.run();
        });
    }
    
    public void link(Snowflake id, String username) {
        try {
            // линк нового
            SelectResult result = degustator.mysql.select("SELECT id FROM linked WHERE id = ?", ps -> ps.setLong(1, id.asLong()));
            if (result.isEmpty()) {
                degustator.mysql.query("INSERT INTO linked (id, username, updated) VALUES (?, ?, ?)", ps -> {
                    ps.setLong(1, id.asLong());
                    ps.setString(2, username);
                    ps.setInt(3, (int) (System.currentTimeMillis() / 1000));
                });
            }
            
            // анлинк старых
            result = degustator.mysql.select("SELECT id FROM linked WHERE username = ?", ps -> ps.setString(1, username));
            for (Row row : result.getRows()) {
                Snowflake old = Snowflake.of(row.getLong("id"));
                if (id.equals(old))
                    continue;
                try {
                    unlink(old);
                    degustator.mysql.query("DELETE FROM linked WHERE id = ?", ps -> ps.setLong(1, old.asLong()));
                } catch (Exception ex) {
                    degustator.mysql.query("UPDATE linked SET username = NULL, rank = NULL WHERE id = ?", ps -> ps.setLong(1, old.asLong()));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return;
        }
        
        getVimeWorldGuild(guild -> {
            guild.getMemberById(id)
                .onErrorResume(error -> Mono.empty())
                .subscribe(member -> {
                    update("Link", member, username, true);
                });
        });
    }
    
    public void unlink(Snowflake id) {
        usernames.synchronous().invalidate(id);
        getVimeWorldGuild(guild -> {
            guild.getMemberById(id)
                .onErrorResume(error -> Mono.empty())
                .subscribe(member -> {
                    Set<Snowflake> roles = member.getRoleIds();
                    int size = roles.size();
                    roles.remove(VERIFIED_ROLE);
                    roles.removeAll(AUTOROLES);
                    if (size != roles.size() || member.getNickname().isPresent()) {
                        member.edit(spec -> {
                            spec.setNickname(null);
                            spec.setRoles(roles);
                        }).subscribe();
                    }
                });
        });
    }
    
    public void getVimeWorldGuild(Consumer<Guild> consumer) {
        degustator.gateway.getGuildById(VIMEWORLD_GUILD_ID)
            .timeout(Duration.ofSeconds(10))
            .doOnSuccess(consumer)
            .doOnCancel(() -> Degustator.log.warn("Get VimeWorld guild timeout"))
            .doOnError(ex -> Degustator.log.warn("Get VimeWorld guild", ex))
            .subscribe();
    }
    
    public CompletableFuture<String> getVimeNick(Snowflake id) {
        return usernames.get(id);
    }
    
    public CompletableFuture<GetDiscordIdResponse> getDiscordId(String username) {
        CompletableFuture<GetDiscordIdResponse> future = new CompletableFuture<>();
        Scheduler.execute(() -> {
            try {
                SelectResult result = degustator.mysql.select("SELECT id, username FROM linked WHERE username = ?", ps -> ps.setString(1, username));
                if (result.isEmpty())
                    future.complete(null);
                else
                    future.complete(new GetDiscordIdResponse(
                        Snowflake.of(result.getFirst().getLong("id")),
                        result.getFirst().getString("username")
                    ));
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
        return future;
    }
}
