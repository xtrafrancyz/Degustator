package net.xtrafrancyz.degustator.command.manage;

import discord4j.core.object.entity.Message;
import reactor.core.publisher.Mono;

import net.xtrafrancyz.degustator.command.Command;
import net.xtrafrancyz.degustator.util.DiscordUtils;

/**
 * @author xtrafrancyz
 */
public class MassBanCommand extends Command {
    public MassBanCommand() {
        super("massban",
            "`!massban` - массовый бан людей на сервере");
    }
    
    @Override
    public void onCommand(Message message, String[] args) throws Exception {
        if (args.length == 0) {
            DiscordUtils.sendMessage(message, "Укажите начало ника");
            return;
        }
        String start = String.join(" ", args);
        if (start.length() < 4) {
            DiscordUtils.sendMessage(message, "Длина начала ника должна быть хотя бы 4 символа, во избежание ошибочных банов");
            return;
        }
        message.getGuild().subscribe(guild -> {
            guild.getMembers()
                .filter(m -> m.getUsername().startsWith(start))
                .all(m -> {
                    m.ban(spec -> {
                        spec.setDeleteMessageDays(1);
                        spec.setReason("Ручной массовый бан");
                    }).subscribe();
                    return true;
                })
                .subscribe(ignored -> {
                    DiscordUtils.sendMessage(message, "Юзеры забанены (хз сколько, апи говно)");
                });
        });
    }
    
    @Override
    public Mono<Boolean> canUse(Message message) {
        return checkIsAuthorIsGuildOwner(message);
    }
}
