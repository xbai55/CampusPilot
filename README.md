# 启航智伴 CampusPilot

AI 原生智慧校园平台 —— 基于金蝶苍穹 KWC 组件库重构，集成金蝶 Agent API 实现"识别-确认-帮扶-反馈-结案"学业预警闭环。

## 架构概览

```text
campuspilot/
├── campuspilot-kwc/          # 前端：React + 金蝶苍穹 KWC 自定义组件
├── campuspilot-official-kwc/ # 苍穹官方 KWC 模板接入版本
├── campuspilot-server/       # 后端：Java 21 HTTP Server（零依赖）
├── campuspilot-home/         # 门户：原生 HTML/CSS/JS（Python 静态服务）
└── docs/                     # 文档、截图、Agent 接入接口表
```

| 模块 | 技术栈 | 说明 |
|---|---|---|
| `campuspilot-kwc` | React 18 + KWC + CRACO + React Router | 管理仪表盘，含驾驶舱/学生/课程/行为/预警/Agent/工作流等页面，并注册 KWC 自定义元素 |
| `campuspilot-official-kwc` | 金蝶官方 KWC webpack 模板 | 面向苍穹自定义控件部署的官方模板版本，可作为真实平台接入参考 |
| `campuspilot-server` | Java 21 + `com.sun.net.httpserver` | 金蝶 KAPI、Agent/任务流网关、REST API、CORS 与认证 |
| `campuspilot-home` | HTML/CSS/JS + Python `http.server` | 门户首页、登录注册、角色路由分发 |

## 本轮 KWC 优化重点

1. `campuspilot-kwc` 从普通 React 单页应用升级为 KWC 组件化承载：`src/index.js` 会挂载 `<cp-campus-pilot>`，便于后续迁入苍穹页面。
2. 新增 `craco.config.js` 与 `kwc-loader.js` / `kwc-html-loader.js` / `kwc-css-loader.js`，让 `.js`、`.html`、`.css` 形式的 KWC 组件能在 React 工程里编译。
3. 新增 `src/kwc/` 通用 KWC 组件，例如侧边栏、顶部栏、指标卡、风险标签、趋势图、散点图和 Toast。简单理解：React 负责页面业务，KWC 负责沉淀可复用控件。
4. 新增 `src/modules/cp/campusPilot/` 标准苍穹组件目录，包含 `campusPilot.html`、`campusPilot.js`、`campusPilot.css` 和 `campusPilot.js-meta.kwc` 元数据。
5. 后端补充低代码蓝图、Agent 工作流、报表中心、云原生部署、多模态能力等演示接口，方便前端展示“平台能力 + 业务闭环”的完整故事。

## 快速开始

### 1. 启动 Java 后端

```powershell
git clone https://github.com/xbai55/CampusPilot.git
cd CampusPilot
git switch feature/kingdee-api-adaptation

# 复制本地配置模板；application.local.ps1 已被 .gitignore 排除
Copy-Item campuspilot-server/config/application.example.ps1 `
  campuspilot-server/config/application.local.ps1

