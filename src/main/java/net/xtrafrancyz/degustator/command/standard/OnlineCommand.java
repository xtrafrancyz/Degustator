package net.xtrafrancyz.degustator.command.standard;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import discord4j.core.object.entity.Message;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;

import net.xtrafrancyz.degustator.command.Command;
import net.xtrafrancyz.degustator.util.HttpUtils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author xtrafrancyz
 */
public class OnlineCommand extends Command {
    private static final Set<String> MODER_RANKS = new HashSet<>(Arrays.asList("MODER", "CHIEF", "WARDEN"));
    private Mono<String> content;
    
    public OnlineCommand() {
        super("online",
            "`!online` - показывает модераторов онлайн на сервере");
        content = Mono
            .create(this::loadText)
            .cache(Duration.ofSeconds(10));
    }
    
    private void loadText(MonoSink<String> sink) {
        HttpUtils.apiGet("/online/staff", (response, error) -> {
            if (error != null) {
                sink.success("Ошибка при получении модеров");
                return;
            }
            List<String> moders = new ArrayList<>(10);
            for (JsonElement elem : new JsonParser().parse(response).getAsJsonArray()) {
                JsonObject player = elem.getAsJsonObject();
                String rank = player.get("rank").getAsString();
                if (MODER_RANKS.contains(rank))
                    moders.add(player.get("username").getAsString());
            }
            HttpUtils.get("http://mc.vimeworld.ru/mon/total.txt", (total, error0) -> {
                if (error0 != null) {
                    sink.success("Ошибка при получении онлайна");
                    return;
                }
                String text;
                if (!moders.isEmpty()) {
                    moders.sort(String.CASE_INSENSITIVE_ORDER);
                    text = "Модераторы онлайн " + moders.size() + ": " + moders.stream().map(s -> '`' + s + '`').collect(Collectors.joining(", ")) + ".";
                } else {
                    text = "Нет ни одного модератора онлайн...";
                }
                text += "\nИгроков: `" + total + "`.";
                sink.success(text);
            });
        });
    }
    
    @Override
    public void onCommand(Message message, String[] args) throws Exception {
        content.subscribe(text -> {
            message.getChannel().subscribe(c -> c.createMessage(text).subscribe());
        });
    }
}
