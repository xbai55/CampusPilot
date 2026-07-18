# CampusPilot Java Backend

CampusPilot 后端采用 Java 21 自带的 HttpServer，不是 FastAPI。本次保持现有架构，增加统一金蝶 KAPI Client、增强型 Token 管理、业务字段映射和学生聚合接口。

## C1-C5 后端覆盖

| 文档要求 | 后端实现 |
|---|---|
| C1 云电脑运行与连通性 | Clone 后本地配置脚本、健康检查、四核心对象主动探测、Agent/任务流独立状态 |
| C2 真实业务对象读取 | 13 类 KAPI、`accessToken`、`data.rows` 解析、PDF 字段映射、Token 缓存与重试 |
| C3 写操作与流程 | 预警/计划/批量筛查/任务更新统一走服务端工作流网关，不再修改内存伪装写回 |
| C4 扩展接口 | trajectory、profile-analysis、opportunities、plans/generate、risk/batch-scan、tasks 全部有路由 |
| C5 CORS 与认证 | 指定 Origin、Authorization 请求头、PATCH、演示身份头及可选预设 Bearer Token |

## 后端架构

~~~text
React/KWC 或 campuspilot-home
            |
            v
Java HttpServer / ApiHandler
            |
            +--> CampusPilot 本地业务与权限控制
            |
            +--> KingdeeClient（Token 缓存、超时、重试、异常与日志）
                         |
                         v
               金蝶 AI 苍穹 KAPI
~~~

主要目录：

~~~text
src/main/java/com/campuspilot/
├── config/AppConfig.java
├── http/ApiHandler.java
├── kingdee/
│   ├── KingdeeClient.java
│   └── KingdeeFieldMappings.java
├── service/
│   ├── GrowthPlanService.java
│   ├── StudentProfileService.java
│   ├── StudentTrajectoryService.java
│   ├── LearningService.java
│   ├── RiskService.java
│   └── OpportunityService.java
└── store/InMemoryCampusPilotStore.java
~~~

## 金蝶接口支持列表

| 模块 | 金蝶对象/接口 | 作用 | 状态 |
|---|---|---|---|
| 学生画像 | cp_student_profile_query | 学业、能力和风险画像 | 已接入 |
| 成长计划 | growthplan | 短中期成长规划与导师建议 | 已接入 |
| 学习行为 | cp_learning_behavior_query | 出勤、作业、活跃度和互动 | 已接入 |
| 成长轨迹 | student_trajectory_query | GPA、学分、规律性与风险变化 | 已接入 |
| 课程成绩 | cp_course_score_query | 课程成绩与挂科识别 | 已接入现有列表 API |
| 多维行为 | stumultibehaviorrec | 图书馆、自习室、实验室等行为 | 已接入 |
| 课堂学情 | classlearningrec | 互动、测验、薄弱点和反馈 | 已接入 |
| 风险预警 | cp_risk_warning_query | 风险识别、建议与处置记录 | 已接入 |
| 学生机会推荐 | studentopprec | 个性化机会匹配 | 已接入 |
| 成长机会库 | growthopportunity | 可推荐机会基础库 | 已接入 |
| 通知提醒 | notificationrecord | 消息提醒记录 | 已接入 |
| 学习打卡 | studycheckinrec | 帮扶后的学习打卡 | 已接入 |
| 课程能力映射 | courseabilitymap | 课程与能力维度映射 | 已接入 |

## REST API

