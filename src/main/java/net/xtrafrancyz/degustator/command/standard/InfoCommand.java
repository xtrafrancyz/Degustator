package net.xtrafrancyz.degustator.command.standard;

import sx.blah.discord.handle.obj.IMessage;

import net.xtrafrancyz.degustator.command.Command;

import java.util.concurrent.ThreadLocalRandom;

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
            "Привет, я люблю пробовать всякие штуки и поэтому меня прозвали Дегустатором.\n"
                + "Моя история началась в далёком 2к16 с того, что у админа VimeWorld.ru появилась бредовая идея - создать своего бота для сервера в Дрискорде. "
                + "Он долго меня растил, потратил множество бессонных ночей, но я все же вырос долбоёбом и так и не научился нормально говорить.\n"
                + "Все, что я умею, это посылать людей нахуй и выполнять самые простые команды. Список команд можно посмотреть написав `!help`.\n\n"
                + "Еще я люблю нести бред:\n"
                + JokeCommand.JOKES[ThreadLocalRandom.current().nextInt(JokeCommand.JOKES.length)]
        );
    }
}
