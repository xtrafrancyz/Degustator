package net.xtrafrancyz.degustator.util;

import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.util.Snowflake;
import reactor.core.publisher.Mono;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

/**
 * @author xtrafrancyz
 */
public class DiscordUtils {
    public static void reply(Message message, String text) {
        if (message.getAuthor().isPresent()) {
            message.getChannel().subscribe(c ->
                c.createMessage(message.getAuthor().get().getMention() + " " + text).subscribe()
            );
        } else {
            message.getChannel().subscribe(c ->
                c.createMessage(text).subscribe()
            );
        }
    }
    
    public static void sendMessage(Message to, String text) {
        to.getChannel().subscribe(c -> c.createMessage(text).subscribe());
    }
    
    public static void getMemberRoles(Member member, Consumer<Set<Snowflake>> consumer) {
        member.getRoles().reduce(new HashSet<Snowflake>(), (roles, role) -> {
            roles.add(role.getId());
            return roles;
        }).subscribe(consumer);
    }
}