# 编辑 application.local.ps1，填写金蝶和 Agent/任务流配置
cd campuspilot-server
.\scripts\test.ps1
.\scripts\run.ps1
```

`run.ps1` 会自动加载 `config/application.local.ps1`。启动后访问：

- 健康检查：http://127.0.0.1:8787/api/campuspilot/health
- 集成状态：http://127.0.0.1:8787/api/campuspilot/integration-status
- 门户：http://127.0.0.1:8787/index.html

### 2. 启动 KWC 前端（开发模式）

```bash
cd campuspilot-kwc
npm install
npm start
```

访问 http://localhost:3000

生产构建：

```bash
cd campuspilot-kwc
npm run build
```

部署到真实苍穹环境前，需要先配置平台方案、领域、开发商和登录信息，然后执行：

```bash
cd campuspilot-kwc
npm run deploy
```

### 3. 启动门户（可选）

```bash
cd campuspilot-home
python server.py
```

访问 http://localhost:8080

## 环境变量

| 变量 | 说明 | 默认值 |
|---|---|---|
| `CAMPUSPILOT_HOST` | 绑定地址 | `0.0.0.0` |
| `CAMPUSPILOT_PORT` | 监听端口 | `8787` |
| `CAMPUSPILOT_STATIC_ROOT` | 静态文件目录 | `../campuspilot-home` |
| `CAMPUSPILOT_CORS_ALLOWED_ORIGINS` | 允许的前端来源，多个值用英文逗号分隔 | `http://127.0.0.1:8881` |
| `CAMPUSPILOT_API_BEARER_TOKEN` | 可选的后端预设 Bearer Token | 空 |
| `CAMPUSPILOT_PUBLIC_BASE_URL` | 金蝶可访问的后端公网地址，用于 Agent 回调 | `http://127.0.0.1:8787`（仅本机开发） |
| `CAMPUSPILOT_AGENT_NAME` | 自动查找的助手名称 | `CampusPilot 启航智伴学业成长助手` |
| `CAMPUSPILOT_AGENT_ASSISTANT_ID` | CampusPilot 助手 ID | `2522880941110602754` |
| `CAMPUSPILOT_AGENT_CALLBACK_TOKEN` | 可选回调校验 Token；留空则启动时自动生成 | 空 |
| `CAMPUSPILOT_AGENT_RESPONSE_WAIT_MS` | 同步等待 Agent 回调的时间(ms) | `30000` |
| `CAMPUSPILOT_WORKFLOW_API_URL` | 独立的写操作/任务流入口 | 空 |
| `CAMPUSPILOT_WORKFLOW_API_KEY` | 独立的任务流 API Key | 空 |
| `KINGDEE_BASE_URL` | 金蝶苍穹平台或 `/ierp` 基础地址 | `http://127.0.0.1:8881` |
| `KINGDEE_ACCESS_TOKEN` | 可选固定业务对象 accessToken | 空 |
| `KINGDEE_CLIENT_ID` | 增强型 Token 第三方应用编码 | 空 |
| `KINGDEE_CLIENT_SECRET` | 增强型 Token 认证密钥，仅后端配置 | 空 |
| `KINGDEE_USERNAME` | 增强型 Token 代理用户 | 空 |
| `KINGDEE_ACCOUNT_ID` | 金蝶数据中心 ID | 空 |
| `KINGDEE_TIMEOUT` | 金蝶业务对象 API 超时(ms) | `8000` |
| `CAMPUSPILOT_WORKER_THREADS` | 工作线程数 | `12` |

固定 `KINGDEE_ACCESS_TOKEN` 与增强型 Token 凭据二选一。旧的
`CAMPUSPILOT_KINGDEE_*` 变量仍兼容，但新部署优先使用上表变量。

Agent 不需要单独配置地址或 Key。后端通过 `KINGDEE_BASE_URL` 和 OpenAPI 凭据自动执行：

```text
POST 获取 accessToken → POST /v2/gai/assistants 按名称找助手
→ POST /v2/gai/newsession 创建并缓存会话
→ POST /v2/gai/chat → 后端 callback 接收回答
```

例如，用户问“分析张明远的风险”时，浏览器只请求 CampusPilot 后端；`client_secret`、
`accessToken` 和回调 Token 都不会下发到前端。若未配置金蝶凭据，Agent 问答使用明确标记的本地演示兜底。
>
> 不配置金蝶凭据时，读取接口会标记 `local-fallback` 或 `unavailable`；未配置任务流时，写接口返回 HTTP 503，不会修改内存并伪装成平台写回。Token、Secret 和 API Key 只能配置在 Java 后端。

## 演示角色

| 角色 | 姓名 | 职责 |
|---|---|---|
| 学生 | 张明远 | 查看成长画像、提交帮扶反馈 |
| 辅导员 | 王老师 | 风险确认、预警管理、复评结案 |
| 导师 | 陈导师 | 制定帮扶计划、课程补强 |
| 学院管理者 | — | 驾驶舱总览、成效评估 |

