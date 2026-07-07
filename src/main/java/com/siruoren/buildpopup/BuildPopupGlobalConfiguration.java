package com.siruoren.buildpopup;

import hudson.Extension;
import jenkins.model.GlobalConfiguration;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;

import java.util.logging.Logger;

/**
 * 全局配置：线程池大小、全局超时、全局开关等。
 * 线程池和队列默认值根据服务器资源（CPU核数）自动计算。
 */
@Extension
public class BuildPopupGlobalConfiguration extends GlobalConfiguration {

    private static final Logger LOGGER = Logger.getLogger(BuildPopupGlobalConfiguration.class.getName());

    /** 服务器可用处理器核数 */
    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();

    /** 根据服务器资源自动计算的默认值 */
    static final int DEFAULT_CORE_SIZE = Math.max(2, CPU_COUNT);
    static final int DEFAULT_MAX_SIZE = Math.max(4, CPU_COUNT * 4);
    static final int DEFAULT_QUEUE_CAPACITY = Math.max(50, CPU_COUNT * 25);
    static final int DEFAULT_KEEP_ALIVE = 60;
    static final int DEFAULT_SCRIPT_TIMEOUT = 60;
    static final int DEFAULT_MAX_CONCURRENT_PER_JOB = 20;

    /** 全局是否启用插件功能（默认 true） */
    private boolean globallyEnabled = true;

    /** 线程池核心大小 */
    private int threadPoolCoreSize;

    /** 线程池最大大小 */
    private int threadPoolMaxSize;

    /** 线程池队列容量 */
    private int threadPoolQueueCapacity;

    /** 线程池空闲线程存活时间（秒） */
    private int threadPoolKeepAliveSeconds;

    /** 全局脚本执行超时时间（秒） */
    private int globalScriptTimeout;

    /** 单个Job的Groovy脚本最大并发执行数 */
    private int maxConcurrentPerJob;

    public BuildPopupGlobalConfiguration() {
        load();
        // 初始化默认值（仅当未从配置文件加载时设置）
        // 线程池参数根据服务器资源自动计算
        if (threadPoolCoreSize <= 0) threadPoolCoreSize = DEFAULT_CORE_SIZE;
        if (threadPoolMaxSize <= 0) threadPoolMaxSize = DEFAULT_MAX_SIZE;
        if (threadPoolQueueCapacity <= 0) threadPoolQueueCapacity = DEFAULT_QUEUE_CAPACITY;
        if (threadPoolKeepAliveSeconds <= 0) threadPoolKeepAliveSeconds = DEFAULT_KEEP_ALIVE;
        if (globalScriptTimeout <= 0) globalScriptTimeout = DEFAULT_SCRIPT_TIMEOUT;
        if (maxConcurrentPerJob <= 0) maxConcurrentPerJob = DEFAULT_MAX_CONCURRENT_PER_JOB;

        LOGGER.info("BuildPopup auto-detected CPU cores: " + CPU_COUNT
            + ", defaults: core=" + DEFAULT_CORE_SIZE
            + ", max=" + DEFAULT_MAX_SIZE
            + ", queue=" + DEFAULT_QUEUE_CAPACITY
            + ", maxConcurrentPerJob=" + DEFAULT_MAX_CONCURRENT_PER_JOB);
    }

    public boolean isGloballyEnabled() {
        return globallyEnabled;
    }

    public void setGloballyEnabled(boolean globallyEnabled) {
        this.globallyEnabled = globallyEnabled;
    }

    public int getThreadPoolCoreSize() {
        return threadPoolCoreSize;
    }

    public void setThreadPoolCoreSize(int threadPoolCoreSize) {
        this.threadPoolCoreSize = threadPoolCoreSize;
    }

    public int getThreadPoolMaxSize() {
        return threadPoolMaxSize;
    }

    public void setThreadPoolMaxSize(int threadPoolMaxSize) {
        this.threadPoolMaxSize = threadPoolMaxSize;
    }

    public int getThreadPoolQueueCapacity() {
        return threadPoolQueueCapacity;
    }

    public void setThreadPoolQueueCapacity(int threadPoolQueueCapacity) {
        this.threadPoolQueueCapacity = threadPoolQueueCapacity;
    }

    public int getThreadPoolKeepAliveSeconds() {
        return threadPoolKeepAliveSeconds;
    }

    public void setThreadPoolKeepAliveSeconds(int threadPoolKeepAliveSeconds) {
        this.threadPoolKeepAliveSeconds = threadPoolKeepAliveSeconds;
    }

    public int getGlobalScriptTimeout() {
        return globalScriptTimeout;
    }

    public void setGlobalScriptTimeout(int globalScriptTimeout) {
        this.globalScriptTimeout = globalScriptTimeout;
    }

    public int getMaxConcurrentPerJob() {
        return maxConcurrentPerJob;
    }

    public void setMaxConcurrentPerJob(int maxConcurrentPerJob) {
        this.maxConcurrentPerJob = maxConcurrentPerJob;
    }

    /** 获取自动计算的默认核心线程数 */
    public int getAutoCoreSize() {
        return DEFAULT_CORE_SIZE;
    }

    /** 获取自动计算的默认最大线程数 */
    public int getAutoMaxSize() {
        return DEFAULT_MAX_SIZE;
    }

    /** 获取自动计算的默认队列容量 */
    public int getAutoQueueCapacity() {
        return DEFAULT_QUEUE_CAPACITY;
    }

    /** 获取CPU核数 */
    public int getCpuCount() {
        return CPU_COUNT;
    }

    @Override
    public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
        globallyEnabled = formData.optBoolean("globallyEnabled", true);
        threadPoolCoreSize = Math.max(1, formData.optInt("threadPoolCoreSize", DEFAULT_CORE_SIZE));
        threadPoolMaxSize = Math.max(threadPoolCoreSize, formData.optInt("threadPoolMaxSize", DEFAULT_MAX_SIZE));
        threadPoolQueueCapacity = Math.max(10, formData.optInt("threadPoolQueueCapacity", DEFAULT_QUEUE_CAPACITY));
        threadPoolKeepAliveSeconds = Math.max(10, formData.optInt("threadPoolKeepAliveSeconds", DEFAULT_KEEP_ALIVE));
        globalScriptTimeout = Math.max(5, formData.optInt("globalScriptTimeout", DEFAULT_SCRIPT_TIMEOUT));
        maxConcurrentPerJob = Math.max(1, formData.optInt("maxConcurrentPerJob", DEFAULT_MAX_CONCURRENT_PER_JOB));
        save();

        // 重新初始化线程池
        BuildPopupThreadPool.getInstance().reinitialize();
        return true;
    }

    /** 获取全局配置单例 */
    public static BuildPopupGlobalConfiguration get() {
        return GlobalConfiguration.all().get(BuildPopupGlobalConfiguration.class);
    }
}
