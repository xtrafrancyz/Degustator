package net.xtrafrancyz.degustator.command;

import discord4j.core.object.entity.Message;

import net.xtrafrancyz.degustator.Degustator;

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
    
    public void process(Message message) {
        String text = message.getContent().orElse("!").substring(1);
        String[] args = text.split(" ");
        Command command = getCommand(args[0].toLowerCase());
        if (command == null)
            return;
        command.canUse(message).subscribe(can -> {
            if (can) {
                try {
                    command.onCommand(message, Arrays.copyOfRange(args, 1, args.length));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
