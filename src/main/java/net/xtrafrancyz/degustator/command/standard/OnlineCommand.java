package net.xtrafrancyz.degustator.command.standard;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import sx.blah.discord.handle.obj.IMessage;

import net.xtrafrancyz.degustator.Degustator;
import net.xtrafrancyz.degustator.command.Command;
import net.xtrafrancyz.degustator.util.HttpUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author xtrafrancyz
 */
public class OnlineCommand extends Command {
    private Degustator app;
    private String cache = null;
    private long cacheExpire = 0;
    
    public OnlineCommand(Degustator app) {
        super("online",
            "`!online` - показывает модераторов онлайн на сервере");
        this.app = app;
    }
    
    @Override
    public void onCommand(IMessage message, String[] args) throws Exception {
        if (System.currentTimeMillis() > cacheExpire) {
            List<String> moders = new ArrayList<>(10);
            String response = HttpUtils.get(app.config.vimeworld.apiUrl + "/players/@list?token=" + app.config.vimeworld.token);
            for (JsonElement elem : new JsonParser().parse(response).getAsJsonArray()) {
                JsonObject player = elem.getAsJsonObject();
                String rank = player.get("rank").getAsString();
                if (Objects.equals(rank, "MODER") ||
                    Objects.equals(rank, "CHIEF") ||
                    Objects.equals(rank, "WARDEN"))
                    moders.add(player.get("username").getAsString());
            }
            if (!moders.isEmpty()) {
                moders.sort(String.CASE_INSENSITIVE_ORDER);
                cache = "Модераторы онлайн " + moders.size() + ": " + moders.stream().map(s -> '`' + s + '`').collect(Collectors.joining(", ")) + ".";
            } else {
                cache = "Нет ни одного модератора онлайн...";
            }
            cache += "\nИгроков: `" + HttpUtils.get("http://mc.vimeworld.ru/mon/total.txt") + "`.";
            cacheExpire = System.currentTimeMillis() + 10_000;
        }
        message.getChannel().sendMessage(cache);
    }
}
