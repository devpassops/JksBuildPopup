package com.siruoren.buildpopup;

import hudson.Extension;
import hudson.model.Action;
import hudson.model.Job;
import jenkins.model.TransientActionFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.logging.Logger;

/**
 * TransientActionFactory：为每个有 BuildPopupJobProperty 的 Job 注入 BuildPopupAction。
 * 使得侧边栏自动显示弹窗入口链接。
 */
@Extension
public class BuildPopupActionFactory extends TransientActionFactory<Job> {

    private static final Logger LOGGER = Logger.getLogger(BuildPopupActionFactory.class.getName());

    @Override
    public Class<Job> type() {
        return Job.class;
    }

    @Override
    public Collection<? extends Action> createFor(Job target) {
        // 兼容 Jenkins 2.277.4: getProperty 返回 JobProperty 需要显式类型检查
        Object prop = target.getProperty(BuildPopupJobProperty.class);
        if (prop instanceof BuildPopupJobProperty && ((BuildPopupJobProperty) prop).isEnabled()) {
            return Collections.singletonList(new BuildPopupAction(target));
        }
        return Collections.emptyList();
    }
}
