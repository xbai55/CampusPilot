import { KingdeeElement, api, track } from '@kdcloudjs/kwc';

const ROLES = [
    { id: 'counselor', label: '辅导员' },
    { id: 'mentor', label: '导师' },
    { id: 'manager', label: '学院管理者' },
    { id: 'student', label: '学生' }
];

const NAV_ITEMS = [
    { id: 'dashboard', label: '驾驶舱' },
    { id: 'students', label: '学生画像' },
    { id: 'warnings', label: '预警闭环' },
    { id: 'agent', label: 'Agent 分析' },
    { id: 'integration', label: '平台接入' }
];

const FALLBACK = {
    overview: {
        highRisk: 8,
        followRisk: 17,
        openWarnings: 12,
        closedWarnings: 31,
        agentMode: 'local-fallback',
        improvementRate: '73%'
    },
    students: [
        {
            id: 'S20260201',
            name: '张明远',
            major: '计算机科学与技术',
            grade: '2024级',
            riskLevel: '高风险',
            riskClass: 'danger',
            score: 86,
            gpa: '2.31',
            attendance: '71%',
            homework: '64%',
            active: '低',
            target: '补齐数据结构与高等数学短板',
            reason: '连续三周作业缺交，核心课成绩低于专业均值。'
        },
        {
            id: 'S20260218',
            name: '李若溪',
            major: '软件工程',
            grade: '2025级',
            riskLevel: '关注',
            riskClass: 'warning',
            score: 63,
            gpa: '2.82',
            attendance: '84%',
            homework: '78%',
            active: '中',
            target: '稳定出勤并提升英语四级训练频率',
            reason: '近期晚归和课堂互动下降，需要辅导员跟进。'
        },
        {
            id: 'S20260112',
            name: '陈知行',
            major: '人工智能',
            grade: '2024级',
            riskLevel: '正常',
            riskClass: 'safe',
            score: 28,
            gpa: '3.56',
            attendance: '96%',
            homework: '93%',
            active: '高',
            target: '参与科研训练营并冲刺竞赛',
            reason: '表现稳定，可进入成长规划推荐。'
        }
    ],
    courses: [
        { name: '数据结构', passRate: '82%', weakPoint: '树与图算法', trend: '下降' },
        { name: '高等数学', passRate: '76%', weakPoint: '多元函数微分', trend: '波动' },
        { name: '大学英语', passRate: '88%', weakPoint: '听力训练', trend: '稳定' }
    ],
    behaviors: [
        { name: '课堂出勤', value: '84%', state: '较上周 +3%' },
        { name: '作业完成', value: '79%', state: '仍需关注' },
        { name: '平台活跃', value: '68%', state: '晚间活跃偏低' }
    ],
    warnings: [
        {
            id: 'RW-202607-001',
            student: '张明远',
            level: '高风险',
            stage: '辅导员确认',
            owner: '王老师',
            action: '核实课程缺勤与作业缺交原因',
            next: '导师帮扶'
        },
        {
            id: 'RW-202607-002',
            student: '李若溪',
            level: '关注',
            stage: '导师帮扶',
            owner: '周导师',
            action: '制定英语听力与晚自习计划',
            next: '学生反馈'
        },
        {
            id: 'RW-202607-003',
            student: '赵一宁',
            level: '改善',
            stage: '复评结案',
            owner: '学院教务',
            action: '复核近两周成绩和行为改善情况',
            next: '结案归档'
        }
    ]
};

const WORKFLOW = ['Agent 分析', '生成预警单', '辅导员确认', '导师帮扶', '学生反馈', '复评结案'];

export default class App extends KingdeeElement {
    @api pageTitle = '启航智伴 CampusPilot';
    @api apiBase = '';
    @api defaultRole = 'counselor';
    @api enableAgent = false;
    @api theme = 'light';

