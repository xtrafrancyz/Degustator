package net.xtrafrancyz.degustator;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import discord4j.common.util.Snowflake;

import net.xtrafrancyz.degustator.module.synchronizer.GetDiscordIdResponse;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author xtrafrancyz
 */
public class WebServer implements HttpHandler {
    private final Degustator degustator;
    
    public WebServer(Degustator degustator) {
        this.degustator = degustator;
    }
    
    public void start() throws IOException {
        HttpServer server = HttpServer.create();
        server.bind(new InetSocketAddress(degustator.config.web.host, degustator.config.web.port), 0);
        server.createContext("/", this);
        server.start();
    }
    
    public static Map<String, String> splitQuery(URI url) throws UnsupportedEncodingException {
        Map<String, String> query_pairs = new LinkedHashMap<>();
        String query = url.getQuery();
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf("=");
            query_pairs.put(URLDecoder.decode(pair.substring(0, idx), "UTF-8"), URLDecoder.decode(pair.substring(idx + 1), "UTF-8"));
        }
        return query_pairs;
    }
    
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            if (!exchange.getRequestURI().getPath().startsWith("/" + degustator.config.web.secret)) {
                respond(exchange, "FAIL");
                return;
            }
            String path = exchange.getRequestURI().getPath().substring(degustator.config.web.secret.length() + 1);
            switch (path) {
                case "":
                case "/link":
                    link(exchange);
                    break;
                case "/getDiscordId":
                    getDiscordId(exchange);
                    break;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            respond(exchange, "ERROR");
        }
    }
    
    private void link(HttpExchange exchange) throws IOException {
        Map<String, String> params = splitQuery(exchange.getRequestURI());
        Snowflake id = Snowflake.of(params.get("id"));
        String username = params.get("username");
        degustator.synchronizer.link(id, username);
        respond(exchange, "OK");
    }
    
    private void getDiscordId(HttpExchange exchange) throws Exception {
        Map<String, String> params = splitQuery(exchange.getRequestURI());
        GetDiscordIdResponse resp = degustator.synchronizer.getDiscordId(params.get("username"))
            .get(5, TimeUnit.SECONDS);
        if (resp == null)
            respond(exchange, "UNKNOWN USER");
        else
            respond(exchange, resp.id.asString());
    }
    
    public void respond(HttpExchange exchange, String response) throws IOException {
        byte[] bytes = response.getBytes();
        exchange.sendResponseHeaders(200, bytes.length);
        OutputStream os = exchange.getResponseBody();
        os.write(bytes);
        os.close();
    }
}