原有 /api/campuspilot/* 接口保持不变，新增：

| 方法 | 路径 | 返回内容 |
|---|---|---|
| GET | /api/student/profile/{student_id} | 学生画像、学业概览和风险概览 |
| GET | /api/student/growth-plan/{student_id} | 成长计划 |
| GET | /api/student/learning/{student_id} | 学习行为、成长轨迹、多维行为和课堂学情 |
| GET | /api/student/risk-warning/{student_id} | 风险预警 |
| GET | /api/student/opportunities/{student_id} | 个性化推荐与机会库 |
| GET | /api/student/notifications/{student_id} | 通知提醒记录 |
| GET | /api/student/study-checkins/{student_id} | 学习打卡记录 |
| GET | /api/student/course-ability-mappings | 全部课程能力映射 |
| GET | /api/campuspilot/students/{studentNo}/trajectory | 成长轨迹/学习打卡趋势 |
| GET | /api/campuspilot/students/{studentNo}/profile-analysis | 画像、课程和行为聚合分析 |
| GET | /api/campuspilot/students/{studentNo}/opportunities | 推荐记录和成长机会库 |
| POST | /api/campuspilot/plans/generate | 调用成长计划任务流 |
| POST | /api/campuspilot/risk/batch-scan | 调用批量风险筛查任务流 |
| GET | /api/campuspilot/tasks?role={role} | 从预警和通知生成角色待办 |
| PATCH | /api/campuspilot/tasks/{id} | 通过任务流更新待办 |

业务接口需携带 `X-CampusPilot-User` 和 `X-CampusPilot-Role-Key`。若配置了
`CAMPUSPILOT_API_BEARER_TOKEN`，还必须携带 `Authorization: Bearer <token>`。

简单示例：

~~~powershell
curl.exe -H "X-CampusPilot-User: 张明远" -H "X-CampusPilot-Role-Key: student" http://127.0.0.1:8787/api/student/profile/S001
~~~

## Token 与配置

金蝶官方增强型 Token 接口为 POST /kapi/oauth2/getToken。Token 默认有效期约 2 小时。苍穹 V7.0.8 已下架 refreshToken，因此本实现会：

1. 在后端缓存 access_token 和 expires_in。
2. 到期前 60 秒重新获取 Token。
3. 业务请求收到 401/403 或 Token 失效错误时，仅重新获取一次并重试。
4. 日志不输出 client_secret 和完整 Token。

配置示例：

~~~text
KINGDEE_BASE_URL=http://127.0.0.1:8881
KINGDEE_CLIENT_ID=第三方应用系统编码
KINGDEE_CLIENT_SECRET=第三方应用AccessToken认证密钥
KINGDEE_USERNAME=代理用户用户名
KINGDEE_ACCOUNT_ID=数据中心ID
KINGDEE_LANGUAGE=zh_CN
KINGDEE_TIMEOUT=8000
~~~

可选的 KINGDEE_ACCESS_TOKEN 用于兼容已有手工 Token 部署。旧 CAMPUSPILOT_KINGDEE_* 变量仍可读取，但新部署建议使用上面的无前缀变量。所有密钥只能放在后端环境变量或密钥管理系统中，不能提交到 Git，也不能写入 KWC 前端。

## Clone 后配置

~~~powershell
git clone https://github.com/xbai55/CampusPilot.git
cd CampusPilot
git switch feature/kingdee-api-adaptation
Copy-Item campuspilot-server/config/application.example.ps1 `
  campuspilot-server/config/application.local.ps1
# 编辑 application.local.ps1，填写真实地址和凭据
cd campuspilot-server
.\scripts\test.ps1
.\scripts\run.ps1
~~~

`run.ps1` 会自动加载 `config/application.local.ps1`。该文件已被 `.gitignore` 排除，不能提交。
也可以完全通过云主机环境变量配置。兼容文档中的旧变量
`CAMPUSPILOT_KINGDEE_BASE_URL`、`CAMPUSPILOT_KINGDEE_ACCESS_TOKEN`。

启动后验证：

~~~powershell
$headers = @{
  'X-CampusPilot-User' = '系统管理员'
  'X-CampusPilot-Role-Key' = 'manager'
}
curl.exe http://127.0.0.1:8787/api/campuspilot/health
Invoke-RestMethod -Headers $headers `
  http://127.0.0.1:8787/api/campuspilot/integration-status
~~~

`integration-status` 会读取学生画像、课程成绩、学习行为、风险预警四个核心对象进行探测。
对象状态只会在实际 KAPI 请求成功后显示 `connected`；仅填写 Token 时显示
`configured-unverified`，失败显示 `failed`，不会把“已配置”误报成“已连接”。

## Agent OpenAPI 接入

后端按照金蝶官方 [Agent 平台 OpenAPI 文档](https://dev.kingdee.com/open/detail/sdk/2377816720032150528) 接入，无需填写 H5 地址或单独的 Agent Key。后端复用 `KINGDEE_BASE_URL` 与 OpenAPI 凭据，并默认使用已确认的助手 ID `2522880941110602754`：

1. `POST /kapi/oauth2/getToken` 获取并缓存 `accessToken`；
2. `POST /v2/gai/newsession` 为每个登录用户创建并缓存独立会话；
3. `POST /v2/gai/chat` 发送问题；
4. 通过 `POST /api/campuspilot/agent/callback` 接收回答并校验回调 Token。

生产环境必须把 `CAMPUSPILOT_PUBLIC_BASE_URL` 配成金蝶平台可以访问的 HTTPS 后端地址。
若 30 秒内没有收到回答，聊天接口会返回 `pending=true` 和 `chatTraceId`，可调用
`GET /api/campuspilot/agent/results/{chatTraceId}` 查询迟到的回调结果。

## 写操作/任务流网关契约

`CAMPUSPILOT_WORKFLOW_API_URL` 和 `CAMPUSPILOT_WORKFLOW_API_KEY` 只用于写操作和任务流，
不与 Agent OpenAPI 共用。后端向工作流入口发送：

~~~json
{
  "operation": "plans.generate",
  "requestId": "uuid",
  "actor": {"name": "张老师", "role": "辅导员"},
  "input": {}
}
~~~

平台入口返回任意 JSON 和 2xx 即视为成功。支持的操作名：

- `plans.generate`
- `risk.batch-scan`
- `warning.create-draft`
- `warning.confirm`
- `warning.mentor-plan`
- `warning.feedback`
- `warning.close`
- `tasks.update`

未配置工作流时，这些写接口返回 HTTP 503，并明确包含
`demo: true`、`dataSource: local-demo`；后端不会再修改内存数据并伪装成平台写回。

## CORS 与认证

默认允许来源为 `http://127.0.0.1:8881`，允许请求头为
`Content-Type, Authorization, X-CampusPilot-User, X-CampusPilot-Role-Key`，允许方法为
`GET, POST, PATCH, OPTIONS`。多个开发来源可通过
`CAMPUSPILOT_CORS_ALLOWED_ORIGINS` 使用英文逗号配置。

## 字段映射

KingdeeFieldMappings 集中维护金蝶字段到 CampusPilot 业务字段的转换，Service 层只向 REST API 暴露业务命名。

| 金蝶字段 | CampusPilot 字段 |
|---|---|
| code_studentnumber / code_studentno | studentId |
| code_studentname / code_student_name | studentName |
| code_gpa | gpa |
| code_creditrate | creditRate |
| code_riskscore / code_risk_score | riskScore |
| code_aisuggestion / code_ai_suggestion | aiSuggestion |

简单理解：这一层像插座转换头。金蝶仍使用 code_ 前缀字段，前端和 Agent 使用稳定、易读的业务字段。

## 启动与测试

~~~powershell
cd campuspilot-server
.\scripts\build.ps1
.\scripts\run.ps1
~~~

默认访问地址：http://127.0.0.1:8787

运行测试：

~~~powershell
.\scripts\test.ps1
~~~

测试使用本地模拟 KAPI，覆盖 Token 获取、Token 失效重试、成功响应、空数据、全部 13 个 KAPI 的关键字段映射和网络异常，不需要真实金蝶账号。

## 失败回退

原有学生列表、课程、行为和预警页面保留本地演示数据回退，但响应头和状态接口会标出
`local-fallback`。新增聚合/C4 接口在未配置 KAPI 时返回空结构及明确的
`demo`、`dataSource` 标记，不会暴露密钥或伪造远程数据。平台写操作仅通过工作流网关执行。
