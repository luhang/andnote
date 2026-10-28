# 笔记 · Local Notes

一个 Android 本地笔记 App：**Google 账户登录 + 笔记只存本机**。

- Kotlin + Jetpack Compose（Material 3）
- Room 数据库（SQLite）本地存储，不联网、不上传
- Google 登录走 **Credential Manager + ID Token**（不需要 SHA-1 登记）
- 登录态用 DataStore 记住，重启仍在登录状态

## 功能

| 模块 | 说明 |
|---|---|
| 登录 | Google 账户一键登录，显示头像/昵称/邮箱，可退出 |
| 列表 | 置顶优先 + 按更新时间倒序，标题/正文模糊搜索，空状态引导 |
| 编辑 | 新建/编辑，停止输入 600ms 自动保存，返回时兜底保存 |
| 管理 | 置顶、删除（列表页删除可撤销）、字数统计 |
| 隔离 | 每条笔记记录所属账户，同设备换账号登录各看各的 |
| 主题 | 跟随系统深色/浅色 |

## 目录结构

```
android-notes/
├── settings.gradle.kts / build.gradle.kts / gradle.properties
├── gradle/wrapper/                     # 已内置 wrapper，可直接 ./gradlew
└── app/src/main/
    ├── AndroidManifest.xml
    ├── res/                            # 图标、主题、备份规则
    └── java/com/localnotes/app/
        ├── MainActivity.kt             # 唯一 Activity，承载 Compose
        ├── NotesApp.kt                 # 手动依赖容器（DB / Repository / Session）
        ├── auth/GoogleAuth.kt          # Credential Manager 登录 + JWT 解析
        ├── data/                       # NoteEntity, NoteDao, AppDatabase,
        │                               # NoteRepository, SessionStore
        └── ui/
            ├── MainViewModel.kt        # 单一 ViewModel，StateFlow 驱动
            ├── AppRoot.kt              # 页面切换（列表 / 编辑 / 登录）
            ├── SignInScreen.kt
            ├── NotesScreen.kt
            ├── EditorScreen.kt
            ├── Format.kt               # 相对时间、标题兜底
            └── theme/
```

## 三步跑起来

**1. 用 Android Studio 打开 `android-notes` 目录**（Ladybug 2024.2 及以上，自带 JDK 17）。
首次同步会下载 Gradle 8.9 与依赖，耐心等几分钟。

**2. 配置 Google 登录客户端 ID**

1. 打开 https://console.cloud.google.com/ → 新建或选择项目
2. 「API 和服务」→「OAuth 同意屏幕」：类型选**外部**，填应用名 + 支持邮箱，保存
   - 仍在「测试」状态时，必须把要登录的 Google 账号加到 **测试用户**，否则会报 `access_denied`
3. 「凭据」→「创建凭据」→「OAuth 客户端 ID」→ 应用类型选 **Web 应用**
   （名称随意，重定向 URI 不用填）
4. 复制生成的客户端 ID，填进 `gradle.properties`：

```properties
GOOGLE_WEB_CLIENT_ID=1234567890-xxxxxxxx.apps.googleusercontent.com
```

5. 点 Android Studio 的 **Sync Now**（改了 gradle.properties 必须重新 Sync 才生效）

**3. 运行**

- 真机：需已登录 Google 账号、装 Google Play 服务
- 模拟器：必须选 **带 Google Play 图标** 的系统镜像（AOSP 镜像没有 Play 服务，登录必失败）

命令行打包：

```bash
cd android-notes
./gradlew assembleDebug          # Windows: gradlew.bat assembleDebug
# 产物 app/build/outputs/apk/debug/app-debug.apk
```

> 如果 `GOOGLE_WEB_CLIENT_ID` 仍为空，App 照样能跑：登录页会显示配置提示，并出现
> 「先跳过登录（调试用）」按钮，方便先验证笔记功能。

## 数据存在哪

- 数据库文件：`/data/data/com.localnotes.app/databases/notes.db`（Room 自动创建）
- 表 `notes`：`id / ownerId / title / content / pinned / createdAt / updatedAt / deletedAt`
- `deletedAt` 是软删除标记，列表只查 `deletedAt IS NULL`；撤销就是把该字段置空。
  超过 30 天的回收站数据会在 App 启动时清理。
- 登录态：`datastore/session.preferences_pb`，只存 `sub / email / name / photo`，不存 token。
- 卸载 App 即全部删除；`allowBackup=true`，所以系统云备份/换机迁移会带上数据库
  （不想要就把 `AndroidManifest.xml` 里改成 `false`，并删掉两个 xml 规则）。

## 常见问题

| 现象 | 原因 / 处理 |
|---|---|
| `DEVELOPER_ERROR` / code 10 | 客户端 ID 不是 Web 类型，或填错；确认是 `...apps.googleusercontent.com` |
| 弹窗后立刻失败、无账号可选 | 设备没登录 Google 账号，或模拟器无 Play 服务 |
| `access_denied` | OAuth 同意屏幕还是「测试」状态，当前账号不在测试用户列表 |
| 登录成功但头像是空白 | 头像 URL 需要联网；离线时回退显示首字母 |
| 编译报 `Unsupported class file major version` | Gradle JDK 不是 17+，在 Settings → Build Tools → Gradle 里指定 JDK 17 |

## 下一步可以加什么

- **云同步**：登录拿到的 `idToken` 发给自己的后端验签，再把笔记存到服务端（当前实现刻意不做）
- 富文本/图片：正文换成 Markdown 渲染 + 图片存 `filesDir`
- 标签与文件夹：加 `tags` 表和关联表
- 生物识别锁：`androidx.biometric` 在进入列表前加一道指纹校验
- 导出/导入：`ACTION_CREATE_DOCUMENT` 导出 JSON 或 Markdown

## 已知限制

1. 只做**本机**存储，换设备/清数据不恢复；需要多端同步得自己接后端。
2. ID Token 只在客户端解析，**没有验签**——仅用于拿 email/昵称，不做安全凭据。
3. 编辑器最多保存 120 字标题（避免标题撑爆列表），正文不限。
