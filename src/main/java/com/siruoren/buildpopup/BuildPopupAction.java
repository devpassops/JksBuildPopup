package com.siruoren.buildpopup;

import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * 弹窗即时消息存储（纯数据层，非 UI Action）。
 * <p>
 * 弹窗结果及信息只作为即时消息，不持久化存储。
 * 使用 ConcurrentHashMap 按 jobName 存储最新弹窗结果，
 * 弹窗超过3分钟自动过期，关闭后立即移除。
 * </p>
 */
public class BuildPopupAction {

    private static final Logger LOGGER = Logger.getLogger(BuildPopupAction.class.getName());

    /** 全局即时弹窗存储：jobName -> 最新弹窗结果 */
    private static final ConcurrentHashMap<String, BuildPopupResult> LATEST_POPUPS = new ConcurrentHashMap<>();

    /** 弹窗关闭标记：jobName -> 被关闭的弹窗ID */
    private static final ConcurrentHashMap<String, String> DISMISSED_IDS = new ConcurrentHashMap<>();

    /** 设置指定 Job 的最新弹窗结果 */
    public static void setLatestPopup(String jobName, BuildPopupResult result) {
        LATEST_POPUPS.put(jobName, result);
        DISMISSED_IDS.remove(jobName);
    }

    /** 获取指定 Job 的最新弹窗结果（仅在有效且未关闭时返回） */
    public static BuildPopupResult getLatestPopupResult(String jobName) {
        BuildPopupResult result = LATEST_POPUPS.get(jobName);
        if (result == null) {
            return null;
        }
        if (!result.isStillValid()) {
            LATEST_POPUPS.remove(jobName);
            DISMISSED_IDS.remove(jobName);
            return null;
        }
        String dismissedId = DISMISSED_IDS.get(jobName);
        if (dismissedId != null && dismissedId.equals(result.getId())) {
            return null;
        }
        return result;
    }

    /** 关闭指定 Job 的弹窗 */
    public static void dismissPopup(String jobName, String popupId) {
        BuildPopupResult latest = LATEST_POPUPS.get(jobName);
        if (latest != null && latest.getId().equals(popupId)) {
            DISMISSED_IDS.put(jobName, popupId);
        }
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
