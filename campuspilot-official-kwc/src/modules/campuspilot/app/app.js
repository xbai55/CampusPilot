/* eslint-disable max-len */
import { KingdeeElement, api, track } from '@kdcloudjs/kwc';

const ROLE_NAMES = {
    student: '学生',
    counselor: '辅导员',
    mentor: '导师',
    manager: '学院管理者'
};

const ROLE_USERS = {
    student: '张明远',
    counselor: '王老师',
    mentor: '陈导师',
    manager: '学院管理员'
};

const FALLBACK = {
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
        averageAttendance: 88
    },
    riskDistribution: [
        { label: '高风险', key: 'high', value: 1, color: '#dc3f3a' },
        { label: '需要关注', key: 'watch', value: 1, color: '#d68519' },
        { label: '正常', key: 'normal', value: 2, color: '#1e6be6' },
        { label: '改善中', key: 'improved', value: 1, color: '#15915f' }
    ],
    riskTrend: [
        { label: '第1周', high: 2, watch: 2, normal: 1 },
        { label: '第2周', high: 2, watch: 1, normal: 2 },
        { label: '第3周', high: 1, watch: 2, normal: 2 },
        { label: '第4周', high: 1, watch: 1, normal: 3 }
    ],
    effectiveness: {
        responseRate: 92,
        improvedRate: 68,
        closedWarnings: 1,
        averageHandleDays: 5.5
    },
    students: [
        { no: 'STU2026001', name: '张明远', college: '信息工程学院', major: '人工智能', grade: '2023级', className: 'AI 2301', goal: 'AI 算法工程师', gpa: 2.31, failed: 2, attendance: 76, assignment: 62, innovation: 38, riskLevel: '高风险', riskKey: 'high', riskScore: 86, reason: '高等数学和数据结构薄弱，出勤率与作业完成率偏低。', advice: '建议辅导员确认预警，导师制定 4 周课程补强计划。', status: '帮扶中' },
        { no: 'STU2026002', name: '李佳怡', college: '信息工程学院', major: '数据科学', grade: '2023级', className: 'DS 2302', goal: '数据分析师', gpa: 3.08, failed: 0, attendance: 78, assignment: 82, innovation: 55, riskLevel: '需要关注', riskKey: 'watch', riskScore: 62, reason: '无挂科，但近期出勤率下降，学习平台活跃度偏低。', advice: '建议辅导员进行学习习惯提醒，并跟踪未来两周出勤。', status: '待确认' },
        { no: 'STU2026003', name: '赵一航', college: '信息工程学院', major: '软件工程', grade: '2023级', className: 'SE 2301', goal: '前端工程师', gpa: 3.42, failed: 0, attendance: 95, assignment: 91, innovation: 72, riskLevel: '正常', riskKey: 'normal', riskScore: 24, reason: '学习节奏稳定，课程任务完成度较高。', advice: '可推荐参与项目制学习，提升实践成果。', status: '正常' },
        { no: 'STU2026004', name: '周若溪', college: '信息工程学院', major: '网络空间安全', grade: '2023级', className: 'NS 2301', goal: '安全工程师', gpa: 3.37, failed: 0, attendance: 92, assignment: 88, innovation: 76, riskLevel: '正常', riskKey: 'normal', riskScore: 28, reason: '学习与实践表现稳定。', advice: '继续保持竞赛训练和课程项目输出。', status: '正常' },
        { no: 'STU2026005', name: '王嘉宁', college: '信息工程学院', major: '计算机科学与技术', grade: '2023级', className: 'CS 2303', goal: '后端工程师', gpa: 2.92, failed: 0, attendance: 89, assignment: 87, innovation: 63, riskLevel: '改善中', riskKey: 'improved', riskScore: 41, reason: '曾有行为风险，近期出勤和作业完成率明显改善。', advice: '建议保持复评，沉淀为帮扶改善案例。', status: '已结案' }
    ],
    courses: [
        { student: '张明远', course: '高等数学', score: 58, type: '核心基础', status: '不及格', advice: '每周 2 次辅导与错题复盘' },
        { student: '张明远', course: '数据结构', score: 61, type: '核心专业', status: '薄弱', advice: '补强线性表、树、图基础' },
        { student: '李佳怡', course: '数据可视化', score: 86, type: '专业方向', status: '良好', advice: '可参与驾驶舱展示优化' },
        { student: '赵一航', course: 'Web 前端工程', score: 91, type: '专业方向', status: '优秀', advice: '鼓励承担小组项目负责人' }
    ],
    behaviors: [
        { student: '张明远', attendance: 76, assignment: 62, activity: 48, interaction: 3, lastLogin: '2026-06-04', note: '出勤和作业完成率均偏低。' },
        { student: '李佳怡', attendance: 78, assignment: 82, activity: 51, interaction: 4, lastLogin: '2026-06-03', note: '近期出勤下降，需要行为关注。' },
        { student: '王嘉宁', attendance: 89, assignment: 87, activity: 70, interaction: 8, lastLogin: '2026-06-04', note: '帮扶后行为改善明显。' }
    ],
    warnings: [
        { code: 'RW2026001', title: '张明远高风险学业预警', studentNo: 'STU2026001', student: '张明远', level: '高风险', riskKey: 'high', score: 86, source: '学习画像分析', status: '帮扶中', statusKey: 'active', owner: '辅导员 / 专业导师', reviewAt: '2026-06-10', counselorNote: '已确认存在学业高风险，需进入重点帮扶。', mentorPlan: '制定高等数学与数据结构 4 周补强计划。', studentFeedback: '愿意参加每周辅导并提交学习记录。' },
        { code: 'RW2026002', title: '李佳怡学习行为关注预警', studentNo: 'STU2026002', student: '李佳怡', level: '需要关注', riskKey: 'watch', score: 62, source: '学习行为监测', status: '待确认', statusKey: 'todo', owner: '辅导员', reviewAt: '2026-06-12', counselorNote: '待辅导员核实近期请假与课程参与情况。', mentorPlan: '暂未进入导师帮扶。', studentFeedback: '未反馈。' },
        { code: 'RW2026003', title: '王嘉宁阶段帮扶改善记录', studentNo: 'STU2026005', student: '王嘉宁', level: '改善中', riskKey: 'improved', score: 41, source: '辅导员复评', status: '已结案', statusKey: 'done', owner: '学院管理者', reviewAt: '2026-06-15', counselorNote: '连续两周出勤与作业完成率改善。', mentorPlan: '继续跟踪数据库课程项目。', studentFeedback: '已完成阶段任务。' }
    ],
    workflow: [
        { step: '风险识别', owner: '系统分析', time: '2026-06-04', detail: '根据画像、课程成绩和学习行为识别张明远为高风险。' },
        { step: '生成预警单', owner: '风险预警', time: '2026-06-04', detail: '形成 RW2026001，进入辅导员待确认队列。' },
        { step: '辅导员确认', owner: '辅导员', time: '2026-06-05', detail: '确认风险，填写辅导员意见并推进导师帮扶。' },
        { step: '导师帮扶', owner: '专业导师', time: '2026-06-05', detail: '制定高等数学与数据结构 4 周补强计划。' },
        { step: '学生反馈', owner: '学生', time: '2026-06-06', detail: '学生提交学习承诺和反馈。' },
        { step: '复评结案', owner: '辅导员 / 学院管理者', time: '2026-06-15', detail: '根据改善情况复评，形成结案记录或继续跟踪。' }
    ],
    agentWorkflow: {
        agentName: 'CampusPilot 学业成长助手',
        tools: [
            { name: '学生画像查询', method: 'GET /api/campuspilot/students', purpose: '查询画像并作为回答证据。' },
            { name: '预警单查询', method: 'GET /api/campuspilot/warnings', purpose: '查询已有预警，避免重复建单。' },
            { name: '帮扶建议生成', method: 'POST /api/campuspilot/warnings/suggest', purpose: '生成结构化预警建议。' }
        ]
    },
    roles: [
        { role: '学生', duty: '查看自己的成长画像、学习建议和帮扶反馈。', actor: '张明远' },
        { role: '辅导员', duty: '查看风险学生列表，确认预警单，填写处理意见。', actor: '王老师' },
        { role: '导师', duty: '查看课程短板，制定帮扶措施。', actor: '陈导师' },
        { role: '学院管理者', duty: '查看整体风险分布、预警处理和帮扶成效。', actor: '学院管理员' }
    ]
};

