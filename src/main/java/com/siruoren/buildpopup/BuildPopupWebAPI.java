package com.siruoren.buildpopup;

import hudson.Extension;
import hudson.model.Job;
import hudson.model.UnprotectedRootAction;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.interceptor.RequirePOST;

import javax.servlet.http.HttpServletResponse;
import java.util.logging.Logger;

/**
 * REST API 端点：供前端 AJAX 轮询弹窗状态。
 * 提供按 Job 查询当前弹窗、关闭弹窗等接口。
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
     * 查询指定 Job 的最新弹窗状态
     * GET /build-popup-api/popup?job=<jobFullName>
     */
    public void doPopup(StaplerRequest req, StaplerResponse rsp) throws Exception {
        String jobName = req.getParameter("job");
        if (jobName == null || jobName.isEmpty()) {
            rsp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing 'job' parameter");
            return;
        }

        BuildPopupResult result = BuildPopupAction.getLatestPopupResult(jobName);

        rsp.setContentType("application/json;charset=UTF-8");
        JSONObject json = new JSONObject();
        if (result != null) {
            json.put("hasPopup", true);
            json.put("id", result.getId());
            json.put("showPopup", result.isShowPopup());
            json.put("blockBuild", result.isBlockBuild());
            json.put("popupContent", result.getPopupContent());
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
     * 关闭指定 Job 的弹窗
     * POST /build-popup-api/dismiss?job=<jobFullName>&id=<popupId>
     */
    @RequirePOST
    public void doDismiss(StaplerRequest req, StaplerResponse rsp) throws Exception {
        String jobName = req.getParameter("job");
        String popupId = req.getParameter("id");
        if (jobName == null || jobName.isEmpty()) {
            rsp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing 'job' parameter");
            return;
        }

        BuildPopupResult result = BuildPopupAction.getLatestPopupResult(jobName);
        if (result != null && result.getId().equals(popupId)) {
            // 标记为已关闭
            Job<?, ?> job = Jenkins.get().getItemByFullName(jobName, Job.class);
            if (job != null) {
                BuildPopupAction action = job.getAction(BuildPopupAction.class);
                if (action != null) {
                    action.doDismissPopup();
                }
            }
        }

        rsp.setContentType("application/json;charset=UTF-8");
        JSONObject json = new JSONObject();
        json.put("success", true);
        rsp.getWriter().write(json.toString());
    }
}
