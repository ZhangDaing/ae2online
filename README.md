# AE2 Online

**通过局域网远程查看和操作 AE2（Applied Energistics 2）存储系统。**

在游戏里放置「线上终端」方块，即可用桌面客户端、手机 App 或浏览器随时随地查看 ME 网络的存储内容、监控合成 CPU、下达合成任务——不必守在电脑前开游戏。

| 组件 | 说明 |
|---|---|
| **Forge 模组**（服务端） | 提供「线上终端」方块与 HTTPS API；web 版内嵌浏览器界面 |
| **Windows 客户端** | 桌面主力，无需安装 |
| **Android 客户端** | 手机/平板使用 |
| **浏览器 Web 版** | 任意设备临时访问，装了模组即可用 |

---

## 功能

- 📦 **存储浏览** —— 实时查看 ME 网络内所有物品与数量，支持搜索、按名称/数量/模组排序
- 🖼️ **物品图标** —— 显示物品贴图（含伪装板等带 NBT 的变体）
- 🖥️ **CPU 监控** —— 查看所有合成 CPU 的状态、进度、已用时间，可取消任务
- ⚒️ **远程合成** —— 计算合成计划（所需/缺失材料、字节数），选择 CPU 下达合成
- 🔐 **多终端管理** —— 一个客户端可同时登录多个线上终端，下拉切换
- 🔒 **内置 HTTPS** —— 自动生成自签名证书；局域网传输加密，且满足浏览器安全上下文要求
- 🌐 **免客户端** —— web 版 jar 内嵌完整界面，浏览器直接打开就能用
- 🌙 **深色/浅色主题** —— 内置两套主题，可在设置中切换

---

## 环境要求

| | 版本 |
|---|---|
| Minecraft | 1.20.1 |
| Forge | 47.4.10+ |
| **AE2**（Applied Energistics 2） | 15.4.10（必需） |
| **GuideME** | 20.1.7（AE2 依赖） |
| Java | 17+（服务端，需含 `keytool` 用于生成 HTTPS 证书），21（构建） |

可选依赖：

| 模组 | 作用 |
|---|---|
| **Jade** | 显示终端的网络状态（在线/离线/缺频道） |
| **icon-exporter** | 导出物品图标，供客户端显示贴图 |
| JEI | 合成配方查询（开发环境） |


---

## 安装

### 服务端（Minecraft）

1. 安装 **Forge 1.20.1**（客户端或专用服务器）
2. 将以下文件放入 `mods/`：
   - `ae2online-1.0.0.jar` **或** `ae2online-1.0.0-web.jar`（二选一，见下表）
   - AE2 及其依赖（Applied Energistics 2、GuideME）
   - （可选）Jade、icon-exporter
3. 启动一次游戏/服务器，生成配置文件 `config/ae2online-common.toml`（含自动生成的 HTTPS 证书）

#### 两个 jar 版本怎么选

| 文件 | 大小 | 含 Web UI | 适用 |
|---|---|---|---|
| `ae2online-1.0.0.jar` | ~60 KB | ❌ | 只用 Windows / Android 客户端 |
| `ae2online-1.0.0-web.jar` | ~12 MB | ✅ | 想用浏览器直接访问 |

> web 版是标准版的超集：API 完全一致，额外内嵌了浏览器界面。
> 装 web 版后，Windows / Android 客户端同样可以正常连接。

### 客户端

| 平台 | 文件 | 说明 |
|---|---|---|
| Windows | `ae2online.exe` | 无需安装，直接运行 |
| Android | `ae2online.apk` | 需允许「安装未知来源应用」 |
| 浏览器 | 无需下载 | 装 web 版 jar 后访问 `https://服务器IP:端口/` |

两种客户端都可分发到局域网内的其他设备使用。

> 从源码导出客户端见下文「从源码构建」。

---

## 使用方法

### 1. 合成「线上终端」

