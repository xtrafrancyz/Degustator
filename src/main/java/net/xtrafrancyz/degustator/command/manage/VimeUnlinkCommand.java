package net.xtrafrancyz.degustator.command.manage;

import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.PartialMember;
import reactor.core.publisher.Mono;

import net.xtrafrancyz.degustator.Degustator;
import net.xtrafrancyz.degustator.command.Command;
import net.xtrafrancyz.degustator.module.synchronizer.Synchronizer;
import net.xtrafrancyz.degustator.util.DiscordUtils;

import java.util.List;

public class VimeUnlinkCommand extends Command {
    private static final Snowflake ADMIN_ROLE = Snowflake.of(106117738677665792L);
    
    private final Synchronizer synchronizer = Degustator.instance().synchronizer;
    
    public VimeUnlinkCommand() {
        super("vimeunlink",
            "`!vimeunlink @юзер` или `!vimenick 123456789` - отвязывает дс акк от вайма");
    }
    
    @Override
    public void onCommand(Message message, String[] args) throws Exception {
        if (args.length == 0) {
            usage(message);
            return;
        }
        List<Snowflake> mentions = message.getUserMentionIds();
        if (mentions.isEmpty()) {
            long userid;
            try {
                userid = Long.parseLong(args[0]);
            } catch (Exception ignored) {
                usage(message);
                return;
            }
            message.getGuild()
                .flatMap(g -> g.getMemberById(Snowflake.of(userid)))
                .subscribe(u -> {
                    unlinkMember(message, u);
                }, error -> {
                    DiscordUtils.sendMessage(message, "Такой юзер не найден");
                });
        } else {
            for (PartialMember m : message.getMemberMentions())
                unlinkMember(message, m);
        }
    }
    
    private void usage(Message message) {
        DiscordUtils.sendMessage(message, "Использование: `!vimeunlink @юзер` или `!vimeunlink 123456789`");
    }
    
    private void unlinkMember(Message message, PartialMember user) {
        synchronizer.unlink(user.getId());
        DiscordUtils.sendMessage(message, "Юзер `" + getDsName(user) + "` отвязан от вайма (наверно)");
    }
    
    private String getDsName(PartialMember member) {
        return member.getDisplayName() + "#" + member.getDiscriminator() + " (" + member.getId().asString() + ")";
    }
    
    @Override
    public Mono<Boolean> canUse(Message message) {
        if (!message.getAuthor().isPresent() ||
            !Synchronizer.VIMEWORLD_GUILD_ID.equals(message.getGuildId().orElse(null)))
            return Mono.just(false);
        
        return message.getAuthorAsMember()
            .map(member -> member.getRoleIds().contains(ADMIN_ROLE));
    }
}
