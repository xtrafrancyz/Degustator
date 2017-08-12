package net.xtrafrancyz.degustator;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.RunnableScheduledFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by xtrafrancyz on 11.10.2015.
 */
public class Scheduler {
    private static ScheduledExecutorService threadPool;
    private static final AtomicInteger ids = new AtomicInteger(1);
    static final ConcurrentHashMap<Integer, Task> runners = new ConcurrentHashMap<>();
    
    static void init(int threads) {
        threadPool = Executors.newScheduledThreadPool(threads);
    }
    
    public static int scheduleAtFixedRate(Runnable runnable, long initialDelay, long delay, TimeUnit timeUnits) {
        Task task = createTask(runnable);
        task.future = threadPool.scheduleAtFixedRate(task, initialDelay, delay, timeUnits);
        return task.id;
    }
    
    public static int scheduleWithFixedDelay(Runnable runnable, long initialDelay, long delay, TimeUnit timeUnits) {
        Task task = createTask(runnable);
        task.future = threadPool.scheduleWithFixedDelay(task, initialDelay, delay, timeUnits);
        return task.id;
    }
    
    public static int schedule(Runnable runnable, long delay, TimeUnit timeUnits) {
        Task task = createTask(runnable);
        task.future = threadPool.schedule(task, delay, timeUnits);
        return task.id;
    }
    
    public static void cancelTask(int id) {
        Task task = runners.remove(id);
        if (task != null)
            task.future.cancel(false);
    }
    
    public static void shutdown() {
        runners.clear();
        threadPool.shutdown();
    }
    
    private static Task createTask(Runnable runnable) {
        Task task = new Task(runnable);
        task.id = ids.getAndIncrement();
        runners.put(task.id, task);
        return task;
    }
    
    static class Task implements Runnable {
        int id;
        ScheduledFuture<?> future;
        Runnable runnable;
        
        public Task(Runnable runnable) {
            this.runnable = runnable;
        }
        
        @SuppressWarnings("ReturnInsideFinallyBlock")
        @Override
        public void run() {
            try {
                runnable.run();
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            } finally {
                if (future instanceof RunnableScheduledFuture)
                    if (((RunnableScheduledFuture) future).isPeriodic())
                        return;
                runners.remove(id);
            }
        }
    }
}
