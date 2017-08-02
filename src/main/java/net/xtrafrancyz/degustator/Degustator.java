package net.xtrafrancyz.degustator;

import com.google.gson.Gson;
import sx.blah.discord.api.ClientBuilder;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.ReadyEvent;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.util.DiscordException;
import sx.blah.discord.util.RateLimitException;

import net.xtrafrancyz.degustator.command.CommandManager;
import net.xtrafrancyz.degustator.command.standard.HelpCommand;
import net.xtrafrancyz.degustator.command.standard.InfoCommand;
import net.xtrafrancyz.degustator.command.standard.JokeCommand;
import net.xtrafrancyz.degustator.command.standard.MeCommand;
import net.xtrafrancyz.degustator.command.standard.OnlineCommand;
import net.xtrafrancyz.degustator.storage.FileJsonStorage;
import net.xtrafrancyz.degustator.storage.IStorage;

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
        client = new ClientBuilder().withToken(config.token).login();
        client.getDispatcher().registerListener(this);
        
        commandManager = new CommandManager(this);
        commandManager.registerCommand(new HelpCommand(commandManager));
        commandManager.registerCommand(new MeCommand());
        commandManager.registerCommand(new InfoCommand());
        commandManager.registerCommand(new JokeCommand());
        commandManager.registerCommand(new OnlineCommand());
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
    
    public static void main(String[] args) throws Exception {
        new Degustator();
    }
    
    public static Degustator instance() {
        return instance;
    }
}
