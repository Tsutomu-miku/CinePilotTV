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

## 后续确认项

- Jellyfin 与 Emby 对 `/Users/{UserId}/Items/{ItemId}` 详情 endpoint 的差异需要在真实服务器或官方 OpenAPI 生成客户端上验证。
- 播放信息获取、direct play / direct stream / transcode 决策和 subtitle delivery profile 需要下一批单独建模。
- Quick Connect 只先写入需求和路线图，尚未进入 P0 请求规格。

