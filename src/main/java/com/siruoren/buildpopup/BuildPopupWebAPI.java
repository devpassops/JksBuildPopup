package com.siruoren.buildpopup;

import hudson.Extension;
import hudson.model.Item;
import hudson.model.Job;
import hudson.model.UnprotectedRootAction;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * REST API 端点：供前端 AJAX 调用。
 * <p>
 * - doCheck: 构建前检查（Active Choices 风格），执行 Groovy 脚本返回判断结果
 * - doPopup: 轮询弹窗状态（保留兼容）
 * - doDismiss: 关闭弹窗（GET 方式，无需 CRUMB）
 * </p>
 */
@Extension
public class BuildPopupWebAPI implements UnprotectedRootAction {

    private static final Logger LOGGER = Logger.getLogger(BuildPopupWebAPI.class.getName());

    @Override
    public String getIconFileName() {
        return null;
    }

    @Override
    public String getDisplayName() {
        return null;
    }

    @Override
    public String getUrlName() {
        return "build-popup-api";
    }

    /**
     * 构建前检查（核心端点，参照 Active Choices）
     * GET /build-popup-api/check?job=<jobFullName>
     * 执行 Groovy 脚本，返回是否阻断构建、是否弹窗、弹窗内容等
     */
    public void doCheck(StaplerRequest req, StaplerResponse rsp) throws Exception {
        String jobName = req.getParameter("job");
        if (jobName == null || jobName.isEmpty()) {
            rsp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing 'job' parameter");
            return;
        }

        jenkins.model.Jenkins j = jenkins.model.Jenkins.getInstanceOrNull();
        if (j == null) {
            rsp.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Jenkins not ready");
            return;
        }

        Item item = j.getItemByFullName(jobName);
        if (!(item instanceof Job)) {
            // 非 Job 页面，允许构建
            rsp.setContentType("application/json;charset=UTF-8");
            writeAllowJson(rsp);
            return;
        }

        Job<?, ?> job = (Job<?, ?>) item;

        // 权限检查：需要 Job.READ 权限
        try {
            job.checkPermission(Item.READ);
        } catch (Exception e) {
            rsp.sendError(HttpServletResponse.SC_FORBIDDEN, "No permission to access job: " + jobName);
            return;
        }

        Object rawProp = job.getProperty(BuildPopupJobProperty.class);
        if (!(rawProp instanceof BuildPopupJobProperty)) {
            // 插件未配置，允许构建
            rsp.setContentType("application/json;charset=UTF-8");
            writeAllowJson(rsp);
            return;
        }

        BuildPopupJobProperty prop = (BuildPopupJobProperty) rawProp;
        if (!prop.isEnabled()) {
            // 插件未启用，允许构建
            rsp.setContentType("application/json;charset=UTF-8");
            writeAllowJson(rsp);
            return;
        }

        BuildPopupGlobalConfiguration globalConfig = BuildPopupGlobalConfiguration.get();
        if (!globalConfig.isGloballyEnabled()) {
            rsp.setContentType("application/json;charset=UTF-8");
            writeAllowJson(rsp);
            return;
        }

        // 获取 Job 默认参数，并用前端传入的实际参数覆盖
        Map<String, String> params = getDefaultParams(job);
        String formParamsJson = req.getParameter("formParams");
        if (formParamsJson != null && !formParamsJson.isEmpty()) {
            try {
                JSONObject providedParams = JSONObject.fromObject(formParamsJson);
                Iterator<?> keys = providedParams.keys();
                while (keys.hasNext()) {
                    String key = (String) keys.next();
                    params.put(key, providedParams.getString(key));
                }
            } catch (Exception e) {
                LOGGER.log(Level.FINE, "Failed to parse formParams for job " + jobName, e);
            }
        }
        Map<String, String> envVars = new HashMap<>();

        // 执行 Groovy 脚本
        BuildPopupResult result = BuildPopupService.getInstance().executeGroovy(job, null, envVars, params);

        // 返回结果 JSON（确保 UTF-8 编码，避免中文乱码）
        rsp.setContentType("application/json;charset=UTF-8");
        rsp.setCharacterEncoding("UTF-8");
        JSONObject json = new JSONObject();
        json.put("blockBuild", result.isBlockBuild());
        json.put("showPopup", result.isShowPopup());
        json.put("popupContent", result.getPopupContent());
        json.put("popupTitle", result.getPopupTitle());
        json.put("error", result.isError());
        json.put("errorMessage", result.getErrorMessage());
        json.put("executionTimeMs", result.getExecutionTimeMs());
        rsp.getWriter().write(json.toString());
    }

