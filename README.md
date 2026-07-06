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
5. **弹窗内容自定义输出**：Groovy 脚本返回结果可自定义弹窗内容
6. **自定义参数控制是否弹窗**：可配置 Groovy 返回结果中的参数名来控制弹窗和阻断行为
7. **弹窗结果即时消息**：弹窗结果及信息只作为即时消息，不持久化存储，3分钟自动过期

## 功能特性

- ✅ 在任务配置中可选启用弹窗检查
- ✅ 读取任务已配置的变量和参数
- ✅ 使用 Groovy 脚本执行判断逻辑（Jenkins 内置 Groovy 支持，无需外部依赖）
- ✅ 自定义弹窗内容输出
- ✅ 自定义参数名控制是否弹窗/是否阻断构建
- ✅ 构建前拦截判断，条件不满足只弹窗不构建
- ✅ 条件满足直接构建，不等待弹窗结果
- ✅ 不影响其他页面触发构建或定时调度
- ✅ 多线程统一控制并发，防止内存泄露
- ✅ 线程池有界队列 + 超时控制 + 优雅关闭
- ✅ 单 Job 并发数限制
- ✅ 弹窗即时消息（3分钟自动过期，不持久化）
- ✅ REST API 供前端 AJAX 轮询弹窗状态
- ✅ 无外部依赖，仅使用 Jenkins/Stapler 内置库

## 配置说明

### 全局配置（Manage Jenkins → System）

| 配置项 | 说明 | 默认值 |
|---|---|---|
| Globally Enabled | 全局启用/禁用插件 | true |
| Thread Pool Core Size | 线程池核心线程数 | 4 |
| Thread Pool Max Size | 线程池最大线程数 | 16 |
| Thread Pool Queue Capacity | 线程池队列容量 | 100 |
| Thread Keep Alive (seconds) | 空闲线程存活时间 | 60 |
| Global Script Timeout (seconds) | 全局脚本超时 | 60 |
| Max Concurrent Per Job | 单 Job 最大并发执行数 | 2 |

### 任务配置（Job Configuration → Build Popup）

| 配置项 | 说明 | 默认值 |
|---|---|---|
| Enable Build Popup Check | 是否启用弹窗检查 | false |
| Groovy Script | 执行的 Groovy 脚本 | 示例脚本 |
| Block Build Parameter Name | 阻断构建的参数名 | blockBuild |
| Show Popup Parameter Name | 是否弹窗的参数名 | showPopup |
| Popup Content Parameter Name | 弹窗内容的参数名 | popupContent |
| Script Timeout (seconds) | 脚本超时时间 | 30 |

## Groovy 脚本说明

### 可用绑定变量

| 变量 | 类型 | 说明 |
|---|---|---|
| job | Job<?, ?> | 当前要构建的任务 |
| jenkins | Jenkins | Jenkins 实例 |
| env | Map<String, String> | 环境变量 |
| params | Map<String, String> | 构建参数 |
| build | Run<?, ?> | 当前构建（可能为 null） |
| currentBuild | Run<?, ?> | 当前构建（同 build） |

### 返回值格式

Groovy 脚本应返回一个 **Map**，包含以下键（键名可自定义）：

```groovy
def result = [:]
result.blockBuild = false   // true = 条件不满足，只弹窗不构建
result.showPopup = true     // true = 显示弹窗提醒
result.popupContent = '...'  // 弹窗内容文本
return result
```

也支持其他返回类型：
- **Boolean**: `true` 表示阻断构建并弹窗，`false` 表示放行
- **String**: 作为弹窗内容，默认阻断构建并弹窗

### 示例脚本

#### 示例 1：检查环境变量

```groovy
def result = [:]
def deployEnv = params.get('DEPLOY_ENV', 'dev')
if (deployEnv == 'prod') {
    result.blockBuild = true
    result.showPopup = true
    result.popupContent = "警告：当前部署环境为 PRODUCTION，请确认是否需要构建！\n操作人: ${params.get('BUILD_USER', 'unknown')}"
} else {
    result.blockBuild = false
    result.showPopup = false
    result.popupContent = ''
}
return result
```

#### 示例 2：检查上游构建状态

```groovy
def result = [:]
def upstreamJob = jenkins.getJob('upstream-project')
def upstreamBuild = upstreamJob?.lastSuccessfulBuild
if (upstreamBuild == null) {
    result.blockBuild = true
    result.showPopup = true
    result.popupContent = "上游任务 upstream-project 没有成功构建，当前构建被阻断！"
} else {
    result.blockBuild = false
    result.showPopup = true
    result.popupContent = "上游任务 upstream-project 最近成功构建: #${upstreamBuild.number}"
}
return result
```

