package net.xtrafrancyz.degustator.command.standard;

import sx.blah.discord.handle.obj.IMessage;

import net.xtrafrancyz.degustator.command.Command;

import java.time.format.DateTimeFormatter;

/**
 * @author xtrafrancyz
 */
public class MeCommand extends Command {
    public MeCommand() {
        super("me", "`!me` - показывает информацию о вас");
    }
    
    @Override
    public void onCommand(IMessage message, String[] args) throws Exception {
        message.reply("Дата регистрации: " + message.getAuthor().getCreationDate().format(DateTimeFormatter.ISO_DATE));
    }
}
