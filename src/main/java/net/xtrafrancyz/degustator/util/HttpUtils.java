package net.xtrafrancyz.degustator.util;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import net.xtrafrancyz.degustator.Degustator;
import net.xtrafrancyz.degustator.Scheduler;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * @author xtrafrancyz
 */
public class HttpUtils {
    public static final HttpClient HTTP_CLIENT = HttpClientBuilder.create()
        .setConnectionTimeToLive(1, TimeUnit.MINUTES)
        .setMaxConnPerRoute(3) // default 2
        .setMaxConnTotal(20) // default 20
        .setDefaultRequestConfig(RequestConfig.custom()
            .setConnectTimeout(5000)
            .setSocketTimeout(5000)
            .setConnectionRequestTimeout(5000)
            .build())
        .build();
    
    public static void get(String url, Callback callback) {
        execute(new HttpGet(url), callback);
    }
    
    public static void apiGet(String query, Callback callback) {
        HttpGet request = new HttpGet("https://api.vime.world" + query);
        request.addHeader("Access-Token", Degustator.instance().config.vimeApiToken);
        execute(request, callback);
    }
    
    private static void execute(HttpRequestBase request, Callback callback) {
        Scheduler.execute(() -> {
            String body;
            HttpResponse response = null;
            try {
                response = HTTP_CLIENT.execute(request);
                body = EntityUtils.toString(response.getEntity());
            } catch (Throwable ex) {
                callback.accept(null, ex);
                return;
            } finally {
                if (response != null)
                    EntityUtils.consumeQuietly(response.getEntity());
                //System.out.println("Executed: " + request.getURI());
            }
            callback.accept(body, null);
        });
    }
    
    public interface Callback {
        void accept(String content, Throwable error);
    }
}
