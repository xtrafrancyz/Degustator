package net.xtrafrancyz.degustator.command.standard;

import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IUser;

import net.xtrafrancyz.degustator.command.Command;
import net.xtrafrancyz.degustator.user.Permission;
import net.xtrafrancyz.degustator.user.Rank;
import net.xtrafrancyz.degustator.user.User;

import java.util.List;

/**
 * @author xtrafrancyz
 */
public class RankCommand extends Command {
    public RankCommand() {
        super("rank",
            "`!rank set @user rank` - устанавливает ранг для пользователя\n"
                + "`!rank del @user` - удаляет всю инфу о пользователе\n"
                + "`!rank perm @user +-perm` - добавляет или удаляет у право у пользователя",
            Permission.RANK);
    }
    
    @Override
    public void onCommand(IMessage message, String[] args) throws Exception {
        if (args.length < 2) {
            message.reply("\n" + help);
            return;
        }
        List<IUser> mentions = message.getMentions();
        if (mentions.size() != 1) {
            message.reply("\n" + help);
            return;
        }
        IUser other = mentions.get(0);
        if (other.equals(message.getAuthor())) {
            message.reply("вы не можете менять свои права");
            return;
        }
        
        switch (args[0]) {
            case "set":
                if (args.length != 3) {
                    message.reply("`!rank set @user rank` - устанавливает ранг для пользователя");
                    return;
                }
                Rank rank;
                try {
                    rank = Rank.valueOf(args[2].toUpperCase());
                } catch (Exception ex) {
                    message.reply("ранг `" + args[2] + "` не найден");
                    return;
                }
                User uother = User.get(other);
                uother.rank = rank;
                uother.save();
                message.getChannel().sendMessage("Пользователю " + other.mention() + " установлен ранг `" + rank.name() + "`. Поздравим же его");
                break;
            case "del":
                uother = User.get(other);
                uother.rank = Rank.USER;
                uother.permissions.clear();
                uother.save();
                message.getChannel().sendMessage("Все права и пранг пользователя " + other.mention() + " удалены");
                break;
            case "perm":
                if (args.length != 3 || (args[2].charAt(0) != '+' && args[2].charAt(0) != '-')) {
                    message.reply("`!rank perm @user +-perm` - добавляет или удаляет у право у пользователя");
                    return;
                }
                Permission perm;
                try {
                    perm = Permission.valueOf(args[2].substring(1).toUpperCase());
                } catch (Exception ex) {
                    message.reply("право `" + args[2].substring(1) + "` не найдено");
                    return;
                }
                uother = User.get(other);
                switch (args[2].charAt(0)) {
                    case '+':
                        uother.permissions.add(perm);
                        break;
                    case '-':
                        uother.permissions.remove(perm);
                        break;
                }
                uother.save();
                message.getChannel().sendMessage("Пользователю " + other.mention() + " добавлено право `" + perm.name() + "`. Поздравим же его");
                break;
            default:
                message.reply("\n" + help);
                break;
        }
    }
}
