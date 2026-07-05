# MyProxy

一个基于 Java Swing 的桌面应用，同时支持 HTTP 正向代理与反向代理。

## 功能特性

- **正向代理** — 基于 [LittleProxy](https://github.com/LittleProxy/LittleProxy)（Netty 实现），支持 IP 白名单过滤
- **反向代理** — 基于 Netty 原生实现，按 Host 头匹配域名规则，转发到指定后端地址
- **桌面 GUI** — Swing 界面，包含状态显示、控制面板、白名单管理、反向代理规则管理与共享日志面板
- **系统托盘** — 支持最小化到托盘，双击恢复窗口
- **国际化** — 支持英文和中文界面语言
- **状态栏** — 显示本机 IP 地址、代理运行状态、版本和作者信息

## 技术栈

| 项 | 说明 |
| --- | --- |
| 语言 | Java 21 |
| 构建 | Maven |
| 代理核心 | `xyz.rogfam:littleproxy:2.0.22`（基于 Netty） |
| JSON | `com.fasterxml.jackson:jackson-databind:2.13.5` |
| 日志 | `ch.qos.logback:logback-classic:1.3.14` |
| 打包 | `maven-shade-plugin` 生成可执行 fat jar |

## 项目结构

```
src/main/java/com/myproxy/
├── MyProxyApplication.java        # 程序入口
├── config/
│   ├── ConfigManager.java         # 配置加载/保存（JSON）
│   ├── NetUtils.java              # 网络工具（本机 IP 检测）
│   └── ProxyConfig.java           # 配置模型
├── proxy/
│   ├── ProxyService.java          # 正向代理服务（LittleProxy）
│   └── ReverseProxyService.java   # 反向代理服务（Netty）
└── ui/
    ├── I18nManager.java           # 国际化管理器
    ├── LogPanel.java              # 日志输出面板
    ├── MainFrame.java             # 主窗口
    ├── ProxyPanel.java            # 正向代理控制面板
    ├── ReverseProxyPanel.java     # 反向代理规则管理
    ├── StatusBar.java             # 状态栏（IP、代理状态、版本）
    ├── SystemTrayManager.java     # 系统托盘
    ├── TableHeightUtil.java       # JTable 高度工具
    ├── UiUtils.java               # 共享 UI 常量和辅助方法
    ├── WhitelistPanel.java        # IP 白名单管理
    └── WrapLayout.java            # 自动换行 FlowLayout 变体
```

## 构建与运行

```bash
# 编译并打包为可执行 jar（target/myproxy-1.0.0.jar）
mvn clean package

# 运行
java -jar target/myproxy-1.0.0.jar

# 开发模式运行
mvn exec:java -Dexec.mainClass="com.myproxy.MyProxyApplication"
```

## 配置

配置文件存储在 `~/.myproxy/config.json`，首次启动时自动创建默认配置。

### 默认设置

| 设置 | 默认值 |
| --- | --- |
| 正向代理端口 | 6666 |
| 反向代理端口 | 6688 |
| 白名单启用 | false |
| 默认允许 IP | `127.0.0.1`、`0:0:0:0:0:0:0:1` |
| 默认反向代理规则 | 本机 IP → `https://www.google.com` |

### 配置示例

```json
{
  "port" : 6666,
  "whitelistEnabled" : false,
  "allowedIps" : [ "127.0.0.1", "0:0:0:0:0:0:0:1" ],
  "reverseProxyPort" : 6688,
  "reverseProxyEnabled" : false,
  "reverseProxyRules" : [ {
    "domain" : "192.168.1.100",
    "target" : "https://www.google.com"
  } ],
  "language" : "zh"
}
```

## 架构说明

### 正向代理（`ProxyService`）
- 基于 `DefaultHttpProxyServer`（LittleProxy），监听 `0.0.0.0:port`
- 通过 `HttpFiltersSourceAdapter` 注入过滤器：提取客户端 IP，按白名单校验，未通过返回 403
- 日志通过 `Consumer<String>` 回调到 UI；状态通过 `Consumer<Boolean>` 回调
- 所有 Swing 回调使用 `SwingUtilities.invokeLater` 保证线程安全

### 反向代理（`ReverseProxyService`）
- 基于 Netty 原生 `ServerBootstrap`，监听 `0.0.0.0:reverseProxyPort`
- `ReverseProxyHandler`：解析请求 Host 头，匹配规则后连接后端，转发请求与响应
- `BackendHandler`：处理后端响应，回写客户端
- 无匹配规则时返回 502 Bad Gateway

### UI（`ui` 包）
- `MainFrame`：左侧 `JSplitPane` 放置正向/反向代理面板，右侧白名单面板，底部共享 `LogPanel`
- 所有耗时操作（启停代理）在独立线程执行，避免阻塞 EDT
- `SystemTrayManager`：动态生成托盘图标，右键菜单提供「显示窗口」「退出」

## 许可证

[Apache License 2.0](LICENSE)