## 业务闭环

```text
风险识别 ──→ 生成预警单 ──→ 辅导员确认 ──→ 导师帮扶 ──→ 学生反馈 ──→ 复评结案
  (Agent)    (Agent建议)    (核实画像)    (课程补强)    (学习记录)    (成效评估)
```

## Agent 接入

详见 [`docs/Agent接入接口表.md`](docs/Agent接入接口表.md)

核心接口：

| 接口 | 方法 | 说明 |
|---|---|---|
| `/api/campuspilot/agent/chat` | POST | Agent 问答（转发金蝶苍穹 / 本地兜底） |
| `/api/campuspilot/warnings/suggest` | POST | 通过任务流生成预警草稿；未配置时明确返回 503 |
| `/api/campuspilot/students/{studentNo}/trajectory` | GET | 成长轨迹与学习打卡 |
| `/api/campuspilot/students/{studentNo}/profile-analysis` | GET | 画像、课程和行为聚合分析 |
| `/api/campuspilot/students/{studentNo}/opportunities` | GET | 推荐记录与机会库 |
| `/api/campuspilot/plans/generate` | POST | 调用成长计划任务流 |
| `/api/campuspilot/risk/batch-scan` | POST | 调用批量风险筛查任务流 |
| `/api/campuspilot/tasks?role={role}` | GET | 返回角色待办 |
| `/api/campuspilot/tasks/{id}` | PATCH | 通过任务流更新待办 |
| `/api/campuspilot/lowcode-blueprint` | GET | 低代码页面和业务对象蓝图 |
| `/api/campuspilot/agent-workflow` | GET | Agent 编排流程与节点状态 |
| `/api/campuspilot/report-center` | GET | 学业风险报表中心数据 |
| `/api/campuspilot/cloud-native` | GET | 云原生部署与运行状态 |
| `/api/campuspilot/multimodal` | GET | 多模态学情数据示例 |

## 项目结构

```text
campuspilot-kwc/src/
├── pages/
│   ├── Dashboard/      # 驾驶舱总览
│   ├── Students/       # 学生画像列表
│   ├── Courses/        # 课程成绩
│   ├── Behavior/       # 学习行为
│   ├── Warnings/       # 风险预警管理
│   ├── Agent/          # AI Agent 对话
│   ├── Workflow/       # 预警流程日志
│   ├── User/           # 个人中心
│   └── Settings/       # 系统设置
├── kwc/                # 可复用 KWC 控件，如指标卡、图表、侧边栏、顶部栏
├── modules/cp/         # 苍穹 KWC 页面组件与元数据
├── components/         # 原 React 组件
├── hooks/useApi.js     # API 调用层
├── utils/toast.js      # KWC Toast 事件工具
└── context/AuthContext.js  # 认证上下文

campuspilot-server/src/main/java/com/campuspilot/
├── CampusPilotApplication.java  # 入口
├── config/AppConfig.java        # 配置加载
├── http/ApiHandler.java         # API 路由
├── http/StaticFileHandler.java  # 静态文件
├── service/AgentClient.java     # Agent 代理客户端
├── store/InMemoryCampusPilotStore.java  # 内存数据
└── util/                        # JSON/请求工具
```

## KWC 接入文档

- [`docs/CampusPilot_KWC苍穹接入说明.md`](docs/CampusPilot_KWC苍穹接入说明.md)：说明 `campuspilot-kwc` 的 KWC 组件目录、元数据属性、构建和部署方式。
- [`docs/金蝶业务对象API接入说明.md`](docs/金蝶业务对象API接入说明.md)：说明四类苍穹业务对象 API、后端 Token 配置、字段转换与 Agent 实时上下文链路。
- [`campuspilot-official-kwc/docs/CampusPilot_官方KWC接入说明.md`](campuspilot-official-kwc/docs/CampusPilot_官方KWC接入说明.md)：说明官方模板版本的结构和平台接入方式。
