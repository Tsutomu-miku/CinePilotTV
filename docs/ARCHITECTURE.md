# 架构说明

## 运行时边界

`app` 负责 Android TV 进程集成、生命周期、焦点处理和 Media3 播放器宿主。

`core` 负责媒体浏览协议身份、endpoint 构造、播放上报语义和纯产品规则。

后续的 `platform` 适配器会负责 HTTP transport、token 持久化、图片加载和 Android 专用存储。

UI 界面应该调用应用控制器或 store。界面组件不能直接构造 Jellyfin / Emby URL、认证请求头或播放上报载荷。

## 协议层

协议层负责：

- 服务器地址规范化。
- 服务器类型表示。
- 认证请求头构造。
- 登录、发现、用户、媒体库和播放相关请求规格。
- 播放 endpoint 路径。
- Jellyfin 与 Emby 共享的播放载荷字段。
- 播放位置 ticks 转换。

这个边界上的缺陷应该通过协议类型和测试修复，而不是靠某个界面里的特殊分支掩盖。

请求规格只表达 method、path、headers、query 和 body。真实网络发送、重试、超时、TLS 证书策略和响应解析属于后续 platform adapter。

## 播放层

播放层将负责 Media3 player 设置、media source 创建、轨道选择、字幕处理和播放 check-in 调度。它把领域播放事件报告给协议层，不关心界面如何渲染。

播放信息请求、HLS URL 构造和播放 check-in 请求规格属于 `core`；Media3 只消费已经选出的播放 URL 和轨道选择结果。

播放源选择规则在 `core` 中执行：优先 direct play，其次 direct stream，最后 transcode。相对 URL 必须按服务器基础地址解析；缺少可用 URL 但支持转码时，由 HLS 请求规格补齐。

## 状态与持久化

会话状态按服务器和用户身份划分作用域。持久化记录必须包含足够身份信息，避免用户修改 URL 或切换多服务器后把 token 发给错误服务器。

## 验证策略

- 协议和领域行为在 JVM 上测试。
- Android instrumentation 只用于生命周期、焦点和 Media3 这类很难便宜地在 JVM 上覆盖的行为。
- TV 遥控器行为、模拟器播放和服务器兼容矩阵保留手工 QA 记录。
