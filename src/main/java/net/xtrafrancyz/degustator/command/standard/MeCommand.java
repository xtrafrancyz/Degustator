package net.xtrafrancyz.degustator.command.standard;

import sx.blah.discord.handle.obj.IMessage;

import net.xtrafrancyz.degustator.command.Command;
import net.xtrafrancyz.degustator.user.Permission;
import net.xtrafrancyz.degustator.user.User;

import java.util.stream.Collectors;

/**
 * @author xtrafrancyz
 */
public class MeCommand extends Command {
    public MeCommand() {
        super("me", "`!me` - показывает информацию о вас");
    }
    
    @Override
    public void onCommand(IMessage message, String[] args) throws Exception {
        User user = User.get(message.getAuthor());
        String perms;
        if (user.permissions.isEmpty())
            perms = "дополнительных прав нет";
        else
            perms = "дополнительные права: " + user.permissions.stream().map(Permission::name).collect(Collectors.joining(", "));
        message.reply("ваш ранг `" + user.rank + "`, " + perms);
    }
}
