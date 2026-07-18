# 复制为 application.local.ps1 后填写；local 文件已加入 .gitignore。
$env:CAMPUSPILOT_HOST = '0.0.0.0'
$env:CAMPUSPILOT_PORT = '8787'
$env:CAMPUSPILOT_CORS_ALLOWED_ORIGINS = 'http://127.0.0.1:8881'

# 可选：启用后，受保护接口还必须携带 Authorization: Bearer <token>。
$env:CAMPUSPILOT_API_BEARER_TOKEN = ''

# 金蝶业务对象 KAPI：固定 accessToken 与增强型 Token 凭据二选一。
$env:KINGDEE_BASE_URL = 'http://127.0.0.1:8881/ierp'
$env:KINGDEE_ACCESS_TOKEN = ''
$env:KINGDEE_CLIENT_ID = ''
$env:KINGDEE_CLIENT_SECRET = ''
$env:KINGDEE_USERNAME = ''
$env:KINGDEE_ACCOUNT_ID = ''
$env:KINGDEE_LANGUAGE = 'zh_CN'
$env:KINGDEE_TIMEOUT = '8000'

# Agent 使用同一套金蝶 OpenAPI 凭据，后端自动获取 accessToken、查找助手并创建会话。
# PUBLIC_BASE_URL 必须是金蝶平台能够回调到的后端公网地址，不能填写 127.0.0.1。
$env:CAMPUSPILOT_PUBLIC_BASE_URL = 'https://campuspilot.example.com'
$env:CAMPUSPILOT_AGENT_NAME = 'CampusPilot 启航智伴学业成长助手'
# CampusPilot 已确认的助手 ID；只有切换到其他助手时才修改。
$env:CAMPUSPILOT_AGENT_ASSISTANT_ID = '2522880941110602754'
# 可选：留空时每次启动自动生成；多实例部署时应配置相同的随机长字符串。
$env:CAMPUSPILOT_AGENT_CALLBACK_TOKEN = ''
$env:CAMPUSPILOT_AGENT_RESPONSE_WAIT_MS = '30000'

# 独立的写操作/任务流入口；与 Agent OpenAPI 不共用地址或 Key。
$env:CAMPUSPILOT_WORKFLOW_API_URL = ''
$env:CAMPUSPILOT_WORKFLOW_API_KEY = ''
$env:CAMPUSPILOT_WORKFLOW_TIMEOUT_MS = '12000'
