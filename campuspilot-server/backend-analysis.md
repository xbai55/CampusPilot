# CampusPilot 后端与最新金蝶 KAPI 适配分析

## 1. 当前后端架构

本次检查目录为 C:\Users\xbai55\Desktop\contest\campuspilot-server。实际技术栈是 Java 21 com.sun.net.httpserver.HttpServer，不是任务模板中假设的 FastAPI/Pydantic。

- 入口：CampusPilotApplication.java
- 路由：http/ApiHandler.java
- 服务层：service/*
- 本地数据：store/InMemoryCampusPilotStore.java
- 配置：config/AppConfig.java
- 数据库迁移草案：db/schema.sql、db/seed.sql
- Agent：service/AgentClient.java
- 原金蝶调用：service/KingdeeDataClient.java

保留现有 Java 架构比重建 FastAPI 更安全，也符合“不重新设计整体架构、优先兼容现有代码”的要求。

## 2. 适配前状态

适配前已调用 4 个金蝶查询接口：学生画像、课程成绩、学习行为、风险预警。

主要问题：

- 固定读取 CAMPUSPILOT_KINGDEE_ACCESS_TOKEN，没有通过第三方应用凭据获取 Token。
- Token 不缓存有效期，也不会在失效后自动重新获取。
- 认证、HTTP、响应校验职责混在 KingdeeDataClient。
- 缺少成长计划、成长轨迹、多维行为、课堂学情和机会推荐。
- 字段映射散落在业务 JSON 拼装代码中。
- 没有针对 Token、空数据和网络异常的自动化测试。
- 默认基础地址包含 /ierp，不利于同时调用平台级 /kapi/oauth2/getToken。

未发现写死的真实账号、密码或生产 Token；旧默认 URL 是本机演示地址。

## 3. PDF 规范核对

附件 PDF 共 34 页，生成时间为 2026-07-15，包含 13 个 GET 查询接口。统一响应结构为：

~~~json
{
  "data": { "rows": [] },
  "errorCode": "",
  "message": "",
  "status": true
}
~~~

业务请求头使用 Content-Type: application/json、accessToken 和可选 Idempotency-Key。

PDF 列出的对象是：成长计划、学习行为、成长轨迹、课程成绩、多维行为、学生机会推荐、成长机会库、课堂学情、通知提醒、风险预警、学生画像、学习打卡、课程能力映射。

## 4. Token 规范核对

金蝶官方《OpenAPI增强型Token认证》显示：

- 获取接口：POST /kapi/oauth2/getToken
- 必填：client_id、client_secret、username、accountId、nonce、timestamp
- Token 默认有效期：2 小时
- expires_in 单位：毫秒
- V7.0.8 已下架 refreshToken

因此本次采用“缓存 + 临期重新获取 + 鉴权失败重新获取并重试一次”，没有调用已下架的刷新接口。

## 5. 修改方案与结果

- 新增统一 KingdeeClient：Token、GET、超时、异常、日志、统一响应校验。
- 新增 KingdeeFieldMappings：集中维护 code_* 到业务字段的映射。
- 新增学生画像、成长计划、成长轨迹、学习分析、风险、机会服务。
- 新增 5 个 /api/student/* REST 聚合接口。
- 保留原 /api/campuspilot/* 路由和本地数据回退。
- 新增无第三方依赖的 Java 测试与 scripts/test.ps1。

数据库模型目前无需调整：新接口是只读 KAPI 聚合层，不需要把远程对象持久化。若后续需要离线报表或审计快照，再为成长计划、轨迹和机会记录增加本地表更合理。

## 6. 尚未开放业务路由的 PDF 接口

通知提醒、学习打卡、课程能力映射已完成文档核对，但不属于任务指定的 5 个新 REST 聚合接口，因此本次没有擅自扩展公开路由。它们可复用同一个 KingdeeClient 和字段映射机制继续接入。