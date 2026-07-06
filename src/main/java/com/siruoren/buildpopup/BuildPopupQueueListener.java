package com.siruoren.buildpopup;

import hudson.Extension;
import hudson.model.Queue;
import hudson.model.Queue.WaitingItem;
import hudson.model.Job;
import hudson.model.ParameterValue;
import hudson.model.ParametersAction;
import hudson.model.queue.QueueListener;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import jenkins.model.Jenkins;

/**
 * 构建队列监听器：在任务进入构建队列时，执行 Groovy 脚本判断。
 * <p>
 * 核心设计：
 * - 条件满足构建：直接放行，不等待弹窗结果（弹窗仅做提醒）
 * - 条件不满足：将此 WaitingItem 从队列中取消（只弹窗不构建）
 *   但不影响该 Job 被其他页面触发构建或被定时调度触发
 * - 弹窗结果作为即时消息存储在 BuildPopupAction 中，不持久化
 * </p>
 */
@Extension
public class BuildPopupQueueListener extends QueueListener {

    private static final Logger LOGGER = Logger.getLogger(BuildPopupQueueListener.class.getName());

    @Override
    public void onEnterWaiting(WaitingItem wi) {
        if (!(wi.task instanceof Job)) {
            return;
        }
        Job<?, ?> job = (Job<?, ?>) wi.task;

        BuildPopupJobProperty prop = (BuildPopupJobProperty) job.getProperty(BuildPopupJobProperty.class);
        if (prop == null || !prop.isEnabled()) {
            return;
        }
        BuildPopupGlobalConfiguration globalConfig = BuildPopupGlobalConfiguration.get();
        if (!globalConfig.isGloballyEnabled()) {
            return;
        }

        String jobName = job.getFullName();
        LOGGER.log(Level.INFO, "BuildPopup checking job {0} before build", jobName);

        try {
            // 读取任务已配置的参数
            Map<String, String> envVars = new HashMap<>();
            Map<String, String> params = extractParams(wi);

            // 执行 Groovy 脚本判断
            BuildPopupResult result = BuildPopupService.getInstance().executeGroovy(job, null, envVars, params);

            if (result.isError()) {
                LOGGER.log(Level.WARNING, "BuildPopup Groovy script error for job {0}: {1}",
                    new Object[]{jobName, result.getErrorMessage()});
                // 脚本出错时，默认不阻断构建，但显示错误弹窗
                result.setBlockBuild(false);
                result.setShowPopup(true);
            }

            // 存储即时弹窗结果
            if (result.isShowPopup()) {
                BuildPopupAction.setLatestPopup(jobName, result);
            }

            // 如果条件不满足构建（blockBuild=true），将此 WaitingItem 从队列中取消
            // 不影响该 Job 被其他页面触发构建或被定时调度触发
            if (result.isBlockBuild()) {
                LOGGER.log(Level.INFO, "BuildPopup blocking build for job {0}, showing popup only", jobName);
                Jenkins.get().getQueue().cancel(wi);
            } else {
                LOGGER.log(Level.INFO, "BuildPopup allowing build for job {0}", jobName);
                // 条件满足，直接构建，弹窗只做提醒（如果需要弹窗）
            }

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "BuildPopup check failed for job " + jobName, e);
        }
    }

    /**
     * 从 WaitingItem 的 Actions 中提取参数
     */
    private Map<String, String> extractParams(Queue.Item item) {
        Map<String, String> params = new HashMap<>();
        ParametersAction paramsAction = item.getAction(ParametersAction.class);
        if (paramsAction != null) {
            for (ParameterValue pv : paramsAction.getParameters()) {
                if (pv.getValue() != null) {
                    params.put(pv.getName(), pv.getValue().toString());
                }
            }
        }
        return params;
    }
}
