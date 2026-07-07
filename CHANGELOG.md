# Changelog

All notable changes to this project will be documented in this file.

## [1.0.0] - 2026-07-07

### Added

- 初始版本发布
- 支持在任务配置中启用构建前弹窗检查
- 支持 Groovy 脚本执行条件判断（使用 Jenkins 内置 Groovy，无需外部依赖）
- 支持自定义弹窗内容、标题、是否弹窗、是否阻断构建
- 支持参数化构建（收集用户填写的实际参数值传给 Groovy 脚本）
- 支持多线程统一并发控制，防止内存泄露
- 支持单 Job 并发数限制（默认 20）
- 支持脚本超时控制（默认 30 秒）
- 支持 REST API 供前端调用（/build-popup-api/check, /build-popup-api/popup, /build-popup-api/dismiss）
- 支持全局配置（线程池参数、脚本超时、单 Job 并发限制）
- 支持弹窗即时消息（3分钟自动过期，不持久化存储）
- 支持国际化（中文/英文）
- 支持 Groovy 沙箱模式（默认启用，安全优先）
- 支持构建参数直接作为变量使用（类似 Active Choices 插件）

### Security

- Groovy 脚本默认在沙箱中执行（useSandbox=true），限制危险操作
- API 端点 `doCheck` 需要 Job.BUILD 权限才能调用
- 支持 `@NonCPS` 和权限白名单校验（通过 Jenkins Script Security 插件）
- 捕获 `RejectedAccessException`，提供友好的错误提示

### Key Features

- 非参数化构建：点击 "Build Now" 链接时执行弹窗检查
- 参数化构建：点击参数页面的 Build 按钮时执行弹窗检查（使用用户填写的实际参数值）
- "Build with Parameters" 链接直接跳转到参数页面，不执行检查
- 通过 API/CLI/调度器触发的构建不受弹窗检查影响
- 弹窗确认后使用隐藏表单提交，与 Jenkins 原生行为一致
- 支持自定义弹窗标题（popupTitle 参数），默认为 "Build Notification"
- 弹窗支持 Cancel/OK 按钮：Cancel 取消构建，OK 继续构建
- blockBuild=true 时只显示关闭按钮，不允许继续构建

### Thread Pool Auto-Configuration

线程池参数根据服务器 CPU 核数自动调整：

| 参数 | 自动计算公式 | 最小保底 | 默认值（8核服务器） |
|---|---|---|---|
| Core Threads | CPU核数 | 2 | 8 |
| Max Threads | CPU核数 × 4 | 4 | 32 |
| Queue Capacity | CPU核数 × 25 | 50 | 200 |

### Groovy Script Parameters

| 参数名 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| blockBuild | Boolean | false | 是否阻断构建（true=只弹窗不构建） |
| showPopup | Boolean | false | 是否显示弹窗 |
| popupContent | String | - | 弹窗内容文本 |
| popupTitle | String | Build Notification | 弹窗标题 |

### Binding Variables in Groovy Script

| 变量 | 类型 | 说明 |
|---|---|---|
| job | Job<?, ?> | 当前要构建的任务 |
| jenkins | Jenkins | Jenkins 实例 |
| env | Map<String, String> | 环境变量 |
| params | Map<String, String> | 构建参数（Map 形式访问） |
| *参数名* | String | 构建参数可直接作为变量使用，如 `DEPLOY_ENV` |
| build | Run<?, ?> | 当前构建（可能为 null） |
| currentBuild | Run<?, ?> | 当前构建（同 build） |

### Testing

- 单元测试：31 个测试用例，覆盖核心逻辑
- 测试类：BuildPopupResultTest、BuildPopupActionTest、BuildPopupServiceTest、BuildPopupThreadPoolTest
- 测试覆盖：默认值、工厂方法、时效性、参数注入、并发控制、超时、脚本错误、安全机制

### Documentation

- 完整的帮助文档（help-*.html）：全局配置 7 个帮助文件，任务配置 4 个帮助文件
- 中文帮助文档：所有帮助文件均有中文版本（*_zh_CN.html）
- 国际化消息：12 条中英文消息（Messages.properties, Messages_zh_CN.properties）

### Technical Stack

- Jenkins: ≥ 2.277.4
- JDK: 11
- Parent POM: 4.62
- HPI Plugin: 3.28
- Script Security: 1.77
- Structs: 1.22
- 无外部依赖，仅使用 Jenkins/Stapler 内置库
