package net.xtrafrancyz.degustator.command.standard;

import sx.blah.discord.handle.obj.IMessage;

import net.xtrafrancyz.degustator.command.Command;

import java.util.concurrent.ThreadLocalRandom;

/**
 * @author xtrafrancyz
 */
public class CoinCommand extends Command {
    public CoinCommand() {
        super("coin",
            "`!coin` - кидает монетку и говорит Орел или Решка");
    }
    
    @Override
    public void onCommand(IMessage message, String[] args) throws Exception {
        boolean coin = ThreadLocalRandom.current().nextBoolean();
        if (coin)
            message.getChannel().sendMessage("Выпал `орел`");
        else
            message.getChannel().sendMessage("Выпала `решка`");
    }
}
