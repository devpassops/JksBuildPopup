package com.siruoren.buildpopup;

import hudson.model.Action;
import hudson.model.Job;

import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * 弹窗 Action：即时弹窗消息管理，作为侧边栏入口。
 * <p>
 * 弹窗结果及信息只作为即时消息，不持久化存储。
 * 使用 ConcurrentHashMap 按 jobName 存储最新弹窗结果，
 * 弹窗超过3分钟自动过期，关闭后立即移除。
 * </p>
 */
public class BuildPopupAction implements Action {

    private static final Logger LOGGER = Logger.getLogger(BuildPopupAction.class.getName());

    private final Job<?, ?> job;

    /** 全局即时弹窗存储：jobName -> 最新弹窗结果 */
    private static final ConcurrentHashMap<String, BuildPopupResult> LATEST_POPUPS = new ConcurrentHashMap<>();

    /** 弹窗关闭标记：jobName -> 被关闭的弹窗ID */
    private static final ConcurrentHashMap<String, String> DISMISSED_IDS = new ConcurrentHashMap<>();

    public BuildPopupAction(Job<?, ?> job) {
        this.job = job;
    }

    @Override
    public String getIconFileName() {
        return "symbol-notification";
    }

    @Override
    public String getDisplayName() {
        return Messages.BuildPopupAction_DisplayName();
    }

    @Override
    public String getUrlName() {
        return "build-popup";
    }

    public Job<?, ?> getJob() {
        return job;
    }

    /** 设置当前 Job 的最新弹窗结果 */
    public static void setLatestPopup(String jobName, BuildPopupResult result) {
        LATEST_POPUPS.put(jobName, result);
        // 新弹窗自动清除旧的关闭标记
        DISMISSED_IDS.remove(jobName);
    }

    /** 获取当前 Job 的最新弹窗结果（仅在有效且未关闭时返回） */
    public BuildPopupResult getLatestPopupResult() {
        return getLatestPopupResult(job.getFullName());
    }

    /** 静态方法：获取指定 Job 的最新弹窗结果 */
    public static BuildPopupResult getLatestPopupResult(String jobName) {
        BuildPopupResult result = LATEST_POPUPS.get(jobName);
        if (result == null) {
            return null;
        }
        // 过期自动清除
        if (!result.isStillValid()) {
            LATEST_POPUPS.remove(jobName);
            DISMISSED_IDS.remove(jobName);
            return null;
        }
        // 已关闭
        String dismissedId = DISMISSED_IDS.get(jobName);
        if (dismissedId != null && dismissedId.equals(result.getId())) {
            return null;
        }
        return result;
    }

    /** 是否有待查看的弹窗 */
    public boolean hasUnreadPopup() {
        return getLatestPopupResult() != null;
    }

    /** 关闭当前弹窗 */
    public void doDismissPopup() {
        BuildPopupResult latest = LATEST_POPUPS.get(job.getFullName());
        if (latest != null) {
            DISMISSED_IDS.put(job.getFullName(), latest.getId());
        }
    }

    /** 清除当前 Job 的弹窗（完全移除） */
    public void doClearPopup() {
        LATEST_POPUPS.remove(job.getFullName());
        DISMISSED_IDS.remove(job.getFullName());
    }

    /** 清理所有过期弹窗（定期调用防内存泄露） */
    public static void cleanupExpired() {
        LATEST_POPUPS.entrySet().removeIf(entry -> {
            if (!entry.getValue().isStillValid()) {
                DISMISSED_IDS.remove(entry.getKey());
                return true;
            }
            return false;
        });
    }
}
