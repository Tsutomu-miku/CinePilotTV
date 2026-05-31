# 协议资料

本文档记录 CinePilot TV 当前采用的 Jellyfin / Emby 协议约定，以及这些约定在代码中的归属。

## 当前实现范围

`core/src/main/java/tv/cinepilot/core/protocol` 已建模以下请求规格：

- `GET /System/Info/Public`：服务器公开信息，用于未登录发现。
- `GET /System/Info`：已登录服务器信息，用于绑定 server id。
- `GET /Users/Public`：public users，用于 TV 登录界面减少输入。
- `POST /Users/AuthenticateByName`：用户名 / 密码登录，body 使用 `Username` 和 `Pw`。
- `POST /Sessions/Logout`：显式登出并撤销 token。
- `GET /Users/{UserId}/Views`：用户可见媒体库。
- `GET /Users/{UserId}/Items`：媒体库浏览。
- `GET /Users/{UserId}/Items/Resume`：继续观看候选。
- `GET /Users/{UserId}/Items/Latest`：最新媒体候选。
- `GET /Users/{UserId}/Items/{ItemId}`：媒体详情。
- `GET /Items/{Id}/PlaybackInfo`：获取播放候选、`PlaySessionId` 和 `MediaSources`。
- `GET /Videos/{Id}/master.m3u8`：生成 HLS 播放 URL，用于 direct stream / transcode 场景。
- `POST /Sessions/Playing`：开始播放上报。
- `POST /Sessions/Playing/Progress`：播放进度与暂停、seek、轨道变化等事件上报。
- `POST /Sessions/Playing/Stopped`：停止播放上报。

`MediaBrowserResponseMapper` 已建模以下响应映射：

- 系统信息响应到 `ServerIdentity`。
- 登录响应到 `AuthSession`。
- playback info 响应到 `PlaybackInfo`、`MediaSourceInfo` 和 `MediaStreamInfo`。
- 媒体列表和详情响应到 `MediaItemPage` 与 `MediaItemSummary`。

`MediaBrowserClient` 已把以下流程串成可测试编排：

- 服务器发现：`publicSystemInfo` -> `ServerIdentity`。
- 用户名 / 密码登录：`authenticateByName` -> `AuthSession` -> `SavedSession`。
- 会话持久化：`SessionScope` -> `InMemorySessionRepository` / `FileSessionRepository`。
- 播放信息：`playbackInfo` -> `PlaybackInfo`。
- 播放源选择：`PlaybackInfo` -> `PlayableMedia`。
- 媒体库浏览：`userViews` / `items` / `resumeItems` / `latestItems` / `item` -> 媒体条目模型。
- TV 首页组合：`HomeRowsLoader` -> `HomeRow` 列表。
- 播放上报调度与发送：`PlaybackSessionController` -> `PlaybackCheckInScheduler` -> `PlaybackCheckIn` -> `MediaBrowserClient.sendPlaybackCheckIn`。
- 登出：`logout` -> 撤销本地 saved session。

## 认证头策略

- Jellyfin 默认使用 `MediaBrowser` scheme。
- Emby 默认使用 `Emby` scheme。
- 请求头使用 `X-Emby-Authorization` 承载客户端身份字段。
- 已登录请求额外带 `X-Emby-Token`，同时在 authorization value 中带 token，兼容媒体浏览协议生态里的常见服务端行为。

## 来源

- Jellyfin Kotlin SDK 认证文档说明用户名密码登录、access token、Quick Connect 和 passwordless 用户语义：<https://kotlin-sdk.jellyfin.org/guide/authentication.html>
- Emby 用户认证文档说明 public users、`/Users/AuthenticateByName`、`X-Emby-Token`、401 token 失效、登出和 server id 绑定：<https://dev.emby.media/doc/restapi/User-Authentication.html>
- Emby `POST /Users/AuthenticateByName` 参考页说明 endpoint、body 字段 `Username` / `Pw` 和返回的 `AccessToken` / `ServerId`：<https://dev.emby.media/reference/RestAPI/UserService/postUsersAuthenticatebyname.html>
- Emby `GET /Users/{UserId}/Items` 参考页说明媒体浏览查询参数和 `BaseItemDto` 形状：<https://dev.emby.media/reference/RestAPI/ItemsService/getUsersByUseridItems.html>
- Emby `GET /Users/Public` 参考页说明 public users 用于登录界面展示：<https://dev.emby.media/reference/RestAPI/UserService/getUsersPublic.html>
- Emby `GET /Items/{Id}/PlaybackInfo` 参考页说明 playback info 返回播放候选、`PlaySessionId`、`MediaSources`、`DirectStreamUrl`、`TranscodingUrl` 和字幕 delivery 信息：<https://dev.emby.media/reference/RestAPI/MediaInfoService/getItemsByIdPlaybackinfo.html>
- Emby HLS 文档说明 `/Videos/{Id}/master.m3u8` 是 HLS 入口，必需参数包括 path 里的 `Id`、`MediaSourceId` 和 `DeviceId`：<https://dev.emby.media/doc/restapi/Http-Live-Streaming.html>
- Emby `GET /Videos/{Id}/master.m3u8` 参考页说明 start time 使用 ticks，并列出音轨、字幕、分辨率、码率、codec 等参数：<https://dev.emby.media/reference/RestAPI/DynamicHlsService/getVideosByIdMasterM3u8.html>
- Jellyfin TypeScript SDK 暴露 `getPlaybackInfo`、`getPostedPlaybackInfo` 和 `openLiveStream`，其中 POST 版支持 max bitrate、start ticks、音轨、字幕、direct play / direct stream / transcoding 等参数：<https://typescript-sdk.jellyfin.org/functions/generated-client.MediaInfoApiFp.html>

## 后续确认项

- Jellyfin 与 Emby 对 `/Users/{UserId}/Items/{ItemId}` 详情 endpoint 的差异需要在真实服务器或官方 OpenAPI 生成客户端上验证。
- direct play / direct stream / transcode 的基础选择规则已在 `PlaybackSourceSelector` 建模；真实服务器联调后需要继续用兼容矩阵校准。
- Android 入口尚未接入 session repository 存储路径。
- Media3 设备能力到 `DeviceProfile` / codec 参数的映射需要在 Android 层可运行后补齐。
- `TvWorkflow` 已定义服务器输入、登录、首页、详情和播放器路由状态，但 Android UI 尚未渲染这些状态。
- Quick Connect 只先写入需求和路线图，尚未进入 P0 请求规格。
