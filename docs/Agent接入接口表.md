# CampusPilot Agent 接入接口表

> 仅列出与金蝶苍穹 Agent 接入直接相关的接口，不含用户注册、登录等业务无关接口。

---

## 一、CampusPilot 后端暴露的 Agent 相关 API

### 1.1 POST /api/campuspilot/agent/chat — Agent 问答（核心）

| 配置项 | 内容 |
|---|---|
| **接口说明** | 前端 Agent 对话入口，后端转发至金蝶苍穹 Agent API；失败时自动降级到本地兜底 |
| **请求方式** | POST |
| **请求路径** | `/api/campuspilot/agent/chat` |
| **认证** | 需登录（Header 传 X-CampusPilot-User + X-CampusPilot-Role-Key） |
| **Content-Type** | application/json |

**输入参数：**

| 参数名 | 类型 | 必填 | 说明 |
|---|---|---|---|
| question | String | 是 | 用户输入的自然语言问题 |
| role | String | 是 | 当前用户角色（学生/辅导员/导师/学院管理者） |

**返回参数：**

| 参数名 | 类型 | 说明 |
|---|---|---|
| answer | String | Agent 生成的回答文本 |
| chips | String[] | 建议操作标签（如"生成预警单""导师帮扶""复评结案"） |
| source | String | 回答来源标识（Kingdee Agent API / Local Fallback） |

**响应示例：**
```json
{
  "answer": "建议优先处理张明远的高风险学业预警...",
  "chips": ["生成预警单", "导师帮扶", "复评结案"],
  "source": "Kingdee Agent API"
}
```

---

### 1.2 GET /api/campuspilot/agent-insight — Agent 洞察摘要

| 配置项 | 内容 |
|---|---|
| **接口说明** | 获取 Agent 对当前学生风险的智能分析摘要 |
| **请求方式** | GET |
| **请求路径** | `/api/campuspilot/agent-insight` |
| **认证** | 需登录 |

**返回参数：**

| 参数名 | 类型 | 说明 |
|---|---|---|
| title | String | 洞察标题 |
| summary | String | 洞察摘要描述 |
| tags | String[] | 关联标签 |

**响应示例：**
```json
{
  "title": "优先处理张明远高风险学业预警",
  "summary": "建议辅导员先确认 RW2026001，导师同步制定高等数学与数据结构 4 周补强计划...",
  "tags": ["高风险", "导师帮扶", "4 周复评", "可生成预警建议"]
}
```

---

### 1.3 GET /api/campuspilot/integration-status — 金蝶集成状态

| 配置项 | 内容 |
|---|---|
| **接口说明** | 查看金蝶苍穹 Agent API 的集成配置和连接状态 |
| **请求方式** | GET |
| **请求路径** | `/api/campuspilot/integration-status` |
| **认证** | 需登录 |

**返回参数：**

| 参数名 | 类型 | 说明 |
|---|---|---|
| kingdeeBaseUrl | String | 金蝶苍穹平台基础地址 |
| agentMode | String | 当前 Agent 运行模式（OpenAPI 自动发现 / 本地兜底） |
| agentName | String | 自动匹配的助手名称 |
| agentApi | String | 从 `KINGDEE_BASE_URL` 自动派生的 `/v2/gai/*` 接口 |
| objects | Object[] | 已建模的业务对象列表 |
| apis | String[] | 已开放的操作 API 路径列表 |

---

### 1.4 POST /api/campuspilot/warnings/suggest — Agent 智能生成预警单

| 配置项 | 内容 |
|---|---|
| **接口说明** | Agent 根据学生画像、成绩、行为数据自动生成结构化预警单 |
| **请求方式** | POST |
| **请求路径** | `/api/campuspilot/warnings/suggest` |
| **认证** | 需登录 + 角色权限（辅导员/学院管理者） |
| **Content-Type** | application/json |

**输入参数：**

| 参数名 | 类型 | 必填 | 说明 |
|---|---|---|---|
| （无业务参数） | — | — | 由后端 Agent 自动分析并生成 |

**返回参数：**

