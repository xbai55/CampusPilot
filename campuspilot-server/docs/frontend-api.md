# CampusPilot 前端接口文档

## 后端地址

同一网络内优先使用：

```text
http://10.0.160.11:8787
```

备用地址：

```text
http://198.19.163.199:8787
```

## 公共请求头

除健康检查、登录、注册接口外，业务接口都需要带登录上下文：

```http
X-CampusPilot-User: test
X-CampusPilot-Role-Key: counselor
Content-Type: application/json
```

当前后端未开启 Bearer Token 校验，所以暂时不需要 `Authorization`。

如果接口返回 401，优先检查是否漏传 `X-CampusPilot-User` 和 `X-CampusPilot-Role-Key`。

## 当前联调状态

测试时间：2026-07-18

| 状态 | 接口范围 | 说明 |
|---|---|---|
| 可用 | `/api/campuspilot/*` 看板类只读接口 | 已返回数据 |
| 可用 | `/api/campuspilot/auth/*` 登录注册接口 | 返回演示用户数据 |
| 可用 | `/api/campuspilot/agent/chat` | 返回本地兜底 AI 答复 |
| 暂不可用 | 工作流写操作接口 | 返回 503，原因是未配置 `CAMPUSPILOT_WORKFLOW_API_URL` |
| 暂不可用 | `/api/student/*` 直连金蝶 KAPI 接口 | 返回 502，原因是当前金蝶 KAPI 返回 `HTTP 405` |

## 健康检查

### GET `/api/campuspilot/health`

不需要登录请求头。

示例响应：

```json
{
  "ok": true,
  "service": "CampusPilot Java API",
  "time": "2026-07-18 19:16",
  "configuration": {
    "kingdeeConfigured": true,
    "agentConfigured": true,
    "workflowConfigured": false,
    "bearerProtectionEnabled": false,
    "corsAllowedOrigins": "http://127.0.0.1:8881"
  }
}
```

## 登录注册

### POST `/api/campuspilot/auth/login`

请求体：

```json
{
  "username": "test",
  "roleKey": "student"
}
```

示例响应：

```json
{
  "ok": true,
  "user": {
    "name": "王老师",
    "role": "辅导员",
    "account": "demo@campus.edu"
  }
}
```

### POST `/api/campuspilot/auth/register`

请求体：

```json
{
  "username": "newuser",
  "roleKey": "student"
}
```

示例响应：

```json
{
  "ok": true,
  "user": {
    "name": "校园用户",
    "role": "学生",
    "account": "demo@campus.edu"
  }
}
```

## 看板只读接口

以下接口已测试通过，可以直接给前端页面使用。

### GET `/api/campuspilot/overview`

返回驾驶舱汇总指标。

示例响应：

```json
{
  "totalStudents": 5,
  "highRisk": 1,
  "watchRisk": 1,
  "normal": 2,
  "improved": 1,
  "pendingWarnings": 1,
  "activeWarnings": 1,
  "closedWarnings": 1,
  "averageGpa": 3.03,
  "averageAttendance": 86.0
}
```

### GET `/api/campuspilot/risk-distribution`

返回风险分布图表数据。

示例响应：

```json
[
  { "label": "高风险", "key": "high", "value": 1, "color": "#e5484d" },
  { "label": "需要关注", "key": "watch", "value": 1, "color": "#f5a524" },
  { "label": "正常", "key": "normal", "value": 2, "color": "#3b82f6" },
  { "label": "改善中", "key": "improved", "value": 1, "color": "#22c55e" }
]
```

### GET `/api/campuspilot/students`

返回学生画像列表。

示例响应项：

```json
{
  "name": "张明远",
  "no": "STU2026001",
  "college": "信息工程学院",
  "major": "人工智能",
  "grade": "2023级",
  "clazz": "AI 2301",
  "careerGoal": "AI 算法工程师",
  "interests": "机器学习、算法竞赛",
  "gpa": 2.31,
  "attendance": 71,
  "failedCourses": 2,
  "courseScore": 76,
  "behaviorScore": 62,
  "riskLevel": "高风险",
  "riskKey": "high",
  "riskScore": 86,
  "status": "帮扶中",
  "updatedAt": "2026-06-05"
}
```

### GET `/api/campuspilot/courses`

返回课程成绩和课程建议。

示例响应项：

```json
{
  "student": "张明远",
  "course": "高等数学",
  "score": 58,
  "type": "核心基础",
  "status": "不及格",
  "suggestion": "每周 2 次辅导与错题复盘"
}
```

### GET `/api/campuspilot/behaviors`

返回学习行为数据。

示例响应项：

```json
{
  "student": "张明远",
  "item": "课堂出勤",
  "score": 71,
  "trend": "下降",
  "evidence": "近两周缺勤 3 次",
  "updatedAt": "2026-06-05"
}
```

### GET `/api/campuspilot/warnings`

返回风险预警单列表。

示例响应项：

