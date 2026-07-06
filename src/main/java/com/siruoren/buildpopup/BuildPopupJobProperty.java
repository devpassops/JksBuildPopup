package com.siruoren.buildpopup;

import hudson.Extension;
import hudson.model.Job;
import hudson.model.JobProperty;
import hudson.model.JobPropertyDescriptor;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.StaplerRequest;

import java.util.logging.Logger;

/**
 * Job 级别配置属性，在任务配置页面中可选启用。
 * 配置 Groovy 脚本、弹窗条件参数名、弹窗内容输出参数名等。
 */
public class BuildPopupJobProperty extends JobProperty<Job<?, ?>> {

    private static final Logger LOGGER = Logger.getLogger(BuildPopupJobProperty.class.getName());

    /** 是否启用弹窗检查 */
    private boolean enabled;

    /** Groovy 脚本内容，用于执行判断逻辑 */
    private String groovyScript;

    /** Groovy 脚本中返回的参数名，值为 true/false，决定是否阻断构建 */
    private String blockBuildParam;

    /** Groovy 脚本中返回的参数名，值为 true/false，决定是否弹窗显示 */
    private String showPopupParam;

    /** Groovy 脚本中返回的参数名，值为字符串，决定弹窗显示内容 */
    private String popupContentParam;

    /** Groovy 脚本执行超时时间（秒），默认 30 */
    private int scriptTimeout;

    public BuildPopupJobProperty() {
        this.enabled = false;
        this.groovyScript = "";
        this.blockBuildParam = "blockBuild";
        this.showPopupParam = "showPopup";
        this.popupContentParam = "popupContent";
        this.scriptTimeout = 30;
    }

    @DataBoundSetter
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    @DataBoundSetter
    public void setGroovyScript(String groovyScript) {
        this.groovyScript = groovyScript;
    }

    public String getGroovyScript() {
        return groovyScript;
    }

    @DataBoundSetter
    public void setBlockBuildParam(String blockBuildParam) {
        this.blockBuildParam = blockBuildParam;
    }

    public String getBlockBuildParam() {
        return blockBuildParam;
    }

    @DataBoundSetter
    public void setShowPopupParam(String showPopupParam) {
        this.showPopupParam = showPopupParam;
    }

    public String getShowPopupParam() {
        return showPopupParam;
    }

    @DataBoundSetter
    public void setPopupContentParam(String popupContentParam) {
        this.popupContentParam = popupContentParam;
    }

    public String getPopupContentParam() {
        return popupContentParam;
    }

    @DataBoundSetter
    public void setScriptTimeout(int scriptTimeout) {
        this.scriptTimeout = scriptTimeout;
    }

    public int getScriptTimeout() {
        return scriptTimeout;
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

        @Override
        public BuildPopupJobProperty newInstance(StaplerRequest req, JSONObject formData) throws FormException {
            BuildPopupJobProperty prop = new BuildPopupJobProperty();
            Object enabledVal = formData.get("enabled");
            if (enabledVal != null) {
                if (enabledVal instanceof Boolean) {
                    prop.setEnabled((Boolean) enabledVal);
                } else if (enabledVal instanceof JSONObject) {
                    JSONObject enabledObj = (JSONObject) enabledVal;
                    prop.setEnabled(true);
                    prop.setGroovyScript(enabledObj.optString("groovyScript", ""));
                    prop.setBlockBuildParam(enabledObj.optString("blockBuildParam", "blockBuild"));
                    prop.setShowPopupParam(enabledObj.optString("showPopupParam", "showPopup"));
                    prop.setPopupContentParam(enabledObj.optString("popupContentParam", "popupContent"));
                    prop.setScriptTimeout(enabledObj.optInt("scriptTimeout", 30));
                }
            } else {
                prop.setEnabled(false);
            }
            return prop;
        }

        public String getDefaultGroovyScript() {
            return "// Example: Check if environment meets build conditions\n"
                + "// Available bindings: job, jenkins, env, params, build, currentBuild\n"
                + "// Return a Map with keys: blockBuild, showPopup, popupContent\n"
                + "def result = [:]\n"
                + "result.blockBuild = false  // true = block this build, only show popup\n"
                + "result.showPopup = false   // true = show popup message\n"
                + "result.popupContent = ''   // popup message content\n"
                + "return result\n";
        }

        public String getDefaultBlockBuildParam() {
            return "blockBuild";
        }

        public String getDefaultShowPopupParam() {
            return "showPopup";
        }

        public String getDefaultPopupContentParam() {
            return "popupContent";
        }

        public int getDefaultScriptTimeout() {
            return 30;
        }
    }
}
