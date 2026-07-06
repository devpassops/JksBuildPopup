package com.siruoren.buildpopup;

import hudson.Extension;
import hudson.model.Job;
import hudson.model.JobProperty;
import hudson.model.JobPropertyDescriptor;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import java.util.logging.Logger;

/**
 * Job 级别配置属性，在任务配置页面中可选启用。
 * 配置 Groovy 脚本和脚本超时时间。
 * <p>
 * Groovy 脚本返回 Map 的固定参数名（写在描述信息中）：
 * - blockBuild (Boolean): 是否阻断构建，默认 false
 * - showPopup (Boolean): 是否显示弹窗，默认 false
 * - popupContent (String): 弹窗内容
 * - popupTitle (String): 弹窗标题，默认 "Build Notification"
 * <p>
 * 参数不定义时默认为 blockBuild=false, showPopup=false
 * </p>
 */
public class BuildPopupJobProperty extends JobProperty<Job<?, ?>> {

    private static final Logger LOGGER = Logger.getLogger(BuildPopupJobProperty.class.getName());

    /** 是否启用弹窗检查 */
    private boolean enabled;

    /** Groovy 脚本内容，用于执行判断逻辑 */
    private String groovyScript;

    /** Groovy 脚本执行超时时间（秒），默认 30 */
    private int scriptTimeout;

    @DataBoundConstructor
    public BuildPopupJobProperty(boolean enabled) {
        this.enabled = enabled;
        this.groovyScript = "";
        this.scriptTimeout = 30;
    }

    @DataBoundSetter
    public void setGroovyScript(String groovyScript) {
        this.groovyScript = groovyScript;
    }

    public String getGroovyScript() {
        return groovyScript;
    }

    @DataBoundSetter
    public void setScriptTimeout(int scriptTimeout) {
        this.scriptTimeout = scriptTimeout;
    }

    public int getScriptTimeout() {
        return scriptTimeout;
    }

    public boolean isEnabled() {
        return enabled;
    }

    @DataBoundSetter
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Extension
    @Symbol("buildPopup")
    public static final class DescriptorImpl extends JobPropertyDescriptor {

        @Override
        public String getDisplayName() {
            return Messages.BuildPopupJobProperty_DisplayName();
        }

        @Override
        public boolean isApplicable(Class<? extends Job> jobType) {
            return true;
        }

        public String getDefaultGroovyScript() {
            return "// Build Popup Groovy Script\n"
                + "// Bindings: job, jenkins, env, params\n"
                + "// Build parameters are available as direct variables (e.g. DEPLOY_ENV, BRANCH)\n"
                + "//   also accessible via params map: params.get('DEPLOY_ENV')\n"
                + "// MUST return a Map: return [blockBuild: true, showPopup: true, popupContent: 'msg', popupTitle: 'title']\n"
                + "// Keys:\n"
                + "//   blockBuild   - Boolean, true=block build (only show popup), default=false\n"
                + "//   showPopup    - Boolean, true=show popup, default=false\n"
                + "//   popupContent - String, popup message\n"
                + "//   popupTitle   - String, popup title, default='Build Notification'\n"
                + "// Use Map literal [key: value], NOT method call like popupContent('xxx')\n"
                + "\n"
                + "if (job.isBuilding()) {\n"
                + "    return [blockBuild: true, showPopup: true, popupContent: \"Job ${job.name} is already building!\"]\n"
                + "} else {\n"
                + "    return [blockBuild: false, showPopup: false, popupContent: '']\n"
                + "}\n";
        }

        public int getDefaultScriptTimeout() {
            return 30;
        }
    }
}