export default class CampusPilot extends KingdeeElement {
  @api pageTitle = 'CampusPilot 学业风险驾驶舱';
  @api apiBase = '';
  @api defaultRole = 'counselor';
  @api enableAgent = false;
  @api theme = 'light';

  @track page = 'home';
  @track role = 'counselor';
  @track loading = false;
  @track errorMessage = '';
  @track overview = FALLBACK.overview;
  @track riskDistribution = FALLBACK.riskDistribution;
  @track riskTrend = FALLBACK.riskTrend;
  @track effectiveness = FALLBACK.effectiveness;
  @track students = FALLBACK.students;
  @track courses = FALLBACK.courses;
  @track behaviors = FALLBACK.behaviors;
  @track warnings = FALLBACK.warnings;
  @track workflow = FALLBACK.workflow;
  @track agentWorkflow = FALLBACK.agentWorkflow;
  @track roles = FALLBACK.roles;
  @track selectedStudentNo = 'STU2026001';
  @track selectedWarningCode = 'RW2026001';
  @track question = '张明远下一步应由谁处理？是否需要生成风险预警单？';
  @track agentAnswer = '你好，我可以结合学生画像、课程成绩、学习行为和预警单，生成结构化帮扶建议。';
  @track dataMode = '本地演示数据';

  connectedCallback() {
      this.role = this.defaultRole || 'counselor';
      this.loadData();
  }

