# Baihub Fork 记录

## 固定约定

- 每次任务时都总结内容写入本文件，并在每条记录开头写明任务完成时间（Asia/Shanghai）。
- 不主动向 GitHub 推送，只有用户明确要求时才推送。
- 不主动同步上游更新，只有用户明确要求时才同步。
- 不向上游提交 PR（上游不接受 PR）。
- 只构建 Release arm64-v8a；versionName / versionCode 与上游保持一致。
- APK 命名：`baihub-<versionName>-arm64-v8a-<本次改动摘要>.apk`。

## 2026-10-19 05:43

任务：将软件包名改为 `me.baicha.baihub`，软件名改为 `Baihub`，然后构建 Release arm64-v8a APK。

改动内容：

- `app/build.gradle.kts`：`applicationId` 由 `me.rerere.rikkahub` 改为 `me.baicha.baihub`。
- `app/src/main/res/values*/strings.xml`：`app_name` 统一改为 `Baihub`（含所有语言）。
- 保留 Kotlin `namespace` 与源码包路径 `me.rerere.rikkahub` 不变，避免大规模移动源码；用户可见的安装包 ID 由 `applicationId` 决定。
- 版本号保持上游一致：`versionName = 3.0.0`，`versionCode = 192`。
- 目标 APK：`baihub-3.0.0-arm64-v8a-修改了包名和软件名.apk`。

状态：已完成（2026-10-19 06:00）。

构建结果：

- 构建命令：`gradlew.bat :app:assembleRelease`（JDK 21，Android SDK Platform 37.2）。
- 结果：`BUILD SUCCESSFUL in 15m 48s`，504 个任务全部执行完成。
- 产物：`dist/baihub-3.0.0-arm64-v8a-修改了包名和软件名.apk`（41,982,126 字节）。
- SHA-256：`DBC205F8074FF79BAA6DDA2E33F362C27B0E9B23F46D249016DD06C027569BDF`。
- 校验：包名 `me.baicha.baihub`，versionCode `192`，versionName `3.0.0`，所有语言的显示名均为 `Baihub`，native-code 仅 `arm64-v8a`，V2 签名有效（CN=Baihub）。

本地构建配置（均未纳入 git）：

- `local.properties`：指向本机 Android SDK，并引用本地签名密钥。
- `app/google-services.json`：本地占位配置，仅用于让 google-services 插件通过构建；不连接真实 Firebase 项目。
- 签名密钥：`D:\0AI\BaiCha\Rikkahub\.keys\baihub-release.jks`（仓库目录之外，请妥善保留；丢失后无法覆盖升级已安装的 APK）。

备注：

- 修改 `applicationId` 后，新包不会继承原 RikkaHub（`me.rerere.rikkahub`）的本地应用数据，需要时通过备份导出/导入迁移。
- 本次改动未提交、未推送。

## 2026-10-19 06:07

任务一：构建配置限定为仅 arm64。

- `app/build.gradle.kts`：`abiFilters` 仅保留 `arm64-v8a`；ABI split 仅保留 `arm64-v8a`，关闭 universal APK。
- `workspace/build.gradle.kts`：原生构建也限定 `arm64-v8a`，避免继续编译 x86 / x86_64 / armeabi-v7a。
- 验证：重新执行 `:app:assembleRelease` 成功（3m 57s），输出目录只有 `app-arm64-v8a-release.apk`，`aapt2` 确认 `native-code: 'arm64-v8a'`。
- 产物已覆盖 `dist/baihub-3.0.0-arm64-v8a-修改了包名和软件名.apk`，SHA-256 与上次一致；构建可复现。

任务二：诊断“从原版 Rikkahub 导入备份后图片不显示，但存储管理里能看到图片”。

结论：聊天记录里保存的是旧包名的绝对文件路径，备份恢复只搬运了文件内容，没有改写数据库中的路径。

- 备份写入时按 `文件夹/相对路径` 打包，见 `data/sync/BackupManager.kt:71`。
- 恢复时文件被还原到新应用的 `filesDir`，见 `data/sync/PendingRestore.kt:129`。
- 新图片写入聊天时保存的是 `file.toUri().toString()` 绝对路径，见 `data/files/FilesManager.kt:252`；原版应用的路径形如 `file:///data/user/0/me.rerere.rikkahub/files/upload/xxx.png`。
- 聊天图片直接用这个 URL 加载，见 `ui/components/message/ChatMessage.kt:468`。
- 存储管理页用 `FilesManager.getFile(entity)` 按 `filesDir + relativePath` 重新解析，见 `FilesManager.kt:102` 与 `SettingFilesPage.kt:236/390`，所以能正常显示缩略图。

候选方案：

1. 推荐：恢复时改写路径 + 启动时一次性修复已有数据。恢复阶段把数据库和 settings 中指向 `/files/` 的旧绝对路径按相对路径重映射到当前 `filesDir`；升级后对已导入的数据做一次性扫描修复（message_node JSON、助手/用户头像）。兼容 `/data/user/0/...` 与 `/data/data/...` 两种形式。
2. 只做启动时扫描修复：改动集中在应用层，但首次启动需要遍历会话记录。
3. 不改代码，重新导入修改过路径的备份：需要手工改备份数据库，易出错，不推荐。

状态：方案已确认并实施，见下一条记录。

## 2026-10-19 06:33

任务：修复从原版备份导入后图片不显示的问题。

实现：

- 新增 `data/files/LocalFileUrlRepair.kt`：把指向旧包名 `/files/` 的绝对 `file://` 路径按相对路径重映射到当前 `filesDir`；只处理已知附件目录，目标文件必须存在，并做路径越界校验。
- `MessageNodeDAO` 新增 `getAllWithFileReferences()`，只扫描包含 `file://` 的消息行。
- `RikkaHubApp` 启动时执行一次修复；检测到本次启动应用了备份恢复时强制再跑一次。
- 修复范围包含聊天消息中的图片/文档/视频/音频/嵌套工具附件，以及助手头像和用户头像。
- 使用 SharedPreferences 记录修复版本与包名，避免每次启动重复扫描；换包名或恢复备份会自动重新执行。
- 新增 `LocalFileUrlRepairTest`，6 个用例全部通过（旧包名路径、`/data/data` 形式、已存在路径、缺失目标、未知目录、路径穿越）。

构建结果：

- 命令：`gradlew.bat :app:assembleRelease`，`BUILD SUCCESSFUL in 3m 51s`。
- 产物：`dist/baihub-3.0.0-arm64-v8a-修复备份导入后图片不显示.apk`（41,986,802 字节）。
- SHA-256：`AFF16DBEFA86CA7471668C1A3DB75033D93A093CE29380345FE81CFB4C3596F6`。
- 校验：包名 `me.baicha.baihub`，versionCode `192`，versionName `3.0.0`，显示名 `Baihub`，仅 `arm64-v8a`，V2 签名有效。

备注：版本号未变，可直接覆盖安装现有 Baihub（同一签名）；首次启动会自动修复已经导入的数据。未提交、未推送。

## 2026-10-19 06:41

补充：上一版只修了消息附件和头像，漏掉了聊天背景。

- 根因：聊天背景保存在 `Assistant.background`（本地 `file://` 地址），与头像字段不同，未纳入修复范围。
- 修复范围补充 `Assistant.background`，以及助手 `presetMessages` 中的本地图片/附件。
- 修复标记版本从 1 升到 2，已运行过上一版的设备会重新扫描一次。
- 新增对应单元测试，现在共 7 个用例全部通过。
- 构建：`BUILD SUCCESSFUL in 3m 35s`。
- 产物：`dist/baihub-3.0.0-arm64-v8a-修复聊天背景和图片路径.apk`（41,987,074 字节）。
- SHA-256：`CAC248D55F644DFFE67BBBE72AFC2DB3CE108C8A6B532285C999B2ECD0D2834D`。
- 校验：包名 `me.baicha.baihub`，versionCode `192`，versionName `3.0.0`，显示名 `Baihub`，仅 `arm64-v8a`。

备注：未提交、未推送。

## 2026-10-19 07:34

任务：让 Baihub 导出的备份可以正常导回原版 RikkaHub（`me.rerere.rikkahub`）。

结论：可以做，且不需要修改原版应用。备份格式本身完全一致，当前唯一的阻断点是数据库和设置里保存的本地附件绝对路径仍指向 `me.baicha.baihub`。

