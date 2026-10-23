# 代码清理记录

基线：`aquamarine5/ChaoxingSignFaker@92a8f66fe46a5058ad6e9f45a62d80e5b9127894`。修改日期：2026-10-23。

| 范围 | 改动 |
| --- | --- |
| 打赏 | 删除 Sponsor 三个组件、收款图片、设置页入口，以及手势/口令/拍照/位置/二维码签到后的弹窗与计时器。 |
| 人脸保存 | 保留 `FaceRecognitionComponent`、`FacePhotoControlComponent`、`SaveFaceImagesDialog` 与二维码/位置成功后的保存提示，删除提示结束后触发的打赏。 |
| 统计 | 删除 UMengHelper、ChaoxingAnalyser、排行榜实体与组件；移除调用点和 SDK 依赖。验证码协议仍需的 MD5 独立为 `DigestHelper.kt`。 |
| 异常上报 | 移除 Sentry SDK、Gradle 插件和 Manifest 配置。`reportLocalError()` 只写本机异常类型，继续传播协程取消。 |
| 更新与发布 | 删除 Stackbricks 服务、强制更新界面、原发布/统计流水线、Qiniu 插件配置及私有 Maven 源。新增只构建调试 APK 的 Actions。 |
| 作者服务 | 移除远程封禁名单、解锁口令及个人歌单。原 `BlockedContent` 暂保留为直接渲染内容的兼容包装，不再发出请求。 |
| 设备信息 | 不再依靠友盟获取 OAID；本机标识改为 Android ID，优先保留已有或明确导入的设备码。 |
| 应用身份 | 独立 applicationId、Provider authority、权限名与导入 scheme，保留 Kotlin 原包名与版权头以记录来源。实验名称、图标与系统字体与原应用区分。 |
| 账号分享 | 生成 `facescan-clean://import` 本地协议；扫码解析只接受该格式。旧版作者域名的分享码不再兼容。分享码仍含登录所需信息，不应公开发布。 |
| 签名与地图 | 删除仓库内原签名文件、签名路径和原地图 Key。默认使用 Android 调试签名。没有独立地图 Key 时，地图组件显示未启用提示。 |
| 本机数据 | 关闭 Android 备份与数据迁移。此修改没有重写整个上游会话/照片存储层，也没有将其变成经过完整安全审计的存储实现。 |
| 来源 | 完整保留 AGPL-3.0、原作者署名与原始项目说明。删除 IDE 私人配置、旧下载网页与统计图表。 |

## 验证范围

构建环境：JDK 21、Gradle 9.7.1、Android SDK 37。最终源码执行 `:app:assembleDebug`，结果为 `BUILD SUCCESSFUL`；产物独立包名为 `io.github.rd227.facescan.cleaned`，最低 Android 8.0，原生库为 ARM64。APK 的 v2 签名校验通过。

静态检查覆盖已删除组件的引用、作者服务域名、统计/更新依赖、Manifest 与签名配置。运行时代码扫描未发现原作者服务域名和已删除 SDK 的引用；对构建产物 DEX 的字符串检查也未发现作者 CDN、友盟、Sentry、Stackbricks 或打赏弹窗类的残留。历史说明保留原链接，仅用于记录来源。

仍存在上游的 Gradle 旧 DSL 与旧数据字段弃用提示，以及百度 SDK 的 D8 提示；未因此修改其协议或数据迁移行为。

没有使用真实超星账号，也没有运行真实课程签到、摄像头或地图。未运行上游空模板单元测试。编译通过不代表超星服务端会接受某次签到、某张照片或设备信息。地图、定位及聊天相关第三方 SDK 仍保留，需要分别遵循其服务条件。

当前仍有上游 Compose 页面和协议层耦合。本分支用于研究与后续提取模块；我们自己的操作界面、导航与照片管理在独立项目中编写更便于维护。

## 保持原样的较大文件

| 文件 | SHA-256 |
| --- | --- |
| `app/libs/onnxruntime-android-1.29.0-minimal-captcha.aar` | `c9acfd3e38b98b6517bd679dcfa2ee1e6929d474a22a6be65a2f8077913a77bd` |
| `app/src/main/assets/captcha.ort` | `ce9623e1f58b46bc5e7485dffc6449d36255af5e9e2d877bb4db94e8c8b21ee7` |

这两个文件由固定的上游提交提供，本次未重新生成或训练。
