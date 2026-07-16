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

# 金蝶 Agent 对话接口。
$env:CAMPUSPILOT_AGENT_API_URL = ''
$env:CAMPUSPILOT_AGENT_API_KEY = ''
$env:CAMPUSPILOT_AGENT_TIMEOUT_MS = '8000'

# 金蝶 Agent/任务流统一入口。为空时沿用 Agent URL/Key。
$env:CAMPUSPILOT_WORKFLOW_API_URL = ''
$env:CAMPUSPILOT_WORKFLOW_API_KEY = ''
$env:CAMPUSPILOT_WORKFLOW_TIMEOUT_MS = '12000'