    @track activePage = 'dashboard';
    @track role = 'counselor';
    @track overview = FALLBACK.overview;
    @track students = FALLBACK.students;
    @track courses = FALLBACK.courses;
    @track behaviors = FALLBACK.behaviors;
    @track warnings = FALLBACK.warnings;
    @track agentQuestion = '张明远为什么是高风险？';
    @track agentAnswer = 'Agent 分析结果会显示在这里。未配置远程服务时，组件使用本地演示数据生成兜底说明。';
    @track loading = false;
    @track notice = '官方 KWC 模板已就绪，可通过 apiBase 接入 Java 后端业务接口。';

    connectedCallback() {
        this.role = this.defaultRole || 'counselor';
        this.loadRemoteData();
    }

    get shellClass() {
        return `campus-shell theme-${this.theme || 'light'}`;
    }

    get roleOptions() {
        return ROLES.map((item) => ({
            ...item,
            className: item.id === this.role ? 'segmented-item active' : 'segmented-item'
        }));
    }

    get navItems() {
        return NAV_ITEMS.map((item) => ({
            ...item,
            className: item.id === this.activePage ? 'nav-button active' : 'nav-button'
        }));
    }

    get isDashboard() { return this.activePage === 'dashboard'; }
    get isStudents() { return this.activePage === 'students'; }
    get isWarnings() { return this.activePage === 'warnings'; }
    get isAgent() { return this.activePage === 'agent'; }
    get isIntegration() { return this.activePage === 'integration'; }

    get metrics() {
        return [
            { id: 'high', label: '高风险学生', value: this.overview.highRisk, note: '需当天确认' },
            { id: 'follow', label: '关注学生', value: this.overview.followRisk, note: '持续观察' },
            { id: 'open', label: '处理中预警', value: this.overview.openWarnings, note: '跨角色流转' },
            { id: 'close', label: '帮扶改善率', value: this.overview.improvementRate, note: '复评结案口径' }
        ];
    }

    get workflowSteps() {
        return WORKFLOW.map((name, index) => ({
            id: name,
            name,
            stepNo: index + 1,
            className: index < 3 ? 'step done' : 'step'
        }));
    }

    get integrationChecklist() {
        const hasApi = Boolean(this.apiBase);
        return [
            { id: 'token', label: 'AccessToken 认证', state: '平台侧配置', detail: '第三方应用、代理用户和授权策略在金蝶环境维护。' },
            { id: 'api', label: 'Java 业务接口', state: hasApi ? '已配置 apiBase' : '等待 apiBase', detail: hasApi ? this.apiBase : '示例：http://127.0.0.1:8787/api/campuspilot' },
            { id: 'objects', label: '低代码对象', state: '待平台建模', detail: '学生画像、课程成绩、学习行为、风险预警单、处理日志。' },
            { id: 'debug', label: '远程调试', state: '可接入', detail: '部署元数据后，在远端页面追加 kdcus_cdn 指向本地静态服务。' }
        ];
    }

    handleNavigate(event) {
        this.activePage = event.currentTarget.dataset.page;
    }

    handleRoleChange(event) {
        this.role = event.currentTarget.dataset.role;
        this.notice = `已切换到${event.currentTarget.textContent.trim()}视角。`;
    }

    handleQuestionInput(event) {
        this.agentQuestion = event.target.value;
    }

    async handleRefresh() {
        await this.loadRemoteData(true);
    }

    handleAdvanceWarning(event) {
        const warningId = event.currentTarget.dataset.id;
        this.warnings = this.warnings.map((warning) => {
            if (warning.id !== warningId) {
                return warning;
            }
            return {
                ...warning,
                stage: warning.next,
                next: warning.next === '结案归档' ? '已完成' : '学生反馈',
                action: `已由${this.currentRoleLabel()}推进处理`
            };
        });
        this.notice = `${warningId} 已推进到下一流程节点。`;
    }

    handleCreateSuggestion() {
        const firstStudent = this.students[0];
        const exists = this.warnings.some((warning) => warning.id === 'RW-202607-NEW');
        if (exists) {
            this.notice = '演示预警单已存在，可继续推进流程。';
            return;
        }
        this.warnings = [
            {
                id: 'RW-202607-NEW',
                student: firstStudent.name,
                level: firstStudent.riskLevel,
                stage: '生成预警单',
                owner: '系统 Agent',
                action: firstStudent.reason,
                next: '辅导员确认'
            },
            ...this.warnings
        ];
        this.notice = '已根据学生画像生成一条演示预警单。';
    }

