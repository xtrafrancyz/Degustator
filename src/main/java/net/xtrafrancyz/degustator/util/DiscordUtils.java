package net.xtrafrancyz.degustator.util;

import discord4j.core.object.entity.Message;

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
}