  get shellClass() { return `workbench page-${this.page} theme-${this.theme || 'light'}`; }
  get roleName() { return ROLE_NAMES[this.role] || ROLE_NAMES.counselor; }
  get userName() { return ROLE_USERS[this.role] || ROLE_USERS.counselor; }
  get userInitial() { return this.userName.slice(0, 1); }
  get statusText() { return this.loading ? '同步中' : this.errorMessage ? '使用本地数据' : this.dataMode; }

  get navGroups() {
      return [
          { group: '首页', items: [{ id: 'home', label: '首页', className: this.navClass('home') }] },
          { group: '数据看板', items: [
              { id: 'dashboard', label: '风险图表', className: this.navClass('dashboard') },
              { id: 'students', label: '学生画像', className: this.navClass('students') },
              { id: 'courses', label: '课程成绩', className: this.navClass('courses') },
              { id: 'behavior', label: '学习行为', className: this.navClass('behavior') }
          ] },
          { group: '帮扶闭环', items: [
              { id: 'warnings', label: '风险预警单', className: this.navClass('warnings') },
              { id: 'agent', label: 'AI 成长助手', className: this.navClass('agent') },
              { id: 'workflow', label: '处理闭环', className: this.navClass('workflow') }
          ] },
          { group: '组织管理', items: [
              { id: 'users', label: '用户中心', className: this.navClass('users') },
              { id: 'settings', label: '系统设置', className: this.navClass('settings') }
          ] }
      ];
  }

  get pageMeta() {
      const meta = {
          home: { kicker: 'CampusPilot 首页', heading: '启航智伴 CampusPilot', subtitle: '面向学生、辅导员、导师和学院管理者的学业风险预警与帮扶工作台。' },
          dashboard: { kicker: '数据看板', heading: this.pageTitle, subtitle: '集中查看风险分布、趋势变化、处理成效和今日重点预警。' },
          students: { kicker: '学生画像', heading: '学生画像中心', subtitle: '沉淀基础信息、成长目标、学业表现、学习行为与风险状态。' },
          courses: { kicker: '课程成绩', heading: '课程成绩分析', subtitle: '定位薄弱课程和补强建议，为导师帮扶提供依据。' },
          behavior: { kicker: '学习行为', heading: '学习行为监测', subtitle: '关注出勤、作业、活跃度和互动情况。' },
          warnings: { kicker: '风险预警', heading: '风险预警单管理', subtitle: '承载风险原因、处理状态、辅导员意见、导师措施和学生反馈。' },
          agent: { kicker: '智能助手', heading: 'AI 成长助手', subtitle: '围绕画像、课程、行为和预警单进行问答与帮扶建议生成。' },
          workflow: { kicker: '处理闭环', heading: '风险处理过程展示', subtitle: '展示从风险识别、预警生成、辅导员确认、导师帮扶到复评结案的链路。' },
          users: { kicker: '用户中心', heading: '角色与协同', subtitle: '按学生、辅导员、导师和学院管理者拆分职责。' },
          settings: { kicker: '系统设置', heading: '基础配置', subtitle: '维护预警阈值、通知策略和演示数据状态。' }
      };
      return meta[this.page] || meta.home;
  }

