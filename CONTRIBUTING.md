# 开发本清理分支

构建步骤见 [README](README.md)。使用 JDK 21 与 Gradle Wrapper；不需要配置 Stackbricks、GitHub Packages 凭据、作者签名或发布服务。代码规范沿用 [AGENTS.md](AGENTS.md)。

调试构建执行 `./gradlew :app:assembleDebug`。上游单元测试是空模板，清理分支的构建流水线不将其作为验证。改动人脸保存、二维码或登录后，需要在自己的设备与账号上补充实际验证，并明确记录验证范围。

地图功能需要为本分支包名及签名配置自己的 `baiduMapsApiKey`。请勿提交账号、Cookie、照片、签名私钥、API Key 或本机 `local.properties`。

复用与修改上游代码时保留署名；来源与修改范围见 [UPSTREAM.md](UPSTREAM.md)。原始开发说明保存在 [docs/UPSTREAM_CONTRIBUTING.md](docs/UPSTREAM_CONTRIBUTING.md)。