#### 示例 3：检查当前是否有正在运行的构建

```groovy
def result = [:]
def building = job.isBuilding()
if (building) {
    result.blockBuild = true
    result.showPopup = true
    result.popupContent = "任务 ${job.name} 当前已有构建正在执行，请等待后再构建！"
} else {
    result.blockBuild = false
    result.showPopup = false
    result.popupContent = ''
}
return result
```

## REST API

### 查询弹窗状态

```
GET /build-popup-api/popup?job=<jobFullName>
```

返回 JSON：
```json
{
  "hasPopup": true,
  "id": "a1b2c3d4",
  "showPopup": true,
  "blockBuild": true,
  "popupContent": "...",
  "error": false,
  "errorMessage": "",
  "executionTimeMs": 123,
  "timestamp": 1700000000000
}
```

### 关闭弹窗

```
POST /build-popup-api/dismiss?job=<jobFullName>&id=<popupId>
```

## 构建与安装

### 构建

```bash
./build.sh          # 默认打包
./build.sh build    # 只编译
./build.sh clean    # 清理
./build.sh package  # 打包
./build.sh deploy   # 部署到本地 Maven 仓库
```

### 安装

1. 构建生成 `.hpi` 文件在 `target/` 目录下
2. 进入 Jenkins → Manage Jenkins → Manage Plugins → Advanced → Upload Plugin
3. 选择生成的 `.hpi` 文件上传安装
4. 重启 Jenkins

## 架构设计

```
┌─────────────────────────────────────────────────────────┐
│                     用户触发构建                          │
│                         ↓                               │
│  ┌──────────────────────────────────────────────────┐   │
│  │  BuildPopupQueueListener (QueueListener)         │   │
│  │  任务进入 WaitingItem 时触发 onEnterWaiting       │   │
│  └──────────────────────────────────────────────────┘   │
│                         ↓                               │
│  ┌──────────────────────────────────────────────────┐   │
│  │     BuildPopupService (核心服务)                   │   │
│  │     ┌──────────────────────────────────────────┐  │   │
│  │     │  检查全局启用 / Job 级启用                   │  │   │
│  │     │  检查单 Job 并发限制                         │  │   │
│  │     │  提交到 BuildPopupThreadPool               │  │   │
│  │     │  GroovyExecutionTask (有超时控制)           │  │   │
│  │     │  解析返回结果 → BuildPopupResult            │  │   │
│  │     └──────────────────────────────────────────┘  │   │
│  └──────────────────────────────────────────────────┘   │
│                         ↓                               │
│  ┌──────────────────────────────────────────────────┐   │
│  │     BuildPopupResult 判断结果（即时消息）           │   │
│  │     ┌─────────┬──────────┬──────────────────┐    │   │
│  │     │blockBuild│showPopup │popupContent       │    │   │
│  │     └─────────┼──────────┼──────────────────┤    │   │
│  │     │ false   │ 任意     │ → 直接构建不等待    │    │   │
│  │     │ true    │ true     │ → 弹窗+取消队列项  │    │   │
│  │     └─────────┴──────────┴──────────────────┘    │   │
│  └──────────────────────────────────────────────────┘   │
│                         ↓                               │
│  ┌──────────────────────────────────────────────────┐   │
│  │  BuildPopupAction (即时弹窗，3分钟过期)           │   │
│  │  BuildPopupWebAPI (REST API 供前端轮询)           │   │
│  │  侧边栏入口 + AJAX 弹窗内容页面                   │   │
│  └──────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────┘

线程池架构:
┌──────────────────────────────────────────┐
│  BuildPopupThreadPool                     │
│  ThreadPoolExecutor                       │
│  ┌──────────────────────────────────────┐│
│  │ core=4, max=16, queue=100            ││
│  │ keepAlive=60s, daemon=true           ││
│  │ CallerRunsPolicy (队列满时调用线程执行) ││
│  │ allowCoreThreadTimeOut=true           ││
│  └──────────────────────────────────────┘│
│  + 单 Job 并发控制 (ConcurrentHashMap)    │
│  + JVM ShutdownHook 优雅关闭              │
│  + 定时清理过期弹窗 (60s)                  │
│  + 配置更新时 reinitialize                │
└──────────────────────────────────────────┘
```

## 许可证

MIT License
