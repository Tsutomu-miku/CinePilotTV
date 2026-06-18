# AGENTS.md — Relay 开发协作约定

本文件供 Relay（本仓库的 CLI coding agent）以及后续接入的自动化 agent 参考。人类贡献者可选择性遵守。

## 0. 总体原则

- 先读后改。任何改动前，必须先用 Read 打开相关文件；不要凭记忆推断 API 签名。
- 不要主动生成文档文件（`*.md`、`README` 等），除非用户明确要求。
- 不引入破坏性、不可逆、共享面外的操作（强制推送、删除未提交分支、发消息到外部服务、上传内容到第三方），除非用户再次确认。
- 保持 diff 聚焦：一次提交解决一个问题，不要顺手重构不相关区域。

## 1. 文件规模软约束（重要）

**单个源文件尽量不超过 300 行。**

- 适用范围：所有新建的 Kotlin / Java / TS / JS / Python 源文件。
- 超过 300 行的**现有文件**不强制拆分，以避免不必要的重构范围。但在对其做下一次结构性改动时，优先把可独立的部分（子组件、辅助类、SPI 边界、样式表、工具函数）抽出到同包下的独立文件。
- 下列情况可以合理突破上限：
  - Jetpack Compose 单屏 UI 文件（屏幕、Dialog、SideSheet），但子组件超过 2–3 个时仍应抽成 `internal` 文件；
  - 纯粹的测试文件（包含大量用例与内嵌 sample data）；
  - 生成或镜像协议的代码（如 Jellyfin DTO、第三方 API 的数据模型）；
  - 无法拆分的第三方适配 / 桥接层。
- 超过 400 行的非例外文件应在改动时写入拆分 TODO，并在本次或下次提交中处理。

## 2. 包与模块边界

- `core/`：纯 Java、JVM 可单测、禁止 Android SDK 引用。协议、TV workflow、领域模型放这里。
- `plugin-spi/`：`@Java SPI` 可加载的插件接口。不要依赖 core 以外的模块。
- `plugins/<name>/`：具体插件实现，**纯 Java**，不得引用 Android。插件按源站聚合（不是按功能拆分）。
- `app/`：Android TV 外壳、UI、播放器集成。
- 新增第三方库优先放到插件模块或 `app` 的 `runtimeOnly`；核心模块尽量零依赖。

## 3. 异步与任务模型

- app 层后台任务统一走 `CinePilotTaskRunner` 三档：
  - `runTask(label, work, onDone)`：有全局 BusyOverlay，适合页面切换类长任务；
  - `runSilentTask(work, onDone, onError)`：行内反馈，适合搜索 / 下载类可并发任务；
  - `runBlockingTask(work, onDone, onError)`：阻塞 UI，用于退出前清理等极端场景。
- 不要混用 `GlobalScope`、裸线程或 `Executors` 在 UI 流程中起任务。

## 4. 命名与文案

- 用户可见中文文案优先明确动作：按钮用 "关闭"、"重试"、"下载"，不用模糊的 "返回"、"好的"。
- 错误文案要告诉用户**做什么**（"点击重试"、"检查年份或季集编号"），而不只是陈述问题。
- 所有颜色派生走 `CinePilotPalette` 扩展函数；不要在组件里硬编码颜色。

## 5. 验证

- 改动插件或 core：至少运行 `:core:test` 与受影响插件模块的 test。
- 改动 Compose UI：至少执行 `:app:compileDebugKotlin`。
- 引入新的模块或 SPI 注册：确认 `settings.gradle.kts` 与对应 `META-INF/services/` 均已更新。
