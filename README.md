# JksBuildPopup - Jenkins 构建弹窗检查插件

**版本**: 1.0.0

## 简介

JksBuildPopup 是一个 Jenkins 插件，在任务构建前执行 Groovy 脚本进行条件判断，根据判断结果决定是否弹窗提醒或阻断构建。

**适用版本**: Jenkins ≥ 2.277.4 + JDK 11 及后续版本

## 核心设计理念

1. **条件满足构建**：直接开始构建，不等待弹窗结果（弹窗只做判断和提醒）
2. **条件不满足**：只对当前触发弹窗提醒，不从队列构建，但不影响该任务被其他页面执行构建或调度
3. **减少任务构建频次**：通过 Groovy 脚本判断，避免不必要的构建
4. **多线程统一并发控制**：组件独立调度执行 Groovy 命令，统一线程池控制并发，防止内存泄露
5. **弹窗内容自定义输出**：Groovy 脚本返回结果可自定义弹窗内容和标题
6. **构建参数直接可用**：Groovy 脚本中可直接使用构建参数变量名（类似 Active Choices 插件）
7. **弹窗结果即时消息**：弹窗结果及信息只作为即时消息，不持久化存储，3分钟自动过期
8. **安全优先**：Groovy 脚本默认在沙箱中执行，限制危险操作

## 功能特性

- 在任务配置中可选启用弹窗检查
- 读取任务已配置的变量和参数
- 使用 Groovy 脚本执行判断逻辑（Jenkins 内置 Groovy 支持，无需外部依赖）
- 构建参数可直接作为变量使用（如 `DEPLOY_ENV`，也可通过 `params.get('DEPLOY_ENV')`）
- 自定义弹窗内容、标题输出
- 自定义参数控制是否弹窗/是否阻断构建
- 非参数化构建：点击 "Build Now" 时拦截检查
- 参数化构建：点击参数页面的 Build 按钮时拦截检查（使用用户填写的实际参数值）
- "Build with Parameters" 链接直接导航到参数页面，不执行检查
- 通过 API/CLI/调度器触发的构建不受弹窗检查影响
- 不影响其他页面触发构建或定时调度
- 多线程统一控制并发，防止内存泄露
- 单 Job 并发数限制（默认 20）
- 弹窗即时消息（3分钟自动过期，不持久化）
- 无外部依赖，仅使用 Jenkins/Stapler 内置库
- 安全检查：仅对当前触发弹窗的用户生效，不涉及其他用户构建任务
- Groovy 沙箱模式（默认启用，安全优先）
- 弹窗支持 Cancel/OK 按钮：Cancel 取消构建，OK 继续构建

## 安全机制

- **Groovy 沙箱**：脚本默认在沙箱中执行，限制文件系统访问、网络操作、进程执行等危险操作
- **权限控制**：API 端点 `doCheck` 需要 Job.BUILD 权限才能调用
- **RejectedAccessException**：捕获沙箱拒绝异常，提供友好的错误提示
- **脚本审批**：管理员可通过 Jenkins Script Console 审批需要的方法调用

## 配置说明

### 全局配置（Manage Jenkins → System → Build Popup Configuration）

| 配置项 | 说明 | 默认值 | 自动计算 |
|---|---|---|---|
| Globally Enabled | 全局启用/禁用插件 | true | - |
| Thread Pool Core Size | 线程池核心线程数 | CPU核数 | min 2 |
| Thread Pool Max Size | 线程池最大线程数 | CPU核数 × 4 | min 4 |
| Thread Pool Queue Capacity | 线程池队列容量 | CPU核数 × 25 | min 50 |
| Thread Keep Alive (seconds) | 空闲线程存活时间 | 60 | - |
| Global Script Timeout (seconds) | 全局脚本超时 | 60 | - |
| Max Concurrent Per Job | 单 Job 最大并发执行数 | 20 | - |

### 任务配置（Job Configuration → Build Popup）

| 配置项 | 说明 | 默认值 |
|---|---|---|
| Enable Build Popup Check | 是否启用弹窗检查 | false |
| Groovy Script | 执行的 Groovy 脚本 | 示例脚本 |
| Script Timeout (seconds) | 脚本超时时间 | 30 |
| Use Groovy Sandbox | 在沙箱中执行脚本（安全） | true |

## Groovy 脚本说明

### 可用绑定变量

