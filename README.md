# CampusPilot

CampusPilot 现在只保留 Java 后端工程。

## 目录结构

```text
CampusPilot/
├─ README.md
└─ campuspilot-server/
   ├─ config/      # 配置模板
   ├─ db/          # 本地数据/脚本
   ├─ docs/        # 后端与前端联调文档
   ├─ scripts/     # 构建、测试、运行脚本
   ├─ src/         # Java 源码
   └─ tests/       # Java 测试
```

## 快速启动

进入后端目录：

```powershell
cd campuspilot-server
.\scripts\test.ps1
.\scripts\run.ps1
```

后端默认监听：

```text
http://127.0.0.1:8787
```

如果需要给同一网络内其他机器访问，请使用启动机器的局域网 IP，例如：

```text
http://10.0.160.11:8787
```

## Java 环境

后端使用 Java 21。脚本会按以下顺序查找 Java：

1. 系统 PATH 中的 `java` / `javac`
2. `JAVA_HOME`
3. 本机 IntelliJ IDEA 自带 JBR：`D:\Program Files\JetBrains\IntelliJ IDEA 2025.3.2\jbr`

## 前端联调文档

接口说明见：

```text
campuspilot-server/docs/frontend-api.md
```

当前可稳定联调的主接口为 `/api/campuspilot/*` 看板接口。`/api/student/*` 直连金蝶 KAPI 的接口需要金蝶侧方法/路径可用后再接入。

## 注意

不要提交本地配置文件：

```text
campuspilot-server/config/application.local.ps1
```

该文件用于本地密钥、Token、金蝶地址等配置，应只保留在本机。
