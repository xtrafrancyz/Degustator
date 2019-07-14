package net.xtrafrancyz.degustator.command.standard;

import discord4j.core.object.entity.Message;
import reactor.core.publisher.Flux;

import net.xtrafrancyz.degustator.command.Command;
import net.xtrafrancyz.degustator.command.CommandManager;
import net.xtrafrancyz.degustator.util.DiscordUtils;

import java.util.stream.Collectors;

/**
 * @author xtrafrancyz
 */
public class HelpCommand extends Command {
    private final CommandManager commands;
    
    public HelpCommand(CommandManager commands) {
        super("help",
            "`!help` - список команд\n"
                + "`!help <команда>` - информация о команде"
        );
        this.commands = commands;
    }
    
    @Override
    public void onCommand(Message message, String[] args) throws Exception {
        if (args.length != 0) {
            Command command = commands.getCommand(args[0]);
            if (command == null) {
                DiscordUtils.reply(message, "нет такой команды");
                return;
            }
            String msg;
            if (command.help != null)
                msg = command.help;
            else
                msg = "У команды !" + command.command + " нет описания :frowning2:";
            DiscordUtils.sendMessage(message, msg);
            return;
        }
        
        Flux.mergeSequential(
            commands.registered.values()
                .stream()
                .map(cmd -> cmd.canUse(message))
                .collect(Collectors.toList()))
            .zipWithIterable(commands.registered.values())
            .reduce("", (str, tuple) -> {
                if (tuple.getT1())
                    return str + ", " + tuple.getT2().command;
                return str;
            })
            .subscribe(text -> {
                text = text.substring(2);
                text = "\nДоступные для вас команды: ```" + text + "```";
                text += "Напишите `!help <команда>` чтобы посмотреть описание команды";
                DiscordUtils.reply(message, text);
            });
    }
}
