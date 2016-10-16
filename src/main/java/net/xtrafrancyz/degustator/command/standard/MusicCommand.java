package net.xtrafrancyz.degustator.command.standard;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import sun.net.www.protocol.http.HttpURLConnection;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IVoiceChannel;
import sx.blah.discord.util.audio.AudioPlayer;

import net.xtrafrancyz.degustator.Degustator;
import net.xtrafrancyz.degustator.command.Command;
import net.xtrafrancyz.degustator.user.Permission;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

/**
 * @author xtrafrancyz
 */
public class MusicCommand extends Command {
    public MusicCommand() {
        super("music",
            "`!music summon` - я подключаюсь в ваш голосовой канал\n"
                + "`!music kick` - отключаюсь от вашего голосового канала\n"
                + "`!music play <url>` - добавляю в плейлист песенку по адресу\n"
                + "`!music skip` - пропускаю текущую песенку\n"
                + "`!music vol <громкость>` - затихаю на значение от 0 до 100\n"
                + "`!music playlist` - показываю плейлист без смс и регистрации\n"
                + "`!music pause` - жмаю, отжимаю паузу\n"
                + "`!music reset` - очищаю плейлист, типо круто",
            Permission.MUSIC
        );
        aliases.add("m");
    }
    
    @Override
    public void onCommand(IMessage message, String[] args) throws Exception {
        if (args.length == 0) {
            message.reply("\n" + help);
            return;
        }
        switch (args[0].toLowerCase()) {
            case "summon":
            case "s":
                List<IVoiceChannel> channels = message.getAuthor().getConnectedVoiceChannels();
                for (IVoiceChannel channel : channels) {
                    if (channel.getGuild().equals(message.getGuild())) {
                        channel.join();
                        message.reply("дратути))0)");
                        try {
                            int vol = Integer.parseInt(Degustator.instance().storage.get("#" + message.getGuild().getID() + ".volume"));
                            if (vol < 1)
                                vol = 1;
                            else if (vol > 100)
                                vol = 100;
                            AudioPlayer.getAudioPlayerForGuild(message.getChannel().getGuild()).setVolume(vol / 100f);
                        } catch (Exception ignored) {}
                        return;
                    }
                }
                message.reply("вы хоть сначала в канал зайдите. Куда мне подключаться?");
                break;
            case "kick":
            case "k":
                if (canControl(message, null)) {
                    channels = message.getClient().getConnectedVoiceChannels();
                    for (IVoiceChannel channel : channels) {
                        if (channel.getGuild().equals(message.getGuild())) {
                            channel.leave();
                            AudioPlayer player = AudioPlayer.getAudioPlayerForGuild(message.getChannel().getGuild());
                            player.clear();
                            player.clean();
                            message.reply("датвиданиня))0");
                        }
                    }
                }
                break;
            case "play":
            case "p":
                if (args.length != 2) {
                    message.reply("`!music play <url>` - добавляю в плейлист песенку по адресу");
                    return;
                }
                AudioPlayer player = AudioPlayer.getAudioPlayerForGuild(message.getChannel().getGuild());
                if (!canControl(message, player))
                    return;
                try {
                    URL url = new URL(args[1]);
                    YouTubeInfo yt = null;
                    switch (url.getHost()) {
                        case "youtube.com":
                        case "www.youtube.com":
                        case "gaming.youtube.com":
                            if (url.getPath().equals("/watch")) {
                                for (String pair : url.getQuery().split("&")) {
                                    if (pair.startsWith("v="))
                                        yt = getYouTubeURL(pair.substring(2));
                                }
                            }
                            break;
                        case "youtu.be":
                            yt = getYouTubeURL(url.getPath().substring(1));
                            break;
                    }
                    if (yt != null)
                        url = new URL(yt.url);
                    AudioPlayer.Track track = player.queue(url);
                    track.getMetadata().put("user", message.getAuthor());
                    if (yt != null) {
                        track.getMetadata().put("name", yt.title);
                        message.getChannel().sendMessage("Добавлено в плейлист: " + yt.title);
                        message.delete();
                    } else {
                        track.getMetadata().put("name", args[1]);
                        message.reply("песенка добавлена в плейлист");
                    }
                } catch (MalformedURLException ex) {
                    message.reply("вы ввели что-то не то");
                } catch (Exception ex) {
                    message.reply("это не песня, это говно какое-то\n" + ex.getMessage());
                }
                break;
            case "skip":
                player = AudioPlayer.getAudioPlayerForGuild(message.getChannel().getGuild());
                if (!canControl(message, player))
                    return;
                if (player.getPlaylistSize() == 0) {
                    message.reply("все, уже нечего пропускать");
                    return;
                }
                player.skip();
                message.reply("песенка пропущена");
                break;
            case "reset":
            case "r":
                player = AudioPlayer.getAudioPlayerForGuild(message.getChannel().getGuild());
                if (!canControl(message, player))
                    return;
                player.clear();
                message.reply("плейлист очищен");
                break;
            case "pause":
                player = AudioPlayer.getAudioPlayerForGuild(message.getChannel().getGuild());
                if (!canControl(message, player))
                    return;
                if (player.togglePause())
                    message.reply("запаузено");
                else
                    message.reply("отпаузено");
                break;
            case "playlist":
            case "list":
                player = AudioPlayer.getAudioPlayerForGuild(message.getChannel().getGuild());
                if (!canControl(message, player))
                    return;
                List<AudioPlayer.Track> playlist = player.getPlaylist();
                String msg;
                if (playlist.isEmpty()) {
                    msg = "Плейлист пуст...";
                } else {
                    msg = "Плейлист:";
                    int i = 1;
                    for (AudioPlayer.Track track : playlist)
                        msg += "\n" + (i++) + ". " + track.getMetadata().get("name").toString();
                }
                message.getChannel().sendMessage(msg);
                break;
            case "volume":
            case "vol":
            case "v":
                player = AudioPlayer.getAudioPlayerForGuild(message.getChannel().getGuild());
                if (!canControl(message, player))
                    return;
                if (args.length != 2) {
                    message.reply("`!music vol <громкость>` - затихаю на значение от **1** до **100**\n Текущая громкость: **" + (int) (player.getVolume() * 100) + "**");
                    return;
                }
                int vol = -1;
                try {
                    vol = Integer.parseInt(args[1]);
                } catch (Exception ignored) {}
                if (vol < 1 || vol > 100) {
                    message.reply("`!music vol <громкость>` - затихаю на значение от *0* до *100*");
                    return;
                }
                Degustator.instance().storage.set("#" + message.getGuild().getID() + ".volume", String.valueOf(vol));
                Degustator.instance().storage.save();
                player.setVolume(vol / 100f);
                break;
            default:
                message.reply("\n" + help);
                break;
        }
    }
    