在合成台中按以下配方合成（材料均来自 AE2 后期）：

```
A B A
C D C
F E F
```

| 位置 | 材料 |
|---|---|
| A | 量子纠缠奇点（Quantum Entangled Singularity） |
| B | 无线接收器（Wireless Receiver） |
| C | 合成单元（Crafting Unit） |
| D | 量子环（Quantum Ring） |
| E | 无线合成终端（Wireless Crafting Terminal） |
| F | 无线增幅器（Wireless Booster） |

### 2. 放置与接线

1. 将「线上终端」接入 **ME 网络**（用线缆连接）
2. 确保网络**供电**且终端**有可用频道**（终端需要 16 AE/t，且占用 1 个频道）
3. 用 Jade 查看方块状态：显示 **「设备在线」** 即就绪
   - 「缺少频道」= 网络频道用尽，需要 ME 控制器/精简线缆
   - 「设备离线」= 未供电或未接入网络

> **注意**：终端未接入网络时不会启动 Web 服务，客户端登录会提示「没有在线的终端」。这是设计如此——避免离线终端对外提供无效服务。

### 3. 配置终端

**右键**线上终端方块，弹出配置界面：

| 字段 | 说明 | 示例 |
|---|---|---|
| **端口** | Web 服务监听的端口号（1-65535） | `8080` |
| **用户名** | 登录账号 | `admin` |
| **密码** | 登录密码 | `123456` |

填好后点**保存**。端口填错（非数字或超范围）会红字提示且不保存。

- 端口会预填 `8080`，可按需修改
- **多个终端可以共用同一端口**，登录时靠用户名/密码区分
- 不同终端用不同端口也行（如 `8080`、`8081`），便于在客户端区分
- 监听地址由配置文件 `bindAddress` 决定，与这里填的端口无关（见下节）

> 旧版本存档里保存的是完整 URL（如 `http://127.0.0.1:8080`），升级后会自动兼容，无需重新配置。

### 4. 局域网访问设置（重要）

`bindAddress` 默认已是 `0.0.0.0`（监听所有网卡，局域网可直接访问）。如需限制为仅本机：

编辑 `config/ae2online-common.toml`：

```toml
# 0.0.0.0 = 局域网可访问（默认）
# 127.0.0.1 = 仅本机，更安全
bindAddress = "0.0.0.0"

# 是否启用 HTTPS（默认开启）
# 浏览器中的 Web 版必须在 HTTPS 下才能运行（Secure Context 限制）
enableHttps = true
```

改完**重启 Minecraft** 生效。

#### HTTPS 与自签名证书

`enableHttps = true`（默认）时，首次启动会调用 JDK 的 `keytool` 自动生成自签名证书到 `config/ae2online-cert.p12`，之后复用。

- 证书 10 年有效，SAN 自动包含本机所有局域网 IPv4 + `127.0.0.1` + `localhost`
- **浏览器首次访问会提示「连接不是私密连接」**，这是自签名证书的正常现象：
  - Chrome / Edge：点「高级」→「继续前往 xxx（不安全）」
  - Firefox：点「高级」→「接受风险并继续」
  - 手机浏览器流程类似
- 换了网络导致 IP 变化时，删除 `config/ae2online-cert.p12` 让其重新生成

> 如需在服务端以纯 HTTP 运行（放弃 Web 版），把 `enableHttps` 改为 `false`。

### 5. 客户端使用

#### 三种客户端

| 客户端 | 适用场景 | 说明 |
|---|---|---|
| **Windows 客户端** | 桌面主力 | `ae2online.exe`，无需安装 |
| **Android 客户端** | 手机/平板 | `ae2online.apk`，需允许安装未知来源应用 |
| **浏览器 Web 版** | 任意设备临时访问 | 使用含 Web UI 的模组 jar，浏览器直接打开 |

> Windows 与 Android 客户端**不需要**模组内置 Web UI（标准版 jar 即可）。
> Web 版需要 `ae2online-*-web.jar`（体积约 12 MB，内含浏览器界面）。

