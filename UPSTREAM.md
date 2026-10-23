# 来源与修改记录

- 原项目：[aquamarine5/ChaoxingSignFaker](https://github.com/aquamarine5/ChaoxingSignFaker)
- 原作者：aquamarine5、Brainspark（此前名为 RenegadeCreation）
- 基线提交：`92a8f66fe46a5058ad6e9f45a62d80e5b9127894`
- 本分支维护项目：[rD227/ChaoXingFaceScanCheckIn](https://github.com/rD227/ChaoXingFaceScanCheckIn/tree/upstream-cleanup)
- 修改日期：2026-10-23
- 本衍生分支沿用上游 AGPL-3.0，[LICENSE](LICENSE) 保留完整文本。原文件中的作者版权头、第三方图标许可与协议实现的来源保留。

本分支不是原作者发布版本，也未获原作者背书。原始 README 与开发说明保存在 `docs/UPSTREAM_README.md`、`docs/UPSTREAM_CONTRIBUTING.md`，包括作者对换皮及二次分发的态度。

本次修改主要删除打赏、作者侧更新/统计/限制服务与相关依赖；调整包名、签名、构建、隐私说明和本地分享格式；增加独立 MD5 工具及实验分支图标。上游的网络协议、验证码实现、人脸图片处理、课程及签到页面仍占本分支绝大部分代码，不能宣称为独立原创。具体记录见 [CLEANUP.md](docs/CLEANUP.md)。

`app/src/main/assets/captcha.ort` 与 `app/libs/onnxruntime-android-1.29.0-minimal-captcha.aar` 为上游基线自带文件，保持原始内容。上游仓库中的资源和库仍受各自适用的第三方许可约束；根目录许可证不将其重新许可。哈希见清理记录。
