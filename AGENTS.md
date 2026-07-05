# AGENTS.md

This file provides guidelines for AI agents (e.g., Claude, Codex) working in this repository.

## Project Overview

**MyProxy** is a Java Swing desktop tool that supports both HTTP forward proxy and reverse proxy.

- **Forward Proxy**: Based on [LittleProxy](https://github.com/LittleProxy/LittleProxy) (Netty implementation), with IP whitelist filtering.
- **Reverse Proxy**: Built on Netty natively, matches domain rules by Host header and forwards to configured backend addresses.
- **Desktop UI**: Swing interface with status display, control panels, whitelist management, reverse proxy rule management, and a shared log panel.
- **System Tray**: Supports minimizing to tray; double-click to restore window.
- **i18n**: Supports English and Chinese via `messages.properties` / `messages_en.properties` / `messages_zh.properties`.

## Tech Stack

| Item | Details |
| --- | --- |
| Language | Java 21 (`maven.compiler.source/target=21`) |
| Build | Maven |
| Proxy Core | `xyz.rogfam:littleproxy:2.0.22` (Netty-based) |
| JSON | `com.fasterxml.jackson:jackson-databind:2.13.5` |
| Logging | `ch.qos.logback:logback-classic:1.3.14` |
| Packaging | `maven-shade-plugin` producing executable fat jar |

## Directory Structure

```
zproxy/
├── pom.xml
├── README.md
├── README_zh.md
├── LICENSE
├── AGENTS.md
└── src/main/
    ├── java/com/myproxy/
    │   ├── MyProxyApplication.java        # Entry point (main method)
    │   ├── config/
    │   │   ├── ConfigManager.java         # Config load/save (JSON)
    │   │   ├── NetUtils.java              # Network utility (local IP)
    │   │   └── ProxyConfig.java           # Config model
    │   ├── proxy/
    │   │   ├── ProxyService.java          # Forward proxy (LittleProxy)
    │   │   └── ReverseProxyService.java   # Reverse proxy (Netty)
    │   └── ui/
    │       ├── I18nManager.java           # Internationalization manager
    │       ├── LogPanel.java              # Log output panel
    │       ├── MainFrame.java             # Main window
    │       ├── ProxyPanel.java            # Forward proxy control panel
    │       ├── ReverseProxyPanel.java     # Reverse proxy rule management
    │       ├── StatusBar.java             # Status bar (IP, proxy status, version)
    │       ├── SystemTrayManager.java     # System tray support
    │       ├── TableHeightUtil.java       # JTable height utility
    │       ├── UiUtils.java               # Shared UI constants and helpers
    │       ├── WhitelistPanel.java        # IP whitelist management
    │       └── WrapLayout.java            # Auto-wrapping FlowLayout variant
    └── resources/
        ├── logback.xml
        └── messages/
            ├── messages.properties        # Default (English) i18n keys
            ├── messages_en.properties     # English
            └── messages_zh.properties     # Chinese
```

## Build & Run

```bash
# Compile and package (target/myproxy-1.0.0.jar)
mvn clean package

# Run
java -jar target/myproxy-1.0.0.jar

# Development
mvn exec:java -Dexec.mainClass="com.myproxy.MyProxyApplication"
```

- Main class: `com.myproxy.MyProxyApplication`
- On startup, loads `~/.myproxy/config.json` (creates with defaults if missing).
- Forward proxy starts automatically; reverse proxy starts if enabled with existing rules.

## Architecture

### Configuration (`config` package)
- `ProxyConfig`: Config model with fields: `port` (forward proxy port, default 6666), `whitelistEnabled`, `allowedIps`, `reverseProxyPort` (default 6688), `reverseProxyEnabled`, `reverseProxyRules`, `language`.
- `ReverseProxyRule`: Inner static class with `domain` and `target` (backend address, must include `http://` or `https://` scheme).
- `ConfigManager`: Uses Jackson to read/write `~/.myproxy/config.json`.
- `NetUtils`: Shared utility for detecting local non-loopback IPv4 address.

### Forward Proxy (`ProxyService`)
- Based on `DefaultHttpProxyServer` (LittleProxy), listens on `0.0.0.0:port`.
- Injects `HttpFiltersSourceAdapter`: extracts client IP, validates against whitelist, returns 403 if denied.
- Logs via `Consumer<String>` callback; status via `Consumer<Boolean>` callback.
- All Swing callbacks use `SwingUtilities.invokeLater` for thread safety.
- On start failure, notifies status consumer with `false`.

### Reverse Proxy (`ReverseProxyService`)
- Based on Netty `ServerBootstrap`, listens on `0.0.0.0:reverseProxyPort`.
- `ReverseProxyHandler`: parses Host header, matches rules, connects to backend, forwards request/response.
- `BackendHandler`: handles backend responses, writes back to client.
- Returns 502 Bad Gateway when no matching rule is found.

### UI (`ui` package)
- `MainFrame`: `JSplitPane` with forward/reverse proxy panels on the left, whitelist panel on the right, shared `LogPanel` at the bottom via another `JSplitPane`.
- `MainFrame` centrally manages all `statusConsumer` callbacks, updating both panel states and the status bar.
- All long-running operations (start/stop proxy) run in separate threads to avoid blocking EDT.
- `SystemTrayManager`: dynamically generated tray icon with right-click menu (Show Window / Exit).
- `UiUtils`: shared constants (`COLOR_RUNNING`, `COLOR_STOPPED`, `COLOR_LABEL`, `COLOR_ICON`, `INPUT_FIELD_WIDTH`), helper methods (`applyFont`, `parsePort`, `styleTable`, `updateStatusLabel`, `toHtmlColor`, `createCircleIcon`).
- `WrapLayout`: `FlowLayout` subclass that wraps components to the next line when the container width is exceeded.

## Coding Conventions

- Package name: `com.myproxy.*`.
- Logging: SLF4J (`org.slf4j.Logger`), configured in `logback.xml`.
  - `com.myproxy` package log level: `DEBUG`.
  - `org.littleshoot.proxy` and `io.netty` log level: `WARN`.
- UI text uses i18n keys via `I18nManager.getString()`. Never hardcode user-visible strings in Java source.
- Swing event callbacks must not execute blocking operations; use separate threads.
- UI updates must go through `SwingUtilities.invokeLater`.
- After config changes, call `ConfigManager.saveConfig()` to persist.

## Runtime Artifacts (gitignored)

- `~/.myproxy/config.json`: runtime configuration.
- `logs/myproxy*.log`: log files, rolling daily, retained 7 days.
- `target/`: Maven build output.

## Important Notes

- Reverse proxy rule `target` must include scheme (`http://` or `https://`). The UI auto-prepends `http://` if missing.
- Whitelist supports IPv4 and simple IPv6 formats. Defaults allow `127.0.0.1` and `0:0:0:0:0:0:0:1`.
- On window close: if system tray is supported, hides to tray; otherwise calls `exitApp()` to stop all services and exit.
- The app icon is a red circle ring, generated programmatically via `UiUtils.createCircleIcon()`.
- Default reverse proxy rule maps the local IP to `https://www.google.com`.
