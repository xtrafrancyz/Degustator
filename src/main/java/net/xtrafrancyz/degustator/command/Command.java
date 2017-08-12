package net.xtrafrancyz.degustator.command;

import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IUser;

import java.util.ArrayList;
import java.util.List;

/**
 * @author xtrafrancyz
 */
public abstract class Command {
    public final String command;
    public final String help;
    public final List<String> aliases = new ArrayList<>();
    
    public Command(String command) {
        this(command, null);
    }
    
    public Command(String command, String help) {
        this.command = command;
        this.help = help;
    }
    
    public boolean canUse(IMessage message) {
        return true;
    }
    
    public abstract void onCommand(IMessage message, String[] args) throws Exception;
}
