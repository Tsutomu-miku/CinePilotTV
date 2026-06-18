# CinePilot TV

CinePilot TV 是一个面向 Android TV 的原生播放器，用来连接兼容 Jellyfin / Emby 媒体浏览协议的个人媒体服务器。项目先把协议身份、播放上报和电视遥控体验定清楚，再逐步扩展成完整播放器。

## 状态

原型脚手架。当前重点是 P0 协议兼容、播放会话语义，以及可接入 Android Studio / Android SDK 的 TV 模块边界。

## 文档

- [需求说明](docs/REQUIREMENTS.md)
- [路线图与进度](docs/ROADMAP.md)
- [项目规格](docs/PROJECT_SPEC.md)
- [架构说明](docs/ARCHITECTURE.md)
- [代码结构](docs/CODE_STRUCTURE.md)
- [协议资料](docs/PROTOCOL_NOTES.md)
- [验证说明](docs/VERIFICATION.md)

## 快速开始

```bash
./scripts/check.sh
./scripts/bootstrap-gradle-wrapper.sh
./gradlew :core:test :app:assembleDebug
./scripts/install-debug-apk.sh
```

## GitHub Action 打包

仓库包含 `Android APK` workflow，会在 push、pull request 和手动触发时运行：

- 设置 JDK 17、Gradle 和 Android SDK 35。
- 执行 `./gradlew --no-daemon :core:test :app:assembleDebug`。
- 上传 `cinepilot-tv-debug-apk` artifact，里面包含可直接安装到设备或模拟器的 debug APK。

## 脚本

- `./scripts/check.sh` 使用 `javac` 编译 JVM 协议核心、运行协议测试，并确认项目指导文档存在。
- `./scripts/bootstrap-gradle-wrapper.sh` 在没有全局 Gradle 时下载临时 Gradle 并生成 wrapper。
- `./scripts/install-debug-apk.sh` 构建、安装并启动 debug APK，需要已连接 Android TV 设备或模拟器。
- `./gradlew :core:test` 已可运行，并会执行协议、TV workflow 和 HTTP transport 集成测试；Android SDK 可用时，`./gradlew :app:assembleDebug` 可生成 debug APK。
- Jellyfin 登录页已具备最小 Quick Connect 入口，可在服务器启用 Quick Connect 时减少 TV 遥控器输入。
- 详情页已提供继续播放、从头播放、低码率播放，以及播放前音轨 / 字幕选择入口。

## 仓库结构

```text
app/        Android TV 应用外壳，以及后续 UI / 播放器集成
core/       JVM 可测试的协议与产品领域规则
docs/        产品、架构和执行指导文档
plugin-spi/  插件 SPI：用户数据同步、播放上报、字幕搜索等标准接口
plugins/    具体插件实现（bangumi 同步 / 中文字幕源）
scripts/    本地验证命令
```

## 字幕检索
CinePilot 通过 `SubtitleSearchPlugin` SPI 对外暴露在线字幕搜索与下载接口，插件按需加载当前启用的插件；默认不强制任何单一提供商。当前内建的中文字幕插件（`plugins/zimuku`）由一个单一聚合插件实现，其内部覆盖：

- **射手网（shooter.cn）公开 API
- **字幕库**（zimuku.org）HTML 抓取
- **迅雷字幕**（sub.xmp.sandai.net）公开 JSON API

并支持从 zip / 7z / rar 打包字幕包解压与 GB18030 文件名识别、按语言优先级（简英双语 > 繁英双语 > 简体 > 繁体 > 英文）自动在归档内挑选最适合中文用户的字幕文件。

### 致谢与参考

中文字幕插件在实现中参考并借鉴了以下开源项目的 URL 模式与思想，在此表示感谢：

- **ChineseSubFinder** ([ChineseSubFinder/ChineseSubFinder](https://github.com/ChineseSubFinder/ChineseSubFinder) — 射手网 API、迅雷 API、字幕库搜索 URL 模式、中文压缩包 GB18030 文件名回退、语言优先级排序、`hasChineseLang() 语言匹配思路、随机 UA / Referer 等反爬实践，以及按 1.2万 / 简体双语优先等中文本地化经验。CinePilot 不是 CSF 的复刻或分支，仅在本插件实现部分**没有**直接复制源码，仅学习其协议约定和接口设计思想、数据转换与 HTTP 实践。

### 对应提供者版权与使用
所使用的三个站点接口均为公开可访问的网络服务，请在遵守对应服务条款与法律范围内使用；**请支持并**，尤其是射手、字幕库和迅雷版权所有。下载内容版权请务必只下载你合法拥有视频版本对应影视对应影视素材的字幕，请遵守版权所有版权，仅供个人使用，请勿用于任何商业分发。

## 许可证
除非另有说明，仓库代码按 MIT 许可证发布，完整文本见根目录 [LICENSE](LICENSE)。
