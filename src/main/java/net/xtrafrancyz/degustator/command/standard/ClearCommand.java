package net.xtrafrancyz.degustator.command.standard;

import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.util.MessageHistory;

import net.xtrafrancyz.degustator.command.Command;
import net.xtrafrancyz.degustator.user.Permission;

import java.util.ArrayList;
import java.util.List;

/**
 * @author xtrafrancyz
 */
public class ClearCommand extends Command {
    public ClearCommand() {
        super("clear",
            "`!clear <@пользователь>` - удаляет сообщения `@пользователя` в последних 100 сообщениях\n"
                + "`!clear <количество>` - удаляет последние `количество` сообщений"
            , Permission.CLEAR);
    }
    
    @Override
    public void onCommand(IMessage message, String[] args) throws Exception {
        if (args.length == 0) {
            message.reply("\n" + help);
            return;
        }
        
        try {
            clearAmount(message, Integer.parseInt(args[0]));
        } catch (NumberFormatException ex) {
            List<IUser> mentions = message.getMentions();
            if (!mentions.isEmpty()) {
                clearUser(message, mentions.get(0));
            }
        }
    }
    
    private void clearAmount(IMessage message, int amount) throws Exception {
        if (amount > 99)
            amount = 99;
        message.getChannel().getMessageHistory(amount).bulkDelete();
        message.getChannel().sendMessage("Удалено сообщений - `" + amount + "`");
    }
    
    private void clearUser(IMessage message, IUser user) throws Exception {
        MessageHistory history = message.getChannel().getMessageHistory();
        ArrayList<IMessage> toRemove = new ArrayList<>(10);
        int max = 99;
        for (IMessage msg : history) {
            if (msg.getAuthor() == user || msg.getAuthor().getLongID() == user.getLongID())
                toRemove.add(msg);
            if (max-- == 0)
                break;
        }
        if (!toRemove.isEmpty())
            message.getChannel().bulkDelete(toRemove);
        message.getChannel().sendMessage("Удалено сообщений от " + user.mention() + " - `" + toRemove.size() + "`");
    }
}
