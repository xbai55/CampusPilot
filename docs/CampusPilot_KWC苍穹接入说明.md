# CampusPilot KWC 苍穹接入说明

## 当前接入形态

`campuspilot-kwc` 已重构出标准 KWC 页面组件：

```text
src/modules/cp/campusPilot/
├── campusPilot.html
├── campusPilot.js
├── campusPilot.css
├── campusPilot.scoped.css
└── campusPilot.js-meta.kwc
```

组件标签名：

```html
<cp-campus-pilot></cp-campus-pilot>
```

命名空间来自 `kwc.config.json`：

```json
{
  "modules": [
    { "name": "cp", "path": "src/modules/cp" }
  ]
}
```

## 元数据

`campusPilot.js-meta.kwc` 使用 `KingdeeComponentBundle`，目标页面类型为：

```xml
<target>KWCFormModel</target>
```

已暴露平台设计器可配置属性：

| 属性 | 类型 | 用途 |
| --- | --- | --- |
| `pageTitle` | String | 页面标题 |
| `apiBase` | String | Java 后端或苍穹代理 API 地址 |
| `defaultRole` | Combo | 默认演示角色 |
| `enableAgent` | Boolean | 是否启用 Agent 问答入口 |
| `theme` | Combo | 明亮/深色主题 |

## 本地验证

```powershell
cd C:\Users\xbai55\Desktop\contest\campuspilot-kwc
npm install
npm run build
npx kd-custom-control-cli -v
```

当前验证结果：

```text
npm run build: passed
kd-custom-control-cli: 1.0.4
```

构建时若出现 `@kdcloudjs/kwc-engine-dom/dist/index.js.map` 缺失警告，是依赖包 source map 警告，不影响产物生成。

## 平台部署

有真实苍穹环境和登录/方案信息后执行：

```powershell
cd C:\Users\xbai55\Desktop\contest\campuspilot-kwc
npm run deploy
```

部署命令会读取组件元数据，将 `campusPilot` 作为 KWC 自定义控件部署到目标环境。

## 平台侧仍需配置

1. 在苍穹平台中准备方案 ID / 领域 ID / 开发商标识，与元数据中的 `moduleid=cp`、`isv=campuspilot` 对齐。
2. 配置真实金蝶 Agent 或 OpenAPI 网关，将地址填入组件属性 `apiBase`，或由 Java 后端通过 `CAMPUSPILOT_AGENT_API_URL` 代理。
3. 若比赛要求真实数据库，继续把 Java 后端从 `InMemoryCampusPilotStore` 切换到 PostgreSQL/JDBC；当前 KWC 组件 API 路径不需要改。
