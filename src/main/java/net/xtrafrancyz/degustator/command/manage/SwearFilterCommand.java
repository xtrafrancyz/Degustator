package net.xtrafrancyz.degustator.command.manage;

import sx.blah.discord.handle.obj.IMessage;

import net.xtrafrancyz.degustator.Degustator;
import net.xtrafrancyz.degustator.command.Command;
import net.xtrafrancyz.degustator.module.SwearFilter;

/**
 * @author xtrafrancyz
 */
public class SwearFilterCommand extends Command {
    public SwearFilterCommand() {
        super("swearfilter",
            "`!swearfilter` - включает/выключает фильтр мата в канале");
    }
    
    @Override
    public void onCommand(IMessage message, String[] args) throws Exception {
        SwearFilter swearFilter = Degustator.instance().swearFilter;
        if (!swearFilter.isEnabled()) {
            message.getChannel().sendMessage("Фильтр мата не настроен.");
            message.delete();
            return;
        }
        if (swearFilter.isActive(message.getChannel())) {
            swearFilter.disableFor(message.getChannel());
            message.getChannel().sendMessage("Фильтр мата для этого канала теперь **отключен**");
            message.delete();
        } else {
            swearFilter.enableFor(message.getChannel());
            message.getChannel().sendMessage("Фильтр мата для этого канала теперь **включен**");
            message.delete();
        }
    }
    
    @Override
    public boolean canUse(IMessage message) {
        return message.getGuild().getOwnerLongID() == message.getAuthor().getLongID();
    }
}
