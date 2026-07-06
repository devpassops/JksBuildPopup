package com.siruoren.buildpopup;

import hudson.Extension;
import jenkins.model.GlobalConfiguration;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;

import java.util.logging.Logger;

/**
 * 全局配置：线程池大小、全局超时、全局开关等。
 */
@Extension
public class BuildPopupGlobalConfiguration extends GlobalConfiguration {

    private static final Logger LOGGER = Logger.getLogger(BuildPopupGlobalConfiguration.class.getName());

    /** 全局是否启用插件功能 */
    private boolean globallyEnabled;

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
        // 初始化默认值
        if (threadPoolCoreSize <= 0) threadPoolCoreSize = 4;
        if (threadPoolMaxSize <= 0) threadPoolMaxSize = 16;
        if (threadPoolQueueCapacity <= 0) threadPoolQueueCapacity = 100;
        if (threadPoolKeepAliveSeconds <= 0) threadPoolKeepAliveSeconds = 60;
        if (globalScriptTimeout <= 0) globalScriptTimeout = 60;
        if (maxConcurrentPerJob <= 0) maxConcurrentPerJob = 2;
        globallyEnabled = true;
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

    @Override
    public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
        globallyEnabled = formData.optBoolean("globallyEnabled", true);
        threadPoolCoreSize = Math.max(1, formData.optInt("threadPoolCoreSize", 4));
        threadPoolMaxSize = Math.max(threadPoolCoreSize, formData.optInt("threadPoolMaxSize", 16));
        threadPoolQueueCapacity = Math.max(10, formData.optInt("threadPoolQueueCapacity", 100));
        threadPoolKeepAliveSeconds = Math.max(10, formData.optInt("threadPoolKeepAliveSeconds", 60));
        globalScriptTimeout = Math.max(5, formData.optInt("globalScriptTimeout", 60));
        maxConcurrentPerJob = Math.max(1, formData.optInt("maxConcurrentPerJob", 2));
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