    private static boolean canControl(IMessage message, AudioPlayer player) throws Exception {
        List<IVoiceChannel> channels = message.getClient().getConnectedVoiceChannels();
        IVoiceChannel neededChannel = null;
        for (IVoiceChannel channel : channels) {
            if (channel.getGuild().equals(message.getGuild()) && channel.getConnectedUsers().contains(message.getAuthor())) {
                neededChannel = channel;
                break;
            }
        }
        if (neededChannel == null) {
            message.reply("вы должны находиться в том же голосовом канале, где и я, чтобы указывать, что мне делать");
            return false;
        }
        if (!neededChannel.isConnected()) {
            message.reply("я все еще подключаюсь...");
            return false;
        }
        return true;
    }
    
    private static YouTubeInfo getYouTubeMP3(String ytid) throws IOException {
        CloseableHttpClient client = HttpClientBuilder.create().build();
        
        HttpGet request = new HttpGet("http://www.youtubeinmp3.com/fetch/?format=text&video=http://www.youtube.com/watch?v=" + ytid);
        request.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/53.0.2785.116 Safari/537.36");
        request.addHeader("Host", "youtubeinmp3.com");
        request.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
        
        HttpResponse response = client.execute(request);
        
        BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
        StringBuilder result = new StringBuilder();
        String line;
        while ((line = rd.readLine()) != null)
            result.append(line);
        String str = result.toString();
        
        int index = str.indexOf("Link: ");
        if (index == -1)
            return null;
        
        String url = URLDecoder.decode(str.substring(index + 6, str.length()), "UTF-8");
        String title = str.substring(str.indexOf("Title: ") + 7, str.indexOf(" <br/>"));
        
        return new YouTubeInfo(url, title);
    }
    
    private static YouTubeInfo getYouTubeURL(String ytid) throws Exception {
        String cmd = "youtube-dl -f 22 -g -e --user-agent " + HttpURLConnection.userAgent + " http://youtu.be/" + ytid;
        Process process = new ProcessBuilder(cmd.split(" ")).redirectErrorStream(true).start();
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        List<String> lines = new ArrayList<>(2);
        String s;
        while ((s = reader.readLine()) != null)
            lines.add(s);
        process.destroy();
        
        if (lines.size() != 2)
            throw new Exception("Ютуб лагает, попробуйте еще раз. Если не помогло, значит это все же не песня, а говно какое-то");
        if (lines.get(1).contains("ERROR: Unable to download webpage: HTTP Error 404: Not Found (caused by HTTPError());"))
            throw new Exception("На ютубе нет такого ролика");
        
        return new YouTubeInfo(lines.get(1), lines.get(0) + " (https://youtu.be/" + ytid + ")");
    }
    
    private static class YouTubeInfo {
        public String url;
        public String title;
        
        public YouTubeInfo(String url, String title) {
            this.url = url;
            this.title = title;
        }
    }
}
