package net.xtrafrancyz.degustator;

import com.google.gson.Gson;
import com.google.gson.stream.JsonWriter;
import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.presence.Activity;
import discord4j.core.object.presence.Presence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.xtrafrancyz.degustator.command.CommandManager;
import net.xtrafrancyz.degustator.command.manage.MassBanCommand;
import net.xtrafrancyz.degustator.command.manage.SwearFilterCommand;
import net.xtrafrancyz.degustator.command.manage.VimenickCommand;
import net.xtrafrancyz.degustator.command.standard.HelpCommand;
import net.xtrafrancyz.degustator.command.standard.JokeCommand;
import net.xtrafrancyz.degustator.command.standard.OnlineCommand;
import net.xtrafrancyz.degustator.module.SwearFilter;
import net.xtrafrancyz.degustator.module.synchronizer.Synchronizer;
import net.xtrafrancyz.degustator.mysql.MysqlPool;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;

public class Degustator {
    private static Degustator instance;
    public static final Logger log = LoggerFactory.getLogger("Degustator");
    
    public final Gson gson = new Gson();
    public Config config;
    public final GatewayDiscordClient gateway;
    public final MysqlPool mysql;
    
    private final CommandManager commandManager;
    public final Synchronizer synchronizer;
    public final SwearFilter swearFilter;
    
    private Degustator() throws Exception {
        instance = this;
        readConfig();
        Scheduler.init(4);
        
        DiscordClient client = DiscordClient.create(config.token);
        gateway = client.gateway()
            .setInitialStatus(s -> Presence.online(Activity.watching("!help")))
            .login().block();
        
        mysql = new MysqlPool(this);
        
        synchronizer = new Synchronizer(this);
        swearFilter = new SwearFilter(this);
        
        commandManager = new CommandManager(this);
        commandManager.registerCommand(new HelpCommand(commandManager));
        commandManager.registerCommand(new JokeCommand());
        commandManager.registerCommand(new OnlineCommand());
        commandManager.registerCommand(new SwearFilterCommand());
        commandManager.registerCommand(new MassBanCommand());
        commandManager.registerCommand(new VimenickCommand());
        
        new WebServer(this).start();
        
        gateway.on(MessageCreateEvent.class).subscribe(this::onMessage);
        
        gateway.onDisconnect().block();
    }
    
    private void readConfig() throws IOException {
        File confFile = new File("config.json");
        if (!confFile.exists()) {
            this.config = new Config();
            JsonWriter writer = new JsonWriter(new FileWriter(confFile));
            writer.setIndent("  ");
            writer.setHtmlSafe(false);
            gson.toJson(config, Config.class, writer);
            writer.close();
            System.out.println("Created config.json");
            System.exit(0);
        } else {
            this.config = gson.fromJson(
                Files.readAllLines(confFile.toPath()).stream()
                    .map(String::trim)
                    .filter(s -> !s.startsWith("#") && !s.isEmpty())
                    .reduce((a, b) -> a += b)
                    .orElse(""),
                Config.class
            );
        }
    }
    
    public void onMessage(MessageCreateEvent event) {
        String content = event.getMessage().getContent();
        if (content.startsWith("!"))
            commandManager.process(event.getMessage());
    }
    
    public static void main(String[] args) throws Exception {
        new Degustator();
    }
    
    public static Degustator instance() {
        return instance;
    }
}
