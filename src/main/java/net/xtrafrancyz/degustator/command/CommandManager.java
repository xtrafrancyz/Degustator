package net.xtrafrancyz.degustator.command;

import sx.blah.discord.handle.obj.IMessage;

import net.xtrafrancyz.degustator.Degustator;
import net.xtrafrancyz.degustator.user.User;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * @author xtrafrancyz
 */
public class CommandManager {
    public final Degustator app;
    public Map<String, Command> registered;
    public Map<String, Command> alises;
    
    public CommandManager(Degustator app) {
        this.app = app;
        registered = new HashMap<>();
        alises = new HashMap<>();
    }
    
    public void registerCommand(Command command) {
        this.registered.put(command.command, command);
        for (String cmd : command.aliases)
            alises.put(cmd, command);
    }
    
    public Command getCommand(String cmd) {
        Command command = registered.get(cmd);
        if (command != null)
            return command;
        return alises.get(cmd);
    }
    
    public void process(IMessage message) {
        try {
            String text = message.getContent().substring(1);
            String[] args = text.split(" ");
            Command command = getCommand(args[0].toLowerCase());
            if (command != null) {
                if (!User.get(message.getAuthor()).hasPerm(command.perm)) {
                    message.reply("у вас недостаточно прав для выполнения этой команды");
                    return;
                }
                command.onCommand(message, Arrays.copyOfRange(args, 1, args.length));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
