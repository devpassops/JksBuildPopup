package com.siruoren.buildpopup;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import hudson.model.Job;
import hudson.model.Run;
import jenkins.model.Jenkins;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 核心服务类：执行 Groovy 脚本、管理并发控制、返回判断结果。
 * <p>
 * 设计要点：
 * 1. 使用 Jenkins 内置 Groovy 支持（无需外部依赖）
 * 2. 组件独立调度执行 Groovy 命令，使用线程池统一控制并发
 * 3. 每个 Job 的并发数受全局配置 maxConcurrentPerJob 限制
 * 4. Groovy 脚本执行有超时控制，防止脚本死循环导致线程泄露
 * 5. 脚本返回 Map 参数名固定：blockBuild, showPopup, popupContent
 * 6. 参数不定义时默认为 true（blockBuild=true, showPopup=true）
 * </p>
 */
public class BuildPopupService {

    private static final Logger LOGGER = Logger.getLogger(BuildPopupService.class.getName());

    private static volatile BuildPopupService INSTANCE;

    /** 固定参数名 */
    static final String KEY_BLOCK_BUILD = "blockBuild";
    static final String KEY_SHOW_POPUP = "showPopup";
    static final String KEY_POPUP_CONTENT = "popupContent";

    /** 每个 Job 正在执行 Groovy 的计数器 */
    private final ConcurrentHashMap<String, AtomicInteger> jobConcurrencyMap = new ConcurrentHashMap<>();

    private BuildPopupService() {}

    public static BuildPopupService getInstance() {
        if (INSTANCE == null) {
            synchronized (BuildPopupService.class) {
                if (INSTANCE == null) {
                    INSTANCE = new BuildPopupService();
                }
            }
        }
        return INSTANCE;
    }

