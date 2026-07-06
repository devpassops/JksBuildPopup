# Changelog

All notable changes to this project will be documented in this file.

## [1.0.0-SNAPSHOT] - 2026-07-07

### Added

- 初始版本发布
- 支持在任务配置中启用构建前弹窗检查
- 支持 Groovy 脚本执行条件判断（使用 Jenkins 内置 Groovy，无需外部依赖）
- 支持自定义弹窗内容、标题、是否弹窗、是否阻断构建
- 支持参数化构建（收集用户填写的实际参数值传给 Groovy 脚本）
- 支持多线程统一并发控制，防止内存泄露
- 支持单 Job 并发数限制
- 支持脚本超时控制
- 支持 REST API 供前端调用
- 支持全局配置（线程池参数、脚本超时、单 Job 并发限制）
- 支持弹窗即时消息（3分钟自动过期，不持久化存储）
- 支持国际化（中文/英文）

### Key Features

- 非参数化构建：点击 "Build Now" 链接时执行弹窗检查
- 参数化构建：点击参数页面的 Build 按钮时执行弹窗检查（使用用户填写的实际参数值）
- "Build with Parameters" 链接直接跳转到参数页面，不执行检查
- 通过 API/CLI/调度器触发的构建不受弹窗检查影响
- 弹窗确认后使用隐藏表单提交，与 Jenkins 原生行为一致
- 支持自定义弹窗标题（popupTitle 参数），默认为 "Build Notification"

### Groovy Script Parameters

| 参数名 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| blockBuild | Boolean | false | 是否阻断构建（true=只弹窗不构建） |
| showPopup | Boolean | false | 是否显示弹窗 |
| popupContent | String | - | 弹窗内容文本 |
| popupTitle | String | Build Notification | 弹窗标题 |

### Technical Stack

- Jenkins: ≥ 2.277.4
- JDK: 11
- Parent POM: 4.62
- HPI Plugin: 3.28
- 无外部依赖，仅使用 Jenkins/Stapler 内置库
