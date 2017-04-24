package net.xtrafrancyz.degustator;

import com.google.gson.Gson;
import sun.net.www.protocol.http.HttpURLConnection;
import sx.blah.discord.api.ClientBuilder;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.ReadyEvent;
import sx.blah.discord.handle.impl.events.guild.channel.message.MentionEvent;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.obj.IVoiceChannel;
import sx.blah.discord.util.DiscordException;
import sx.blah.discord.util.MissingPermissionsException;
import sx.blah.discord.util.RateLimitException;

import net.xtrafrancyz.degustator.command.CommandManager;
import net.xtrafrancyz.degustator.command.standard.ClearCommand;
import net.xtrafrancyz.degustator.command.standard.CoinCommand;
import net.xtrafrancyz.degustator.command.standard.HelpCommand;
import net.xtrafrancyz.degustator.command.standard.InfoCommand;
import net.xtrafrancyz.degustator.command.standard.JokeCommand;
import net.xtrafrancyz.degustator.command.standard.MeCommand;
import net.xtrafrancyz.degustator.command.standard.MusicCommand;
import net.xtrafrancyz.degustator.command.standard.OnlineCommand;
import net.xtrafrancyz.degustator.command.standard.RankCommand;
import net.xtrafrancyz.degustator.storage.FileJsonStorage;
import net.xtrafrancyz.degustator.storage.IStorage;
import net.xtrafrancyz.degustator.util.Reflect;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;

public class Degustator {
    private static Degustator instance;
    
    public final Gson gson = new Gson();
    public final Config config;
    public final IStorage storage;
    public final IDiscordClient client;
    public final Notifier notifier;
    
    private final CommandManager commandManager;
    
    private Degustator() throws DiscordException, IOException {
        instance = this;
        this.config = gson.fromJson(
            Files.readAllLines(FileSystems.getDefault().getPath("config.json")).stream()
                .map(String::trim)
                .filter(s -> !s.startsWith("#") && !s.isEmpty())
                .reduce((a, b) -> a += b)
                .orElse(""),
            Config.class
        );
        storage = new FileJsonStorage(new File("storage.json"));
        storage.load();
        ClientBuilder builder = new ClientBuilder();
        builder.withToken(config.token);
        client = builder.login();
        client.getDispatcher().registerListener(this);
        notifier = new Notifier(this);
        
        Reflect.setFinal(HttpURLConnection.class, "userAgent", "Degustator/1");
        
        commandManager = new CommandManager(this);
        commandManager.registerCommand(new HelpCommand(commandManager));
        commandManager.registerCommand(new MeCommand());
        commandManager.registerCommand(new ClearCommand());
        commandManager.registerCommand(new RankCommand());
        commandManager.registerCommand(new MusicCommand());
        commandManager.registerCommand(new InfoCommand());
        commandManager.registerCommand(new JokeCommand());
        commandManager.registerCommand(new OnlineCommand(this));
        commandManager.registerCommand(new CoinCommand());
        
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            client.getConnectedVoiceChannels().forEach(IVoiceChannel::leave);
        }));
    }
    
    @EventSubscriber
    public void onReady(ReadyEvent event) throws RateLimitException, DiscordException {
        client.changePlayingText("бубенчики");
    }
    
    @EventSubscriber
    public void onMessage(MessageReceivedEvent event) {
        if (event.getMessage().getContent().startsWith("!"))
            commandManager.process(event.getMessage());
    }
    
    @EventSubscriber
    public void onMention(MentionEvent event) {
        try {
            if (event.getAuthor().equals(client.getOurUser()))
                return;
            if (event.getMessage().getContent().contains("сам иди")) {
                event.getMessage().reply("нет ты иди нахуй");
            } else {
                event.getMessage().reply("иди нахуй");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    //@EventSubscriber временно выключено
    public void onPrivateMessage(MessageReceivedEvent event) throws RateLimitException, DiscordException, MissingPermissionsException {
        if (event.getMessage().getChannel().isPrivate()) {
            String msg = event.getMessage().getContent();
            String[] args = msg.split(" ");
            switch (args[0]) {
                case "!info":
                    event.getMessage().getChannel().sendMessage("Я пидорский бот. Иди нахуй");
                    break;
                case "!reg":
                    if (notifier.contains(event.getMessage().getAuthor().getStringID())) {
                        event.getMessage().getChannel().sendMessage("Вы уже зарегистрированы");
                        return;
                    }
                    notifier.notify("Зарегался новый юзер: " + event.getMessage().getAuthor().getName());
                    notifier.add(event.getMessage().getAuthor().getStringID());
                    event.getMessage().getChannel().sendMessage(":white_check_mark: Ура!");
                    break;
                case "!unreg":
                    if (!notifier.contains(event.getMessage().getAuthor().getStringID())) {
                        event.getMessage().getChannel().sendMessage("Вы не зарегистрированы");
                        return;
                    }
                    notifier.remove(event.getMessage().getAuthor().getStringID());
                    event.getMessage().getChannel().sendMessage(":white_check_mark: Ура!");
                    break;
                case "!test":
                    notifier.notify("Привет!");
                    break;
            }
        }
    }
    
    public static void main(String[] args) throws Exception {
        new Degustator();
    }
    
    public static Degustator instance() {
        return instance;
    }
}