    public BuildPopupResult executeGroovy(Job<?, ?> job, Run<?, ?> run,
                                           Map<String, String> envVars,
                                           Map<String, String> params) {
        BuildPopupGlobalConfiguration globalConfig = BuildPopupGlobalConfiguration.get();

        if (!globalConfig.isGloballyEnabled()) {
            return BuildPopupResult.success(false, false, "");
        }

        Object rawProp = job.getProperty(BuildPopupJobProperty.class);
        if (!(rawProp instanceof BuildPopupJobProperty)) {
            return BuildPopupResult.success(false, false, "");
        }
        BuildPopupJobProperty prop = (BuildPopupJobProperty) rawProp;
        if (!prop.isEnabled()) {
            return BuildPopupResult.success(false, false, "");
        }

        String jobName = job.getFullName();
        String groovyScript = prop.getGroovyScript();
        if (groovyScript == null || groovyScript.trim().isEmpty()) {
            return BuildPopupResult.success(false, false, "");
        }

        AtomicInteger concurrency = jobConcurrencyMap.computeIfAbsent(jobName, k -> new AtomicInteger(0));
        int maxConcurrent = globalConfig.getMaxConcurrentPerJob();
        if (concurrency.incrementAndGet() > maxConcurrent) {
            concurrency.decrementAndGet();
            LOGGER.log(Level.WARNING, "Job {0} exceeded max concurrent Groovy executions ({1}), skipping",
                new Object[]{jobName, maxConcurrent});
            return BuildPopupResult.error(Messages.BuildPopupService_concurrencyExceeded(jobName, maxConcurrent));
        }

        try {
            int timeoutSeconds = prop.getScriptTimeout() > 0 ? prop.getScriptTimeout() : globalConfig.getGlobalScriptTimeout();
            if (timeoutSeconds <= 0) {
                timeoutSeconds = 30;
            }

            Future<BuildPopupResult> future = BuildPopupThreadPool.getInstance().submit(
                new GroovyExecutionTask(job, run, envVars, params)
            );

            try {
                return future.get(timeoutSeconds, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                future.cancel(true);
                LOGGER.log(Level.WARNING, "Groovy script execution timed out for job {0} after {1}s",
                    new Object[]{jobName, timeoutSeconds});
                return BuildPopupResult.error(Messages.BuildPopupService_scriptTimeout(jobName, timeoutSeconds));
            } catch (ExecutionException e) {
                LOGGER.log(Level.WARNING, "Groovy script execution failed for job " + jobName, e.getCause());
                return BuildPopupResult.error(Messages.BuildPopupService_scriptError(jobName, e.getCause().getMessage()));
            } catch (InterruptedException e) {
                future.cancel(true);
                Thread.currentThread().interrupt();
                return BuildPopupResult.error(Messages.BuildPopupService_interrupted(jobName));
            }
        } finally {
            concurrency.decrementAndGet();
            if (concurrency.get() == 0) {
                jobConcurrencyMap.remove(jobName);
            }
        }
    }

    /**
     * Groovy 脚本执行任务
     * 参数名固定：blockBuild, showPopup, popupContent
     * 未定义的参数默认为 true
     */
    private static class GroovyExecutionTask implements Callable<BuildPopupResult> {
        private final Job<?, ?> job;
        private final Run<?, ?> run;
        private final Map<String, String> envVars;
        private final Map<String, String> params;

        GroovyExecutionTask(Job<?, ?> job, Run<?, ?> run,
                            Map<String, String> envVars,
                            Map<String, String> params) {
            this.job = job;
            this.run = run;
            this.envVars = envVars != null ? envVars : new HashMap<>();
            this.params = params != null ? params : new HashMap<>();
        }

        @Override
        public BuildPopupResult call() {
            long startTime = System.currentTimeMillis();
            try {
                Binding binding = new Binding();
                binding.setVariable("job", job);
                binding.setVariable("jenkins", Jenkins.getInstanceOrNull());
                binding.setVariable("env", envVars);
                binding.setVariable("params", params);
                if (run != null) {
                    binding.setVariable("build", run);
                    binding.setVariable("currentBuild", run);
                }

                GroovyShell shell = new GroovyShell(binding);
                Object scriptResult = shell.evaluate(job.getProperty(BuildPopupJobProperty.class) instanceof BuildPopupJobProperty
                    ? ((BuildPopupJobProperty) job.getProperty(BuildPopupJobProperty.class)).getGroovyScript() : "");

                long executionTime = System.currentTimeMillis() - startTime;
                return parseScriptResult(scriptResult, executionTime);

            } catch (Exception e) {
                long executionTime = System.currentTimeMillis() - startTime;
                LOGGER.log(Level.WARNING, "Groovy script execution exception for job " + job.getFullName(), e);
                BuildPopupResult result = BuildPopupResult.error(e.getMessage());
                result.setExecutionTimeMs(executionTime);
                return result;
            }
        }

        /**
         * 解析 Groovy 脚本返回结果。
         * 参数名固定：blockBuild, showPopup, popupContent
         * 未定义的参数默认为 true
         */
        private BuildPopupResult parseScriptResult(Object scriptResult, long executionTime) {
            BuildPopupResult result = new BuildPopupResult();
            result.setExecutionTimeMs(executionTime);

            if (scriptResult instanceof Map) {
                Map<?, ?> resultMap = (Map<?, ?>) scriptResult;

                // 参数不定义默认为 true（showPopup）、false（blockBuild）
                Object blockVal = resultMap.get(KEY_BLOCK_BUILD);
                Object showVal = resultMap.get(KEY_SHOW_POPUP);
                Object contentVal = resultMap.get(KEY_POPUP_CONTENT);

                result.setBlockBuild(toBoolean(blockVal, false));
                result.setShowPopup(toBoolean(showVal, true));
                result.setPopupContent(contentVal != null ? contentVal.toString() : "");
            } else if (scriptResult instanceof Boolean) {
                // Boolean 返回值直接作为 blockBuild 判断，showPopup 默认跟随
                result.setBlockBuild((Boolean) scriptResult);
                result.setShowPopup((Boolean) scriptResult);
                result.setPopupContent(Messages.BuildPopupService_defaultPopupContent(job.getFullName()));
            } else if (scriptResult != null) {
                // 其他类型转为字符串作为弹窗内容，默认阻断
                result.setPopupContent(scriptResult.toString());
                result.setBlockBuild(true);
                result.setShowPopup(true);
            } else {
                // 返回 null，默认弹窗但不阻断
                result.setBlockBuild(false);
                result.setShowPopup(true);
                result.setPopupContent(Messages.BuildPopupService_defaultPopupContent(job.getFullName()));
            }

            return result;
        }

        private boolean toBoolean(Object value, boolean defaultValue) {
            if (value == null) return defaultValue;
            if (value instanceof Boolean) return (Boolean) value;
            if (value instanceof String) return Boolean.parseBoolean((String) value);
            return defaultValue;
        }
    }
}