    async handleAskAgent() {
        const question = this.agentQuestion.trim();
        if (!question) {
            this.notice = '请先输入要询问 Agent 的问题。';
            return;
        }
        this.loading = true;
        try {
            if (this.apiBase && this.enableAgent) {
                const response = await fetch(`${this.apiBase}/agent/chat`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ question, role: this.role })
                });
                if (response.ok) {
                    const payload = await response.json();
                    this.agentAnswer = payload.answer || payload.message || this.localAgentAnswer(question);
                    this.notice = '已从 apiBase 获取 Agent 分析结果。';
                    return;
                }
            }
            this.agentAnswer = this.localAgentAnswer(question);
            this.notice = '当前使用本地兜底分析。接入平台后可由 Java 后端代理金蝶 Agent。';
        } catch {
            this.agentAnswer = this.localAgentAnswer(question);
            this.notice = '远程 Agent 暂不可用，已切换到本地演示分析。';
        } finally {
            this.loading = false;
        }
    }

    async loadRemoteData(showNotice = false) {
        if (!this.apiBase) {
            if (showNotice) {
                this.notice = '未配置 apiBase，继续使用组件内置演示数据。';
            }
            return;
        }
        try {
            const [overview, students, warnings] = await Promise.all([
                this.fetchJson('/overview'),
                this.fetchJson('/students'),
                this.fetchJson('/warnings')
            ]);
            if (overview) {
                this.overview = { ...this.overview, ...overview };
            }
            if (Array.isArray(students) && students.length) {
                this.students = students.map((student) => this.normalizeStudent(student));
            }
            if (Array.isArray(warnings) && warnings.length) {
                this.warnings = warnings.map((warning, index) => this.normalizeWarning(warning, index));
            }
            if (showNotice) {
                this.notice = '已从 Java 后端刷新业务数据。';
            }
        } catch {
            if (showNotice) {
                this.notice = '后端暂不可达，保留本地演示数据。';
            }
        }
    }

    async fetchJson(path) {
        const response = await fetch(`${this.apiBase}${path}`);
        if (!response.ok) {
            throw new Error(`Request failed: ${path}`);
        }
        return response.json();
    }

    normalizeStudent(student) {
        const score = Number(student.riskScore || student.score || 0);
        const riskLevel = student.riskLevel || (score >= 80 ? '高风险' : score >= 50 ? '关注' : '正常');
        return {
            id: student.id || student.studentId || student.code,
            name: student.name || student.studentName,
            major: student.major || '未配置专业',
            grade: student.grade || '未配置年级',
            riskLevel,
            riskClass: riskLevel === '高风险' ? 'danger' : riskLevel === '关注' ? 'warning' : 'safe',
            score,
            gpa: student.gpa || '-',
            attendance: student.attendance || student.attendanceRate || '-',
            homework: student.homework || student.homeworkRate || '-',
            active: student.active || student.activityLevel || '-',
            target: student.target || student.goal || '等待生成成长目标',
            reason: student.reason || student.riskReason || '等待 Agent 生成风险解释'
        };
    }

    normalizeWarning(warning, index) {
        return {
            id: warning.id || warning.warningId || `RW-REMOTE-${index + 1}`,
            student: warning.student || warning.studentName || '未命名学生',
            level: warning.level || warning.riskLevel || '关注',
            stage: warning.stage || warning.status || '辅导员确认',
            owner: warning.owner || warning.assignee || '待分派',
            action: warning.action || warning.reason || '等待处理意见',
            next: warning.next || '导师帮扶'
        };
    }

    localAgentAnswer(question) {
        const student = this.students[0];
        return `问题：${question}\n分析：${student.name} 当前风险分为 ${student.score}，主要原因是${student.reason}\n建议：先由辅导员确认真实困难，再由导师制定课程补救计划，最后通过学生反馈和复评结案形成闭环。`;
    }

    currentRoleLabel() {
        const role = ROLES.find((item) => item.id === this.role);
        return role ? role.label : '当前角色';
    }
}
