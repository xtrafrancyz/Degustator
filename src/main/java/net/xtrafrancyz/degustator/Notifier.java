package net.xtrafrancyz.degustator;

import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.util.DiscordException;
import sx.blah.discord.util.MissingPermissionsException;
import sx.blah.discord.util.RateLimitException;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author xtrafrancyz
 */
public class Notifier {
    private final Degustator degustator;
    private Set<String> users = new HashSet<>();
    
    public Notifier(Degustator degustator) {
        this.degustator = degustator;
        String data = degustator.storage.get("notifier.users");
        if (data != null)
            users.addAll(Arrays.asList(data.split(";")));
    }
    
    public void notify(String message) throws RateLimitException, DiscordException, MissingPermissionsException {
        for (String id : users) {
            IUser user = degustator.client.getUserByID(Long.parseUnsignedLong(id));
            if (user != null)
                degustator.client.getOrCreatePMChannel(user).sendMessage(message);
        }
    }
    
    public boolean contains(String id) {
        return users.contains(id);
    }
    
    public boolean add(String id) {
        boolean success = users.add(id);
        save();
        return success;
    }
    
    public boolean remove(String id) {
        boolean success = users.remove(id);
        save();
        return success;
    }
    
    private void save() {
        degustator.storage.set("notifier.users", users.stream().collect(Collectors.joining(";")));
        degustator.storage.save();
    }
}