  get pageKicker() { return this.pageMeta.kicker; }
  get pageHeading() { return this.pageMeta.heading; }
  get pageSubtitle() { return this.pageMeta.subtitle; }
  get isHome() { return this.page === 'home'; }
  get isDashboard() { return this.page === 'dashboard'; }
  get isStudents() { return this.page === 'students'; }
  get isCourses() { return this.page === 'courses'; }
  get isBehavior() { return this.page === 'behavior'; }
  get isWarnings() { return this.page === 'warnings'; }
  get isAgent() { return this.page === 'agent'; }
  get isWorkflow() { return this.page === 'workflow'; }
  get isUsers() { return this.page === 'users'; }
  get isSettings() { return this.page === 'settings'; }

  get metrics() {
      return [
          { label: '学生画像', value: this.overview.totalStudents || this.students.length, note: '覆盖画像、成绩、行为', className: 'metric' },
          { label: '高风险', value: this.overview.highRisk || 0, note: '需优先确认', className: 'metric danger' },
          { label: '待确认', value: this.overview.pendingWarnings || 0, note: `${this.roleName}当前待办`, className: 'metric warning' },
          { label: '平均出勤', value: this.percent(this.overview.averageAttendance), note: '学习行为监测', className: 'metric success' }
      ];
  }

  get homeCards() {
      return [
          { title: '学生端', value: '成长画像', text: '查看成绩、行为、建议和反馈记录。' },
          { title: '辅导员端', value: '风险处置', text: '确认预警单，跟踪学生帮扶进度。' },
          { title: '导师端', value: '课程补强', text: '定位薄弱课程并制定补强计划。' },
          { title: '管理端', value: '整体看板', text: '查看风险分布、闭环效率和帮扶成效。' }
      ];
  }

  get riskChartRows() {
      const total = Math.max(1, this.riskDistribution.reduce((sum, item) => sum + (Number(item.value) || 0), 0));
      return this.riskDistribution.map((item) => ({
          ...item,
          percent: Math.round(((Number(item.value) || 0) / total) * 100),
          barStyle: this.barStyle(Math.round(((Number(item.value) || 0) / total) * 100), item.color || '#1e6be6')
      }));
  }

  get trendRows() {
      return this.riskTrend.map((item) => ({
          ...item,
          highStyle: `--bar-height:${Math.max(10, (Number(item.high) || 0) * 26)}px;--bar-color:#dc3f3a;`,
          watchStyle: `--bar-height:${Math.max(10, (Number(item.watch) || 0) * 26)}px;--bar-color:#d68519;`,
          normalStyle: `--bar-height:${Math.max(10, (Number(item.normal) || 0) * 26)}px;--bar-color:#1e6be6;`
      }));
  }

  get selectedStudent() {
      return this.students.find((student) => student.no === this.selectedStudentNo) || this.students[0] || FALLBACK.students[0];
  }

  get selectedStudentRiskClass() {
      return this.riskClass(this.selectedStudent.riskKey, this.selectedStudent.riskLevel);
  }

  get selectedWarning() {
      return this.warnings.find((warning) => warning.code === this.selectedWarningCode) || this.warnings[0] || FALLBACK.warnings[0];
  }

  get studentCards() {
      return this.students.map((student) => ({
          ...student,
          rowClass: student.no === this.selectedStudentNo ? 'student-row active' : 'student-row',
          riskClass: this.riskClass(student.riskKey, student.riskLevel)
      }));
  }

  get warningRows() {
      return this.warnings.map((warning) => ({
          ...warning,
          rowClass: warning.code === this.selectedWarningCode ? 'warning-row active' : 'warning-row',
          riskClass: this.riskClass(warning.riskKey, warning.level),
          statusClass: this.statusClass(warning.statusKey, warning.status)
      }));
  }

