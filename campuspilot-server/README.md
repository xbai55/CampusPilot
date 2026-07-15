# CampusPilot Java Backend

CampusPilot 后端采用 Java 21 自带的 HttpServer，不是 FastAPI。本次保持现有架构，增加统一金蝶 KAPI Client、增强型 Token 管理、业务字段映射和学生聚合接口。

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
| 通知提醒 | notificationrecord | 消息提醒记录 | 文档已核对，暂未开放业务路由 |
| 学习打卡 | studycheckinrec | 帮扶后的学习打卡 | 文档已核对，暂未开放业务路由 |
| 课程能力映射 | courseabilitymap | 课程与能力维度映射 | 文档已核对，暂未开放业务路由 |

## REST API

原有 /api/campuspilot/* 接口保持不变，新增：

| 方法 | 路径 | 返回内容 |
|---|---|---|
| GET | /api/student/profile/{student_id} | 学生画像、学业概览和风险概览 |
| GET | /api/student/growth-plan/{student_id} | 成长计划 |
| GET | /api/student/learning/{student_id} | 学习行为、成长轨迹、多维行为和课堂学情 |
| GET | /api/student/risk-warning/{student_id} | 风险预警 |
| GET | /api/student/opportunities/{student_id} | 个性化推荐与机会库 |

这些接口沿用现有登录鉴权。请求时需携带 X-CampusPilot-User 和 X-CampusPilot-Role-Key。

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

测试使用本地模拟 KAPI，覆盖 Token 获取、Token 失效重试、成功响应、空数据、学生画像/风险/成长计划字段映射和网络异常，不需要真实金蝶账号。

## 失败回退

原有学生列表、课程、行为和预警页面仍保留本地演示数据回退，避免金蝶环境暂时不可用时破坏现有演示。新增 /api/student/* 聚合接口在未配置 KAPI 时返回空业务结构，不会暴露密钥或伪造远程数据。