| 变量 | 类型 | 说明 |
|---|---|---|
| job | Job<?, ?> | 当前要构建的任务 |
| jenkins | Jenkins | Jenkins 实例 |
| env | Map<String, String> | 环境变量 |
| params | Map<String, String> | 构建参数（Map 形式访问） |
| *参数名* | String | 构建参数可直接作为变量使用，如 `DEPLOY_ENV` |
| build | Run<?, ?> | 当前构建（可能为 null） |
| currentBuild | Run<?, ?> | 当前构建（同 build） |

### 返回值格式

Groovy 脚本应返回一个 **Map**，包含以下键：

| 键名 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| blockBuild | Boolean | false | true = 条件不满足，只弹窗不构建 |
| showPopup | Boolean | false | true = 显示弹窗提醒 |
| popupContent | String | - | 弹窗内容文本 |
| popupTitle | String | Build Notification | 弹窗标题 |

```groovy
// 使用 Map 字面量语法 [key: value]
return [blockBuild: true, showPopup: true, popupContent: 'message', popupTitle: '自定义标题']
```

也支持其他返回类型：
- **Boolean**: `true` 表示阻断构建并弹窗，`false` 表示放行
- **String**: 作为弹窗内容，默认阻断构建并弹窗
- **null**: 不弹窗不阻断，直接构建

### 示例脚本

#### 示例 1：检查部署环境（参数直接作为变量使用）

假设任务配置了构建参数 `DEPLOY_ENV`（Choice 参数）和 `BUILD_USER`（String 参数）：

```groovy
// DEPLOY_ENV 和 BUILD_USER 可直接作为变量使用
// 也可以通过 params.get('DEPLOY_ENV') 访问
if (DEPLOY_ENV == 'prod') {
    return [blockBuild: true, showPopup: true,
            popupContent: "警告：当前部署环境为 PRODUCTION！\n操作人: ${BUILD_USER}",
            popupTitle: '生产环境部署确认']
} else {
    return [blockBuild: false, showPopup: false, popupContent: '']
}
```

#### 示例 2：检查上游构建状态

```groovy
def upstreamJob = jenkins.getJob('upstream-project')
def upstreamBuild = upstreamJob?.lastSuccessfulBuild
if (upstreamBuild == null) {
    return [blockBuild: true, showPopup: true,
            popupContent: "上游任务 upstream-project 没有成功构建，当前构建被阻断！",
            popupTitle: '上游构建检查']
} else {
    return [blockBuild: false, showPopup: true,
            popupContent: "上游任务最近成功构建: #${upstreamBuild.number}"]
}
```

#### 示例 3：检查当前是否有正在运行的构建

```groovy
if (job.isBuilding()) {
    return [blockBuild: true, showPopup: true,
            popupContent: "任务 ${job.name} 当前已有构建正在执行，请等待后再构建！",
            popupTitle: '构建冲突']
} else {
    return [blockBuild: false, showPopup: false, popupContent: '']
}
```

#### 示例 4：根据分支名判断

假设任务配置了构建参数 `BRANCH`：

```groovy
// BRANCH 参数可直接作为变量使用
if (BRANCH == 'master' || BRANCH == 'main') {
    return [blockBuild: true, showPopup: true,
            popupContent: "注意：即将构建主分支 ${BRANCH}！\n请确认是否继续。",
            popupTitle: '主分支构建提醒']
} else {
    return [blockBuild: false, showPopup: true,
            popupContent: "构建分支: ${BRANCH}"]
}
```

#### 示例 5：综合条件判断

```groovy
def messages = []
def shouldBlock = false

// 检查是否正在构建
if (job.isBuilding()) {
    messages << "任务正在构建中"
    shouldBlock = true
}

// 检查环境参数（直接使用变量名）
if (DEPLOY_ENV == 'prod') {
    messages << "生产环境部署，请确认"
}

// 检查分支（通过 params map 访问）
def branch = params.get('BRANCH', '')
if (branch == 'master' || branch == 'main') {
    messages << "主分支构建"
}

if (shouldBlock || messages) {
    return [blockBuild: shouldBlock, showPopup: true,
            popupContent: messages.join('\n'),
            popupTitle: '构建条件检查']
} else {
    return [blockBuild: false, showPopup: false, popupContent: '']
}
```

## REST API

### 构建前检查

```
GET /build-popup-api/check?job=<jobFullName>&formParams=<json>
```

`formParams` 为可选参数，JSON 格式传递用户填写的实际构建参数值。