  get selectedStudentMetrics() {
      const s = this.selectedStudent;
      return [
          { label: '风险分数', value: s.riskScore || '-' },
          { label: 'GPA', value: s.gpa || '-' },
          { label: '挂科数', value: this.present(s.failed) },
          { label: '出勤率', value: this.percent(s.attendance) },
          { label: '作业完成率', value: this.percent(s.assignment) }
      ];
  }

  get evidenceBars() {
      const s = this.selectedStudent;
      return [
          { label: 'GPA', value: Math.round(((Number(s.gpa) || 0) / 4) * 100), text: s.gpa || '-', barStyle: this.barStyle(Math.round(((Number(s.gpa) || 0) / 4) * 100), '#1e6be6') },
          { label: '出勤率', value: Number(s.attendance) || 0, text: this.percent(s.attendance), barStyle: this.barStyle(Number(s.attendance) || 0, '#dc3f3a') },
          { label: '作业完成率', value: Number(s.assignment) || 0, text: this.percent(s.assignment), barStyle: this.barStyle(Number(s.assignment) || 0, '#15915f') }
      ];
  }

  get courseRows() {
      return this.courses.map((course) => ({
          ...course,
          scoreStyle: this.barStyle(Number(course.score) || 0, Number(course.score) < 60 ? '#dc3f3a' : Number(course.score) < 75 ? '#d68519' : '#15915f')
      }));
  }

  get behaviorRows() {
      return this.behaviors.map((item) => ({
          ...item,
          attendanceStyle: this.barStyle(Number(item.attendance) || 0, '#1e6be6'),
          assignmentStyle: this.barStyle(Number(item.assignment) || 0, '#15915f'),
          activityStyle: this.barStyle(Number(item.activity) || 0, '#d68519')
      }));
  }

  get workflowCards() {
      return this.workflow.map((step, index) => ({ ...step, order: `0${index + 1}` }));
  }

  get toolRows() {
      const {tools} = this.agentWorkflow;
      return Array.isArray(tools) ? tools : FALLBACK.agentWorkflow.tools;
  }

  get agentReservedTools() {
      return [
          { name: 'query_student_profile', status: '已预留' },
          { name: 'query_course_score', status: '已预留' },
          { name: 'query_learning_behavior', status: '已预留' },
          { name: 'query_warning_order', status: '已预留' },
          { name: 'create_warning_order', status: '已预留' },
          { name: 'generate_growth_plan', status: '已预留' }
      ];
  }
  get settingRows() {
      return [
          { label: '高风险阈值', value: '80 分及以上', note: '进入重点帮扶' },
          { label: '关注阈值', value: '60-79 分', note: '辅导员确认' },
          { label: '复评周期', value: '14 天', note: '到期自动提醒' },
          { label: '数据状态', value: this.statusText, note: '接口不可用时自动兜底' }
      ];
  }

  navClass(page) {
      return this.page === page ? 'nav-item active' : 'nav-item';
  }

  handleNavigate(event) {
      this.page = event.currentTarget.dataset.page;
  }

  handleRoleChange(event) {
      this.role = event.target.value;
      this.loadData();
  }

  handleRefresh() {
      this.loadData();
  }

  handleSelectStudent(event) {
      this.selectedStudentNo = event.currentTarget.dataset.no;
  }

  handleSelectWarning(event) {
      this.selectedWarningCode = event.currentTarget.dataset.code;
  }

  handleQuestionInput(event) {
      this.question = event.target.value;
  }

  async handleAskAgent() {
      if (!this.enableAgent) {
          this.agentAnswer = '当前演示未启用远程问答，已展示本地结构化建议：先确认预警单，再由导师制定课程补强计划，并约定复评时间。';
          return;
      }
      const response = await this.postJson('/api/campuspilot/agent/chat', { question: this.question, role: this.roleName });
      this.agentAnswer = response.answer || '建议先确认当前预警单，再由导师制定课程补强计划，并约定复评时间。';
  }

  async handleCreateSuggestion() {
      const response = await this.postJson('/api/campuspilot/warnings/suggest', {});
      if (response && response.code) {
          this.warnings = [response, ...this.warnings];
          this.selectedWarningCode = response.code;
          this.page = 'warnings';
      }
  }