方案（待确认）：

1. 把现有的路径修复逻辑抽成通用映射：修复模式仍按当前 `filesDir` 查找文件；导出模式则把 `me.baicha.baihub/files/...` 无条件重写为 `me.rerere.rikkahub/files/...`，保留相对路径和 URI 编码。
2. 导出设置时，在内存里重写 `assistants[].avatar`、`assistants[].background`、`assistants[].presetMessages` 中的附件，以及 `displaySetting.userAvatar`，再写入 `settings.json`。
3. 导出数据库时，只改写 `VACUUM INTO` 生成的数据库快照，不动在线数据库：扫描 `message_node.messages`，重写图片/文档/视频/音频和嵌套工具附件中的 `file://` 路径，然后 checkpoint、关闭并清理 sidecar，再放入 zip。
4. 附件在 zip 中仍使用 `upload/...`、`skills/...`、`fonts/...`、`media_creation/...` 相对路径，与原版备份格式完全一致。
5. 导回 Baihub 也保持可用：Baihub 恢复备份时会强制运行路径修复，把原版路径重新映射到当前包名。
6. 新增单元测试：路径前缀改写、设置字段改写、预设消息附件改写；构建后实际导出一次备份检查 zip 内容和 URL。

边界与风险：

- 原版与 Baihub 需要处于同一上游数据库版本（当前都是 3.0.0 / versionCode 192）；更旧的 RikkaHub 可能无法打开新 schema。
- 重写后的绝对路径按当前设备/用户推算，适用于同一设备同一用户；跨设备或工作资料导入时绝对路径可能仍需目标端修复。
- 工作区文件本来就不在备份范围内，这是上游行为；工作区记录不会因此变得可用。
- `JsonInstant` 开启了 `ignoreUnknownKeys`，以后 fork 新增普通设置字段时原版可以忽略；不要新增原版不认识的枚举值。
- 已导出的旧备份不会自动变化，需要安装新版本后重新导出；必要时可以后续增加“转换已有备份”的独立功能。

状态：方案已确认并实施，见下一条记录。

## 2026-10-19 07:45

任务：实现 Baihub 导出的备份可以导回原版 RikkaHub。

实现：

- `LocalFileUrlRepair.kt` 抽成通用映射：保留原来的修复模式，新增 `rewriteFileUrl`、`mapAssistantLocalFiles`、`mapSettingsLocalFiles`、`mapMessageNodeLocalFiles`。
- 新增 `data/sync/BackupPathRewriter.kt`：在导出时把 `me.baicha.baihub` 的文件路径重写为 `me.rerere.rikkahub`，目标目录按当前设备/用户目录推算。
- `BackupManager.createBackup()` 在写 `settings.json` 前重写头像、聊天背景、预设消息和用户头像；数据库快照写入 zip 前扫描并重写 `message_node.messages`，只修改快照，不动在线数据库。
- 导出快照改写后会 checkpoint、关闭并清理 sidecar，保证 zip 里的数据库是完整独立的。
- 导回 Baihub 仍由恢复时的强制路径修复兜底。

验证：

- 新增 3 个目录映射测试，加上原有路径/设置/消息测试，共 14 个用例全部通过。
- `BUILD SUCCESSFUL in 3m 52s`。
- 产物：`dist/baihub-3.0.0-arm64-v8a-备份可导回原版.apk`（41,988,526 字节）。
- SHA-256：`9726CC4DDC0A658486111BE158E9ACED95DD5F206870F92C25C4C115682CA00E`。
- 校验：包名 `me.baicha.baihub`，versionCode `192`，versionName `3.0.0`，显示名 `Baihub`，仅 `arm64-v8a`，V2 签名有效。

备注：当前没有连接设备，尚未做“导出后实际导入原版”的端到端验证；已导出的旧备份需要重新导出才会带新路径。未提交、未推送。

## 2026-10-19 08:16

任务：排查“备份导回原版没效果”。

排查结果：

- 本地导出链路确认是 `BackupPage -> BackupVM.exportToFile() -> WebDavSync.prepareBackupFile() -> BackupManager.createBackup()`，改写逻辑确实接在导出路径上。
- 发现一个代码缺口：导出改写原先只匹配当前 `context.filesDir` 的精确前缀；如果数据库里仍是 `/data/data/<包名>/files/...` 或其它用户目录形式，会静默跳过。已改为同时识别 `/data/` 下的当前包名路径。
- 新增 `baihub_export_info.json` 到每个备份：记录源包名、目标包名、源/目标目录和实际改写的消息行数，用于确认导出时是否执行了改写。原版应用会忽略这个额外文件。
- 新增对应的 `/data/data` 路径测试，目前共 15 个用例全部通过。
- 构建：`BUILD SUCCESSFUL in 3m 44s`。
- 产物：`dist/baihub-3.0.0-arm64-v8a-备份导回原版v2.apk`（41,988,674 字节）。
- SHA-256：`6C742CC490D31C47CF1D0A25AB946ED5A17C44BFF7A0D2124FDAB6184DCF454D`。
- 校验：包名 `me.baicha.baihub`，versionCode `192`，versionName `3.0.0`，显示名 `Baihub`，仅 `arm64-v8a`。

待确认：需要实际导出的备份 zip（检查 `settings.json`、`rikka_hub.db` 和 `baihub_export_info.json`），以及导入目标是正式版还是 debug 版、是否同时勾选了“聊天记录”和“文件”、导入后是否重启。未提交、未推送。

## 2026-10-19 08:31

任务：对比原版备份与 Baihub 备份，检查改进点；删除旧的解压目录。

对比文件：

- 原版：`rikkahub_backup_20261019_060236.zip`
- Baihub：`baihub_backup_20261019_081826.zip`

结果：

- 两份备份都是 `user_version=30`，`message_node` 605 行，且都没有消息附件 URL；数据库逐表逻辑内容一致，文件字节差异来自 SQLite `VACUUM` 的页布局。
- Baihub 备份的 `settings.json` 中 `me.baicha.baihub` 出现 0 次，`me.rerere.rikkahub` 出现 26 次；17 个本地图片引用全部改写成功。
- 两份备份的 17 个引用附件都能在 zip 中找到，缺失 0 个；skills/fonts 也完整。
- zip 条目原版 21 个、Baihub 22 个，唯一差异是 `baihub_export_info.json`，原版会忽略。
- 发现 Baihub 备份的 URL 被写成 `/data/data/...`，原版是 `/data/user/0/...`。同一用户下可用，但多用户/工作资料会指向错误目录。
- 修复：`rewriteFileUrl` 与 `repairFilePath` 不再使用 `canonicalFile` 输出路径，保留 `/data/user/<id>` 形式；新增路径穿越测试。
- 原版备份缺少 `autoConsolidateMemory`、`voiceModeListenWhileReplying` 等新字段，属于版本差异，原版 `ignoreUnknownKeys` 会忽略；`launchCount`、`lastBackupTime` 差异正常。

构建：

- 16 个单元测试全部通过。
- `BUILD SUCCESSFUL in 3m 48s`。
- 产物：`dist/baihub-3.0.0-arm64-v8a-备份导回原版v3.apk`（41,988,526 字节）。
- SHA-256：`7B2F85A6E5823E60D2C2704B6A137BFC4887BBD0BF71C76A1A6A637928D6D18C`。

清理：已删除旧的解压目录 `D:\0AI\BaiCha\Rikkahub\Baihub-master`（确认不是 git 仓库，正式仓库 `Baihub` 保留）。

## 2026-10-19 08:35

任务：把本地导出备份的默认文件名前缀改成 baihub；关于页的 RikkaHub 品牌改成 Baihub。

改动：

- `ImportExportTab.kt`：本地导出保存对话框的默认文件名由 `rikkahub_backup_<时间>.zip` 改为 `baihub_backup_<时间>.zip`。
- `SettingAboutPage.kt`：关于页大标题改用 `R.string.app_name`（Baihub）；GitHub 链接改为 `https://github.com/Baicha0108/Baihub`；License 链接改为该仓库的 `LICENSE`。
- 关于页的 Website 行仍指向上游 `https://rikka-ai.com/`，本次未改；如果以后有 Baihub 自己的站点再替换。
- WebDAV/S3 的远程备份文件名仍保持 `backup_...`，远程目录仍为 `rikkahub_backups`，避免已有备份和原版应用的列表过滤失效。