#### 使用浏览器访问（Web 版）

1. 服务端安装 **web 版** jar 并重启
2. 在终端方块里保存端口（如 `11595`）
3. 浏览器访问 `https://<服务器IP>:11595/`，例如 `https://192.168.31.195:11595/`
4. 跳过证书警告（见上节）后进入登录界面
5. **URL 会自动填成当前页面地址**，只需输入用户名密码

> Web 版首次加载需下载约 40 MB 的 wasm，请耐心等待。
> 浏览器里填写 `127.0.0.1` 会指向**用户自己的设备**，必须用服务器的局域网 IP（Web 版已自动填好）。

#### 桌面 / 移动客户端

运行 `ae2online.exe` 或安装 `ae2online.apk`，进入主界面：

**① 登录配置**（右侧「设置」面板）
- 添加登录配置：URL 已预填，填写用户名、密码
- 点**登录**，成功后左侧出现「终端 1 / 终端 2 ...」

**② 存储页**
- 顶部搜索框：按物品名过滤（**右键搜索框清空**）
- 卡片按名称/数量/模组排序，方向可升/降（设置面板）
- 卡片显示物品图标与数量，悬停显示物品名，**左键点击**打开合成面板
- 滚轮/触摸板滚动列表

**③ CPU 页**
- 每个合成 CPU 一行：名称、当前任务、进度条、已用时间
- **右键** CPU 卡片 → 弹出「取消」按钮，点击取消该任务
- 悬停显示存储字节数与并行处理器数

**④ 合成页**
- 显示所有可合成的物品（来自合成样板）
- 点击物品 → 弹出合成确认窗
- 填写数量 → **计算**：显示所需字节数、材料清单（可用/合成/缺失数量）
- 选择目标 CPU（或「自动分配」）→ **合成**下达任务
- 材料不足时合成按钮禁用

**⑤ 终端切换**
- 顶部下拉框切换不同终端（多个登录配置时）

**⑥ 主题**
- 设置面板中切换深色/浅色主题

### 6. 导出物品图标（显示贴图用）

物品图标由 **icon-exporter** 模组导出，供客户端显示贴图。**未导出图标的物品会显示空白**。

1. 安装 icon-exporter 模组（服务端 `mods/`）
2. 进入游戏，执行：
   ```
   /iconexporter export scale:64
   ```
   （参数格式可输 `/iconexporter export ` 后按 Tab 补全确认）
3. 图标 PNG 会导出到游戏目录的 `icon-exports-x64/`（与配置项 `iconPath` 对应）
4. 重启客户端即可显示

> **提示**：
> - 新增模组后需**重新导出**一次图标
> - 伪装板等带 NBT 的物品：导出会生成带 NBT 后缀的图标文件；服务端会自动匹配，找不到时回退到基础图标
> - 修改 `iconPath` 配置后，导出目录与客户端读取目录需保持一致

---

## 配置项（`config/ae2online-common.toml`）

```toml
# 图标目录（相对游戏运行目录）
iconPath = "icon-exports-x64"

# Web 服务监听地址
#   0.0.0.0   = 所有网卡，局域网可访问（默认）
#   127.0.0.1 = 仅本机（更安全）
#   也可填具体网卡 IP，只在该网卡监听
bindAddress = "0.0.0.0"

# 是否启用 HTTPS（自签名证书，默认开启）
# 浏览器中的 Web 版必须在 HTTPS 下才能运行
enableHttps = true

# 额外允许的浏览器跨域来源（CORS）
# 同源请求与 localhost / 127.0.0.1 默认放行，一般无需配置
# Godot 桌面客户端不发送 Origin 头，不受此项影响
allowedOrigins = []
```

---

## API 接口（供开发者）

所有接口返回 JSON，除 `/login` 外均需 `Authorization: Bearer <token>`。

