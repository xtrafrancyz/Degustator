package net.xtrafrancyz.degustator.command.manage;

import discord4j.core.object.entity.Message;
import reactor.core.publisher.Mono;

import net.xtrafrancyz.degustator.Degustator;
import net.xtrafrancyz.degustator.command.Command;
import net.xtrafrancyz.degustator.module.SwearFilter;
import net.xtrafrancyz.degustator.util.DiscordUtils;

/**
 * @author xtrafrancyz
 */
public class SwearFilterCommand extends Command {
    public SwearFilterCommand() {
        super("swearfilter",
            "`!swearfilter` - включает/выключает фильтр мата в канале");
    }
    
    @Override
    public void onCommand(Message message, String[] args) throws Exception {
        SwearFilter swearFilter = Degustator.instance().swearFilter;
        if (!swearFilter.isEnabled()) {
            DiscordUtils.sendMessage(message, "Фильтр мата не настроен.");
            message.delete();
            return;
        }
        if (swearFilter.isActive(message.getChannelId())) {
            swearFilter.disableFor(message.getChannelId());
            DiscordUtils.sendMessage(message, "Фильтр мата для этого канала теперь **отключен**");
            message.delete();
        } else {
            swearFilter.enableFor(message.getChannelId());
            DiscordUtils.sendMessage(message, "Фильтр мата для этого канала теперь **включен**");
            message.delete();
        }
    }
    
    @Override
    public Mono<Boolean> canUse(Message message) {
        return checkIsAuthorIsGuildOwner(message);
    }
}