说明：备份文件名不影响导入导出；导入端读取的是 zip 内容，不校验文件名。本地保存对话框的名字只是默认建议。

未改的可见品牌：`setting_page_share_text` 和 `setting_page_web_server_desc` 在 7 个语言资源里仍含 RikkaHub，留待后续统一处理。

本次按用户要求未构建、未提交、未推送。

## 2026-10-19 09:19

任务：把主设置页和偏好设置页的顶栏改成和对话页一致的透明顶栏 + 内容顶部淡出。

实现：

- 把原本只在 `ChatList` 里私有的 `topFadingEdge` 抽到 `ui/components/ui/TopFadingEdge.kt`，聊天列表和设置页共用同一实现，行为保持一致。
- `SettingPage`、`SettingPreferencesPage` 的 `LargeFlexibleTopAppBar` 改为透明容器色（普通和滚动状态都透明）。
- 两个页面的 `LazyColumn` 增加 `topFadingEdge(当前顶栏高度 + 24.dp)`，内容滚到顶栏下方时从状态栏位置逐渐淡出。
- 页面背景色和整体布局保持不变；没有引入 Haze、磨砂或玻璃效果。

验证：

- `:app:compileReleaseKotlin` 编译通过。
- `:app:assembleRelease` 成功，耗时 3 分 53 秒。
- 产物：`dist/baihub-3.0.0-arm64-v8a-设置页透明顶栏.apk`（41,987,198 字节）。
- SHA-256：`0B87DABAA517091D43327D0CBCBCC5EC50D7AA99E684B8BD8A7A4822EA905694`。
- 校验：包名 `me.baicha.baihub`，versionCode `192`，versionName `3.0.0`，显示名 `Baihub`，仅 `arm64-v8a`。

备注：当前没有连接设备，尚未做真机视觉验证；其余设置子页暂未改动。未提交、未推送。

## 2026-10-19 09:30

任务：修复设置页顶栏淡出出现明显分界线和横向色带的问题。

原因：

- 原 `topFadingEdge` 使用三个硬停点：`0f` 透明、`0.6f` 处 alpha 0.15、`1f` 全黑。
- 60% 位置的 alpha 斜率从约 0.25 突然变成约 2.1，形成明显的斜率断点；在卡片/内容背景上表现为一条横向色带。

修复：

- 改为 64 段采样的四次曲线 alpha（`t^4`），从 0 连续增长到 1，不再有硬停点。
- 保留原有的“内容靠近顶栏时更透明、靠近底边时完全显示”的视觉分布。
- 共享修饰符同时作用于聊天列表和设置页，聊天的淡出也会更平滑。

验证：

- `:app:assembleRelease` 成功，耗时 4 分 7 秒。
- 产物：`dist/baihub-3.0.0-arm64-v8a-设置页透明顶栏v2.apk`（41,987,186 字节）。
- SHA-256：`D17F053AEA42485FD787A278DE2A8C084328CF8B9D1D7A51B2702B55AD6527F6`。
- 校验：包名 `me.baicha.baihub`，versionCode `192`，versionName `3.0.0`，显示名 `Baihub`，仅 `arm64-v8a`。

备注：当前没有连接设备，尚未做真机视觉验证。未提交、未推送。

## 2026-10-21 10:42

任务：将本地提交推送到 GitHub fork。

推送：

- 远端：`https://github.com/Baicha0108/Baihub.git`
- 分支：`master`
- 范围：`57a758db..31c205b9`
- 包含：`BAIHUB.md` 项目差异说明、自定义提示词底部胶囊、`fork.md` 记录约定调整。
- 未向上游提交 PR。

## 2026-10-21 11:00

任务：把手机端聊天侧栏改成 Yuihub 风格的缩放式侧滑效果，不修改侧栏内部内容。

实现：

- 新增 `ChatScalingDrawer`：侧栏从左侧滑入，聊天页右移并围绕左边缘缩放到 90%。
- 展开时聊天页带 22dp 圆角、20dp 阴影和上下留白；背景使用抽屉容器色。
- 保留 300dp 侧栏宽度；`ChatDrawerContent` 内部布局和功能不变。
- 关闭时可在页面任意位置向右滑打开，展开后向左滑关闭；点击聊天页和返回键也可关闭。
- 横屏大屏的 `PermanentNavigationDrawer` 保持不变。
- `BAIHUB.md` 补充该界面差异。

验证：

- `:app:assembleRelease` 成功，耗时 4 分 49 秒。
- 产物：`dist/baihub-3.1.0-arm64-v8a-缩放式侧栏.apk`（42,122,946 字节）。
- SHA-256：`0B90A421C3F929A760A4DDDE68F491A7F0D83D18C2D16877DD5A60DBAE51BBED`。
- 校验：包名 `me.baicha.baihub`，versionCode `193`，versionName `3.1.0`，仅 `arm64-v8a`。

备注：当前没有连接设备，尚未做真机拖动和动画验证。未提交、未推送。

## 2026-10-19 11:25

任务：新增助手专属的“系统提示词 V2”，独立入口、独立文件存储、追加到经典系统提示词。

实现：

- 助手详情页的“提示词”块下新增“系统提示词 V2”入口，进入独立页面。
- 页面支持条目标题、内容、启用开关、长按拖动排序、复制、删除、编辑；空状态为“还没有 V2 提示词”。
- 编辑面板包含标题、内容、启用开关；标题必填，内容支持原有的 `{{变量}}`。
- V2 数据按助手 ID 存到 `filesDir/system_prompt_v2/<assistantId>.json`，不写入 settings/数据库，避免原版导入时解析未知结构。
- 构建请求时按条目顺序追加到经典 `assistant.systemPrompt` 后面；禁用、空白内容跳过；会话自定义系统提示词生效时 V2 不追加。
- 删除助手时清理对应 V2 文件；复制助手时复制 V2 条目。
- V2 目录已加入 Baihub 的备份附件列表；原版应用遇到未知目录会跳过，不会导入。
- 按用户要求，暂不提供 V2 的独立一键导入/导出。

验证：

- 新增 4 个拼接单元测试，覆盖顺序、禁用/空白、无启用条目、经典提示词为空；全部通过。
- `:app:assembleRelease` 成功，耗时 3 分 56 秒。
- 产物：`dist/baihub-3.0.0-arm64-v8a-系统提示词V2.apk`（42,006,538 字节）。
- SHA-256：`4897C99B2EC4BA8A6079A01F1C770E9D89E4B0369969D8B6BF74E7F8D2C32692`。
- 校验：包名 `me.baicha.baihub`，versionCode `192`，versionName `3.0.0`，显示名 `Baihub`，仅 `arm64-v8a`。

备注：当前没有连接设备，尚未做真机 UI 与备份恢复验证。未提交、未推送。

## 2026-10-19 12:16

任务：完善“自定义提示词”（原系统提示词 V2）的标题、全屏编辑、复制、分隔和导入导出。

实现：

- 用户可见名称统一从“系统提示词 V2”改为“自定义提示词”；内部文件目录仍保留 `system_prompt_v2`，避免迁移已有数据。
- 标题不再必填：新建默认“未命名”，清空后保存也落为“未命名”。
- 编辑面板内容框右下角增加全屏按钮，打开全屏内容编辑器；完成/取消后回到原编辑面板。
- “复制”改为复制该条内容到剪贴板，并提示“已复制”；不再生成副本。
- 自定义提示词之间在最终 system prompt 中用 `\n\n---\n\n` 分隔，方便模型区分相邻条目。
- 正常模式底部工具栏改为 [导出] [导入] [添加提示词]。
- 导出进入选择模式：卡片出现选择框，顶栏右上角“全选”在无选中时全选、有选中时清空；右下角出现方形大圆角“导出”按钮。
- 导出只包含选中条目，文件名默认是助手名字（非法字符替换为 `_`），后缀 `.json`。
- JSON 结构为 `version + entries[]`，每条包含 `title`、`content`、`enabled`，条目彼此独立。
- 导入支持对象格式和裸数组；同标题原位替换，新标题追加；应用前显示二次确认，列出替换和追加的标题。
- 导入空文件、解析失败、导出失败均有提示；导入成功后保存并提示。

验证：

