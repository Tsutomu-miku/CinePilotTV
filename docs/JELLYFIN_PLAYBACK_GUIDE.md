# Jellyfin / Emby 播放协议项目指南

本文是 CinePilot TV 的播放链路开发准则。所有规则先按 Jellyfin 官方文档、官方 API / SDK、Jellyfin 官方源码审计；只有真机调试得到的内容会标为“项目观察”，不能冒充官方结论。

Emby 兼容可以复用大部分 Emby/Jellyfin 历史 API 形状，但本文件的证据源只审计 Jellyfin。涉及 Emby 差异时必须另开文档补证。

## 证据分级

- A 官方说明：Jellyfin 官网文档，例如 codec support、transcoding。
- B 官方 API：`api.jellyfin.org` OpenAPI 和 `@jellyfin/sdk` 生成文档。
- C 官方源码：`jellyfin/jellyfin` 服务端源码。
- D 项目观察：本项目真机、日志、服务器表现；只能指导排查，不能当作通用协议事实。

## 逐条审计

| 准则 | 审计结果 | 证据 |
| --- | --- | --- |
| 播放前先走 playback info 协商。 | 保留。 | A：Jellyfin 文档说明客户端发送 transcoding profile，服务器根据能力选择输出。C：`MediaInfoController.GetPostedPlaybackInfo` 将 `DeviceProfile`、码率、音轨、字幕和 direct/transcode 开关交给 `MediaInfoHelper.SetDeviceSpecificData`。 |
| `TranscodingUrl` / `DirectStreamUrl` 是协商产物，改轨道时优先复用。 | 调整后保留。 | B：`MediaSourceInfo` 暴露 `SupportsDirectPlay`、`SupportsDirectStream`、`SupportsTranscoding`、`TranscodingContainer`、`TranscodingSubProtocol`、`TranscodingUrl`。C：`MediaInfoHelper` 用 `StreamInfo.ToUrl(...)` 生成 `TranscodingUrl`。OpenAPI/SDK 没有承诺“可随意改 query”，所以复用是项目约束。 |
| 不要从零手写 `/Videos/{id}/master.m3u8`。 | 保留为项目禁令。 | B/C：`master.m3u8` 参数很多，包含 `mediaSourceId`、`audioCodec`、`videoCodec`、`audioStreamIndex`、`subtitleStreamIndex`、`subtitleMethod`、copy 策略、码率、profile 等。SDK 明确说明 `audioCodec` / `videoCodec` 省略时服务端会用 URL 扩展名自动选择。D：本项目曾因手写参数不完整触发 HLS 下级请求异常。 |
| 普通文字字幕优先外部字幕或 HLS，不默认烧录。 | 调整后保留。 | A：Jellyfin 文档说明字幕可能导致 direct stream 或烧录视频转码，烧录字幕是最重的转码方式。B/C：`SubtitleDeliveryMethod` 有 `External`、`Hls`、`Embed`、`Encode`、`Drop`；`Encode` 含义是烧录到视频轨。具体哪种字幕该用哪种方式由服务端 profile 决定，客户端不能硬编码所有格式。 |
| `DeliveryUrl` 只在外部字幕时可靠。 | 保留。 | C：`MediaInfoHelper.SetDeviceSpecificSubtitleInfo` 会写 `DeliveryMethod`；只有 `DeliveryMethod == External` 时才设置 `DeliveryUrl`。B：`MediaStream` 同时有 `DeliveryMethod` 和 `DeliveryUrl`。 |
| `SubtitleStreamIndex` 正确不等于画面会显示字幕。 | 保留。 | B：HLS 请求只说明可传 `subtitleStreamIndex` / `subtitleMethod`；MediaStream 还包含 `DeliveryMethod`、`DeliveryUrl`、`IsTextSubtitleStream`。A：字幕兼容会影响 direct stream 或转码。D：本项目已出现 index 正确但画面未显示。 |
| `SubtitleMethod=Encode` 会触发视频重编码。 | 保留，但措辞改为“代表烧录路径，通常带来视频转码”。 | A：烧录字幕属于视频转码且成本最高。B/C：`Encode` 枚举说明是把字幕烧录进视频轨。 |
| device profile 是播放协商输入。 | 保留。 | A：客户端发送 transcoding profile，服务器按能力选择输出。B：`PlaybackInfoDto.DeviceProfile` 字段存在。C：`SetDeviceSpecificData` 使用 `DeviceProfile` 创建 `MediaOptions` 并调用 `StreamBuilder.GetOptimalVideoStream`。 |
| HLS transcode profile 必须明确 codec。 | 调整为“需要明确声明客户端期望，不能依赖手写 URL 猜测”。 | B：视频流请求支持 `audioCodec`、`videoCodec`；SDK 说明省略时由 URL 扩展名自动选择。A：输出受输入、目标码率、输入/输出 codec、客户端约束影响。 |
| 调试信息必须展示播放方式、URL 来源、query、字幕方法。 | 保留为项目工程要求。 | B/C：这些字段来自 `MediaSourceInfo`、`MediaStream` 和 HLS query。不是 Jellyfin 官方 UI 要求，但对本项目排查 direct play / transcode / 字幕问题必要。 |