| 参数名 | 类型 | 说明 |
|---|---|---|
| ok | Boolean | 操作是否成功 |
| created | Boolean | 是否创建了新预警单 |
| warning | Object | 预警单完整对象 |
| warning.code | String | 预警单编号（如 RW2026001） |
| warning.title | String | 预警标题 |
| warning.studentNo | String | 学号 |
| warning.student | String | 学生姓名 |
| warning.level | String | 风险等级（高风险/需要关注/改善中） |
| warning.riskKey | String | 风险标识（high/watch/improved） |
| warning.score | Number | 风险评分（0-100） |
| warning.source | String | 预警来源（金蝶 Agent API 建议） |
| warning.status | String | 处理状态（待确认/帮扶中/已结案） |
| warning.statusKey | String | 状态标识（todo/active/done） |
| warning.owner | String | 当前处理人 |
| warning.deadline | String | 处理截止日期 |
| warning.counselorNote | String | 辅导员意见 |
| warning.mentorPlan | String | 导师帮扶计划 |
| warning.studentFeedback | String | 学生反馈 |

---

## 二、后端 → 金蝶苍穹 Agent OpenAPI 自动调用

### 2.1 Agent 远程调用参数（CampusPilot → 金蝶苍穹）

| 配置项 | 内容 |
|---|---|
| **接口说明** | CampusPilot 后端自动鉴权、发现助手、创建会话并通过回调获取 AI 分析结果 |
| **请求方式** | POST |
| **请求路径** | `/v2/gai/assistants`、`/v2/gai/newsession`、`/v2/gai/chat`（由后端自动拼接） |
| **超时时间** | `KINGDEE_TIMEOUT`；回调等待使用 `CAMPUSPILOT_AGENT_RESPONSE_WAIT_MS` |
| **认证方式** | 后端通过 OpenAPI 凭据自动获取 accessToken，并放入 `accessToken` Header |
| **Content-Type** | application/json |

**输入参数（请求体）：**

| 参数名 | 类型 | 说明 |
|---|---|---|
| sessionId | String | 后端自动创建并缓存的 Agent 会话 ID |
| chatTraceId | String | 后端为每次问题自动生成的消息 ID |
| message.query | String | 用户自然语言问题 |
| message.inputParams | Object | 当前角色和校园上下文数据 |

**campusContext 结构：**

| 参数名 | 类型 | 说明 |
|---|---|---|
| overview | Object | 校园总览数据 |
| overview.totalStudents | Number | 学生总数 |
| overview.highRisk | Number | 高风险学生数 |
| overview.watchRisk | Number | 需关注学生数 |
| overview.normal | Number | 正常学生数 |
| overview.improved | Number | 改善中学生数 |
| overview.pendingWarnings | Number | 待处理预警数 |
| overview.activeWarnings | Number | 进行中预警数 |
| overview.closedWarnings | Number | 已结案预警数 |
| overview.averageGpa | Number | 平均 GPA |
| overview.averageAttendance | Number | 平均出勤率 |
| riskDistribution | Object[] | 风险分布数据（4个分组） |
| warningCount | Number | 当前预警单总数 |

**返回参数：**

| 参数名 | 类型 | 说明 |
|---|---|---|
| answer | String | Agent 生成的回答文本 |
| chips | String[] | 建议操作标签 |
| source | String | 来源标识 |

---

## 三、响应头中的 Agent 模式标识

所有 API 响应均包含以下 Header：

| Header 名 | 值 | 说明 |
|---|---|---|
| X-CampusPilot-Agent-Mode | `openapi-auto` | 金蝶 Agent OpenAPI 凭据已配置，自动接入模式 |
| X-CampusPilot-Agent-Mode | `local-fallback` | 未配置金蝶 API，本地规则兜底 |

---

## 四、环境变量配置（Agent 接入相关）

| 变量名 | 说明 | 默认值 |
|---|---|---|
| `KINGDEE_BASE_URL` | 金蝶苍穹平台基础地址，Agent 路径自动派生 | `http://127.0.0.1:8881` |
| `KINGDEE_CLIENT_ID` / `KINGDEE_CLIENT_SECRET` | OpenAPI 第三方应用凭据 | 空 |
| `KINGDEE_USERNAME` / `KINGDEE_ACCOUNT_ID` | OpenAPI 代理用户和数据中心 | 空 |
| `CAMPUSPILOT_PUBLIC_BASE_URL` | 金蝶可访问的后端公网回调地址 | 本机地址（仅开发） |
| `CAMPUSPILOT_AGENT_NAME` | 自动查找的助手名称 | CampusPilot 启航智伴学业成长助手 |
| `CAMPUSPILOT_AGENT_ASSISTANT_ID` | 可选助手 ID，填写后跳过清单查询 | 空 |
| `CAMPUSPILOT_AGENT_CALLBACK_TOKEN` | 可选回调校验 Token，留空自动生成 | 空 |
| `CAMPUSPILOT_AGENT_RESPONSE_WAIT_MS` | 同步等待回答回调时间（毫秒） | 30000 |
