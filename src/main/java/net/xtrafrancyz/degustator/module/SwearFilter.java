package net.xtrafrancyz.degustator.module;

import sx.blah.discord.Discord4J;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IMessage;

import net.xtrafrancyz.degustator.Degustator;
import net.xtrafrancyz.degustator.Scheduler;
import net.xtrafrancyz.degustator.mysql.Row;
import net.xtrafrancyz.degustator.mysql.SelectResult;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

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
            Discord4J.LOGGER.info("SwearFilter disabled. File 'badwords.txt' not found");
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
            e.printStackTrace();
            Discord4J.LOGGER.info("SwearFilter disabled. Database error");
            return;
        }
        
        degustator.client.getDispatcher().registerListeners(this);
    }
    
    @EventSubscriber
    public void onMessage(MessageReceivedEvent event) {
        if (isActive(event.getChannel()) && (!event.getAuthor().equals(event.getGuild().getOwner()) && !event.getAuthor().equals(degustator.client.getOurUser()))) {
            String[] words = event.getMessage().getContent().split(" ");
            for (String word : words)
                if (badWords.contains(normalizeWord(word))) {
                    try {
                        event.getMessage().delete();
                        IMessage message = event.getChannel().sendMessage("**" + event.getAuthor().getDisplayName(event.getGuild()) + "**, пожалуйста, следите за словами.");
                        Scheduler.schedule(message::delete, 5, TimeUnit.SECONDS);
                    } catch (Exception ignored) {}
                    break;
                }
        }
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public boolean isActive(IChannel channel) {
        return enabledChannels.contains(channel.getLongID());
    }
    
    public void disableFor(IChannel channel) {
        if (!enabled || !isActive(channel))
            return;
        enabledChannels.remove(channel.getLongID());
        
        try {
            degustator.mysql.query("DELETE FROM swearfilter WHERE channel = ?", ps -> ps.setLong(1, channel.getLongID()));
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    public void enableFor(IChannel channel) {
        if (!enabled || isActive(channel))
            return;
        enabledChannels.add(channel.getLongID());
        
        try {
            degustator.mysql.query("INSERT INTO swearfilter (channel) VALUES (?)", ps -> ps.setLong(1, channel.getLongID()));
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