- 拼接测试 4 个、导出/合并/解析测试 3 个，共 7 个全部通过。
- `:app:assembleRelease` 成功，耗时 4 分 24 秒。
- 产物：`dist/baihub-3.0.0-arm64-v8a-自定义提示词完整版.apk`（42,020,722 字节）。
- SHA-256：`303DF1FA3E162945BCC9AF0FC0C26E49C6B537EC62B071573B92CE2033B5C32B`。
- 校验：包名 `me.baicha.baihub`，versionCode `192`，versionName `3.0.0`，显示名 `Baihub`，仅 `arm64-v8a`。

备注：当前没有连接设备，尚未做真机 UI、文件选择器和导入导出流程验证。未提交、未推送。

## 2026-10-19 10:40

任务：让标题胶囊的高度与左侧返回按钮的圆形直径一致。

实现：

- 返回按钮使用 Material3 `FilledTonalIconButton`，默认容器直径 40dp。
- `SettingsTopBarTitle` 改为 `heightIn(min = 40.dp)`，去掉上下 padding，只保留水平 12dp；文字在 40dp 高度内垂直居中。
- 所有 21 个设置页共用该组件，自动同步。

验证：

- `:app:assembleRelease` 成功，耗时 4 分 15 秒。
- 产物：`dist/baihub-3.0.0-arm64-v8a-设置页标题胶囊对齐.apk`（41,990,098 字节）。
- SHA-256：`468CAEF4D8995A31597913145113EA764CAD2D3493E94E50800D8AE03B5C6BD0`。
- 校验：包名 `me.baicha.baihub`，versionCode `192`，versionName `3.0.0`，显示名 `Baihub`，仅 `arm64-v8a`。

备注：当前没有连接设备，尚未做真机视觉验证。未提交、未推送。

## 2026-10-19 10:26

任务：给设置页左上角标题加胶囊背景，透明度跟随标题淡入淡出；其他设置页面同样处理。

实现：

- 新增 `SettingsTopBarTitle`：在标题文字外绘制 `CircleShape` 胶囊，背景为 `surfaceContainerHigh` 85% 不透明度，内边距 12dp × 4dp。
- 胶囊放在 Material3 顶栏的 `title` 槽内部，标题容器的 graphicsLayer 透明度会同时作用于文字和胶囊，因此两者一起淡入淡出。
- 21 个设置页的顶栏标题全部套用该组件，包括设置首页、偏好设置、各偏好子页、关于、主题、Web、搜索、MCP、赞助、存储、媒体、模型、供应商和语音。

验证：

- `:app:compileReleaseKotlin` 编译通过。
- `:app:assembleRelease` 成功，耗时 3 分 58 秒。
- 产物：`dist/baihub-3.0.0-arm64-v8a-设置页标题胶囊.apk`（41,990,058 字节）。
- SHA-256：`0D5B5DAA640B67D2298B2F7E130590484FF89B36B5F0D8CCFF27B29109F189D4`。
- 校验：包名 `me.baicha.baihub`，versionCode `192`，versionName `3.0.0`，显示名 `Baihub`，仅 `arm64-v8a`。

备注：当前没有连接设备，尚未做真机视觉验证。未提交、未推送。

## 2026-10-19 10:11

任务：把遮罩最大不透明度改成 70%，并把其余设置页顶栏统一成透明顶栏。

实现：

- `topScrimEdge` 默认 `maxAlpha` 改为 0.7f；新增 `CustomColors.transparentTopBarColors` 和 `Modifier.settingsTopScrim(topPadding)`，所有设置页复用。
- 21 个设置页顶栏全部改为透明色。
- 13 个内容可以滚到顶栏下方的页面加了 70% 柔和遮罩：SettingPage、Preferences、PreferencesGeneral、PreferencesNetwork、PreferencesNotification、PreferencesTheme、PreferencesUI、About、Theme、Web、Search、SearchDetail、Mcp。
- 8 个页面有固定子标题/搜索栏，内容不会滚到顶栏下方，因此只做透明顶栏、不加遮罩：Donate、Files、McpDetail、Media、Model、Provider、ProviderDetail、Speech。

验证：

- `:app:compileReleaseKotlin` 编译通过。
- `:app:assembleRelease` 成功，耗时 4 分 2 秒。
- 产物：`dist/baihub-3.0.0-arm64-v8a-设置页顶栏全透明70.apk`（41,988,362 字节）。
- SHA-256：`AB5FA93CAFDCA5CA6F92515CE16CE28557AD6E0A4973FAA3A6EBA2018E89FA70`。
- 校验：包名 `me.baicha.baihub`，versionCode `192`，versionName `3.0.0`，显示名 `Baihub`，仅 `arm64-v8a`。

备注：当前没有连接设备，尚未做真机视觉验证。未提交、未推送。

## 2026-10-19 10:00

任务：把设置页柔和遮罩的最大不透明度从 30% 调到 50%。

实现：

- `topScrimEdge` 默认 `maxAlpha` 从 0.3f 改为 0.5f；设置页和偏好设置页自动跟随。
- 遮罩范围、曲线和聊天页逻辑不变。

验证：

- `:app:assembleRelease` 成功，耗时 4 分 11 秒。
- 产物：`dist/baihub-3.0.0-arm64-v8a-设置页柔和遮罩50.apk`（41,986,858 字节）。
- SHA-256：`842D7C1AA035AD953D9FF106CFF00896396D76CF0D56C2D446AA6EB032886198`。
- 校验：包名 `me.baicha.baihub`，versionCode `192`，versionName `3.0.0`，显示名 `Baihub`，仅 `arm64-v8a`。

备注：当前没有连接设备，尚未做真机视觉验证。未提交、未推送。

## 2026-10-19 09:53

任务：把设置页淡化改得更柔和，按 0%→30% 的背景色遮罩处理。

实现：

- 新增 `topScrimEdge`：在内容上方绘制页面背景色遮罩，顶部 30% 不透明度，向下平滑降到 0%，不再把内容 alpha 直接压到透明。
- 设置页和偏好设置页改用 `topScrimEdge`，范围保持上次的一半。
- 聊天页继续使用原来的 `topFadingEdge`，不受影响。
- 遮罩使用二次曲线衰减，底部导数为 0，避免再次出现硬边界。

验证：

- `:app:assembleRelease` 成功，耗时 4 分 7 秒。
- 产物：`dist/baihub-3.0.0-arm64-v8a-设置页柔和遮罩.apk`（41,986,822 字节）。
- SHA-256：`77D671D5412A7056D56CC53A6C4AB714B82AFC472CF40B4EEF59AFDD74383775`。
- 校验：包名 `me.baicha.baihub`，versionCode `192`，versionName `3.0.0`，显示名 `Baihub`，仅 `arm64-v8a`。

备注：当前没有连接设备，尚未做真机视觉验证。未提交、未推送。

## 2026-10-19 09:40

任务：继续处理设置页淡出色带，并把淡化范围缩小到约一半。

原因：

- 截图里的色带正好落在第一张卡片和第二张卡片之间的分隔位置；淡出末端与卡片边界重合，第一张卡片被压暗、下一张完全恢复，形成明显的横向对比。

修复：

- 设置页和偏好设置页的淡出高度从 `顶栏高度 + 24dp` 改为 `(顶栏高度 + 24dp) / 2`，让淡出结束在卡片内部，不再压在卡片分隔线上。
- 聊天页仍使用原来的完整高度，不受影响。

验证：

- `:app:assembleRelease` 成功，耗时 4 分 4 秒。
- 产物：`dist/baihub-3.0.0-arm64-v8a-设置页透明顶栏v3.apk`（41,987,186 字节）。
- SHA-256：`D329AD5977A84077AFA0E0C9A445D54B2E547DB8885AA8826095A460FC2EE857`。
- 校验：包名 `me.baicha.baihub`，versionCode `192`，versionName `3.0.0`，显示名 `Baihub`，仅 `arm64-v8a`。

备注：当前没有连接设备，尚未做真机视觉验证。未提交、未推送。

## 2026-10-19 12:29

任务：聊天界面右上角按钮不再跟随主题配色，改为与左侧标题同色（深色模式白、浅色模式黑）。

实现：

- `ChatPage.kt` 的 `TopAppBar` 显式设置 `actionIconContentColor = MaterialTheme.colorScheme.onSurface`。
- 标题原本就使用 `onSurface`，右侧“预览/新对话”两个按钮现在与标题颜色一致，不再使用 `onSurfaceVariant` 的偏灰主题色。

验证：

