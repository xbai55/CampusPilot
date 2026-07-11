import { mockData, riskTrend, effectiveness, integrationStatus } from "../data/mockData";

const API_BASE = window.CAMPUSPILOT_API_BASE ?? "";

function clone(value) {
  return JSON.parse(JSON.stringify(value));
}

function requestHeaders(withBody = false) {
  const stored = localStorage.getItem("campuspilot:user");
  const user = stored ? JSON.parse(stored) : null;
  const headers = { Accept: "application/json" };
  if (user) {
    headers["X-CampusPilot-Role-Key"] = { 学生: "student", 辅导员: "counselor", 导师: "mentor", 学院管理者: "manager" }[user.role] || "counselor";
    headers["X-CampusPilot-User"] = encodeURIComponent(user.name);
  }
  if (withBody) headers["Content-Type"] = "application/json";
  return headers;
}

async function fetchOrMock(path, fallback) {
  try {
    const res = await fetch(`${API_BASE}${path}`, { headers: requestHeaders() });
    if (!res.ok) throw new Error(`Request failed: ${res.status}`);
    return res.json();
  } catch {
    return clone(fallback);
  }
}

async function postOrMock(path, body, fallback) {
  try {
    const stored = localStorage.getItem("campuspilot:user");
    const user = stored ? JSON.parse(stored) : null;
    const res = await fetch(`${API_BASE}${path}`, {
      method: "POST",
      headers: requestHeaders(true),
      body: JSON.stringify({ ...body, __user: user }),
    });
    if (!res.ok) throw new Error(`Request failed: ${res.status}`);
    return res.json();
  } catch {
    return clone(fallback);
  }
}

function buildAgentAnswer(question) {
  if (question.includes("李佳怡")) {
    return "【风险等级】需要关注。李佳怡没有挂科，但出勤率为 78%，学习平台活跃度偏低。建议辅导员先确认近期请假和课堂参与情况，可生成学习行为关注预警单，初始状态为待确认。";
  }
  if (question.includes("陈思琪")) {
    return "【成长规划】陈思琪学业表现稳定，但职业目标暂不明确。建议短期完成前端项目实践，中期明确软件工程或产品技术方向，导师可推荐课程项目并在 2 周后复盘。";
  }
  if (question.includes("待辅导员") || question.includes("确认")) {
    return "【待处理事项】当前优先确认 RW2026002 李佳怡学习行为关注预警；RW2026001 张明远已进入帮扶中，需要跟进导师补强计划和复评时间。";
  }
  return "【风险等级】高风险。张明远当前 GPA 2.31，挂科数 2，出勤率 76%，作业完成率 62%，且高等数学和数据结构薄弱。建议辅导员确认预警，导师制定 4 周补强计划，并在 2026-06-10 复评。";
}

const api = {
  fetchOverview: () => fetchOrMock("/api/campuspilot/overview", mockData.overview),
  fetchRiskDistribution: () => fetchOrMock("/api/campuspilot/risk-distribution", mockData.riskDistribution),
  fetchStudents: () => fetchOrMock("/api/campuspilot/students", mockData.students),
  fetchCourses: () => fetchOrMock("/api/campuspilot/courses", mockData.courses),
  fetchBehaviors: () => fetchOrMock("/api/campuspilot/behaviors", mockData.behaviors),
  fetchWarnings: () => fetchOrMock("/api/campuspilot/warnings", mockData.warnings),
  fetchWorkflow: () => fetchOrMock("/api/campuspilot/workflow", mockData.workflow),
  fetchWorkflowLogs: () => fetchOrMock("/api/campuspilot/workflow-logs", []),
  fetchRiskTrend: () => fetchOrMock("/api/campuspilot/risk-trend", riskTrend),
  fetchEffectiveness: () => fetchOrMock("/api/campuspilot/effectiveness", effectiveness),
  fetchIntegrationStatus: () => fetchOrMock("/api/campuspilot/integration-status", integrationStatus),
  fetchLowcodeBlueprint: () => fetchOrMock("/api/campuspilot/lowcode-blueprint", mockData.lowcodeBlueprint),
  fetchAgentWorkflow: () => fetchOrMock("/api/campuspilot/agent-workflow", mockData.agentWorkflow),
  fetchReportCenter: () => fetchOrMock("/api/campuspilot/report-center", mockData.reportCenter),
  fetchCloudNative: () => fetchOrMock("/api/campuspilot/cloud-native", mockData.cloudNative),
  fetchMultimodal: () => fetchOrMock("/api/campuspilot/multimodal", mockData.multimodal),
  fetchAgentInsight: () => fetchOrMock("/api/campuspilot/agent-insight", mockData.agentInsight),
  fetchRoles: () => fetchOrMock("/api/campuspilot/roles", mockData.roles),
  fetchAuditLogs: () => fetchOrMock("/api/campuspilot/audit-logs", mockData.auditLogs),
  agentChat: (question, role) =>
    postOrMock("/api/campuspilot/agent/chat", { question, role }, {
      answer: buildAgentAnswer(question),
      chips: ["本地兜底", "风险规则", "预警单字段"],
    }),
  createWarningSuggestion: () =>
    postOrMock("/api/campuspilot/warnings/suggest", {}, {
      title: "张明远高风险学业预警", student_no: "STU2026001", student_name: "张明远",
      risk_level: "高风险", risk_score: 86, source: "Agent 风险分析", status: "待确认",
    }),
  confirmWarning: (code, counselorNote) =>
    postOrMock(`/api/campuspilot/warnings/${code}/confirm`, { counselorNote }, { ok: true }),
  saveMentorPlan: (code, mentorPlan) =>
    postOrMock(`/api/campuspilot/warnings/${code}/mentor-plan`, { mentorPlan }, { ok: true }),
  submitFeedback: (code, studentFeedback) =>
    postOrMock(`/api/campuspilot/warnings/${code}/feedback`, { studentFeedback }, { ok: true }),
  closeWarning: (code, closeNote) =>
    postOrMock(`/api/campuspilot/warnings/${code}/close`, { closeNote }, { ok: true }),
};

export default api;
