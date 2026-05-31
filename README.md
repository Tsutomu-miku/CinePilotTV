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

## 脚本

- `./scripts/check.sh` 使用 `javac` 编译 JVM 协议核心、运行协议测试，并确认项目指导文档存在。
- `./scripts/bootstrap-gradle-wrapper.sh` 在没有全局 Gradle 时下载临时 Gradle 并生成 wrapper。
- `./scripts/install-debug-apk.sh` 构建、安装并启动 debug APK，需要已连接 Android TV 设备或模拟器。
- `./gradlew :core:test` 已可运行，并会执行协议、TV workflow 和 HTTP transport 集成测试；Android SDK 可用时，`./gradlew :app:assembleDebug` 可生成 debug APK。

## 仓库结构

```text
app/      Android TV 应用外壳，以及后续 UI / 播放器集成
core/     JVM 可测试的协议与产品领域规则
docs/     产品、架构和执行指导文档
scripts/  本地验证命令
```
