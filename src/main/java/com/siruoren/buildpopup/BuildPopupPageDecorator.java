package com.siruoren.buildpopup;

import hudson.Extension;
import hudson.model.PageDecorator;

/**
 * 页面装饰器：向所有 Jenkins Job 页面注入构建弹窗检查 JavaScript。
 * 参照 Active Choices 插件方式，在构建提交前拦截并执行 Groovy 检查，
 * 条件不满足时弹窗阻止构建，条件满足时直接构建不等待弹窗结果。
 * <p>
 * header.jelly 使用 ${rootURL} 变量构造 API URL，
 * 无需 Java 端提供 apiBaseUrl 方法。
 * </p>
 */
@Extension
public class BuildPopupPageDecorator extends PageDecorator {

    public BuildPopupPageDecorator() {
        super(BuildPopupPageDecorator.class);
    }
}