- `:app:assembleRelease` 成功，耗时 4 分 21 秒。
- 产物：`dist/baihub-3.0.0-arm64-v8a-聊天顶栏按钮配色.apk`（42,020,746 字节）。
- SHA-256：`2C470FEE47FD1C8E138C695936F8C0468237822CB9B34FD93A503EF2844EF797`。
- 校验：包名 `me.baicha.baihub`，versionCode `192`，versionName `3.0.0`，仅 `arm64-v8a`。

备注：当前没有连接设备，尚未做真机视觉验证。未提交、未推送。

## 2026-10-19 12:40

任务：在聊天页输入栏下方加号左侧增加当前助手的“自定义提示词”快捷入口；调整自定义提示词多选状态的导出按钮样式。

实现：

- `ChatInput.kt` 新增 `onCustomPromptClick`，在加号按钮左侧增加 `Message02` 图标按钮，点击进入当前助手的自定义提示词页面。
- `ChatPage.kt` 通过 `navController.navigate(Screen.AssistantSystemPromptV2(assistant.id))` 压入提示词页面；页面返回时弹栈回到原聊天页，不新开聊天。
- 多选状态右下角导出按钮由 88dp 正方形改为 148×56dp、20dp 圆角的长方形，按钮文字使用 `titleMedium` 字号。

验证：

- `:app:assembleRelease` 成功，耗时 4 分 20 秒。
- 产物：`dist/baihub-3.0.0-arm64-v8a-聊天页快捷入口与导出按钮.apk`（42,027,610 字节）。
- SHA-256：`940EB3FB75F0431348D6C5BDA40278CF9CAB7E62AC90080CE3783C135F7EBCE0`。
- 校验：包名 `me.baicha.baihub`，versionCode `192`，versionName `3.0.0`，仅 `arm64-v8a`。

备注：当前没有连接设备，尚未做真机视觉验证。未提交、未推送。

## 2026-10-19 13:01

任务：继续放大自定义提示词多选状态的导出按钮文字；去掉新增提示词时标题框默认预填的“未命名”。

实现：

- 导出按钮文字由 `titleMedium`（16sp）改为 22sp、`FontWeight.SemiBold`。
- “添加提示词”改为创建空标题的 `SystemPromptEntry()`，标题输入框不再预填“未命名”。
- 保留标题为空时列表显示“未命名”的兜底，保存空白标题时仍按原逻辑存为“未命名”。

验证：

- `:app:assembleRelease` 成功，耗时 4 分 26 秒。
- 产物：`dist/baihub-3.0.0-arm64-v8a-导出字号与新增标题.apk`（42,026,770 字节）。
- SHA-256：`293CC3210E661388FAB2EF8AD3F416BAC266ECEE0101C9580431D83399396755`。
- 校验：包名 `me.baicha.baihub`，versionCode `192`，versionName `3.0.0`，仅 `arm64-v8a`。

备注：当前没有连接设备，尚未做真机视觉验证。未提交、未推送。

## 2026-10-20 05:05

任务：同步合并上游 `rikkahub/rikkahub` 更新。

合并内容：

- 拉取并合并上游 `upstream/master`（`e5bb7073`，22 个提交），合并提交为 `33fa65be`。
- 唯一冲突为 `di/AppModule.kt` 的 import 块：保留我们的 `LocalFileUrlRepair`、`SystemPromptV2Repository`，同时保留上游新增的 `LocalTools`；没有逻辑冲突。
- 版本跟随上游升级到 `3.1.0`、versionCode `193`；`applicationId = me.baicha.baihub`、显示名 `Baihub`、仅 `arm64-v8a` 均保留。
- 合并前已将本地未提交改动提交为 `237ac2db`，避免丢失。

验证：

- `:app:assembleRelease` 成功，耗时 8 分 19 秒。
- 自定义提示词、导出合并、文件路径修复、备份路径改写相关单元测试全部通过，耗时 2 分 22 秒。
- 产物：`dist/baihub-3.1.0-arm64-v8a-同步上游3.1.0.apk`（42,133,186 字节）。
- SHA-256：`904C4AAF3253912A9CEAF45D7E56CA8397FDEADBAD1A2207CC67D24D40B05A1B`。
- 校验：包名 `me.baicha.baihub`，versionCode `193`，versionName `3.1.0`，仅 `arm64-v8a`。

备注：未推送。当前没有连接设备，尚未做真机验证。

## 2026-10-20 05:17

任务：恢复聊天页顶部渐隐为上游原版效果，让内容在状态栏附近仍隐约可见。

实现：

- `TopFadingEdge.kt` 的 `topFadingEdge` 恢复上游三段渐变：顶部透明、60% 位置 15% alpha、底部完全不透明。
- 设置页继续使用独立的 `topScrimEdge`，不受影响，不会重新出现此前的设置页色带。

验证：

- `:app:assembleRelease` 成功，耗时 3 分 30 秒。
- 产物：`dist/baihub-3.1.0-arm64-v8a-聊天顶部渐隐恢复原版.apk`（42,133,214 字节）。
- SHA-256：`C6E129F3D160017810A7408A8F5E616FD0A21F9A26B318C73B2490D645504D3D`。
- 校验：包名 `me.baicha.baihub`，versionCode `193`，versionName `3.1.0`，仅 `arm64-v8a`。

备注：当前没有连接设备，尚未做真机视觉验证。未提交、未推送。

## 2026-10-20 05:47

任务：精简自定义提示词卡片、统一项目弹出菜单的大圆角，并稍微缩小多选导出按钮。

实现：

- 自定义提示词卡片移除左侧 `Message02` 图标，内容左边距调整为 20dp；提示词注入页面布局保持不变。
- 新增 `PopupMenuShape`（24dp 圆角），应用到 `ItemActionMenu` 以及聊天输入、聊天抽屉、会话列表、搜索、翻译、网页、媒体创作等全部直接使用 `DropdownMenu` 的位置。
- 多选导出按钮从 148×56dp 调整为 140×52dp，保持 20dp 圆角和 22sp 半粗体文字。

验证：

- `:app:assembleRelease` 成功，耗时 3 分 37 秒。
- 产物：`dist/baihub-3.1.0-arm64-v8a-自定义提示词卡片与菜单圆角.apk`（42,132,526 字节）。
- SHA-256：`B9304B5698E602C9A772557FE09FD9790D81C57F601F9E32BF62130FDEB956B7`。
- 校验：包名 `me.baicha.baihub`，versionCode `193`，versionName `3.1.0`，仅 `arm64-v8a`。

备注：当前没有连接设备，尚未做真机视觉验证。未提交、未推送。

## 2026-10-20 05:52

任务：将本地提交推送到 GitHub fork。

推送：

- 远端：`https://github.com/Baicha0108/Baihub.git`
- 分支：`master`
- 范围：`6acf13e5..5550034c`
- 包含：上游 3.1.0 同步合并、聊天顶部渐隐恢复、菜单圆角统一、自定义提示词卡片与导出按钮调整。
- 未向上游提交 PR。

## 2026-10-20 05:58

任务：新增一份简洁的项目差异说明。

内容：

- 新建 `BAIHUB.md`，只记录 Baihub 与上游 RikkaHub 的差异。
- 覆盖身份与构建、助手自定义提示词、备份兼容、界面调整和维护约定。
- 不重复上游通用功能介绍。

备注：未构建、未推送。

## 2026-10-20 06:11

任务：把自定义提示词页底部的三个按钮改成可随滚动展开/收拢的胶囊。

实现：

- 使用与提示词注入页一致的 `HorizontalFloatingToolbar` 和 `floatingToolbarVerticalNestedScroll`。
- 胶囊内为导出图标、导入图标和“添加提示词”主按钮；滚动时隐藏按钮文字并收拢。
- 多选状态的“取消 + 导出”底部栏保持原样；空列表和退出多选时胶囊自动展开。
- `BAIHUB.md` 补充该界面差异。

验证：

- `:app:assembleRelease` 成功，耗时 4 分 8 秒。
- 产物：`dist/baihub-3.1.0-arm64-v8a-自定义提示词底部胶囊.apk`（42,133,886 字节）。
- SHA-256：`E1F89EAE142CBFC61EC93E40853F3D860702EEEBFB817D13647523A7006B8FC0`。
- 校验：包名 `me.baicha.baihub`，versionCode `193`，versionName `3.1.0`，仅 `arm64-v8a`。

