# CampusPilot（金蝶苍穹 KWC 接入版）

CampusPilot 是面向高校学业风险预警场景的智慧校园演示系统。本仓库只保留实际交付所需的两部分：

- `campuspilot-official-kwc/`：基于金蝶官方原生 KWC 模板的自定义控件；
- `campuspilot-server/`：Java 后端，负责业务 API、金蝶业务对象 API 适配和 Agent 代理。

业务闭环为：**风险识别 → 生成预警单 → 辅导员确认 → 导师帮扶 → 学生反馈 → 复评结案**。

## 架构与安全边界

```text
金蝶 KWC 控件
    │  调用 /api/campuspilot/*
    ▼
CampusPilot Java 后端
    ├── 金蝶业务对象 OpenAPI（学生、成绩、学习行为、风险预警）
    └── 金蝶 Agent API（携带 campusContext）
```

KWC 前端不保存 `accessToken`、`client_secret` 或 Agent 密钥。所有带凭证的调用均由 Java 后端通过环境变量配置并完成转发。

## 已完成内容

- 已使用官方 KWC 模板注册控件：`KDApi.register('campuspilot', ...)`。
- 控件元数据已配置为 `framework=kwc`、`target=KWCFormModel`、`isv=code`、`moduleid=code`。
- 已迁入 CampusPilot 驾驶舱、学生画像、课程成绩、学习行为、风险预警和 Agent 分析页面。
- Java 后端已接入四类金蝶业务对象查询 API，并将平台 `data.rows` 适配成页面所需字段。
- 未配置 Token 或平台不可访问时，后端会回退到本地演示数据，便于离线答辩。
- Agent 问答已保留远程代理接口；真实 Agent 配置后会携带学生、成绩、行为和预警数据作为上下文。

完整状态及后续上线步骤见 [docs/后续接入与已完成情况.md](docs/后续接入与已完成情况.md)。

## 本地启动

### 1. 启动 Java 后端

需要 JDK 21 或更高版本。

```powershell
cd campuspilot-server
.\scripts\build.ps1
.\scripts\run.ps1
```

默认地址为 `http://127.0.0.1:8787`。

### 2. 启动 KWC 本地预览

需要 Node.js（建议使用与 `package-lock.json` 匹配的 LTS 版本）。首次运行必须先安装依赖，否则会出现“webpack 不是内部或外部命令”。

```powershell
cd campuspilot-official-kwc
npm ci
npm run lint
npm start
```

生产构建：

```powershell
npm run build
```

构建后的部署资源位于 `dist/`；命令同时生成 CLI 使用的部署暂存目录 `server/isv/code/code/campuspilot/`。

## 金蝶页面属性示例

在苍穹页面设计器中配置 CampusPilot 控件时，推荐使用：

```text
pageTitle   = 启航智伴 CampusPilot
apiBase     = http://127.0.0.1:8787
defaultRole = counselor
enableAgent = false
theme       = light
```

其中 `apiBase` 是 **Java 后端根地址**，控件会自动追加 `/api/campuspilot`。例如，`http://127.0.0.1:8787` 最终会请求 `http://127.0.0.1:8787/api/campuspilot/overview`。

## 后端环境变量

```powershell
$env:CAMPUSPILOT_KINGDEE_BASE_URL = 'http://127.0.0.1:8881/ierp'
$env:CAMPUSPILOT_KINGDEE_ACCESS_TOKEN = '你的 accessToken'
$env:CAMPUSPILOT_KINGDEE_TIMEOUT_MS = '8000'

$env:CAMPUSPILOT_AGENT_API_URL = '你的 Agent API 地址'
$env:CAMPUSPILOT_AGENT_API_KEY = '你的 Agent API 密钥'
$env:CAMPUSPILOT_AGENT_TIMEOUT_MS = '8000'
```

不要将以上 Token 或密钥提交到 Git 仓库，也不要写入 KWC 前端代码。

## 文档导航

- [官方 KWC 控件接入说明](campuspilot-official-kwc/docs/CampusPilot_官方KWC接入说明.md)
- [金蝶业务对象 API 接入说明](docs/金蝶业务对象API接入说明.md)
- [Agent 接入接口表](docs/Agent接入接口表.md)
- [后续接入与已完成情况](docs/后续接入与已完成情况.md)

