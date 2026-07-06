package com.siruoren.buildpopup;

import hudson.Extension;
import hudson.model.queue.QueueListener;
import hudson.model.Queue.WaitingItem;
import hudson.model.Job;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 构建队列监听器：仅做日志记录。
 * <p>
 * 弹窗拦截已由前端 JavaScript（Active Choices 风格表单拦截）处理，
 * 此监听器不再取消构建，避免影响 API/CLI/调度触发的构建。
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

        Object rawProp = job.getProperty(BuildPopupJobProperty.class);
        if (!(rawProp instanceof BuildPopupJobProperty)) {
            return;
        }
        BuildPopupJobProperty prop = (BuildPopupJobProperty) rawProp;
        if (!prop.isEnabled()) {
            return;
        }

        LOGGER.log(Level.FINE, "BuildPopup: job {0} entered queue (UI interception handles popup)",
            job.getFullName());
    }
}