```json
{
  "code": "RW2026003",
  "title": "王嘉宁阶段帮扶改善记录",
  "studentNo": "STU2026005",
  "student": "王嘉宁",
  "level": "改善中",
  "riskKey": "improved",
  "score": 41,
  "source": "辅导员复评",
  "status": "已结案",
  "statusKey": "done"
}
```

### 其他可用只读接口

| 方法 | 路径 | 用途 |
|---|---|---|
| GET | `/api/campuspilot/workflow` | 风险处置流程步骤 |
| GET | `/api/campuspilot/workflow-logs` | 流程流转日志 |
| GET | `/api/campuspilot/risk-trend` | 风险趋势图表 |
| GET | `/api/campuspilot/effectiveness` | 帮扶效果统计 |
| GET | `/api/campuspilot/integration-status` | 金蝶、Agent、工作流集成状态 |
| GET | `/api/campuspilot/lowcode-blueprint` | 低代码蓝图数据 |
| GET | `/api/campuspilot/agent-workflow` | Agent 工作流数据 |
| GET | `/api/campuspilot/report-center` | 报表中心数据 |
| GET | `/api/campuspilot/cloud-native` | 云原生运行数据 |
| GET | `/api/campuspilot/multimodal` | 多模态数据 |
| GET | `/api/campuspilot/agent-insight` | Agent 洞察数据 |
| GET | `/api/campuspilot/roles` | 角色说明 |
| GET | `/api/campuspilot/audit-logs` | 审计日志 |

## Agent 问答

### POST `/api/campuspilot/agent/chat`

请求体：

```json
{
  "question": "请分析 STU2026001 的风险"
}
```

示例响应：

```json
{
  "answer": "可以从学生画像、课程成绩、学习行为和风险预警数据形成识别、确认、帮扶、反馈、结案闭环。",
  "chips": ["画像", "闭环", "驾驶舱"],
  "source": "CampusPilot Java Local Fallback",
  "dataSource": "local-demo",
  "demo": true
}
```

## 工作流写操作接口

这些接口已经实现，但当前环境未配置 `CAMPUSPILOT_WORKFLOW_API_URL`，所以会返回 503。前端可以先完成调用封装和错误态展示，暂不作为可用链路。

| 方法 | 路径 | 请求体示例 |
|---|---|---|
| POST | `/api/campuspilot/plans/generate` | `{ "studentId": "STU2026001" }` |
| POST | `/api/campuspilot/risk/batch-scan` | `{ "className": "AI 2301" }` |
| POST | `/api/campuspilot/warnings/suggest` | 预警草稿数据 |
| POST | `/api/campuspilot/warnings/{code}/confirm` | 确认预警数据 |
| POST | `/api/campuspilot/warnings/{code}/mentor-plan` | 导师帮扶计划数据 |
| POST | `/api/campuspilot/warnings/{code}/feedback` | 学生/辅导员反馈数据 |
| POST | `/api/campuspilot/warnings/{code}/close` | 结案数据 |
| PATCH | `/api/campuspilot/tasks/{id}` | `{ "status": "done" }` |

当前 503 示例：

```json
{
  "ok": false,
  "demo": true,
  "dataSource": "local-demo",
  "operation": "plans.generate",
  "message": "未配置 CAMPUSPILOT_WORKFLOW_API_URL，未执行平台写入或任务流操作。"
}
```

## 直连金蝶 KAPI 接口

这些接口代码已实现，但当前联调环境返回 502，原因是金蝶 KAPI 返回 `HTTP 405`。前端暂时不要把这些接口作为主链路，建议先使用上面的 `/api/campuspilot/*` 看板接口。

| 方法 | 路径 |
|---|---|
| GET | `/api/student/profile/{student_id}` |
| GET | `/api/student/growth-plan/{student_id}` |
| GET | `/api/student/learning/{student_id}` |
| GET | `/api/student/risk-warning/{student_id}` |
| GET | `/api/student/opportunities/{student_id}` |
| GET | `/api/student/notifications/{student_id}` |
| GET | `/api/student/study-checkins/{student_id}` |
| GET | `/api/student/course-ability-mappings` |
| GET | `/api/campuspilot/students/{studentNo}/trajectory` |
| GET | `/api/campuspilot/students/{studentNo}/profile-analysis` |
| GET | `/api/campuspilot/students/{studentNo}/opportunities` |
| GET | `/api/campuspilot/tasks?role={role}` |

当前 502 示例：

```json
{
  "ok": false,
  "demo": false,
  "dataSource": "kingdee-api",
  "message": "Kingdee HTTP 405"
}
```

## 前端接入建议

1. 页面基础数据优先接 `/api/campuspilot/overview`、`/students`、`/courses`、`/behaviors`、`/warnings`。
2. 所有业务请求统一注入 `X-CampusPilot-User` 和 `X-CampusPilot-Role-Key`。
3. 对 502 显示“金蝶接口暂不可用”；对 503 显示“工作流暂未配置”。
4. `/api/student/*` 这批接口等金蝶 KAPI 方法或路径修正后再切换使用。
