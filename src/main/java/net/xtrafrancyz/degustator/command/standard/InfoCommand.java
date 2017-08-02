package net.xtrafrancyz.degustator.command.standard;

import sx.blah.discord.handle.obj.IMessage;

import net.xtrafrancyz.degustator.command.Command;

/**
 * @author xtrafrancyz
 */
public class InfoCommand extends Command {
    public InfoCommand() {
        super("info",
            "`!info` - показывает информацию о боте"
        );
    }
    
    @Override
    public void onCommand(IMessage message, String[] args) throws Exception {
        message.getChannel().sendMessage(
            "Бот создан специально для сервера VimeWorld.ru"
                + "\nИсходный код: https://github.com/xtrafrancyz/Degustator"
        );
    }
}
