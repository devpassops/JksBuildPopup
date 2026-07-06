package com.siruoren.buildpopup;

/**
 * Groovy 脚本执行结果模型（即时消息，不持久化存储）。
 * 包含是否弹窗、是否阻断构建、弹窗内容等判断结果。
 * 弹窗结果及信息只作为即时消息，不存储。
 */
public class BuildPopupResult {

    /** 是否显示弹窗 */
    private boolean showPopup;

    /** 是否阻断构建（true = 条件不满足，只弹窗不构建） */
    private boolean blockBuild;

    /** 弹窗显示内容 */
    private String popupContent;

    /** 执行是否出错 */
    private boolean error;

    /** 错误信息 */
    private String errorMessage;

    /** 脚本执行耗时（毫秒） */
    private long executionTimeMs;

    /** 产生时间戳 */
    private long timestamp;

    /** 唯一标识，用于前端关闭确认 */
    private String id;

    public BuildPopupResult() {
        this.showPopup = false;
        this.blockBuild = false;
        this.popupContent = "";
        this.error = false;
        this.errorMessage = "";
        this.executionTimeMs = 0;
        this.timestamp = System.currentTimeMillis();
        this.id = java.util.UUID.randomUUID().toString().substring(0, 8);
    }

    public boolean isShowPopup() {
        return showPopup;
    }

    public void setShowPopup(boolean showPopup) {
        this.showPopup = showPopup;
    }

    public boolean isBlockBuild() {
        return blockBuild;
    }

    public void setBlockBuild(boolean blockBuild) {
        this.blockBuild = blockBuild;
    }

    public String getPopupContent() {
        return popupContent;
    }

    public void setPopupContent(String popupContent) {
        this.popupContent = popupContent;
    }

    public boolean isError() {
        return error;
    }

    public void setError(boolean error) {
        this.error = error;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public long getExecutionTimeMs() {
        return executionTimeMs;
    }

    public void setExecutionTimeMs(long executionTimeMs) {
        this.executionTimeMs = executionTimeMs;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    /** 是否仍然有效（3分钟内） */
    public boolean isStillValid() {
        return System.currentTimeMillis() - timestamp < 3 * 60 * 1000;
    }

    /** 创建一个成功的结果 */
    public static BuildPopupResult success(boolean showPopup, boolean blockBuild, String content) {
        BuildPopupResult result = new BuildPopupResult();
        result.setShowPopup(showPopup);
        result.setBlockBuild(blockBuild);
        result.setPopupContent(content);
        return result;
    }

    /** 创建一个错误的结果 */
    public static BuildPopupResult error(String errorMessage) {
        BuildPopupResult result = new BuildPopupResult();
        result.setError(true);
        result.setErrorMessage(errorMessage);
        result.setShowPopup(true);
        return result;
    }

    @Override
    public String toString() {
        return "BuildPopupResult{id=" + id + ", showPopup=" + showPopup + ", blockBuild=" + blockBuild
            + ", popupContent='" + popupContent + "', error=" + error
            + ", errorMessage='" + errorMessage + "', executionTimeMs=" + executionTimeMs + "}";
    }
}