自签名证书环境下，`curl` 需加 `-k` 跳过证书校验。

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/login` | 登录获取 token。请求体 `{"username","password"}` |
| GET | `/storage` | 存储物品列表（含 `uid`/`rawId`/`icon`/`count`） |
| GET | `/craftable` | 可合成物品列表 |
| GET | `/cpu` | 合成 CPU 状态 |
| POST | `/cpu/cancel` | 取消任务。请求体 `{"index"}` |
| POST | `/craft/plan` | 计算合成计划。请求体 `{"rawId","count"}` |
| POST | `/craft` | 下达合成。请求体 `{"rawId","count","cpu"}` |
| GET | `/icon/<文件名>` | 获取物品图标 PNG |
| GET | `/` | Web UI（仅 web 版 jar；标准版返回说明页） |

**登录示例**：

```bash
curl -k -X POST https://127.0.0.1:11595/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"123456"}'
# 返回 {"token":"<uuid>"}

curl -k https://127.0.0.1:11595/storage \
  -H "Authorization: Bearer <uuid>"
```

**错误说明**：

| 状态码 | 含义 |
|---|---|
| 401 | 未登录 / 凭据错误 |
| 503 | 没有在线的终端（未接网络/无频道）或终端已离线 |
| 400 | 参数错误（如缺少 `count`） |

---

## 常见问题

**Q：客户端登录提示「没有在线的终端」？**
A：终端方块未接入 ME 网络，或网络没供电/没有可用频道。用 Jade 确认方块显示「设备在线」。

**Q：客户端连不上服务端？**
A：
1. 确认 `bindAddress` 是 `0.0.0.0`（默认值，局域网可访问）
2. 确认 Windows 防火墙放行了该端口
3. 确认客户端填的 URL 是**服务器的局域网 IP**（如 `https://192.168.1.10:11595`），不是 `127.0.0.1`
4. 协议要与服务端一致：`enableHttps = true` 时用 `https://`

**Q：浏览器打开是空白 / 报 Secure Context 错误？**
A：Web 版必须通过 **HTTPS** 访问。确认：
1. 访问的是 `https://` 不是 `http://`
2. `enableHttps = true`
3. 已跳过自签名证书的警告
4. 首次加载需下载约 40 MB wasm，请等待
5. 强制刷新（`Ctrl+Shift+R`）清除旧缓存

**Q：浏览器提示「连接不是私密连接」？**
A：自签名证书的正常现象，不是故障。点「高级 → 继续前往」即可。每次新设备首次访问都需要点一次。

**Q：浏览器里的 Web 版登录失败？**
A：浏览器中 `127.0.0.1` 指向**用户自己的设备**而非服务器。Web 版已自动填充当前页面地址，若手动填写必须用服务器局域网 IP。

**Q：浏览器里物品贴图全空？**
A：已修复（图标接口此前缺 CORS 头，浏览器会丢弃图片）。如果仍空白，强制刷新或确认用的是最新模组版本。

**Q：物品贴图显示空白？**
A：未导出图标，见上文「导出物品图标」。新增模组后需重新导出。

**Q：加了新模组后没有图标？**
A：重新执行 `/iconexporter export`，并重启客户端。

**Q：显示「登录过期」？**
A：token 已失效（终端离线/被拆/服务重启）。重新登录即可。

**Q：多个终端怎么区分？**
A：在客户端配置多个登录配置（不同用户名/密码），顶部下拉框切换。同一端口也可以，靠用户名密码区分。

**Q：合成按钮是灰的？**
A：材料不足（材料清单里有「缺失数量」），或目标 CPU 不可用。

**Q：拆掉终端方块后要重新登录？**
A：是。方块被拆/离线时其令牌会失效（安全设计），重新登录即可。

**Q：改了 IP / 换了网络，HTTPS 失效？**
A：自签名证书的 SAN 里记录的是生成时的 IP。删除 `config/ae2online-cert.p12` 后重启，会按新 IP 重新生成。

