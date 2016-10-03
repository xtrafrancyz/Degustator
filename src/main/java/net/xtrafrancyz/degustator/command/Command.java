package net.xtrafrancyz.degustator.command;

import sx.blah.discord.handle.obj.IMessage;

import net.xtrafrancyz.degustator.user.Permission;

import java.util.ArrayList;
import java.util.List;

/**
 * @author xtrafrancyz
 */
public abstract class Command {
    public final String command;
    public final String help;
    public final Permission perm;
    public final List<String> aliases = new ArrayList<>();
    
    public Command(String command) {
        this(command, null, null);
    }
    
    public Command(String command, String help) {
        this(command, help, null);
    }
    
    public Command(String command, String help, Permission perm) {
        this.command = command;
        this.help = help;
        this.perm = perm;
    }
    
    public abstract void onCommand(IMessage message, String[] args) throws Exception;
}
