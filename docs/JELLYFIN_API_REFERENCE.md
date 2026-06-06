# Jellyfin API 渐进式速查

本文给开发时随手查，不代替官方文档。新增播放相关代码时先读本文件，再读 `docs/JELLYFIN_PLAYBACK_GUIDE.md` 的审计规则。

## 0. 查证顺序

1. 先看 Jellyfin OpenAPI / `@jellyfin/sdk` 字段是否存在。
2. 再看 Jellyfin 官网文档确认语义和成本。
3. 最后看 Jellyfin 服务端源码确认实际控制流。
4. 如果只有本项目真机现象，写成“项目观察”，不要写成协议事实。

## 1. 认证和请求

- 使用 Jellyfin / Emby token 访问 API。
- 播放 URL 离开 app HTTP transport 后仍要带认证；本项目用 `PlaybackUrlAuthorizer` 追加 `api_key`。
- 任何诊断、分享、日志都必须脱敏 token 和 `api_key`。

## 2. 媒体详情要看的字段

从 item / media source 的 `MediaStreams` 里展示和选择：

- 通用：`Index`、`Type`、`Codec`、`DisplayTitle`、`Language`、`Title`、`IsDefault`、`IsForced`。
- 视频：`Width`、`Height`、`BitRate`、`BitDepth`、`Profile`、`Level`、`VideoRange`、`VideoRangeType`、`ColorPrimaries`、`ColorTransfer`、`ColorSpace`、`DvProfile`、`Hdr10PlusPresentFlag`。
- 音频：`Channels`、`ChannelLayout`、`SampleRate`、`AudioSpatialFormat`。
- 字幕：`IsTextSubtitleStream`、`IsExternal`、`IsExternalUrl`、`DeliveryMethod`、`DeliveryUrl`。

## 3. 播放协商

首选：

```text
POST /Items/{itemId}/PlaybackInfo
```

常用 body 字段：

- `UserId`
- `MediaSourceId`
- `AudioStreamIndex`
- `SubtitleStreamIndex`
- `MaxStreamingBitrate`
- `MaxAudioChannels`
- `StartTimeTicks`
- `DeviceProfile`
- `EnableDirectPlay`
- `EnableDirectStream`
- `EnableTranscoding`
- `AllowVideoStreamCopy`
- `AllowAudioStreamCopy`
- `AlwaysBurnInSubtitleWhenTranscoding`

服务端行为要点：

- query 参数为了兼容仍可用，但源码注释说明 query 已 obsolete，且 query 优先级高于 body。
- 有 `DeviceProfile` 时，服务端会为每个 media source 调用 `SetDeviceSpecificData`。
- `SetDeviceSpecificData` 会决定 direct play / direct stream / transcode、生成 `TranscodingUrl`、填默认音轨和字幕交付信息。

## 4. 选择播放 URL

按这个顺序判断：

1. `SupportsDirectPlay=true` 且可构造原始 stream URL：走 direct play。
2. 有服务端返回的 `TranscodingUrl`：优先复用。
3. 只有必须手写 HLS 时，先从同一次 playback info 复制上下文，再最小覆盖目标字段。

不要只用 URL 后缀判断 direct/transcode。应同时看 `PlayMethod`、`SupportsDirectPlay`、`SupportsDirectStream`、`SupportsTranscoding`、`TranscodingContainer`、`TranscodingSubProtocol`、`TranscodeReasons`。

## 5. HLS 参数

`/Videos/{itemId}/master.m3u8` 和相关 video stream endpoint 常见参数：

- 身份：`itemId`、`mediaSourceId`、`deviceId`、`playSessionId`
- 容器：`container`、`segmentContainer`、`segmentLength`、`minSegments`
- 音频：`audioCodec`、`audioStreamIndex`、`audioBitRate`、`audioChannels`、`maxAudioChannels`、`transcodingMaxAudioChannels`
- 视频：`videoCodec`、`videoStreamIndex`、`videoBitRate`、`width`、`height`、`maxWidth`、`maxHeight`、`profile`、`level`
- 字幕：`subtitleStreamIndex`、`subtitleMethod`、`subtitleCodec`
- 策略：`enableAutoStreamCopy`、`allowVideoStreamCopy`、`allowAudioStreamCopy`、`enableAdaptiveBitrateStreaming`、`alwaysBurnInSubtitleWhenTranscoding`
- 诊断：`transcodeReasons`、`tag`、`params`

SDK 明确说明 `audioCodec` 和 `videoCodec` 省略时服务端会用 URL 扩展名自动选择，所以手写 HLS 时不能丢这些关键上下文。

## 6. 字幕交付

`SubtitleDeliveryMethod` 枚举：

- `External`：外部字幕文件。通常看 `DeliveryUrl`。
- `Hls`：独立 HLS 字幕流。
- `Embed`：嵌入文件或流。
- `Encode`：烧录到视频轨。
- `Drop`：不交付字幕。

开发规则：

- 详情页选择字幕后，重新请求 playback info 或复用协商上下文，不能只改 app 内状态。
- `DeliveryUrl` 只代表外部字幕路径；HLS / Embed / Encode 不一定有 `DeliveryUrl`。
- 播放中切换字幕时，要让 Media3 和服务端交付方式一致：外部字幕走 text track，HLS 字幕看 manifest，烧录字幕看转码 URL。

## 7. 播放上报

播放状态要通过官方 `PlaystateApi` 上报：

- start / progress / stopped 对应 `reportPlaybackStart`、`reportPlaybackProgress`、`reportPlaybackStopped`。
- `PlaybackProgressInfo` 包含 `ItemId`、`MediaSourceId`、`AudioStreamIndex`、`SubtitleStreamIndex`、`IsPaused`、`PositionTicks`、`PlayMethod`、`PlaySessionId`、`SessionId`。
- 同一次播放尽量使用同一 `PlaySessionId`。
- 上报里不要混入 UI 焦点状态；焦点属于 TV workflow。

## 8. 排障速查

- 播放失败：先看 playback info response，再看最终 URL 来源。
- 字幕不显示：看 `MediaStream.DeliveryMethod`，再看 HLS manifest 是否有字幕组，最后看 Media3 tracks。
- 服务器说转码：看 `TranscodeReasons`，不要猜。
- Segment 400/500：检查手写 URL 是否丢了 codec、container、stream index、copy 策略或 play session。
- UI 显示“选中了字幕”但画面没字：这只证明 app 状态变了，不证明服务端交付或播放器轨道生效。

## 9. 常用官方入口

- OpenAPI: https://api.jellyfin.org/openapi/
- SDK docs: https://typescript-sdk.jellyfin.org/
- SDK PlaystateApi: https://typescript-sdk.jellyfin.org/classes/generated-client.PlaystateApi.html
- SDK PlaybackProgressInfo: https://typescript-sdk.jellyfin.org/interfaces/generated-client.PlaybackProgressInfo.html
- Codec support: https://jellyfin.org/docs/general/clients/codec-support/
- Transcoding: https://jellyfin.org/docs/general/post-install/transcoding/
- Server source: https://github.com/jellyfin/jellyfin