**Q：服务端日志里有 `keytool 生成证书失败`？**
A：说明运行环境缺少 `keytool`（精简 JRE）。日志会打印手动生成证书的完整命令，执行后把文件放到 `config/ae2online-cert.p12` 即可；或临时改 `enableHttps = false` 用 HTTP。

---

## 从源码构建

**环境**：JDK 17+（推荐 21，需含 `keytool`）、Gradle 8.8（wrapper 自带）

### 构建模组

```bash
# 标准版（不含 Web UI，约 60 KB）
./gradlew build

# web 版（内嵌浏览器界面，约 12 MB）—— 需先完成下方「导出 Web 界面」
./gradlew webJar

# 两个版本一起构建
./gradlew buildAll
```

产物：`build/libs/ae2online-1.0.0.jar` 与 `build/libs/ae2online-1.0.0-web.jar`

### 导出客户端

用 Godot 4.7 打开 `godot/` 目录，项目 → 导出，选择预设：

| 预设 | 产物 | 说明 |
|---|---|---|
| `Windows Desktop` | `ae2online.exe` | 约 107 MB |
| `Android` | `ae2online.apk` | 约 30 MB，需 Godot 的 Android 导出模板 |
| `Web` | `index.html` 等 | 输出到 `godot/export/web/`，供 `webJar` 内嵌 |

### 构建 Web 版 jar 的完整流程

```bash
# 1. 先在 Godot 里用 Web 预设导出到 godot/export/web/
# 2. 构建内嵌 Web UI 的 jar
./gradlew webJar
```

`webJar` 会：
- 基于已 reobf 的标准版 jar 打包（避免运行期 `NoSuchFieldError`）
- 把 `godot/export/web/` 的内容放进 `assets/ae2online/web/`
- 输出 `build/libs/ae2online-1.0.0-web.jar`

> **不要**给 Godot 导出的 `index.js` 打补丁绕过 Secure Context 检查——
> 绕过 `isSecureContext` 后 `AudioWorklet` 在非安全上下文仍不可用，会连锁报错。
> 正确做法是用模组内置的 HTTPS 满足安全上下文要求。

### 发布产物

打包好的 jar / exe / apk 统一放在 `export/`（已在 `.gitignore` 中，不进版本库）。
发布时上传到 GitHub Releases。

### 其他提示

- 项目根目录 `.vscode/` 已配置 VS Code 任务，按 **Ctrl+Shift+B** 直接启动客户端
- `gradle.properties` 里固定了 `org.gradle.java.home`，避免 IDE 用过高版本 JDK 导致 Gradle 配置失败

---

## 目录结构

```
├── src/                    Forge 模组（Java）
│   ├── main/java/...       方块、方块实体、Web 服务、HTTPS、Jade 集成
│   └── main/resources/     模型、贴图、配方、语言文件
├── godot/                  Godot 客户端（GDScript）
│   ├── gd/                 脚本（登录、存储、CPU、合成）
│   ├── tscn/               场景
│   │   ├── font/           字体（仅 fusion-pixel 像素字体）
│   │   └── export/web/     Web 导出产物（供内嵌）
├── export/                 最终产物 jar/exe/apk（不入库）
├── Applied-Energistics-2-forge-1.20.1/   AE2 源码（参考用，不入库）
└── AppliedWebTerminal-main/              参考模组（不入库）
```

---

## 参考项目

- [Applied Energistics 2](https://github.com/AppliedEnergistics/Applied-Energistics-2) —— 存储系统
- [AppliedWebTerminal](https://github.com/Takeneko/AppliedWebTerminal) —— Web 终端参考实现
- [icon-exporter](https://modrinth.com/mod/icon-exporter) —— 物品图标导出

---

## 许可

All Rights Reserved. © ZhangDaing

（AE2 及其依赖遵循各自许可）
