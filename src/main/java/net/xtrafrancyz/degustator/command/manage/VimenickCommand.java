package net.xtrafrancyz.degustator.command.manage;

import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IRole;
import sx.blah.discord.handle.obj.IUser;

import net.xtrafrancyz.degustator.Degustator;
import net.xtrafrancyz.degustator.command.Command;
import net.xtrafrancyz.degustator.module.VimeWorldRankSynchronizer;

import java.util.List;

/**
 * @author xtrafrancyz
 */
public class VimenickCommand extends Command {
    private static final long DISCORD_MODER_ROLE = 344881335401316356L;
    private static final long ADMIN_ROLE = 106117738677665792L;
    
    public VimenickCommand() {
        super("vimenick",
            "`!vimenick @юзер` или `!vimenick 123456789` - по акку в дискорде ищет ник на вайме\n" +
                "`!vimenick $nickNaVime` - по нику вайма ищет акк в дискорде");
    }
    
    @Override
    public void onCommand(IMessage message, String[] args) throws Exception {
        if (args.length == 0) {
            usage(message);
            return;
        }
        IUser user;
        if (message.getMentions().isEmpty()) {
            if (args[0].startsWith("$")) {
                vimeToDs(message, args[0].substring(1));
                return;
            }
            long userid = -1;
            try {
                userid = Long.parseLong(args[0]);
            } catch (Exception ignored) {}
            if (userid == -1) {
                usage(message);
                return;
            }
            user = message.getGuild().getUserByID(userid);
        } else {
            user = message.getMentions().get(0);
        }
        dsToVime(message, user);
    }
    
    private void usage(IMessage message) {
        message.getChannel().sendMessage("Использование: `!vimenick @юзер` или `!vimenick 123456789` или `!vimenick $nickNaVime`");
    }
    
    private void dsToVime(IMessage message, IUser user) throws Exception {
        if (user == null) {
            message.getChannel().sendMessage("Такой юзер не найден");
            return;
        }
        String nick = Degustator.instance().synchronizer.getVimeNick(user.getLongID());
        if (nick != null) {
            message.getChannel().sendMessage("Ник юзера `" + getDsName(message.getGuild(), user) + "` на вайме - `" + nick + "`");
            Degustator.instance().synchronizer.link(message.getGuild(), user, nick);
        } else {
            message.getChannel().sendMessage("Юзер `" + getDsName(message.getGuild(), user) + "` не привязан к аккаунту вайма");
        }
    }
    
    private void vimeToDs(IMessage message, String nick) throws Exception {
        long id = Degustator.instance().synchronizer.getDsId(nick);
        IUser user = message.getGuild().getUserByID(id);
        if (user != null) {
            message.getChannel().sendMessage("Ник юзера `" + getDsName(message.getGuild(), user) + "` на вайме - `" + nick + "`");
            Degustator.instance().synchronizer.link(message.getGuild(), user, nick);
        } else {
            message.getChannel().sendMessage("Ник `" + nick + "` не привязан к дискорду");
        }
    }
    
    private String getDsName(IGuild guild, IUser user) {
        return user.getDisplayName(guild) + "#" + user.getDiscriminator() + " (" + user.getLongID() + ")";
    }
    
    @Override
    public boolean canUse(IMessage message) {
        if (message.getGuild().getLongID() != VimeWorldRankSynchronizer.VIMEWORLD_GUILD_ID)
            return false;
        List<IRole> roles = message.getAuthor().getRolesForGuild(message.getGuild());
        for (IRole role : roles)
            if (role.getLongID() == DISCORD_MODER_ROLE || role.getLongID() == ADMIN_ROLE)
                return true;
        return false;
    }
}
