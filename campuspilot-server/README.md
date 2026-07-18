# CampusPilot

CampusPilot 当前仓库只保留 Java 后端工程 `campuspilot-server`。

## 快速启动

```powershell
cd campuspilot-server
.\scripts\test.ps1
.\scripts\run.ps1
```

默认服务地址：

```text
http://127.0.0.1:8787
```

局域网联调地址使用启动机器的 IP，例如：

```text
http://10.0.160.11:8787
```

## 金蝶 Agent 回调

后端通过金蝶苍穹 OpenAPI 调用 Agent：

```text
POST /ierp/kapi/oauth2/getToken
POST /ierp/kapi/v2/gai/newsession
POST /ierp/kapi/v2/gai/chat
```

`/v2/gai/chat` 的直接响应只是受理结果，真正回答来自回调：

```text
{CAMPUSPILOT_PUBLIC_BASE_URL}/api/campuspilot/agent/callback?token=...
```

回调解析遵循官方结构：

```text
message.actionList[*].type
message.actionList[*].data.message
```

后端会按 `chatTraceId` 关联请求和回调，拼接多个 `type=chat` 分片；`streamDone` 和 `waitingDone` 只作为结束事件，不会覆盖已收到的正文。若上游只返回完成事件而没有正文，后端会返回明确诊断，不伪造 Agent 回答。

排查同一请求的所有回调事件：

```text
GET /api/campuspilot/agent/results/{chatTraceId}
```

## 文档

前端接口文档：

```text
campuspilot-server/docs/frontend-api.md
```

后端说明：

```text
campuspilot-server/README.md
```

## 注意

不要提交本地配置文件：

```text
campuspilot-server/config/application.local.ps1
```

该文件用于保存本地密钥、Token、金蝶地址等配置。
