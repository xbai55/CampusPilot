import React, { useState, useEffect, useRef } from "react";
import { Link } from "react-router-dom";
import { useAuth } from "../../context/AuthContext";
import { scopeStudents, scopeWarnings } from "../../utils/permissions";

function riskClass(key) {
  return (
    { high: "high", watch: "watch", normal: "normal", improved: "improved" }[
      key
    ] || "normal"
  );
}

function riskLabel(key) {
  return (
    { high: "高风险", watch: "需关注", normal: "正常", improved: "改善中" }[
      key
    ] || "正常"
  );
}

function trendPoints(values) {
  const max = Math.max(...values, 1);
  const min = Math.min(...values, 0);
  const span = Math.max(max - min, 1);
  return values
    .map((value, index) => {
      const x = 22 + index * (236 / Math.max(values.length - 1, 1));
      const y = 138 - ((value - min) / span) * 98;
      return x + "," + y;
    })
    .join(" ");
}

export default function Dashboard() {
  const { data, user, loading } = useAuth();
  const [searchQuery, setSearchQuery] = useState("");
  const donutRef = useRef(null);

  useEffect(() => {
    const handler = (e) => setSearchQuery(e.detail);
    window.addEventListener("search", handler);
    return () => window.removeEventListener("search", handler);
  }, []);

  const matchQuery = (item) => {
    if (!searchQuery.trim()) return true;
    return Object.values(item)
      .join(" ")
      .toLowerCase()
      .includes(searchQuery.trim().toLowerCase());
  };

  useEffect(() => {
    if (!data) return;
    if (donutRef.current) {
      donutRef.current.distribution = data.riskDistribution;
    }
  });

  if (loading || !data) {
    return (
      <div className="agent-main" style={{ padding: 40 }}>
        <strong>加载数据中...</strong>
      </div>
    );
  }

  const students = scopeStudents(data.students, user);
  const warnings = scopeWarnings(data.warnings, user);
  const visibleWarnings = warnings.filter(matchQuery);
  const priorityStudents = [...students]
    .sort((a, b) => b.riskScore - a.riskScore)
    .slice(0, 3);
  const focusStudent = priorityStudents[0] || students[0];
  const highRiskCount = students.filter((s) => s.riskKey === "high").length;
  const forecastValues = [1, 2, 2, 3, 3, 2, 2];
  const forecastPath = trendPoints(forecastValues);
  const workflowSteps = [
    { name: "发现风险", value: 12, tone: "blue" },
    { name: "AI分析", value: 10, tone: "violet" },
    { name: "处理中", value: 5, tone: "orange" },
    { name: "效果反馈", value: 8, tone: "green" },
    { name: "完成闭环", value: 8, tone: "slate" },
  ];

  return (
    <>
      <section className="ai-hero-panel">
        <div className="ai-hero-copy">
          <p className="eyebrow">CampusPilot Intelligence</p>
          <h2>AI学业风险驾驶舱</h2>
          <p>
            实时监测学生状态，基于多维学习数据进行风险预测、智能分析与精准帮扶。
          </p>
        </div>
        <div className="ai-engine-card" aria-label="AI运行状态">
          <div>
            <span className="ai-orb" aria-hidden="true"></span>
            <strong>AI Engine</strong>
            <em>在线运行</em>
          </div>
          <p>今日分析</p>
          <b>328条学习行为数据</b>
          <span>生成15条风险预警</span>
        </div>
      </section>

      <section className="ai-metric-grid" aria-label="AI决策指标">
        <div className="ai-metric-card risk-high">
          <span>当前风险学生</span>
          <strong>{highRiskCount}人</strong>
          <em>高风险</em>
          <b>↑ 较昨日增加</b>
        </div>
        <div className="ai-metric-card risk-ai">
          <span>AI预警数量</span>
          <strong>15条</strong>
          <em>本周</em>
          <b>↗ 持续生成</b>
        </div>
        <div className="ai-metric-card risk-success">
          <span>已完成帮扶</span>
          <strong>8人</strong>
          <em>闭环完成率80%</em>
          <b>✓ 效果反馈完成</b>
        </div>
        <div className="ai-metric-card risk-score">
          <span>平均风险评分</span>
          <strong>36.5</strong>
          <em>AI综合评分</em>
          <b>↓ 风险趋稳</b>
        </div>
      </section>

      <section className="ai-dashboard-grid">
        <main className="ai-dashboard-main">
          <section className="ai-glass-card risk-intelligence-card">
            <div className="ai-section-head">
              <div>
                <p className="eyebrow">Risk Intelligence</p>
                <h3>核心风险分布与预测</h3>
              </div>
              <span className="ai-chip">AI预测模型已更新</span>
            </div>
            <div className="risk-intelligence-layout">
              <div className="risk-donut-zone">
                <cp-donut-chart ref={donutRef}></cp-donut-chart>
              </div>
              <div className="risk-legend-stack">
                {data.riskDistribution.map((item) => {
                  const pct = Math.round((item.value / Math.max(students.length, 1)) * 100);
                  return (
                    <div key={item.key} className={"risk-legend-item " + riskClass(item.key)}>
                      <span>{riskLabel(item.key)}</span>
                      <strong>{pct}%</strong>
                    </div>
                  );
                })}
              </div>
              <div className="forecast-panel">
                <div className="forecast-head">
                  <span>未来7天风险趋势预测</span>
                  <b>风险人数变化趋势</b>
                </div>
                <svg className="forecast-line" viewBox="0 0 280 160" role="img" aria-label="未来7天风险趋势预测折线图">
                  <defs>
                    <linearGradient id="forecastGradient" x1="0" x2="1" y1="0" y2="0">
                      <stop offset="0%" stopColor="#2563eb" />
                      <stop offset="100%" stopColor="#8b5cf6" />
                    </linearGradient>
                  </defs>
                  <path d="M22 138 H258" className="forecast-axis" />
                  <path d="M22 104 H258" className="forecast-grid" />
                  <path d="M22 70 H258" className="forecast-grid" />
                  <polyline points={forecastPath} className="forecast-polyline" />
                  {forecastValues.map((value, index) => {
                    const max = Math.max(...forecastValues, 1);
                    const min = Math.min(...forecastValues, 0);
                    const span = Math.max(max - min, 1);
                    const x = 22 + index * (236 / Math.max(forecastValues.length - 1, 1));
                    const y = 138 - ((value - min) / span) * 98;
                    return <circle key={index} cx={x} cy={y} r="4.5" className="forecast-dot" />;
                  })}
                </svg>
                <div className="forecast-days">
                  {["D1", "D2", "D3", "D4", "D5", "D6", "D7"].map((d) => <span key={d}>{d}</span>)}
                </div>
              </div>
            </div>
          </section>

          <section className="student-ai-section">
            <div className="ai-risk-profile-card ai-glass-card">
              <div className="student-profile-head">
                <div>
                  <p className="eyebrow">重点学生</p>
                  <h3>{focusStudent.name}</h3>
                </div>
                <strong className={"risk-score-badge " + riskClass(focusStudent.riskKey)}>{focusStudent.riskScore}</strong>
              </div>
              <div className="risk-source-list">
                <span>高等数学成绩下降35%</span>
                <span>连续4周缺勤</span>
                <span>学习活跃度下降</span>
              </div>
              <div className="ai-analysis-box">
                <b>AI分析</b>
                <p>预计未来两周存在挂科风险，需要尽快进入导师沟通与专项学习计划。</p>
              </div>
              <div className="recommendation-tags">
                <span>安排导师沟通</span>
                <span>制定专项学习计划</span>
                <span>两周后重新评估</span>
              </div>
              <div className="profile-actions">
                <Link className="primary-link" to="/students">查看学生画像</Link>
                <Link className="smart-button" to="/agent">进入AI帮扶</Link>
              </div>
            </div>

            <div className="quick-profile-card ai-glass-card">
              <div className="profile-avatar">张</div>
              <div>
                <p className="eyebrow">学生AI画像</p>
                <h3>张明远</h3>
                <span className="ai-chip">AI综合画像</span>
              </div>
              <div className="profile-field">
                <span>学习能力</span>
                <strong>★★★☆☆</strong>
              </div>
              <div className="profile-field">
                <span>风险因素</span>
                <strong>数学成绩 · 出勤情况</strong>
              </div>
              <div className="profile-field">
                <span>优势</span>
                <strong>编程能力较强</strong>
              </div>
              <p>AI建议：优化学习时间安排，优先补齐高等数学与数据结构短板。</p>
            </div>
          </section>

          <section className="ai-bottom-grid">
            <div className="ai-glass-card workflow-card">
              <div className="ai-section-head">
                <div>
                  <p className="eyebrow">风险闭环流程</p>
                  <h3>从发现到完成闭环</h3>
                </div>
              </div>
              <div className="workflow-flow">
                {workflowSteps.map((step, index) => (
                  <React.Fragment key={step.name}>
                    <div className={"workflow-node " + step.tone}>
                      <span>{step.name}</span>
                      <strong>{step.value}</strong>
                    </div>
                    {index < workflowSteps.length - 1 && <i className="workflow-arrow">↓</i>}
                  </React.Fragment>
                ))}
              </div>
            </div>

            <div className="ai-glass-card ticket-card">
              <div className="ai-section-head">
                <div>
                  <p className="eyebrow">近期预警单</p>
                  <h3>企业工单视图</h3>
                </div>
                <Link className="text-button" to="/warnings">进入管理</Link>
              </div>
              <div className="ticket-list">
                {visibleWarnings.slice(0, 3).map((w) => (
                  <Link key={w.code} to="/warnings" className="warning-ticket">
                    <span><b>编号</b><strong>{w.code}</strong></span>
                    <span><b>学生</b><strong>{w.student}</strong></span>
                    <span><b>风险等级</b><cp-risk-pill level={w.level}></cp-risk-pill></span>
                    <span><b>AI原因</b><em>成绩下降+缺勤</em></span>
                    <span><b>负责人</b><em>辅导员</em></span>
                    <span><b>状态</b><cp-status-pill status={w.status} status-key={w.statusKey}></cp-status-pill></span>
                  </Link>
                ))}
              </div>
            </div>
          </section>
        </main>

        <aside className="ai-copilot-rail" aria-label="AI风险助手">
          <div className="ai-copilot-card">
            <div className="copilot-visual" aria-hidden="true">
              <span></span>
            </div>
            <p className="eyebrow">AI Copilot</p>
            <h3>AI风险助手</h3>
            <div className="copilot-message">
              <b>我发现：</b>
              <p>张明远近期存在：① 成绩下降趋势 ② 出勤异常 ③ 学习行为减少。</p>
            </div>
            <div className="copilot-judgement">
              <span>风险判断</span>
              <strong>高概率进入学业预警</strong>
            </div>
            <div className="copilot-actions-list">
              <b>推荐行动</b>
              <span>1. 联系导师进行沟通</span>
              <span>2. 制定个性化学习计划</span>
              <span>3. 持续跟踪两周</span>
            </div>
            <div className="copilot-buttons">
              <Link className="smart-button primary" to="/workflow">执行帮扶计划</Link>
              <Link className="smart-button" to="/agent">进入AI对话</Link>
            </div>
          </div>
        </aside>
      </section>
    </>
  );
}
