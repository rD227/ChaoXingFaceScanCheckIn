# FaceScan：上游代码清理实验

这是 [aquamarine5/ChaoxingSignFaker](https://github.com/aquamarine5/ChaoxingSignFaker) 的实验性衍生分支，基线为 [`92a8f66fe46a5058ad6e9f45a62d80e5b9127894`](https://github.com/aquamarine5/ChaoxingSignFaker/commit/92a8f66fe46a5058ad6e9f45a62d80e5b9127894)。原作者为 aquamarine5 与 Brainspark，源码与署名保留，沿用 [AGPL-3.0](LICENSE)。本分支由 rD227 项目维护，未经上游作者背书。

目标是拆除作者个人服务依赖，留下可在手机本机编译、研究的 Android 代码。不需要部署我们自己的后端或购买云服务器；登录、课程、人脸图片和签到相关请求仍由手机访问超星及其服务方。

## 已清理

- 打赏卡片、收款图片、签到完成后的打赏计时器和弹窗。
- 友盟统计、Sentry 上报、作者的统计接口与排行榜。
- Stackbricks 更新服务、发布插件、GitHub Packages 凭据要求和原发布流水线。
- 作者域名的远程限制查询、个人歌单入口、渠道与应用名称强绑定检查。
- 原作者的地图 Key、签名路径、发布配置和原应用图标。应用使用独立包名 `io.github.rd227.facescan.cleaned`。
- 带登录信息的 HTTP 分享链接改为 `facescan-clean://import` 本地协议；分享码只应交给需要导入的设备。此分支不生成指向作者服务器的账号链接。
- 关闭系统备份与设备数据迁移，防止会话及人脸配置被自动迁出。

人脸照片选择、保存、二维码解析和原有签到结果处理仍保留。原先位于统计工具中的 MD5 计算独立为 `DigestHelper.kt`，避免删除统计时破坏原协议逻辑。异常处理只在本机记录异常类型，并继续传播协程取消。

## 构建

需要 JDK 21、Android SDK 和网络。依赖通过 Google Maven、Maven Central，以及限定为 PatternLocker 的 JitPack 源获取。无需作者的 GitHub Packages Token、签名文件或发布账号。

```bash
git clone --branch upstream-cleanup https://github.com/rD227/ChaoXingFaceScanCheckIn.git
cd ChaoXingFaceScanCheckIn
printf 'sdk.dir=/你的/Android/Sdk\n' > local.properties
chmod +x gradlew
./gradlew :app:assembleDebug
```

产物为 `app/build/outputs/apk/debug/app-debug.apk`，调试版本使用本机生成的 Android 调试签名。当前保留上游的 ARM64 配置，要求 Android 8.0 及以上。

二维码和人脸相关逻辑不要求我们自建服务器。地图与定位仍使用上游的百度 SDK：若要测试地图，需要为新的包名与实际签名申请自己的 Key，然后以 `./gradlew -PbaiduMapsApiKey=你的Key :app:assembleDebug` 构建。仓库不附带任何人的可用 Key。

GitHub Actions 会构建调试 APK，入口在仓库的 Actions 页面。构建、静态检查与真机结果见 [清理记录](docs/CLEANUP.md)。本分支未用真实账号验证完整签到流程，不能据编译通过宣称所有服务端限制均可通过。

## 后续代码如何写

本分支用于研究和拆分上游代码，当前仍有大量上游 Compose 页面。我们自己的首页、照片管理与操作流程应独立编写；可复用的协议层逐步整理为单独模块。修改上游代码时保留其来源与许可记录。[main 分支](https://github.com/rD227/ChaoXingFaceScanCheckIn/tree/main) 是此前独立编写的原生 Android 小型实验，与本分支功能范围不同。

上游原始说明保存在 [UPSTREAM_README.md](docs/UPSTREAM_README.md)，其中包括作者对换皮与二次分发的态度；本分支明确标识来源、修改范围与维护者。详见 [来源记录](UPSTREAM.md)。