    /**
     * 查询指定 Job 的最新弹窗状态（保留轮询兼容）
     * GET /build-popup-api/popup?job=<jobFullName>
     */
    public void doPopup(StaplerRequest req, StaplerResponse rsp) throws Exception {
        String jobName = req.getParameter("job");
        if (jobName == null || jobName.isEmpty()) {
            rsp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing 'job' parameter");
            return;
        }

        // Security: Check job read permission
        jenkins.model.Jenkins j = jenkins.model.Jenkins.getInstanceOrNull();
        if (j != null) {
            Item item = j.getItemByFullName(jobName);
            if (item instanceof Job) {
                try {
                    ((Job<?, ?>) item).checkPermission(Item.READ);
                } catch (Exception e) {
                    rsp.sendError(HttpServletResponse.SC_FORBIDDEN, "No permission to access job: " + jobName);
                    return;
                }
            }
        }

        BuildPopupResult result = BuildPopupAction.getLatestPopupResult(jobName);

        rsp.setContentType("application/json;charset=UTF-8");
        rsp.setCharacterEncoding("UTF-8");
        JSONObject json = new JSONObject();
        if (result != null) {
            json.put("hasPopup", true);
            json.put("id", result.getId());
            json.put("showPopup", result.isShowPopup());
            json.put("blockBuild", result.isBlockBuild());
            json.put("popupContent", result.getPopupContent());
            json.put("popupTitle", result.getPopupTitle());
            json.put("error", result.isError());
            json.put("errorMessage", result.getErrorMessage());
            json.put("executionTimeMs", result.getExecutionTimeMs());
            json.put("timestamp", result.getTimestamp());
        } else {
            json.put("hasPopup", false);
        }
        rsp.getWriter().write(json.toString());
    }

    /**
     * 关闭指定 Job 的弹窗（GET 方式，无需 CRUMB）
     * GET /build-popup-api/dismiss?job=<jobFullName>&id=<popupId>
     */
    public void doDismiss(StaplerRequest req, StaplerResponse rsp) throws Exception {
        String jobName = req.getParameter("job");
        String popupId = req.getParameter("id");
        if (jobName == null || jobName.isEmpty()) {
            rsp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing 'job' parameter");
            return;
        }

        // Security: Check job read permission
        jenkins.model.Jenkins j = jenkins.model.Jenkins.getInstanceOrNull();
        if (j != null) {
            Item item = j.getItemByFullName(jobName);
            if (item instanceof Job) {
                try {
                    ((Job<?, ?>) item).checkPermission(Item.READ);
                } catch (Exception e) {
                    rsp.sendError(HttpServletResponse.SC_FORBIDDEN, "No permission to access job: " + jobName);
                    return;
                }
            }
        }

        BuildPopupAction.dismissPopup(jobName, popupId);

        rsp.setContentType("application/json;charset=UTF-8");
        rsp.setCharacterEncoding("UTF-8");
        JSONObject json = new JSONObject();
        json.put("success", true);
        rsp.getWriter().write(json.toString());
    }

    /** 获取 Job 默认参数值 */
    private Map<String, String> getDefaultParams(Job<?, ?> job) {
        Map<String, String> params = new HashMap<>();
        hudson.model.ParametersDefinitionProperty paramsDef = job.getProperty(hudson.model.ParametersDefinitionProperty.class);
        if (paramsDef != null) {
            for (hudson.model.ParameterDefinition pd : paramsDef.getParameterDefinitions()) {
                hudson.model.ParameterValue defaultVal = pd.getDefaultParameterValue();
                if (defaultVal != null && defaultVal.getValue() != null) {
                    params.put(pd.getName(), defaultVal.getValue().toString());
                }
            }
        }
        return params;
    }

    /** 写入允许构建的 JSON */
    private void writeAllowJson(StaplerResponse rsp) throws Exception {
        rsp.setCharacterEncoding("UTF-8");
        JSONObject json = new JSONObject();
        json.put("blockBuild", false);
        json.put("showPopup", false);
        rsp.getWriter().write(json.toString());
    }
}
