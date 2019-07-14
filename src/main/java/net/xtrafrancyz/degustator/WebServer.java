package net.xtrafrancyz.degustator;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import discord4j.core.object.util.Snowflake;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.util.LinkedHashMap;
import java.util.Map;

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
            if (!exchange.getRequestURI().getPath().equals("/" + degustator.config.web.secret)) {
                respond(exchange, "FAIL");
                return;
            }
            Map<String, String> params = splitQuery(exchange.getRequestURI());
            Snowflake id = Snowflake.of(params.get("id"));
            String username = params.get("username");
            degustator.synchronizer.link(id, username);
            respond(exchange, "OK");
        } catch (Exception ex) {
            ex.printStackTrace();
            respond(exchange, "ERROR");
        }
    }
    
    public void respond(HttpExchange exchange, String response) throws IOException {
        byte[] bytes = response.getBytes();
        exchange.sendResponseHeaders(200, bytes.length);
        OutputStream os = exchange.getResponseBody();
        os.write(bytes);
        os.close();
    }
}
