export const mockData = {
  overview: {
    totalStudents: 5,
    highRisk: 1,
    watchRisk: 1,
    normal: 2,
    improved: 1,
    pendingWarnings: 1,
    activeWarnings: 1,
    closedWarnings: 1,
    averageGpa: 3.02,
    averageAttendance: 88,
  },
  riskDistribution: [
    { name: "高风险", value: 1, color: "#d43f3a", key: "high" },
    { name: "需要关注", value: 1, color: "#d98314", key: "watch" },
    { name: "正常", value: 2, color: "#24966d", key: "normal" },
    { name: "改善中", value: 1, color: "#60758d", key: "improved" },
  ],
  students: [
    {
      name: "张明远", no: "STU2026001", college: "信息工程学院", major: "人工智能",
      grade: "2023级", className: "AI 2301", goal: "AI 算法工程师",
      interest: "机器学习、算法竞赛", gpa: 2.31, creditRate: 71, failed: 2,
      attendance: 76, assignment: 62, innovation: 38, riskLevel: "高风险",
      riskKey: "high", riskScore: 86,
      reason: "高等数学和数据结构薄弱，出勤率与作业完成率偏低。",
      advice: "建议辅导员确认预警，导师制定 4 周课程补强计划。",
      status: "帮扶中", updatedAt: "2026-06-05",
    },
    {
      name: "李佳怡", no: "STU2026002", college: "信息工程学院", major: "数据科学",
      grade: "2023级", className: "DS 2302", goal: "数据分析师",
      interest: "数据可视化、商业分析", gpa: 3.08, creditRate: 88, failed: 0,
      attendance: 78, assignment: 82, innovation: 55, riskLevel: "需要关注",
      riskKey: "watch", riskScore: 62,
      reason: "无挂科，但近期出勤率下降，学习平台活跃度偏低。",
      advice: "建议辅导员进行学习习惯提醒，并跟踪未来两周出勤。",
      status: "待确认", updatedAt: "2026-06-05",
    },
    {
      name: "陈思琪", no: "STU2026003", college: "计算机学院", major: "软件工程",
      grade: "2023级", className: "SE 2301", goal: "待明确",
      interest: "前端开发、产品设计", gpa: 3.21, creditRate: 91, failed: 0,
      attendance: 91, assignment: 89, innovation: 42, riskLevel: "正常",
      riskKey: "normal", riskScore: 28,
      reason: "学业表现稳定，但职业目标不够明确。",
      advice: "建议导师帮助其明确成长方向，推荐参与课程项目。",
      status: "成长规划中", updatedAt: "2026-06-04",
    },
    {
      name: "周泽宇", no: "STU2026004", college: "网络空间安全学院", major: "网络安全",
      grade: "2023级", className: "NS 2301", goal: "安全工程师",
      interest: "攻防演练、网络协议", gpa: 3.62, creditRate: 96, failed: 0,
      attendance: 96, assignment: 94, innovation: 81, riskLevel: "正常",
      riskKey: "normal", riskScore: 16,
      reason: "学业和行为表现良好，科创参与度高。",
      advice: "建议继续参加安全竞赛和项目实践。",
      status: "正常跟踪", updatedAt: "2026-06-03",
    },
    {
      name: "王嘉宁", no: "STU2026005", college: "信息工程学院", major: "计算机科学与技术",
      grade: "2023级", className: "CS 2303", goal: "后端工程师",
      interest: "数据库、分布式系统", gpa: 2.92, creditRate: 84, failed: 0,
      attendance: 89, assignment: 87, innovation: 63, riskLevel: "改善中",
      riskKey: "improved", riskScore: 41,
      reason: "曾有行为风险，近期出勤和作业完成率明显改善。",
      advice: "建议保持复评，沉淀为帮扶改善案例。",
      status: "已结案", updatedAt: "2026-06-02",
    },
  ],
  courses: [
    { student: "张明远", course: "高等数学", score: 58, type: "核心基础", status: "不及格", advice: "每周 2 次辅导与错题复盘" },
    { student: "张明远", course: "数据结构", score: 61, type: "核心专业", status: "薄弱", advice: "补强线性表、树、图基础" },
    { student: "张明远", course: "Python 程序设计", score: 72, type: "程序设计", status: "一般", advice: "结合算法题巩固语法与调试" },
    { student: "李佳怡", course: "数据可视化", score: 86, type: "专业方向", status: "良好", advice: "可参与驾驶舱展示优化" },
    { student: "陈思琪", course: "Java 程序设计", score: 84, type: "程序设计", status: "良好", advice: "建议参与 Web 项目实践" },
    { student: "周泽宇", course: "网络安全基础", score: 92, type: "专业方向", status: "优秀", advice: "推荐参加攻防演练" },
    { student: "王嘉宁", course: "数据库原理", score: 81, type: "核心专业", status: "良好", advice: "可拓展事务与索引优化" },
  ],
  behaviors: [
    { student: "张明远", attendance: 76, assignment: 62, activity: 48, interaction: 3, lastLogin: "2026-06-04", note: "出勤和作业完成率均偏低" },
    { student: "李佳怡", attendance: 78, assignment: 82, activity: 51, interaction: 4, lastLogin: "2026-06-03", note: "近期出勤下降，需要行为关注" },
    { student: "陈思琪", attendance: 91, assignment: 89, activity: 67, interaction: 7, lastLogin: "2026-06-05", note: "学习行为稳定" },
    { student: "周泽宇", attendance: 96, assignment: 94, activity: 88, interaction: 12, lastLogin: "2026-06-05", note: "活跃度和互动次数高" },
    { student: "王嘉宁", attendance: 89, assignment: 87, activity: 70, interaction: 8, lastLogin: "2026-06-04", note: "帮扶后行为改善明显" },
  ],
  warnings: [
    {
      code: "RW2026001", title: "张明远高风险学业预警", studentNo: "STU2026001",
      student: "张明远", level: "高风险", riskKey: "high", score: 86,
      source: "Agent 风险分析", status: "帮扶中", statusKey: "active",
      owner: "辅导员 / 专业导师", reviewAt: "2026-06-10",
      counselorNote: "已确认存在学业高风险，需进入重点帮扶。",
      mentorPlan: "制定高等数学与数据结构 4 周补强计划。",
      studentFeedback: "愿意参加每周辅导并提交学习记录。",
    },
    {
      code: "RW2026002", title: "李佳怡学习行为关注预警", studentNo: "STU2026002",
      student: "李佳怡", level: "需要关注", riskKey: "watch", score: 62,
      source: "学习行为监测", status: "待确认", statusKey: "todo",
      owner: "辅导员", reviewAt: "2026-06-12",
      counselorNote: "待辅导员核实近期请假与课程参与情况。",
      mentorPlan: "暂未进入导师帮扶。", studentFeedback: "未反馈。",
    },
    {
      code: "RW2026003", title: "王嘉宁阶段帮扶改善记录", studentNo: "STU2026005",
      student: "王嘉宁", level: "改善中", riskKey: "improved", score: 41,
      source: "辅导员复评", status: "已结案", statusKey: "done",
      owner: "学院管理者", reviewAt: "2026-06-15",
      counselorNote: "连续两周出勤与作业完成率改善。",
      mentorPlan: "继续跟踪数据库课程项目。",
      studentFeedback: "已完成阶段任务。",
    },
  ],
  workflow: [
    { step: "风险识别", owner: "系统 / Agent", time: "2026-06-04", detail: "根据画像、成绩和行为识别张明远为高风险。" },
    { step: "生成预警单", owner: "风险预警对象", time: "2026-06-04", detail: "形成 RW2026001，预警来源为 Agent 风险分析。" },
    { step: "辅导员确认", owner: "辅导员", time: "2026-06-05", detail: "确认风险，填写辅导员意见并推进导师帮扶。" },
    { step: "导师帮扶", owner: "专业导师", time: "2026-06-05", detail: "制定高等数学与数据结构 4 周补强计划。" },
    { step: "学生反馈", owner: "学生", time: "2026-06-06", detail: "学生确认参加辅导并提交阶段学习记录。" },
    { step: "复评结案", owner: "辅导员 / 学院", time: "待复评", detail: "根据后续出勤、作业和课程测验决定是否结案。" },
  ],
  agentInsight: {
    title: "优先处理张明远高风险学业预警",
    summary: "建议辅导员先确认 RW2026001，导师同步制定高等数学与数据结构 4 周补强计划，并在下次复评中重点观察出勤率和作业完成率。",
    tags: ["高风险", "导师帮扶", "4 周复评", "可生成预警建议"],
  },
  roles: [
    { role: "学生", duty: "查看自己的成长画像、查看 AI 学习建议、提交帮扶反馈。", route: "#students", actor: "张明远" },
    { role: "辅导员", duty: "查看风险学生列表、确认风险预警单、填写辅导员意见、跟踪处理状态。", route: "#warnings", actor: "王老师" },
    { role: "导师", duty: "查看学生课程短板、制定帮扶措施、推荐课程和项目。", route: "#courses", actor: "陈导师" },
    { role: "学院管理者", duty: "查看驾驶舱、风险分布、预警处理情况和帮扶成效。", route: "#dashboard", actor: "学院管理者" },
  ],
  auditLogs: [
    { time: "2026-06-06 18:40", actor: "王老师", action: "查看张明远学生画像" },
    { time: "2026-06-06 18:47", actor: "CampusPilot Agent", action: "生成高风险帮扶建议" },
    { time: "2026-06-06 18:55", actor: "陈导师", action: "更新导师帮扶措施" },
  ],
  lowcodeBlueprint: {
    objects: [
      { name: "学生画像", code: "cp_student_profile", type: "领域模型 + 表单", purpose: "沉淀学生基础信息、目标、学业表现、行为指标和风险标签。" },
      { name: "课程成绩", code: "cp_course_score", type: "业务对象 + 列表报表", purpose: "为风险解释和导师帮扶提供课程证据。" },
      { name: "学习行为", code: "cp_learning_behavior", type: "采集表单 + 异常规则", purpose: "记录出勤、作业、平台活跃和课堂互动。" },
      { name: "风险预警单", code: "cp_risk_warning", type: "流程表单", purpose: "承载风险识别、确认、帮扶、反馈和结案。" },
      { name: "风险处理日志", code: "cp_warning_log", type: "流程日志对象", purpose: "保留每次状态变更和处理说明。" },
    ],
    platformCapabilities: [
      { name: "表单及插件开发", status: "已设计", detail: "确认、导师计划、学生反馈和结案动作对应表单插件事件。" },
      { name: "流程服务", status: "已设计", detail: "风险识别 -> 预警生成 -> 确认 -> 帮扶 -> 反馈 -> 复评结案。" },
      { name: "报表", status: "已实现前端看板", detail: "风险分布、状态分布、趋势、专业对比和帮扶成效。" },
      { name: "集成开发", status: "已预留", detail: "通过环境变量与金蝶 Agent API / OpenAPI 边界对接。" },
    ],
  },
  agentWorkflow: {
    agentName: "CampusPilot 学业成长助手",
    modelRoute: "通过苍穹平台统一接入 DeepSeek / 豆包 / 通义千问等模型",
    rag: [
      { source: "学生画像知识库", content: "学生基础信息、成长目标、课程短板和风险规则。" },
      { source: "帮扶政策知识库", content: "教育强国、拔尖创新人才培养、心理健康和学业帮扶政策摘要。" },
      { source: "历史预警案例库", content: "预警单、处理轨迹、导师措施、学生反馈和结案结果。" },
    ],
    tools: [
      { name: "query_student_profile", method: "GET /api/campuspilot/students", purpose: "查询学生画像并作为回答主证据。" },
      { name: "query_warning_order", method: "GET /api/campuspilot/warnings", purpose: "查询已有预警单，避免重复建单。" },
      { name: "create_warning_order", method: "POST /api/campuspilot/warnings/suggest", purpose: "生成结构化预警建议并进入待确认。" },
      { name: "update_warning_status", method: "POST /api/campuspilot/warnings/{code}/{action}", purpose: "推进确认、导师计划、学生反馈和结案。" },
    ],
    workflow: [
      { step: "理解问题", detail: "识别学生、角色、风险类型和期望输出。" },
      { step: "检索知识库", detail: "RAG 拉取规则、政策和历史案例。" },
      { step: "调用工具", detail: "查询画像、课程、行为、预警单等实时数据。" },
      { step: "多步推理", detail: "综合 GPA、挂科、出勤、作业、目标和历史处理判断风险。" },
      { step: "结构化输出", detail: "输出风险等级、依据、建议处理人、复评时间和预警单字段。" },
    ],
  },
  reportCenter: [
    { name: "学业风险驾驶舱", metric: "风险等级分布、预警状态、结案率", decision: "帮助学院管理者判断风险治理压力。" },
    { name: "学生画像报告", metric: "GPA、挂科、出勤、作业、科创参与度", decision: "帮助辅导员和导师定位学生主要短板。" },
    { name: "帮扶成效报表", metric: "处理周期、结案数、改善案例", decision: "验证帮扶措施是否真正产生改善。" },
    { name: "Agent 建议审计报表", metric: "建议来源、工具调用、人工确认结果", decision: "避免 AI 黑箱，便于答辩说明可控性。" },
  ],
  cloudNative: [
    { layer: "前端", unit: "campuspilot-kwc build 静态资源", deploy: "可由 Java 静态服务、Nginx 或对象存储托管。" },
    { layer: "业务服务", unit: "campuspilot-server Java 21 HTTP 服务", deploy: "无框架依赖，适合容器化和云主机部署。" },
    { layer: "数据层", unit: "PostgreSQL schema.sql / seed.sql", deploy: "当前内存数据可平滑迁移到 PostgreSQL。" },
    { layer: "Agent 集成", unit: "CAMPUSPILOT_AGENT_API_URL", deploy: "通过环境变量切换本地兜底与远程金蝶 Agent API。" },
  ],
  multimodal: [
    { input: "学生上传学习计划 PDF / 文档", model: "多模态/文档理解模型", output: "提取目标、困难和阶段计划，写入成长画像。", status: "接口预留" },
    { input: "课堂签到或活动图片", model: "视觉理解模型", output: "辅助识别出勤异常和活动参与证据。", status: "演示设计" },
    { input: "学生语音反馈", model: "语音识别 + 情感分析", output: "生成帮扶反馈摘要并提示辅导员复核。", status: "演示设计" },
    { input: "成绩单截图", model: "OCR + 结构化抽取", output: "提取课程成绩，触发风险规则或补强建议。", status: "后续增强" },
  ],
};

