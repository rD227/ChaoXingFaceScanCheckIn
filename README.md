# FaceScan · 超星人脸与扫码实验 App

**Android 本机运行，不需要购买服务器。** 当前版本为 `0.1.0-experimental`，支持 Android 8.0 及以上。

## 已实现

- 在独立 WebView 中打开超星官方登录页面，登录信息直接交给官方页面。
- 使用手机原生 HTTP 读取官方会话状态，与网页登录共享本机 Cookie。
- 扫描二维码或手动粘贴超星官方链接，提取活动编号并在打开前确认。
- 从系统相册选择本人照片，缩放后使用 Android Keystore + AES-GCM 加密保存。
- 官方网页允许选择图片时，提供“使用本机保存的本人照片”选项；要求实时拍摄时打开相机。
- 清除本机照片与登录状态。
- GitHub Actions 自动执行测试、Android Lint 和 APK 构建。

## 实验范围

本版先验证“本机保存照片 → 扫码 → 官方页面操作”这条路径。**尚未实现自动人脸验证凭据复用、协议自动签到或学习通私有原生 SDK 桥接。**

本机保存照片不等于通过官方本次人脸验证。是否签到成功以超星官方页面和记录为准。部分官方页面可能要求在学习通 App 中运行；这类页面在本实验版中可能无法完成，需要后续适配。机构跨站单点登录和位置签到也尚未验证。

没有使用真实账号或课程完成端到端签到验证。构建通过与接口实际可用是两项独立验证。

## 安装实验 APK

进入 [Actions](https://github.com/rD227/ChaoXingFaceScanCheckIn/actions/workflows/android.yml)，打开成功的构建，在 **Artifacts** 下载 `FaceScan-0.1.0-experimental-debug`，解压并安装 APK。

这是调试签名版本，不是正式商店发行包。不同构建环境的调试签名可能不同；若提示签名冲突，需要先卸载旧包，卸载会删除本机照片和会话。

## 使用

1. 点击“在超星官方页面登录”，完成官方登录后点“完成”。
2. 点击“检查本机登录会话”，确认是否成功读取账号。
3. 在“本人人脸照片”中选择并保存自己的照片。
4. 扫描课堂二维码，确认来源和活动编号，然后打开官方签到页。
5. 官方页面出现图片选择器时，可选择本机照片；如要求实时拍摄，按页面要求完成。

扫码本身只解析链接，不会由 App 自动提交签到。不支持第三方短链接，也不会将账号提交给第三方签到服务。

## 本地构建

需要 **JDK 17+、Android SDK Platform 35、Build Tools 35.0.0**。可以直接使用 Android Studio 打开项目，或设置 `ANDROID_HOME` 后运行：

```sh
./gradlew testDebugUnitTest lintDebug assembleDebug
```

Windows 使用 `gradlew.bat`。APK 位于：

```text
app/build/outputs/apk/debug/app-debug.apk
```

`local.properties`、签名密钥、构建目录和本机资料均不应提交到仓库。

## 代码结构

```text
app/src/main/java/io/github/rd227/facescan/
├── MainActivity.java          # 首页、扫码、照片管理与会话检查
├── OfficialWebActivity.java   # 官方网页、相机权限与图片选择器
├── OfficialHttp.java          # 原生 HTTP、跳转与响应大小限制
├── OfficialUrl.java           # 链接校验与活动编号解析
├── WebSessionCookies.java     # 原生 HTTP 与 WebView 会话共享
├── FacePhotoStore.java        # 私有目录中的加密照片
└── FaceScanApplication.java   # 启动时清理遗留上传临时文件
```

应用不会连接本项目的服务器。访问官方服务时，必要的会话数据仍会发往超星；选择上传照片时，会临时解密图片交给官方网页上传。临时文件在关闭官方页面或下一次启动应用时清理。

## 后续实验

- 真机核对网页登录、账号读取、相机和图片上传的兼容性。
- 查询官方活动和签到结果，加入明确的结果确认。
- 根据真实接口行为评估人脸文件 ID 与本次验证流程的适配。

[架构说明](docs/ARCHITECTURE.md) · [许可证](LICENSE) · [第三方组件](THIRD_PARTY_NOTICES.md)

