# Windows 开发环境搭建记录（2026-10-28）

> 目标：让 `android-notes/` 工程能在本机构建、运行、调试。
> 机器：Ryzen 7 7735HS（8C16T）/ 30GB 内存 / C 盘剩余 700GB+ / 已开启 Hypervisor。

## 已完成（自动装好的部分）

| 组件 | 版本 | 位置 |
|---|---|---|
| JDK (Temurin) | 21.0.12.1 LTS | `C:\Users\luhang\AppData\Local\Programs\jdk21` |
| Android cmdline-tools | 最新 | `%LOCALAPPDATA%\Android\Sdk\cmdline-tools\latest` |
| platform-tools (adb) | 37.0.1 | `%LOCALAPPDATA%\Android\Sdk\platform-tools` |
| platforms;android-35 | API 35 | `%LOCALAPPDATA%\Android\Sdk\platforms\android-35` |
| build-tools | 35.0.0 / 34.0.0 | `%LOCALAPPDATA%\Android\Sdk\build-tools` |
| emulator + 系统镜像 | android-35 google_apis_playstore x86_64 | `%LOCALAPPDATA%\Android\Sdk\emulator`、`system-images` |
| AVD 模拟器 | Pixel_7_API_35 | `%USERPROFILE%\.android\avd` |
| Android Studio | 2026.2.1.8（**未装完，见下**） | `C:\Program Files\Android\Android Studio` |

环境变量（用户级，已设置）：`JAVA_HOME`、`PATH` 追加 `%JAVA_HOME%\bin`。

## ⚠️ 待你手动完成：Android Studio 装完最后一步

安装包在：**`下载\android-studio-2026.2.1.8-windows.exe`**

目前只落盘了约 700MB（bin/jbr/lib），`plugins` 目录是空的 —— 静默安装被系统的
SmartScreen/安全策略反复打断。请双击安装包走一遍图形向导：

1. 双击 `android-studio-2026.2.1.8-windows.exe`（若弹「Windows 已保护你的电脑」→ 点
   **更多信息** → **仍要运行**；弹 UAC → 点**是**）
2. Next → 选择默认路径 `C:\Program Files\Android\Android Studio` → Install
3. 勾选 Start → Finish

装完它会检测到 `%LOCALAPPDATA%\Android\Sdk`（SDK 不用重下）。

## 已验证可用的功能

命令行构建（不依赖 Studio）：

```bash
cd C:\Users\luhang\WorkBuddy\2026-10-28-09-30-42\android-notes
set JAVA_HOME=C:\Users\luhang\AppData\Local\Programs\jdk21
gradlew.bat assembleDebug
# 产物：app\build\outputs\apk\debug\app-debug.apk（19MB，BUILD SUCCESSFUL）
```

模拟器（带窗口模式）：

```bash
%LOCALAPPDATA%\Android\Sdk\emulator\emulator.exe -avd Pixel_7_API_35
# 另开一个窗口装 App：
%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe install -r app\build\outputs\apk\debug\app-debug.apk
```

已在无头模拟器上完成端到端验证（截图在 `shots/` 目录）：

| 截图 | 内容 |
|---|---|
| 01-login.png | 登录页（G 标志 + Google 登录按钮 + OAuth 未配置提示） |
| 03-list.png | 列表页空状态（搜索框 + 新建按钮 + 空态引导） |
| 05-editor.png | 编辑页（自动保存「已保存 01:29」） |
| 06-list-with-note.png | 新建笔记已入库，退出重开后仍在（持久化验证通过） |

## 接下来（在 Studio 里）

1. 打开工程 `android-notes/` → 等 Gradle Sync（首次 3~5 分钟）
2. 选 `Pixel_7_API_35` 模拟器 → 点 Run ▶
3. 要用 Google 登录：把 Cloud Console 的 Web 客户端 ID 填进 `gradle.properties`
   的 `GOOGLE_WEB_CLIENT_ID` 再 Sync（详见 README.md 第二节）

## 常见问题

- **模拟器起不来/黑屏**：任务管理器确认「虚拟化」已启用（本机 Hypervisor 已开，正常情况没问题）
- **Gradle 报 `Unsupported class file major version`**：`JAVA_HOME` 没指到 JDK 17+，
  新开一个终端让用户级环境变量生效，或在 Studio 设置里指定 Gradle JDK = jbr
- **adb 找不到设备**：`adb kill-server && adb start-server` 重试
- **安装器又弹 SmartScreen**：文件已用 `Unblock-File` 处理过一次；若重新下载会再次触发，
  点「更多信息 → 仍要运行」即可