export const routeMeta = {
  home: {
    eyebrow: "AI 原生学业风险治理平台",
    title: "启航智伴 CampusPilot",
    subtitle: "面向学生成长画像、风险预警、帮扶协同与治理分析的一体化工作系统。",
  },
  "student-home": {
    eyebrow: "个人工作台", title: "我的成长画像",
    subtitle: "查看个人学业状态、AI 学习建议和帮扶反馈入口。",
  },
  "teacher-home": {
    eyebrow: "教学工作台", title: "课程短板与帮扶措施",
    subtitle: "查看学生课程短板，制定导师帮扶计划并跟踪复评结果。",
  },
  "counselor-home": {
    eyebrow: "风险处置", title: "风险学生处置工作台",
    subtitle: "确认风险预警、跟踪帮扶状态，并协调教师与学生反馈。",
  },
  "admin-home": {
    eyebrow: "治理驾驶舱", title: "学院风险治理驾驶舱",
    subtitle: "查看学院风险分布、预警处理进度、帮扶成效和系统配置。",
  },
  dashboard: {
    eyebrow: "AI 原生智慧校园平台", title: "CampusPilot 学业风险驾驶舱",
    subtitle: "学生画像、风险预警、Agent 分析与帮扶闭环的一体化工作台。",
  },
  students: {
    eyebrow: "低代码业务对象", title: "学生画像中心",
    subtitle: "沉淀学生基础信息、成长目标、学业表现、学习行为与风险状态。",
  },
  courses: {
    eyebrow: "课程成绩数据", title: "课程成绩与短板分析",
    subtitle: "识别核心课程薄弱点，为 Agent 风险解释和导师帮扶提供证据。",
  },
  behavior: {
    eyebrow: "学习行为数据", title: "学习行为监测",
    subtitle: "出勤、作业、平台活跃度和课堂互动共同支撑行为风险判断。",
  },
  warnings: {
    eyebrow: "风险预警闭环", title: "风险预警单管理",
    subtitle: "承载风险原因、AI 建议、处理状态、辅导员意见、导师措施和学生反馈。",
  },
  agent: {
    eyebrow: "Agent 智能分析", title: "CampusPilot 学业成长助手",
    subtitle: "标准化输出风险判断、成长规划和可映射到预警单字段的结构化建议。",
  },
  workflow: {
    eyebrow: "业务处理闭环", title: "风险处理过程展示",
    subtitle: "展示从风险识别、预警生成、辅导员确认、导师帮扶到复评结案的业务链路。",
  },
  user: {
    eyebrow: "组织与权限", title: "用户中心",
    subtitle: "展示当前账号、角色职责、权限范围与操作记录。",
  },
  settings: {
    eyebrow: "系统配置", title: "设置中心",
    subtitle: "配置接口地址、Agent 策略、风险规则、展示偏好和审计开关。",
  },
};

export const riskTrend = [
  { week: "第1周", high: 2, watch: 2, done: 0 },
  { week: "第2周", high: 2, watch: 1, done: 1 },
  { week: "第3周", high: 1, watch: 2, done: 1 },
  { week: "第4周", high: 1, watch: 1, done: 1 },
];

export const effectiveness = {
  closedRate: 33, activeCount: 1, closedCount: 1, averageCycleDays: 7, highPriority: [],
};

export const integrationStatus = {
  kingdeeBaseUrl: "http://10.0.160.250:8080/ierp",
  agentMode: "金蝶 Agent API 接入",
  objects: [
    { name: "学生画像", code: "cp_student_profile", status: "已建模", fields: 12 },
    { name: "风险预警单", code: "cp_warning_order", status: "已建模", fields: 13 },
  ],
  thirdPartyApp: { appId: "campuspilot_isv", auth: "AccessToken / API 授权 / IP 白名单", status: "待配置真实密钥" },
};
