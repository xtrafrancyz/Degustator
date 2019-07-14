package net.xtrafrancyz.degustator.module;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.event.domain.message.MessageUpdateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.MessageChannel;
import discord4j.core.object.util.Snowflake;

import net.xtrafrancyz.degustator.Degustator;
import net.xtrafrancyz.degustator.mysql.Row;
import net.xtrafrancyz.degustator.mysql.SelectResult;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

/**
 * @author xtrafrancyz
 */
public class SwearFilter {
    private final Degustator degustator;
    private final Set<Long> enabledChannels;
    private final Set<String> badWords;
    private boolean enabled = true;
    
    public SwearFilter(Degustator degustator) {
        this.degustator = degustator;
        this.enabledChannels = new HashSet<>();
        this.badWords = new HashSet<>();
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream("badwords.txt"), StandardCharsets.UTF_8))) {
            String word;
            while ((word = reader.readLine()) != null) {
                word = normalizeWord(word.trim());
                if (!word.isEmpty())
                    badWords.add(word);
            }
        } catch (Exception ex) {
            enabled = false;
            Degustator.log.info("SwearFilter disabled. File 'badwords.txt' not found");
            return;
        }
        
        try {
            degustator.mysql.query("CREATE TABLE IF NOT EXISTS `swearfilter` (\n" +
                "  `channel` bigint(20) NOT NULL,\n" +
                "  PRIMARY KEY (`channel`)\n" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8;");
            
            SelectResult result = degustator.mysql.select("SELECT channel FROM swearfilter");
            for (Row row : result.getRows())
                enabledChannels.add(row.getLong(0));
        } catch (SQLException e) {
            enabled = false;
            Degustator.log.warn("SwearFilter disabled. Database error", e);
            return;
        }
        
        degustator.client.getEventDispatcher()
            .on(MessageUpdateEvent.class).subscribe(this::onMessageUpdate);
        degustator.client.getEventDispatcher()
            .on(MessageCreateEvent.class).subscribe(this::onMessageCreate);
    }
    
    public void onMessageCreate(MessageCreateEvent event) {
        if (event.getMember().isPresent()
            && event.getMessage().getContent().isPresent()
            && event.getMessage().getAuthor().isPresent()
            && !event.getMessage().getAuthor().get().isBot()
            && isActive(event.getMessage().getChannelId())
            && hasSwear(event.getMessage().getContent().get())) {
            event.getMessage().getChannel().subscribe(c -> deleteMessage(c, event.getMessage()));
        }
    }
    
    public void onMessageUpdate(MessageUpdateEvent event) {
        if (event.isContentChanged() && isActive(event.getChannelId())) {
            event.getMessage()
                .filter(m -> m.getAuthor().isPresent() && !m.getAuthor().get().isBot())
                .filter(m -> m.getContent()
                    .filter(this::hasSwear)
                    .isPresent())
                .zipWith(event.getChannel())
                .subscribe(t -> deleteMessage(t.getT2(), t.getT1()));
        }
    }
    
    private void deleteMessage(MessageChannel channel, Message message) {
        message.delete("Swear detected")
            .then(message.getAuthorAsMember()
                .flatMap(author -> channel.createMessage("**" + author.getDisplayName() + "**, пожалуйста, следите за словами."))
                .delayElement(Duration.ofSeconds(5))
                .flatMap(Message::delete)
            ).subscribe();
    }
    
    private boolean hasSwear(String message) {
        for (String word : message.split(" "))
            if (badWords.contains(normalizeWord(word)))
                return true;
        return false;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public boolean isActive(Snowflake channel) {
        return enabledChannels.contains(channel.asLong());
    }
    
    public void disableFor(Snowflake channel) {
        if (!enabled || !isActive(channel))
            return;
        enabledChannels.remove(channel.asLong());
        
        try {
            degustator.mysql.query("DELETE FROM swearfilter WHERE channel = ?", ps -> ps.setLong(1, channel.asLong()));
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    public void enableFor(Snowflake channel) {
        if (!enabled || isActive(channel))
            return;
        enabledChannels.add(channel.asLong());
        
        try {
            degustator.mysql.query("INSERT INTO swearfilter (channel) VALUES (?)", ps -> ps.setLong(1, channel.asLong()));
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    private static String normalizeWord(String str) {
        if (str.isEmpty())
            return "";
        char[] chars = str.toCharArray();
        int len = chars.length;
        int st = 0;
        while (st < len && !Character.isAlphabetic(chars[st]))
            st++;
        while (st < len && !Character.isAlphabetic(chars[len - 1]))
            len--;
        str = ((st > 0) || (len < chars.length)) ? str.substring(st, len) : str;
        return str.toLowerCase()
            .replace('a', 'а')
            .replace('e', 'е')
            .replace('э', 'е')
            .replace('ё', 'е')
            .replace('y', 'у')
            .replace('p', 'р')
            .replace('x', 'х')
            .replace('o', 'о')
            .replace('c', 'с');
    }
}
