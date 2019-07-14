package net.xtrafrancyz.degustator.command;

import discord4j.core.object.entity.Message;
import reactor.core.publisher.Mono;

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
    
    public Mono<Boolean> canUse(Message message) {
        return Mono.just(true);
    }
    
    public Mono<Boolean> checkIsAuthorIsGuildOwner(Message message) {
        if (!message.getAuthor().isPresent())
            return Mono.just(false);
        return Mono.create(sink -> {
            message.getGuild().subscribe(guild -> {
                sink.success(
                    guild.getOwnerId().equals(message.getAuthor().get().getId())
                );
            });
        });
    }
    
    public abstract void onCommand(Message message, String[] args) throws Exception;
}
