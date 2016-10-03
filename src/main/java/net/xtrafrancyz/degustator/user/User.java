package net.xtrafrancyz.degustator.user;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import sx.blah.discord.handle.obj.IUser;

import net.xtrafrancyz.degustator.Degustator;

import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author xtrafrancyz
 */
public class User {
    private static LoadingCache<String, User> cache = Caffeine.<String, User>newBuilder()
        .expireAfterAccess(1, TimeUnit.HOURS)
        .build(User::new);
    
    public final String id;
    public Rank rank;
    public Set<Permission> permissions;
    public Degustator app = Degustator.instance();
    
    private User(String id) {
        this.id = id;
        boolean hasProblems = false;
        try {
            rank = Rank.valueOf(app.storage.get("@" + id + ".rank"));
        } catch (Exception ex) {
            rank = Rank.USER;
            hasProblems = true;
        }
        permissions = EnumSet.noneOf(Permission.class);
        String perms = app.storage.get("@" + id + ".permissions");
        if (perms != null) {
            for (String perm : perms.split(";")) {
                try {
                    permissions.add(Permission.valueOf(perm));
                } catch (Exception ex) {
                    hasProblems = true;
                }
            }
        }
        if (hasProblems)
            save();
    }
    
    public boolean hasPerm(Permission perm) {
        return perm == null || rank.permissions.contains(perm) || permissions.contains(perm);
    }
    
    public void save() {
        if (rank != Rank.USER)
            app.storage.set("@" + id + ".rank", rank.name());
        else
            app.storage.remove("@" + id + ".rank");
        if (!permissions.isEmpty())
            app.storage.set("@" + id + ".permissions", permissions.stream().map(Permission::name).collect(Collectors.joining(";")));
        else
            app.storage.remove("@" + id + ".permissions");
        app.storage.save();
    }
    
    public static User get(String id) {
        return cache.get(id);
    }
    
    public static User get(IUser user) {
        return get(user.getID());
    }
}