备注：当前没有连接设备，尚未做真机视觉验证。未提交、未推送。

## 2026-10-21 12:33

任务：优化缩放式侧栏动画的流畅度，不改变现有视觉和交互。

实现：

- 仅修改 `ChatScalingDrawer.kt`。
- 动画进度改为 `State<Float>`，位移、缩放、圆角、阴影在 `graphicsLayer` 中读取，避免逐帧重组侧栏和聊天页。
- 遮罩改为 `drawBehind` 绘制；点击拦截层使用 `derivedStateOf`，只在阈值变化时重组。

验证：

- `:app:assembleRelease` 成功，耗时 4 分 56 秒。
- 产物：`dist/baihub-3.1.0-arm64-v8a-缩放式侧栏流畅优化.apk`（42,123,094 字节）。
- SHA-256：`F7B5CDC12F5F57F2158205BE52B9CEAA3F03E3FAA53DF1F46D1200321164ECF2`。
- 校验：包名 `me.baicha.baihub`，versionCode `193`，versionName `3.1.0`，仅 `arm64-v8a`。

备注：当前没有连接设备，尚未做真机流畅度验证。未提交、未推送。

## 2026-10-21 12:50

任务：为聊天输入栏底部阴影和聊天背景图遮罩增加独立开关。

实现：

- `DisplaySetting` 新增 `enableChatInputEdgeShadow` 和 `enableChatBackgroundScrim`，默认均开启，旧配置自动兼容。
- 常规设置“输入栏背景效果”块下新增两个独立设置块。
- 输入栏底部阴影开关控制玻璃输入栏的 `edgeShadow`；关闭时使用透明阴影。
- 聊天背景图遮罩开关控制背景图上的主题色渐变遮罩；关闭后只保留背景图和原透明度。
- `BAIHUB.md` 补充两个开关的界面差异。

验证：

- `:app:assembleRelease` 成功，耗时 5 分 13 秒。
- 产物：`dist/baihub-3.1.0-arm64-v8a-聊天阴影与背景遮罩开关.apk`（42,127,014 字节）。
- SHA-256：`942AC24281AB5FA8A45FA4701C529C78DFDE0DC2128BD069DEAE0BBAE90190C9`。
- 校验：包名 `me.baicha.baihub`，versionCode `193`，versionName `3.1.0`，仅 `arm64-v8a`。

备注：当前没有连接设备，尚未做真机视觉验证。未提交、未推送。

## 2026-10-21 13:07

任务：修正“聊天界面底部阴影”开关指向的目标。

修正：

- 之前误把玻璃输入栏的 `edgeShadow` 当成底部阴影；现已恢复玻璃输入栏原逻辑。
- 开关改为控制 `ChatInput.kt` 中聊天界面最底部的背景色渐变阴影。
- 配置字段改为 `enableChatBottomShadow`，设置标题改为“聊天界面底部阴影”。
- 聊天背景图遮罩开关保持不变。

验证：

- `:app:assembleRelease` 成功，耗时 5 分 52 秒。
- 产物：`dist/baihub-3.1.0-arm64-v8a-聊天阴影与背景遮罩开关.apk`（42,126,694 字节）。
- SHA-256：`32E81E22B38A6546C9A3644D52D8562D3704C0E0C922062330BACE073355DB72`。
- 校验：包名 `me.baicha.baihub`，versionCode `193`，versionName `3.1.0`，仅 `arm64-v8a`。

备注：当前没有连接设备，尚未做真机视觉验证。未提交、未推送。

## 2026-10-21 17:10

任务：把世界书编辑从弹窗拆成独立页面，并把条目编辑改成模式注入风格的底部面板。

实现：

- 世界书列表点击和“添加世界书”改为跳转新的 `Screen.LorebookEditor` 页面。
- 新增 `LorebookEditorPage.kt`，使用本地草稿编辑名称、描述、启用状态和条目，顶部保存后写回设置。
- 条目编辑改为 `ModalBottomSheet`，保留关键词、正则、常驻激活、扫描深度、注入位置和内容等全部字段。
- 移除 `PromptPage.kt` 中旧的 `LorebookEditSheet` 和条目 `AlertDialog`。
- 模式注入编辑面板和逻辑保持不变。

验证：

- `:app:assembleRelease` 成功，耗时 3 分 27 秒。
- 产物：`dist/baihub-3.1.0-arm64-v8a-世界书独立编辑页.apk`（42,127,894 字节）。
- SHA-256：`3C4BAA7C1241DE7537E382D1EF11451112B2C0845B97E76E0D191E9AAE98414A`。
- 校验：包名 `me.baicha.baihub`，versionCode `193`，versionName `3.1.0`，仅 `arm64-v8a`。

备注：当前没有连接设备，尚未做真机操作验证。未提交、未推送。

## 2026-10-21 17:36

任务：为模式注入和世界书条目的“注入内容”增加全屏编辑按钮。

实现：

- 提取共用 `FullScreenContentField` 和 `FullScreenContentEditor`。
- 自定义提示词、模式注入、世界书条目统一使用同一套全屏编辑组件。
- 模式注入和世界书条目的内容框右下角新增全屏按钮，确认后写回当前编辑草稿。
- 新增“编辑注入内容”和“全屏编辑注入内容”文案。

验证：

- `:app:assembleRelease` 成功，耗时 3 分 30 秒。
- 产物：`dist/baihub-3.1.0-arm64-v8a-注入内容全屏编辑.apk`（42,129,218 字节）。
- SHA-256：`F9A67DCFC26F73416208C1D049CF47299D756B20E5046666F819F5AE4EB3AE65`。
- 校验：包名 `me.baicha.baihub`，versionCode `193`，versionName `3.1.0`，仅 `arm64-v8a`。

备注：当前没有连接设备，尚未做真机操作验证。未提交、未推送。

## 2026-10-23 14:45

任务：同步合并上游 `rikkahub/rikkahub` 最新 28 个提交。

合并：

- 上游范围：`e5bb7073..41ce12f6`，最新提交为 models.dev 模型能力识别。
- 合并提交：`15e616ba`。
- 同步前创建本地备份标签：`pre-upstream-20261023`。
- 冲突：`RikkaHubApp.kt` 与 `SettingSearchPage.kt` 的 import 块；保留 `LocalFileUrlRepair` 和 `SettingsTopBarTitle`，其余采用上游结构。
- 上游新增 github/play 分发渠道；Baihub 包名 `me.baicha.baihub`、显示名和仅 arm64-v8a 配置均保留。
- 版本继续为 `3.1.0 (193)`。

验证：

- `:app:assembleGithubRelease` 成功，耗时 4 分 17 秒。
- 自定义提示词、导出合并、文件路径修复、备份路径改写单元测试通过，耗时 2 分 23 秒。
- 产物：`dist/baihub-3.1.0-arm64-v8a-同步上游20261023.apk`（16,233,538 字节）。
- SHA-256：`6A47BD25372638D78C1C2108830E4105487F3A99705809214FAEFCD52C332C26`。
- 校验：包名 `me.baicha.baihub`，versionCode `193`，versionName `3.1.0`，仅 `arm64-v8a`。

备注：当前没有连接设备，尚未做真机验证。未推送。

## 2026-10-23 15:22

任务：修复设置页大标题胶囊在顶栏收起时被裁切的问题。

实现：

- 新增 `SettingsLargeTopAppBar`，改用 `TwoRowsTopAppBar` 的独立 expanded/small title 布局。
- 展开状态使用无胶囊的大标题，收起状态使用带胶囊的小标题。
- `SettingsTopBarTitle` 通过 CompositionLocal 区分展开与收起状态。
- 18 个使用大标题栏的设置页统一切换到新组件。

验证：

- `:app:assembleGithubRelease` 成功，耗时 3 分 8 秒。
- 产物：`dist/baihub-3.1.0-arm64-v8a-设置标题双层修复.apk`（16,232,746 字节）。
- SHA-256：`EBE738145A99E0664BAB80AD87DD882853C537412E0C8F69EE8587A53A7EA8A2`。
- 校验：包名 `me.baicha.baihub`，versionCode `193`，versionName `3.1.0`，仅 `arm64-v8a`。

备注：当前没有连接设备，尚未做真机滚动和标题动画验证。未提交、未推送。

## 2026-10-23 15:42

