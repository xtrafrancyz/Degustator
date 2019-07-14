package net.xtrafrancyz.degustator.module.synchronizer;

import com.github.benmanes.caffeine.cache.AsyncCacheLoader;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.xtrafrancyz.degustator.util.HttpUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * @author xtrafrancyz
 */
public class RankLoader implements AsyncCacheLoader<String, String> {
    @Override
    public CompletableFuture<String> asyncLoad(String username, Executor executor) {
        CompletableFuture<String> future = new CompletableFuture<>();
        HttpUtils.apiGet("/user/name/" + username, (body, ex) -> {
            if (ex != null) {
                future.completeExceptionally(ex);
            } else {
                JsonArray arr = new JsonParser().parse(body).getAsJsonArray();
                if (arr.size() == 1)
                    future.complete(arr.get(0).getAsJsonObject().get("rank").getAsString());
                else
                    future.complete("PLAYER");
            }
        });
        return future;
    }
    
    @Override
    public CompletableFuture<Map<String, String>> asyncLoadAll(Iterable<? extends String> keys, Executor executor) {
        CompletableFuture<Map<String, String>> future = new CompletableFuture<>();
        Iterator<? extends String> it = keys.iterator();
        List<List<String>> splitted = new ArrayList<>();
        List<String> list = new ArrayList<>();
        Map<String, String> map = new HashMap<>();
        int count = 0;
        while (it.hasNext()) {
            list.add(it.next());
            count++;
            if (list.size() == 50) {
                splitted.add(list);
                list = new ArrayList<>();
            }
        }
        if (!list.isEmpty())
            splitted.add(list);
        for (List<String> load : splitted)
            load(load, map, future, count);
        return future;
    }
    
    private void load(List<String> list, Map<String, String> saveTo, CompletableFuture<Map<String, String>> future, int needed) {
        HttpUtils.apiGet("/user/name/" + String.join(",", list), (body, ex) -> {
            synchronized (saveTo) {
                if (future.isCompletedExceptionally())
                    return;
                if (ex != null) {
                    future.completeExceptionally(ex);
                    return;
                }
                for (JsonElement element : new JsonParser().parse(body).getAsJsonArray()) {
                    JsonObject json = element.getAsJsonObject();
                    String name = json.get("username").getAsString();
                    saveTo.put(name, json.get("rank").getAsString());
                }
                for (String name : list)
                    saveTo.putIfAbsent(name, "PLAYER");
                if (needed == saveTo.size())
                    future.complete(saveTo);
            }
        });
    }
}
