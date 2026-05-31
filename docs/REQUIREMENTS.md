# 需求说明

## 产品意图

CinePilot TV 是一个客厅优先的 Android TV 播放器，服务于使用 Jellyfin 或 Emby 管理个人媒体库的用户。第一个可用工作流是：连接服务器、认证登录、浏览可播放媒体、开始播放、把播放进度回报给服务器，并在之后从同一媒体项恢复播放。

## 文档规则

- `README.md` 只放项目定位和常用命令。
- 优先级、进度和实施批次放在 `docs/ROADMAP.md`。
- 产品行为和不变量放在本文档。
- 工程方向放在 `docs/PROJECT_SPEC.md`。
- 运行时边界放在 `docs/ARCHITECTURE.md`。

## 优先级与身份规则

- 媒体服务器的身份由规范化后的基础 URL 和 `/System/Info` 返回的 server id 共同确定。
- 已保存的会话 token 必须绑定到 server id、服务器 URL、user id、客户端名、device id 和应用版本。
- 媒体项的身份是 server id 加 item id。标题、季号、集号、海报和时长都是可变元数据，不是身份。
- 播放会话的身份是 server id、user id、item id、media source id 和 play session id。
- 播放位置使用媒体浏览协议里的 ticks，1 tick 等于 100 纳秒。界面可以使用毫秒，但 API 载荷必须使用 ticks。
- Jellyfin 与 Emby 的差异必须隔离在协议适配器后面。UI 代码不应该基于服务器类型分支，除非是在展示兼容性提示。

## 当前范围

### 服务器连接

- 用户必须能输入 HTTP 或 HTTPS 服务器地址。
- 系统必须规范化尾部斜杠，同时保留 `/jellyfin` 或 `/emby` 这样的路径前缀。
- 应用必须能记住多个服务器，且不能在服务器之间共享 token。
- 应用必须检测并存储服务器类型：`jellyfin`、`emby` 或 `unknown`。

### 认证

- 用户必须能用用户名和密码登录。
- 当服务器允许时，应用应支持 Jellyfin Quick Connect。
- TV 登录界面可以展示 public users，但选择某个用户后仍必须认证。
- token 被撤销或失效时，只应让用户回到该服务器的登录界面。

### 媒体库浏览

- 用户必须能浏览媒体库、继续观看、最新媒体、电影、剧集、季、集和可播放文件夹。
- UI 在排序、筛选、焦点移动和页面恢复时必须保留媒体项身份。
- 图片加载失败不能让某一行或某个媒体项变得不可访问。
- 详情页标题过长时不得遮挡第一行元信息；标题应换行、截断或压缩到安全区域内。
- 详情页应次级展示播放相关技术信息，包括分辨率、容器 / 编码、大小、声道、HDR / Dolby Vision / Dolby Atmos 等能力，以及可用字幕概览。
- 字幕必须支持选择；选择字幕后，播放准备和实际播放必须使用对应字幕 stream index，并在需要 HLS 字幕交付时携带协议参数。

### 播放

- 播放器必须优先 direct play，其次 direct stream，最后在服务器需要时 transcode。
- 播放器必须使用 Android Media3 做播放能力检查和实际播放。
- 开始播放时必须调用 `/Sessions/Playing`。
- 播放进度必须约每 10 秒调用 `/Sessions/Playing/Progress`，并且在暂停、seek、切换音轨、切换清晰度、切换字幕或调整播放速度后立即上报。
- 停止播放时必须调用 `/Sessions/Playing/Stopped`。
- 音轨、字幕轨、字幕偏移、播放速度、暂停状态、是否可 seek、播放方式、media source id 和 play session id 必须一致上报。

### Android TV 体验

- 应用必须能完全通过 D-pad 遥控器使用。
- 焦点必须可见、稳定，且不能困在不可播放的行里。
- 文本输入应尽量减少遥控器打字，优先使用 public users、已保存服务器和 Quick Connect。
- 启动后的首屏必须是服务器选择页，或上一次已认证用户的首页。
- 播放器内按 Back 退出播放前必须二次确认，避免误操作导致播放中断。

## 外部 API 备注

- Jellyfin 认证通常返回 access token，后续 API 调用需要带上该 token。服务器启用时，Jellyfin Quick Connect 是更适合电视端的登录方式。
- Emby 文档包含 `/Users/AuthenticateByName`、通过 `X-Emby-Token` 复用 token、通过 `/Sessions/Logout` 登出，以及用 `/System/Info` 的 server id 约束已保存 token。
- Emby 文档包含 `/Sessions/Playing`、`/Sessions/Playing/Progress`、`/Sessions/Playing/Stopped` 这些播放 check-in；CinePilot 会把这些视为共享媒体浏览协议语义，除非某个服务器专用适配器证明需要不同处理。
