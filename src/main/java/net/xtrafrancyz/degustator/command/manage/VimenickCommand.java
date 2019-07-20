package net.xtrafrancyz.degustator.command.manage;

import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.object.util.Snowflake;
import reactor.core.publisher.Mono;

import net.xtrafrancyz.degustator.Degustator;
import net.xtrafrancyz.degustator.command.Command;
import net.xtrafrancyz.degustator.module.synchronizer.Synchronizer2;
import net.xtrafrancyz.degustator.util.DiscordUtils;

import java.util.Set;

/**
 * @author xtrafrancyz
 */
public class VimenickCommand extends Command {
    private static final Snowflake DISCORD_MODER_ROLE = Snowflake.of(344881335401316356L);
    private static final Snowflake ADMIN_ROLE = Snowflake.of(106117738677665792L);
    
    private Synchronizer2 synchronizer = Degustator.instance().synchronizer;
    
    public VimenickCommand() {
        super("vimenick",
            "`!vimenick @юзер` или `!vimenick 123456789` - по акку в дискорде ищет ник на вайме\n" +
                "`!vimenick $nickNaVime` - по нику вайма ищет акк в дискорде");
    }
    
    @Override
    public void onCommand(Message message, String[] args) throws Exception {
        if (args.length == 0) {
            usage(message);
            return;
        }
        Set<Snowflake> mentions = message.getUserMentionIds();
        if (mentions.isEmpty()) {
            if (args[0].startsWith("$")) {
                vimeToDs(message, args[0].substring(1));
                return;
            }
            long userid;
            try {
                userid = Long.parseLong(args[0]);
            } catch (Exception ignored) {
                usage(message);
                return;
            }
            message.getGuild().subscribe(guild -> {
                guild.getMemberById(Snowflake.of(userid))
                    .subscribe(u -> {
                        dsToVime(message, u);
                    }, error -> {
                        dsToVime(message, null);
                    });
            });
        } else {
            message.getUserMentions().subscribe(u -> {
                dsToVime(message, u);
            });
        }
    }
    
    private void usage(Message message) {
        DiscordUtils.sendMessage(message, "Использование: `!vimenick @юзер` или `!vimenick 123456789` или `!vimenick $nickNaVime`");
    }
    
    private void dsToVime(Message message, User user) {
        if (user == null) {
            DiscordUtils.sendMessage(message, "Такой юзер не найден");
            return;
        }
        synchronizer.getVimeNick(user.getId()).thenAccept(username -> {
            if (username != null && !username.isEmpty()) {
                synchronizer.getVimeWorldGuild(guild -> {
                    guild.getMemberById(user.getId())
                        .subscribe(member -> {
                            DiscordUtils.sendMessage(message, "Ник юзера `" + getDsName(member) + "` на вайме - `" + username + "`");
                            synchronizer.update(member, username, true);
                        }, error -> {
                            DiscordUtils.sendMessage(message, "Какая-то ошибка, я сломался");
                        });
                });
            } else {
                synchronizer.getVimeWorldGuild(guild -> {
                    guild.getMemberById(user.getId())
                        .subscribe(member -> {
                            DiscordUtils.sendMessage(message, "Юзер `" + getDsName(member) + "` не привязан к аккаунту вайма");
                        }, error -> {
                            DiscordUtils.sendMessage(message, "Какая-то ошибка, я сломался");
                        });
                });
            }
        });
    }
    
    private void vimeToDs(Message message, String nick) throws Exception {
        synchronizer.getDiscordId(nick).thenAccept(id -> {
            if (id != null) {
                synchronizer.getVimeWorldGuild(guild -> {
                    guild.getMemberById(id)
                        .subscribe(member -> {
                            DiscordUtils.sendMessage(message, "Ник юзера `" + getDsName(member) + "` на вайме - `" + nick + "`");
                            synchronizer.update(member, nick, true);
                        }, error -> {
                            DiscordUtils.sendMessage(message, "Ник юзера `" + id.asString() + "` (ливнул с серва) на вайме - `" + nick + "`");
                        });
                });
            } else {
                DiscordUtils.sendMessage(message, "Ник `" + nick + "` не привязан к дискорду");
            }
        });
    }
    
    private String getDsName(Member member) {
        return member.getDisplayName() + "#" + member.getDiscriminator() + " (" + member.getId().asString() + ")";
    }
    
    @Override
    public Mono<Boolean> canUse(Message message) {
        if (!message.getAuthor().isPresent())
            return Mono.just(false);
        return Mono.create(sink -> {
            message.getAuthorAsMember().subscribe(member -> {
                if (!member.getGuildId().equals(Synchronizer2.VIMEWORLD_GUILD_ID)) {
                    sink.success(false);
                    return;
                }
                DiscordUtils.getMemberRoles(member, roles -> {
                    sink.success(roles.contains(DISCORD_MODER_ROLE) || roles.contains(ADMIN_ROLE));
                });
            });
        });
    }
}
