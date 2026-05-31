# 验证说明

## 当前可运行检查

本仓库当前默认健康检查是：

```bash
./scripts/check.sh
```

它会验证：

- 中文项目文档存在。
- Gradle 多模块结构包含 `:app` 和 `:core`。
- Android app 依赖 `:core`。
- Android runtime、TV workflow controller、最小 TV UI、Media3 player host 和 Media3 playback bridge 入口存在。
- Android manifest 允许 HTTP 明文流量，Media3 player host 会授权播放 URL。
- `core` 的协议、媒体库、播放、session 和 TV workflow JVM 测试通过。

## Android 构建检查

当前机器没有 `ANDROID_HOME`，也没有全局 `gradle` 命令，因此本线程尚未完成 Android 编译、模拟器启动或真实设备播放验证。

具备 Android SDK 后，应运行：

```bash
./gradlew :core:test :app:assembleDebug
```

如果没有 Gradle wrapper，需要先用 Android Studio 或系统 Gradle 生成 wrapper，再运行：

```bash
./gradlew wrapper
./gradlew :core:test :app:assembleDebug
```

## 设备 QA

Android TV 设备或模拟器上需要验证：

- 可用 D-pad 完成服务器输入、登录、首页浏览和详情打开。
- 点击播放后 Media3 player 能打开可播放 URL。
- HTTP 本地服务器地址如 `http://host:8096` 可以连接。
- 播放 URL 携带 token 后，Media3 能访问受保护流。
- 播放开始、暂停、seek、停止会触发 Jellyfin / Emby 播放上报。
- token 失效时只影响对应服务器并回到登录。
- 多服务器 session 不串用。
