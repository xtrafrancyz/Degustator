package net.xtrafrancyz.degustator.user;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * @author xtrafrancyz
 */
public enum Rank {
    USER,
    ADMIN(
        Permission.CLEAR,
        Permission.RANK,
        Permission.MUSIC
    );
    
    Set<Permission> permissions = EnumSet.noneOf(Permission.class);
    
    Rank(Permission... permissions) {
        Collections.addAll(this.permissions, permissions);
    }
}