任务：彻底修复透明设置顶栏的大标题向上移动时被硬裁剪的问题。

原因：

- 透明顶栏改造后，Material3 大标题所在的 `TopAppBarLayout` 仍使用 `clipToBounds`；大标题向上收起时，裁剪边界在透明背景下直接可见。
- 只拆分标题插槽或调整胶囊高度都不能绕过这条裁剪线。

实现：

- `SettingsLargeTopAppBar` 改为自定义图层，不再使用 `TwoRowsTopAppBar` 的裁剪容器。
- 小标题固定在 64dp 上行；大标题放在独立、不裁剪的图层中。
- 大标题按滚动进度向上移动并提前淡出，小标题胶囊按进度淡入。
- 保留顶部 insets、透明容器、导航按钮和 actions。
- 18 个设置大标题页继续统一使用新组件。

验证：

- `:app:assembleGithubRelease` 成功，耗时 3 分 20 秒。
- 产物：`dist/baihub-3.1.0-arm64-v8a-设置标题自定义图层.apk`（16,237,394 字节）。
- SHA-256：`2C8DE51C54D7EB5E11ADC5EB5B33BCED0493AE8367B22633C47E8A759EA0C7F3`。
- 校验：包名 `me.baicha.baihub`，versionCode `193`，versionName `3.1.0`，仅 `arm64-v8a`。

备注：当前没有连接设备，尚未做真机滚动和动画验证。未提交、未推送。

## 2026-10-23 19:16

任务：同步上游 `41ce12f6..e48db7bc`，共 6 个提交。

上游主要内容：

- 后台生成时显示可查看进度、快速回复的聊天悬浮窗。
- 抽出应用 Logo 的绘制与变形轮廓。
- Tavily 搜索可关闭生成答案，完善搜索 query 参数说明。
- 修复记忆文件删除内容后仍超限时无法保存的问题。

合并处理：

- 创建同步前标签 `pre-upstream-20261023-2`。
- `SettingAboutPage.kt` 唯一冲突为双方各自新增的 import；合并保留上游 Logo 组件与 Baihub 顶部遮罩。
- 包名继续为 `me.baicha.baihub`，显示名继续为 `Baihub`，仅构建 `arm64-v8a`。

验证：

- `:app:compileGithubDebugKotlin` 成功，耗时 4 分 45 秒。
- 上游新增的 `GenerationPhaseTest`、`ChatFloatingTrackerTest`、`ConversationSessionTest`、`FloatingChatUiStateTest` 全部通过。
- `:app:assembleGithubRelease` 成功，耗时 5 分 38 秒。
- 产物：`dist/baihub-3.1.0-arm64-v8a-同步上游第二批.apk`（16,281,930 字节）。
- SHA-256：`CB90069F0A3583CD04C739C11FF469C22C281FDF127D8D9D185662FE345B12DD`。
- 校验：包名 `me.baicha.baihub`，显示名 `Baihub`，versionCode `193`，versionName `3.1.0`，仅 `arm64-v8a`。

备注：本次已同步、提交并构建，未推送。

## 2026-10-25 09:35

任务：同步上游 `e48db7bc..ad2c4430`，共 14 个提交，并构建 Release APK。

上游主要内容：

- 版本提升到 `3.1.1 (194)`。
- 新增记忆整理模型设置，媒体提供商配置改为独立详情页。
- 修复 TTS 朗读 HTML 标签、收藏模型拖动抽搐、连续返回导致导航栈弹空、Markdown 公式 `<` 被误判为 HTML。
- 联网搜索入口移到输入栏加号面板，新增公共 `SwipeToDelete` 组件。
- 更新依赖，启用 Gradle 并行构建并调整 R8 keep 规则。

合并处理：

- 创建同步前标签 `pre-upstream-20261025`。
- `ChatInput.kt` 与 `ChatPage.kt` 冲突已解决：采用上游加号面板搜索入口，同时保留自定义提示词快捷按钮。
- 包名继续为 `me.baicha.baihub`，显示名继续为 `Baihub`，仅构建 `arm64-v8a`。

验证：

- `:app:testGithubDebugUnitTest` 共运行 514 项，507 项通过。
- 7 项失败均为 Windows 下 DataStore 临时文件原子重命名 `IOException`，串行复跑结果相同，应用编译正常。
- `:app:assembleGithubRelease` 成功，耗时 5 分 40 秒。
- 产物：`dist/baihub-3.1.1-arm64-v8a-同步上游3.1.1.apk`（16,291,460 字节）。
- SHA-256：`9B9033163ADA7B0152DBA586B8B074053A6DF185C1D7B4D2902E149528E98728`。
- 校验：包名 `me.baicha.baihub`，显示名 `Baihub`，versionCode `194`，versionName `3.1.1`，仅 `arm64-v8a`。

备注：未推送。

## 2026-10-25 18:58

任务：调整侧栏底部设置按钮的水平位置，使其与上方助手头像对齐。

实现：

- 将底部快捷操作行的右侧内边距从 8dp 增加到 16dp。
- 设置圆钮向左移动 8dp，共享胶囊和其余按钮位置不变。

验证：

- `:app:assembleGithubRelease` 成功，耗时 4 分 14 秒。
- 产物：`dist/baihub-3.1.1-arm64-v8a-设置按钮对齐.apk`（16,292,800 字节）。
- SHA-256：`CC221FAFCE632F01EDA64A59637F1A5BAB0BCE870F5927DE87A140C06AC84B6A`。
- 校验：包名 `me.baicha.baihub`，显示名 `Baihub`，versionCode `194`，versionName `3.1.1`，仅 `arm64-v8a`。

备注：当前没有连接设备，尚未做真机对齐检查。未推送。

## 2026-10-25 20:25

任务：修复缩放式侧栏在完全打开或完全关闭时最后一帧可能卡顿、跳动的问题。

原因：

- 原有弹簧动画使用归一化进度 `0f..1f`，但 Compose 浮点弹簧默认约 `0.01f` 的完成阈值会让动画在距离终点约 1% 时结束并写入准确目标。
- 300dp 的侧栏位移对应约 3dp 的末尾跳变。
- 透明点击拦截层也使用 `progress > 0.01f` 控制组合，切换时间与弹簧收尾重合。

实现：

- 将侧栏 `SettleSpec` 从弹簧改为 340ms 的 `tween`，使用 `FastOutSlowInEasing`，保持原弹簧约 330ms 的总体节奏并准确缓动到 0/1。
- 点击拦截层改为跟随 `open` 状态组合，在打开时加入、开始关闭时移除，不再在动画末尾因进度阈值触发重组。
- 缩放、位移、圆角、阴影和拖拽参数保持不变。

验证：

- `:app:assembleGithubRelease` 成功，耗时 2 分 57 秒。
- 产物：`dist/baihub-3.1.1-arm64-v8a-侧栏动画终点修复.apk`（16,293,592 字节）。
- SHA-256：`23E64DC6CBDC3836174BAF0B3642CA99DCE5CE9A30FE4E1A7D5400A41DC01AE3`。
- 校验：包名 `me.baicha.baihub`，显示名 `Baihub`，versionCode `194`，versionName `3.1.1`，仅 `arm64-v8a`。

备注：当前没有连接设备，尚未做真机动画对比。未推送。

## 2026-10-25 20:38

任务：修复快速轻扫侧栏后，从手动拖动切换到自动吸附时出现的停顿感。

原因：

- 每个拖动事件都会独立启动 `snapTo` 协程；松手后的迟到更新可能取消刚开始的吸附动画。
- 手势检测器以 `open` 为 key，状态改变时会重建检测器并可能重复触发吸附。
- 拖动结束沿用从静止起步的 `FastOutSlowInEasing`，会让原有拖动速度先归零再重新加速。

实现：

- 拖动位置使用同步累计目标，并通过 `Channel.CONFLATED` 合并最新值，由单个协程串行执行 `snapTo`。
- 松手前关闭通道并等待最后一次拖动位置应用完成，再启动吸附动画。
- 手势检测器不再随 `open` 状态重建，并过滤未进入拖动状态的普通点击。
- 拖动松手使用 300ms 的 `LinearOutSlowInEasing`，保留起始速度；按钮、返回键和点击外部仍使用 340ms 的 `FastOutSlowInEasing`。
- 吸附动画先以 `CoroutineStart.UNDISPATCHED` 启动，再更新 `open`，避免状态切换取消动画或重复启动。

