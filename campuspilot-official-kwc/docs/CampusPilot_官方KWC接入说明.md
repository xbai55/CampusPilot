# CampusPilot 官方 KWC 接入说明

## 1. 项目定位

本目录由 `@kdcloudjs/kd-custom-control-cli create campuspilot` 生成，并选择原生 KWC 模板。前端组件用于承载 A6 智慧校园赛题中的学业风险驾驶舱、学生画像、预警单闭环和 Agent 分析入口。

它和普通网页的区别可以这样理解：

```text
普通网页：浏览器直接打开页面。
KWC 控件：金蝶页面先加载自定义控件元数据，再由 KDApi.register 挂载组件。
```

## 2. 目录与元数据

关键文件：

```text
src/index.js                         平台入口，注册 campuspilot 控件
src/devIndex.js                      本地调试入口
src/campuspilot.js-meta.kwc          金蝶控件元数据
src/modules/campuspilot/app/app.js   KWC 组件逻辑
src/modules/campuspilot/app/app.html KWC 组件模板
src/modules/campuspilot/app/app.css  Shadow DOM 样式
kwc.config.json                      KWC 模块目录配置
server/config.js                     远程调试静态服务配置
```

元数据已配置：

```text
framework = kwc
target = KWCFormModel
isv = code
moduleid = code
name = campuspilot
```

## 3. 平台属性

金蝶页面可配置以下属性：

| 属性 | 含义 |
| --- | --- |
| `pageTitle` | 顶部标题 |
| `apiBase` | Java 后端 `/api/campuspilot` 基础地址 |
| `defaultRole` | 默认角色视角 |
| `enableAgent` | 是否通过后端调用远程 Agent |
| `theme` | `light` 或 `dark` |

示例：

```text
apiBase = http://127.0.0.1:8787/api/campuspilot
defaultRole = counselor
enableAgent = false
```

## 4. 本地运行

```bash
npm install
npm start
```

构建：

```bash
npm run build
```

本地静态服务：

```bash
npm run server
```

## 5. 金蝶接入流程

1. 在金蝶环境准备第三方应用、代理用户、AccessToken 认证策略和必要 API 授权。
2. 在本项目执行：

```bash
npx kd-custom-control-cli deploy
```

3. 如果只修改了 `src/campuspilot.js-meta.kwc`，执行：

```bash
npx kd-custom-control-cli deploy -f
```

4. 远程调试时先启动本地静态服务，然后在金蝶远程页面 URL 后追加：

```text
&kdcus_cdn=http://localhost:3001
```

## 6. 安全边界

前端 KWC 组件不保存平台应用密钥，也不直接拼接真实 Token。真实认证建议放在：

```text
金蝶平台配置
或
Java 后端 Agent/OpenAPI 代理层
```

这样做的原因很简单：前端代码会被浏览器加载，密钥放在前端就等于交给用户查看；后端代理则可以把密钥留在服务器侧。