返回 JSON：
```json
{
  "blockBuild": false,
  "showPopup": true,
  "popupContent": "...",
  "popupTitle": "Build Notification",
  "error": false,
  "errorMessage": "",
  "executionTimeMs": 123
}
```

### 查询弹窗状态

```
GET /build-popup-api/popup?job=<jobFullName>
```

### 关闭弹窗

```
GET /build-popup-api/dismiss?job=<jobFullName>&id=<popupId>
```

## 构建与安装

### 构建

```bash
mvn clean package -DskipTests
```

生成的 `.hpi` 文件在 `target/` 目录下。

### 安装

1. 进入 Jenkins → Manage Jenkins → Manage Plugins → Advanced → Upload Plugin
2. 选择生成的 `.hpi` 文件上传安装
3. 重启 Jenkins

## 架构设计

```
┌─────────────────────────────────────────────────────────┐
│                   用户点击构建按钮                        │
│                         ↓                               │
│  ┌──────────────────────────────────────────────────┐   │
│  │  BuildPopupPageDecorator (header.jelly)           │   │
│  │  拦截 Build Now / 参数表单 Build 按钮点击          │   │
│  │  收集用户填写的参数值 → AJAX 调用 doCheck          │   │
│  └──────────────────────────────────────────────────┘   │
│                         ↓                               │
│  ┌──────────────────────────────────────────────────┐   │
│  │  BuildPopupWebAPI.doCheck (REST API)              │   │
│  │  权限检查（Job.BUILD）→ 合并参数 → 调用 Service    │   │
│  └──────────────────────────────────────────────────┘   │
│                         ↓                               │
│  ┌──────────────────────────────────────────────────┐   │
│  │     BuildPopupService (核心服务)                   │   │
│  │     ┌──────────────────────────────────────────┐  │   │
│  │     │  检查全局启用 / Job 级启用                   │  │   │
│  │     │  检查单 Job 并发限制 (maxConcurrentPerJob)   │  │   │
│  │     │  提交到 BuildPopupThreadPool               │  │   │
│  │     │  GroovyExecutionTask (有超时控制)           │  │   │
│  │     │  构建参数注入为直接变量 + params Map          │  │   │
│  │     │  沙箱执行 / 直接执行 (useSandbox)            │  │   │
│  │     │  解析返回结果 → BuildPopupResult            │  │   │
│  │     └──────────────────────────────────────────┘  │   │
│  └──────────────────────────────────────────────────┘   │
│                         ↓                               │
│  ┌──────────────────────────────────────────────────┐   │
│  │     BuildPopupResult 判断结果（即时消息）           │   │
│  │     ┌──────────┬───────────┬──────────────────┐  │   │
│  │     │blockBuild│showPopup  │ 行为              │  │   │
│  │     ├──────────┼───────────┼──────────────────┤  │   │
│  │     │ false    │ false     │ 直接构建          │  │   │
│  │     │ false    │ true      │ 弹窗→Cancel/OK    │  │   │
│  │     │ true     │ true      │ 弹窗→仅关闭       │  │   │
│  │     └──────────┴───────────┴──────────────────┘  │   │
│  └──────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────┘

线程池架构（根据 CPU 自动调整）:
┌──────────────────────────────────────────────────────────┐
│  BuildPopupThreadPool                                    │
│  ThreadPoolExecutor                                      │
│  ┌────────────────────────────────────────────────────┐│
│  │ core=N, max=N×4, queue=N×25 (N=CPU核数)            ││
│  │ keepAlive=60s, daemon=true, allowCoreThreadTimeOut ││
│  │ CallerRunsPolicy (队列满时调用线程执行)               ││
│  └────────────────────────────────────────────────────┘│
│  + 单 Job 并发控制 (ConcurrentHashMap, 默认 max=20)    │
│  + 定期清理过期弹窗 (ScheduledExecutorService)         │
│  + JVM ShutdownHook (优雅关闭)                        │
└──────────────────────────────────────────────────────────┘
```

## 测试

项目包含完整的单元测试套件：

| 测试类 | 测试数 | 覆盖范围 |
|---|---|---|
| BuildPopupResultTest | 8 | 默认值、工厂方法、时效性、popupTitle、setters |
| BuildPopupActionTest | 6 | 存取、关闭、过期清理、覆盖 |
| BuildPopupServiceTest | 14 | 全局/任务开关、脚本执行、参数变量、超时、并发限制、沙箱 |
| BuildPopupThreadPoolTest | 3 | 提交任务、并发、活跃数 |

运行测试：
```bash
mvn test
```

## 许可证

MIT License