  async loadData() {
      this.loading = true;
      this.errorMessage = '';
      try {
          const [overview, riskDistribution, students, courses, behaviors, warnings, workflow, riskTrend, effectiveness, agentWorkflow, roles] = await Promise.all([
              this.getJson('/api/campuspilot/overview'),
              this.getJson('/api/campuspilot/risk-distribution'),
              this.getJson('/api/campuspilot/students'),
              this.getJson('/api/campuspilot/courses'),
              this.getJson('/api/campuspilot/behaviors'),
              this.getJson('/api/campuspilot/warnings'),
              this.getJson('/api/campuspilot/workflow'),
              this.getJson('/api/campuspilot/risk-trend'),
              this.getJson('/api/campuspilot/effectiveness'),
              this.getJson('/api/campuspilot/agent-workflow'),
              this.getJson('/api/campuspilot/roles')
          ]);
          if (overview) {this.overview = overview;}
          if (Array.isArray(riskDistribution) && riskDistribution.length) {this.riskDistribution = riskDistribution;}
          if (Array.isArray(students) && students.length) {this.students = students;}
          if (Array.isArray(courses) && courses.length) {this.courses = courses;}
          if (Array.isArray(behaviors) && behaviors.length) {this.behaviors = behaviors;}
          if (Array.isArray(warnings) && warnings.length) {this.warnings = warnings;}
          if (Array.isArray(workflow) && workflow.length) {this.workflow = workflow;}
          if (Array.isArray(riskTrend) && riskTrend.length) {this.riskTrend = riskTrend;}
          if (effectiveness) {this.effectiveness = effectiveness;}
          if (agentWorkflow) {this.agentWorkflow = agentWorkflow;}
          if (Array.isArray(roles) && roles.length) {this.roles = roles;}
          this.dataMode = '后端数据已同步';
          this.ensureSelection();
      } catch {
          this.errorMessage = '后端暂不可用，已使用内置演示数据。';
          this.dataMode = '本地演示数据';
      } finally {
          this.loading = false;
      }
  }

  ensureSelection() {
      if (!this.students.some((student) => student.no === this.selectedStudentNo) && this.students[0]) {
          this.selectedStudentNo = this.students[0].no;
      }
      if (!this.warnings.some((warning) => warning.code === this.selectedWarningCode) && this.warnings[0]) {
          this.selectedWarningCode = this.warnings[0].code;
      }
  }

  async getJson(path) {
      try {
          const response = await fetch(`${this.apiBase}${path}`, { headers: this.headers() });
          if (!response.ok) {return null;}
          return response.json();
      } catch {
          return null;
      }
  }

  async postJson(path, body) {
      try {
          const response = await fetch(`${this.apiBase}${path}`, {
              method: 'POST',
              headers: this.headers(true),
              body: JSON.stringify(body)
          });
          if (!response.ok) {return {};}
          return response.json();
      } catch {
          return {};
      }
  }

  headers(withBody = false) {
      const headers = {
          Accept: 'application/json',
          'X-CampusPilot-Role-Key': this.role,
          'X-CampusPilot-User': encodeURIComponent(this.userName)
      };
      if (withBody) {headers['Content-Type'] = 'application/json';}
      return headers;
  }

  riskClass(key, label) {
      if (key === 'high' || label === '高风险') {return 'pill risk-high';}
      if (key === 'watch' || label === '需要关注') {return 'pill risk-watch';}
      if (key === 'improved' || label === '改善中') {return 'pill risk-improved';}
      return 'pill risk-normal';
  }

  statusClass(key, label) {
      if (key === 'active' || label === '帮扶中') {return 'pill status-active';}
      if (key === 'done' || label === '已结案') {return 'pill status-done';}
      if (label === '新建') {return 'pill status-new';}
      return 'pill status-todo';
  }

  barStyle(value, color) {
      const width = Math.max(0, Math.min(100, Number(value) || 0));
      return `--bar-width:${width}%;--bar-color:${color};`;
  }

  percent(value) {
      if (value === undefined || value === null || value === '') {return '-';}
      const text = String(value);
      return text.includes('%') ? text : `${text}%`;
  }

  present(value) {
      if (value === undefined || value === null || value === '') {return '-';}
      return value;
  }
}
