package com.siruoren.buildpopup;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import hudson.init.InitMilestone;
import hudson.init.Initializer;

/**
 * 统一管理线程池，防止内存泄露。
 * 使用 ThreadPoolExecutor + 有界队列 + CallerRunsPolicy，
 * 保证大量任务并发使用时不超出线程池限制。
 * Jenkins 关闭时优雅关闭线程池。
 */
public class BuildPopupThreadPool {

    private static final Logger LOGGER = Logger.getLogger(BuildPopupThreadPool.class.getName());

    private static volatile BuildPopupThreadPool INSTANCE;

    private volatile ThreadPoolExecutor executor;

    private BuildPopupThreadPool() {
        initialize();
    }

    public static BuildPopupThreadPool getInstance() {
        if (INSTANCE == null) {
            synchronized (BuildPopupThreadPool.class) {
                if (INSTANCE == null) {
                    INSTANCE = new BuildPopupThreadPool();
                }
            }
        }
        return INSTANCE;
    }

    private void initialize() {
        BuildPopupGlobalConfiguration config = BuildPopupGlobalConfiguration.get();
        int coreSize = 4;
        int maxSize = 16;
        int queueCapacity = 100;
        int keepAlive = 60;

        if (config != null) {
            coreSize = config.getThreadPoolCoreSize();
            maxSize = config.getThreadPoolMaxSize();
            queueCapacity = config.getThreadPoolQueueCapacity();
            keepAlive = config.getThreadPoolKeepAliveSeconds();
        }

        executor = new ThreadPoolExecutor(
            coreSize, maxSize, keepAlive, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(queueCapacity),
            new BuildPopupThreadFactory(),
            new ThreadPoolExecutor.CallerRunsPolicy()
        );
        executor.allowCoreThreadTimeOut(true);
    }

    /** 自定义线程工厂，统一命名且为守护线程 */
    private static class BuildPopupThreadFactory implements ThreadFactory {
        private final AtomicInteger counter = new AtomicInteger(0);

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "BuildPopup-Worker-" + counter.incrementAndGet());
            t.setDaemon(true);
            return t;
        }
    }

    /** 重新初始化线程池（配置更新时调用） */
    public synchronized void reinitialize() {
        ThreadPoolExecutor old = executor;
        initialize();
        // 优雅关闭旧线程池
        if (old != null && !old.isShutdown()) {
            old.shutdown();
            try {
                if (!old.awaitTermination(10, TimeUnit.SECONDS)) {
                    old.shutdownNow();
                    LOGGER.warning("Old thread pool did not terminate within 10 seconds, forced shutdown");
                }
            } catch (InterruptedException e) {
                old.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    /** 提交有返回值的任务到线程池 */
    public <T> Future<T> submit(Callable<T> task) {
        return executor.submit(task);
    }

    /** 获取线程池活跃线程数 */
    public int getActiveCount() {
        return executor.getActiveCount();
    }

    /** 获取线程池队列中等待的任务数 */
    public int getQueueSize() {
        return executor.getQueue().size();
    }

    /** 获取线程池已完成任务总数 */
    public long getCompletedTaskCount() {
        return executor.getCompletedTaskCount();
    }

    /**
     * Jenkins 关闭时优雅关闭线程池，防止内存泄露
     * 使用 COMPLETED 而非 JENKINS_STARTED（兼容 2.277.4）
     */
    @Initializer(after = InitMilestone.COMPLETED)
    public static void shutdownOnJenkinsStop() {
        // 注册 JVM 关闭钩子，确保 Jenkins 停止时线程池关闭
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOGGER.info("JVM shutting down, closing BuildPopup thread pool");
            if (INSTANCE != null && INSTANCE.executor != null && !INSTANCE.executor.isShutdown()) {
                INSTANCE.executor.shutdown();
                try {
                    if (!INSTANCE.executor.awaitTermination(30, TimeUnit.SECONDS)) {
                        INSTANCE.executor.shutdownNow();
                        LOGGER.warning("Thread pool did not terminate, forced shutdown");
                    }
                } catch (InterruptedException e) {
                    INSTANCE.executor.shutdownNow();
                    Thread.currentThread().interrupt();
                }
            }
        }));
        // 启动定期清理任务
        startCleanupTask();
    }

    /**
     * 定期清理过期弹窗，防止内存泄露
     */
    private static void startCleanupTask() {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "BuildPopup-Cleanup");
            t.setDaemon(true);
            return t;
        });
        // 每60秒清理一次过期弹窗
        scheduler.scheduleAtFixedRate(BuildPopupAction::cleanupExpired, 60, 60, TimeUnit.SECONDS);
        LOGGER.info("BuildPopup cleanup scheduler started");
    }
}
