# CampusPilot 金蝶业务对象 API 接入说明

## 1. 接入边界

CampusPilot KWC 前端只调用 Java 后端的 `/api/campuspilot/*` 接口。Java 后端再携带 `accessToken` 调用金蝶 AI 苍穹业务对象 API，因此 Token 不会进入浏览器、KWC 组件属性或前端构建产物。

```text
KWC 页面 -> CampusPilot Java API -> 金蝶 AI 苍穹业务对象 API
                                  -> 金蝶 Agent API
```

未配置 Token 或平台暂时不可达时，Java 后端自动使用 `InMemoryCampusPilotStore` 演示数据，页面接口路径不变。

## 2. 已接入的业务对象

| 业务对象 | 苍穹查询接口 | CampusPilot 页面接口 |
|---|---|---|
| 学生画像 `cp_student_profile` | `cp_student_profile_query` | `GET /api/campuspilot/students` |
| 课程成绩 `cp_course_score` | `cp_course_score_query` | `GET /api/campuspilot/courses` |
| 学习行为 `cp_learning_behavior` | `cp_learning_behavior_query` | `GET /api/campuspilot/behaviors` |
| 风险预警单 `cp_risk_warning` | `cp_risk_warning_query` | `GET /api/campuspilot/warnings` |

平台响应统一从 `data.rows` 读取。Java 适配器将 `code_*` 字段转成现有 KWC 页面使用的字段，并保留多角色数据范围过滤。

例如，学生画像中的 `code_student_name`/`name`、`code_risklevel`、`code_riskscore`，会转换为页面使用的 `name`、`riskLevel`、`riskScore`；风险等级同时归一化为 `riskKey`（`high`、`watch`、`normal`、`improved`），便于图表和标签组件使用。

## 3. 后端配置

PowerShell 示例：

```powershell
$env:CAMPUSPILOT_KINGDEE_BASE_URL="http://127.0.0.1:8881/ierp"
$env:CAMPUSPILOT_KINGDEE_ACCESS_TOKEN="你的 accessToken"
$env:CAMPUSPILOT_KINGDEE_TIMEOUT_MS="8000"
```

若金蝶地址不是本机回环地址，应把 `CAMPUSPILOT_KINGDEE_BASE_URL` 改成真实苍穹环境的 `/ierp` 基础地址。不要把 `client_secret`、accessToken 或 Agent 密钥写入 `campuspilot-kwc/src`。

## 4. 运行状态判断

`GET /api/campuspilot/integration-status` 返回：

- `dataMode=kingdee-api`：已配置金蝶业务对象 API；
- `dataMode=local-fallback`：未配置 Token，使用本地演示数据；
- `connection`：首次连接、成功或回退状态；
- `lastError`：最近一次平台调用错误，不包含 Token。

所有 Java API 响应还带有 `X-CampusPilot-Data-Mode` 响应头。

## 5. Agent 数据闭环

调用 `/api/campuspilot/agent/chat` 时，Java 后端会把学生画像、课程成绩、学习行为和风险预警单放入 `campusContext`，再转发给金蝶 Agent。这样 Agent 可以基于实时业务对象解释“为什么高风险”，而不是只依赖静态知识库。
