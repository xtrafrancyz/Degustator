package net.xtrafrancyz.degustator.module.synchronizer;

import com.github.benmanes.caffeine.cache.AsyncCacheLoader;
import discord4j.common.util.Snowflake;

import net.xtrafrancyz.degustator.Degustator;
import net.xtrafrancyz.degustator.mysql.SelectResult;

import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * @author xtrafrancyz
 */
public class UsernameLoader implements AsyncCacheLoader<Snowflake, String> {
    private final Degustator degustator;
    
    public UsernameLoader(Degustator degustator) {
        this.degustator = degustator;
    }
    
    @Override
    public CompletableFuture<String> asyncLoad(Snowflake id, Executor executor) {
        CompletableFuture<String> future = new CompletableFuture<>();
        executor.execute(() -> {
            try {
                SelectResult result = degustator.mysql.select("SELECT username FROM linked WHERE id = ?", ps ->
                    ps.setLong(1, id.asLong())
                );
                if (result.isEmpty())
                    future.complete("");
                else
                    future.complete(result.getFirst().getString(0));
            } catch (SQLException e) {
                e.printStackTrace();
                future.complete("");
            }
        });
        return future;
    }
}
