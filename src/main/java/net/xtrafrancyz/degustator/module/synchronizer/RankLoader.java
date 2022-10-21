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
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author xtrafrancyz
 */
public class RankLoader implements AsyncCacheLoader<String, VimeApiPlayer> {
    @Override
    public CompletableFuture<VimeApiPlayer> asyncLoad(String username, Executor executor) {
        CompletableFuture<VimeApiPlayer> future = new CompletableFuture<>();
        HttpUtils.apiGet("/user/name/" + username, (body, ex) -> {
            if (ex != null) {
                future.completeExceptionally(ex);
            } else {
                try {
                    JsonArray arr = JsonParser.parseString(body).getAsJsonArray();
                    if (arr.size() == 1) {
                        JsonObject json = arr.get(0).getAsJsonObject();
                        future.complete(new VimeApiPlayer(json.get("username").getAsString(), json.get("rank").getAsString()));
                    } else {
                        future.complete(new VimeApiPlayer(username, "PLAYER"));
                    }
                } catch (Exception ex0) {
                    future.completeExceptionally(ex0);
                }
            }
        });
        return future;
    }
    
    @Override
    public CompletableFuture<Map<String, VimeApiPlayer>> asyncLoadAll(Iterable<? extends String> keys, Executor executor) {
        CompletableFuture<Map<String, VimeApiPlayer>> future = new CompletableFuture<>();
        Iterator<? extends String> it = keys.iterator();
        List<List<String>> splitted = new ArrayList<>();
        List<String> list = new ArrayList<>();
        Map<String, VimeApiPlayer> map = new HashMap<>();
        while (it.hasNext()) {
            list.add(it.next());
            if (list.size() == 50) {
                splitted.add(list);
                list = new ArrayList<>();
            }
        }
        if (!list.isEmpty())
            splitted.add(list);
        AtomicInteger loads = new AtomicInteger(splitted.size());
        for (List<String> load : splitted)
            load(load, map, future, loads);
        return future;
    }
    
    private void load(List<String> list, Map<String, VimeApiPlayer> saveTo, CompletableFuture<Map<String, VimeApiPlayer>> future, AtomicInteger loads) {
        HttpUtils.apiGet("/user/name/" + String.join(",", list), (body, ex) -> {
            synchronized (saveTo) {
                try {
                    if (future.isCompletedExceptionally())
                        return;
                    if (ex != null) {
                        future.completeExceptionally(ex);
                        return;
                    }
                    for (JsonElement element : JsonParser.parseString(body).getAsJsonArray()) {
                        JsonObject json = element.getAsJsonObject();
                        String name = json.get("username").getAsString();
                        saveTo.put(name.toLowerCase(), new VimeApiPlayer(name, json.get("rank").getAsString()));
                    }
                    for (String name : list)
                        saveTo.putIfAbsent(name, new VimeApiPlayer(name, "PLAYER"));
                } finally {
                    if (loads.decrementAndGet() == 0)
                        future.complete(saveTo);
                }
            }
        });
    }
}
