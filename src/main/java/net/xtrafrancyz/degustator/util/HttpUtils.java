package net.xtrafrancyz.degustator.util;

import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import net.xtrafrancyz.degustator.Degustator;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * @author xtrafrancyz
 */
public class HttpUtils {
    public static final org.apache.http.client.HttpClient HTTP_CLIENT = HttpClientBuilder.create()
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
        HttpGet request = new HttpGet(url);
        HttpResponse response = null;
        try {
            response = HTTP_CLIENT.execute(request);
            callback.accept(EntityUtils.toString(response.getEntity()), null);
        } catch (IOException ex) {
            callback.accept(null, ex);
        } finally {
            if (response != null)
                EntityUtils.consumeQuietly(response.getEntity());
        }
    }
    
    public static void apiGet(String query, Callback callback) {
        HttpGet request = new HttpGet("https://api.vime.world" + query);
        request.addHeader("Access-Token", Degustator.instance().config.vimeApiToken);
        HttpResponse response = null;
        try {
            response = HTTP_CLIENT.execute(request);
            callback.accept(EntityUtils.toString(response.getEntity()), null);
        } catch (IOException ex) {
            callback.accept(null, ex);
        } finally {
            if (response != null)
                EntityUtils.consumeQuietly(response.getEntity());
        }
    }
    
    public interface Callback {
        void accept(String content, Throwable error);
    }
}