验证：

- `:app:assembleGithubRelease` 成功，耗时 3 分 13 秒。
- 产物：`dist/baihub-3.1.1-arm64-v8a-侧栏滑动衔接修复.apk`（16,293,944 字节）。
- SHA-256：`22D1329E1F173566B4B2F9604220BFA6577D824917249D67B5271312001C3F29`。
- 校验：包名 `me.baicha.baihub`，显示名 `Baihub`，versionCode `194`，versionName `3.1.1`，仅 `arm64-v8a`。

备注：当前没有连接设备，尚未做真机快速滑动对比。未推送。

## 2026-10-25 14:13

任务：调整聊天侧栏快捷入口。

实现：

- 底部四个快捷按钮改为共用一个胶囊背景，每个图标仍保留独立点击、波纹、Tooltip 和无障碍描述。
- 第一个入口改为 AI 翻译，第二个入口改为媒体生成；收藏和统计数据保持不变。
- 设置按钮继续作为胶囊右侧的独立圆形按钮。
- 删除侧栏上方原有的媒体生成和 AI 翻译入口，只保留历史记录入口。
- 移除原“助手”和“菜单”快捷按钮及菜单弹层。

验证：

- `:app:assembleGithubRelease` 成功，耗时 4 分 11 秒。
- 产物：`dist/baihub-3.1.1-arm64-v8a-侧栏快捷胶囊.apk`（16,290,072 字节）。
- SHA-256：`C02966F36C8BD5150779E3FF349C536869C3D9C29E3C06E5C6DD7C9F6E05F40A`。
- 校验：包名 `me.baicha.baihub`，显示名 `Baihub`，versionCode `194`，versionName `3.1.1`，仅 `arm64-v8a`。

备注：当前没有连接设备，尚未做真机侧栏视觉检查。未推送。

## 2026-10-25 14:20

任务：缩小侧栏共享胶囊高度，使其与右侧圆形设置按钮的直径一致。

实现：

- 胶囊总高度由 56dp 调整为 48dp。
- 四个内部快捷入口的独立点击区域由 48dp 调整为 40dp，图标尺寸和间距保持不变。
- 右侧设置圆钮继续保持 48dp。

验证：

- `:app:assembleGithubRelease` 成功，耗时 2 分 52 秒。
- 产物：`dist/baihub-3.1.1-arm64-v8a-侧栏胶囊高度.apk`（16,290,252 字节）。
- SHA-256：`2249554BBD6757BD4156A0FF475C6D3B8DC53A60F42044E9EC7AC5D08A4DDFF4`。
- 校验：包名 `me.baicha.baihub`，显示名 `Baihub`，versionCode `194`，versionName `3.1.1`，仅 `arm64-v8a`。

备注：当前没有连接设备，尚未做真机视觉检查。未推送。

## 2026-10-25 14:37

任务：为侧栏头像区域增加透明的大圆角交互层。

实现：

- 头像、昵称和问候语区域外层增加透明大圆角点击区域，不设置背景色、边框、阴影或点击波纹。
- 支持单击和长按，新增 `onProfileAreaClick`、`onProfileAreaLongClick` 两个预留回调，当前为空实现。
- 头像、昵称和问候语的位置保持不变，继续作为前景叠在交互层上。
- 头像更换、昵称编辑等现有点击行为保持不变。

验证：

- `:app:assembleGithubRelease` 成功，耗时 3 分 18 秒。
- 产物：`dist/baihub-3.1.1-arm64-v8a-侧栏头像交互区.apk`（16,290,180 字节）。
- SHA-256：`5B2045ABF4A318838BCB960E8E65AE3135BC1C39650218FD57679B960C65AA50`。
- 校验：包名 `me.baicha.baihub`，显示名 `Baihub`，versionCode `194`，versionName `3.1.1`，仅 `arm64-v8a`。

备注：当前没有连接设备，尚未做真机点击、长按和布局检查。未推送。

## 2026-10-25 14:43

任务：为侧栏头像透明交互区域增加点击反馈。

实现：

- 使用系统默认 `LocalIndication` 波纹作为单击和长按反馈。
- 波纹范围继续由 24dp 大圆角区域裁剪，不增加背景色、边框或阴影。

验证：

- `:app:assembleGithubRelease` 成功，耗时 3 分 8 秒。
- 产物：`dist/baihub-3.1.1-arm64-v8a-侧栏头像点击反馈.apk`（16,290,160 字节）。
- SHA-256：`DA621ABC253B6428173D3C5BF8CA39450BD945C05BD55A7B1E5784D398DC25BA`。
- 校验：包名 `me.baicha.baihub`，显示名 `Baihub`，versionCode `194`，versionName `3.1.1`，仅 `arm64-v8a`。

备注：当前没有连接设备，尚未做真机点击和长按反馈检查。未推送。

## 2026-10-25 15:08

任务：为侧栏头像区域增加模式注入快捷选择和编辑功能。

实现：

- 长按头像区域弹出居中选择器，标题为“选择提示词”，说明文字和空状态均已配置中英文资源。
- 选择器展示全部模式注入，支持滚动和当前选中标记；点击条目后立即保存选择并关闭。
- 单击头像区域时，若快捷条目有效则直接打开现有“编辑模式注入”底部编辑面板；无选择或原条目已删除时改为打开选择器。
- 快捷目标使用独立的 `sidebar_mode_injection_shortcut_id` SharedPreferences 键保存，不修改设置数据结构和备份格式。
- 复用现有 `ModeInjectionEditSheet`，保存时按条目 ID 更新最新设置，避免覆盖其他设置修改。
- 不修改当前助手的 `modeInjectionIds`，选择快捷编辑目标不会改变模式注入的启用分配。
- 空列表提供“前往提示词管理”入口。

验证：

- `:app:assembleGithubRelease` 成功，耗时 3 分 56 秒。
- 产物：`dist/baihub-3.1.1-arm64-v8a-侧栏提示词快捷编辑.apk`（16,292,836 字节）。
- SHA-256：`77ADD47C4139D5A739F25437C922C80A5D01C0696C1FF262564A3A50E41D4092`。
- 校验：包名 `me.baicha.baihub`，显示名 `Baihub`，versionCode `194`，versionName `3.1.1`，仅 `arm64-v8a`。

备注：当前没有连接设备，尚未做真机选择、编辑和返回流程检查。未推送。

## 2026-10-25 15:14

任务：精简侧栏提示词选择器底部空间。

实现：

- 移除选择提示词弹窗底部的“取消”按钮。
- 弹窗仍可通过点击外部区域或系统返回键关闭。

验证：

- `:app:assembleGithubRelease` 成功，耗时 3 分 25 秒。
- 产物：`dist/baihub-3.1.1-arm64-v8a-侧栏提示词选择器精简.apk`（16,292,688 字节）。
- SHA-256：`29E70058D2A7952561E52C4454A3FA9FB6867955101FAD75C01173228FA63BCF`。
- 校验：包名 `me.baicha.baihub`，显示名 `Baihub`，versionCode `194`，versionName `3.1.1`，仅 `arm64-v8a`。

备注：未推送。

## 2026-10-23 15:50

任务：修复大标题向上收起时从下沿开始被裁剪的问题。

原因：

- 大标题放在随滚动缩小的 `Column.weight(1f)` 中，文本在布局阶段就被压缩高度。
- `graphicsLayer` 只能做平移和淡出，无法恢复已被布局约束裁掉的文字。

实现：

- 大标题移出 `Column.weight`，改为外层 `Box` 的独立覆盖层。
- 使用 `requiredHeight(56.dp)` 固定大标题测量高度，布局尺寸不再随外层收起高度缩小。
- 大标题以底边对齐外层，随顶栏高度变化整体上移，再通过 `graphicsLayer` 额外平移和提前淡出。

验证：

- `:app:assembleGithubRelease` 成功，耗时 3 分 19 秒。
- 产物：`dist/baihub-3.1.0-arm64-v8a-设置标题固定图层.apk`（16,237,458 字节）。
- SHA-256：`B36A9C00FD84FC0118C61E0231275AABDA3F2E3B46BBE0D1772E262AFC49141D`。
- 校验：包名 `me.baicha.baihub`，versionCode `193`，versionName `3.1.0`，仅 `arm64-v8a`。

备注：当前没有连接设备，尚未做真机滚动和动画验证。未提交、未推送。