## 开发流程

1. 获取 item 详情和 media source，展示视频、音频、字幕流的 `Index`、`Codec`、`DisplayTitle`、`Language`、`IsDefault`、`IsForced`、`IsTextSubtitleStream`、HDR / Dolby Vision 字段。
2. 播放前 POST `Items/{itemId}/PlaybackInfo`，body 里放 `DeviceProfile`、`MediaSourceId`、`AudioStreamIndex`、`SubtitleStreamIndex`、`MaxStreamingBitrate`、direct/transcode 开关。
3. 根据返回的 `MediaSources` 选择候选，不在 UI 层重新发明选择算法。优先使用服务端给出的 direct play 或 `TranscodingUrl`。
4. 如果用户在详情页或播放中切换音轨 / 字幕，重新获取 playback info 或基于已协商 URL 做最小改动；必须保留原 query 中 codec、bitrate、container、copy 策略和 `PlaySessionId` 等上下文。
5. 播放失败时先导出诊断：playback info 请求、选中的 media source、`TranscodingUrl` path/query、`MediaStream.DeliveryMethod`、HLS master/variant/segment 状态、Media3 text tracks。

## 字幕策略

- `DeliveryMethod=External` 且有 `DeliveryUrl`：作为 Media3 外挂字幕轨附加。
- `DeliveryMethod=Hls`：优先播放服务端返回的 HLS URL，并检查 master playlist 是否有字幕媒体组；不要额外附加同一字幕的外部 URL。
- `DeliveryMethod=Encode`：这是烧录路径，详情页应提示可能转码；播放调试信息必须显示它。
- `DeliveryMethod=Embed`：表示字幕嵌入到文件或流中，客户端要依赖播放器轨道选择能力。
- 没有 `DeliveryMethod` 或 `DeliveryUrl` 时，不要猜；重新用具体 `SubtitleStreamIndex` 请求 playback info，并把返回结果写入诊断。

## 验证清单

- playback info 请求是否包含目标 `DeviceProfile`、`MediaSourceId`、音轨、字幕和码率。
- 返回的 `SupportsDirectPlay` / `SupportsDirectStream` / `SupportsTranscoding` 是否符合预期。
- `TranscodingUrl` 是否来自服务端返回，还是本项目构造；如果是构造，必须说明来源和覆盖字段。
- HLS master playlist、variant playlist、第一个 segment 是否都能 200。
- 字幕选择后，`MediaStream.DeliveryMethod`、HLS manifest、Media3 text track 三者是否一致。
- 诊断和日志不得泄露 `api_key`、`X-Emby-Token`、`X-MediaBrowser-Token`。

## 已降级为项目观察的内容

- 某次手写 HLS query 不完整时，Jellyfin 下级 playlist/segment 曾出现异常参数并返回 400。这不是官方保证，只说明我们不能靠猜参数开发。
- 当前应用出现过“字幕行选中正确但画面不显示”，不能只改 UI 状态，必须检查服务端交付方式和 Media3 track。

## 官方参考

- Jellyfin Codec Support: https://jellyfin.org/docs/general/clients/codec-support/
- Jellyfin Transcoding: https://jellyfin.org/docs/general/post-install/transcoding/
- Jellyfin OpenAPI: https://api.jellyfin.org/openapi/
- Jellyfin SDK PlaybackInfoDto: https://typescript-sdk.jellyfin.org/interfaces/generated-client.PlaybackInfoDto.html
- Jellyfin SDK MediaSourceInfo: https://typescript-sdk.jellyfin.org/interfaces/generated-client.MediaSourceInfo.html
- Jellyfin SDK MediaStream: https://typescript-sdk.jellyfin.org/interfaces/generated-client.MediaStream.html
- Jellyfin SDK video stream request: https://typescript-sdk.jellyfin.org/interfaces/generated-client.VideosApiGetVideoStreamByContainerRequest.html
- Jellyfin SDK SubtitleDeliveryMethod: https://typescript-sdk.jellyfin.org/variables/generated-client.SubtitleDeliveryMethod.html
- Jellyfin source MediaInfoController: https://github.com/jellyfin/jellyfin/blob/master/Jellyfin.Api/Controllers/MediaInfoController.cs
- Jellyfin source MediaInfoHelper: https://github.com/jellyfin/jellyfin/blob/master/Jellyfin.Api/Helpers/MediaInfoHelper.cs
- Jellyfin source DynamicHlsController: https://github.com/jellyfin/jellyfin/blob/master/Jellyfin.Api/Controllers/DynamicHlsController.cs
- Jellyfin source SubtitleDeliveryMethod: https://github.com/jellyfin/jellyfin/blob/master/MediaBrowser.Model/Dlna/SubtitleDeliveryMethod.cs
