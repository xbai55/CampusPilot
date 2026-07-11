import React from "react";
import { Link } from "react-router-dom";
import { useAuth } from "../../context/AuthContext";
import { scopeStudents, scopeWarnings } from "../../utils/permissions";

export default function AdminHome() {
  const { data, user } = useAuth();
  if (!data) return null;

  const students = scopeStudents(data.students, user);
  const warnings = scopeWarnings(data.warnings, user);
  const targetStudent = students[0];
  const targetWarning = warnings[0];

  return (
    <>
      <section className="workbench-summary role-home-summary">
        <div className="summary-title">
          <p className="eyebrow">治理驾驶舱</p>
          <h2>查看学院风险分布、预警处理进度和系统治理配置。</h2>
          <p>系统会根据当前账号权限呈现对应任务、数据范围和可执行操作，界面保持统一的工作台体验。</p>
        </div>
        <div className="summary-actions">
          <Link className="primary-link" to="/dashboard">查看治理驾驶舱</Link>
          <Link className="text-button" to="/settings">系统配置</Link>
        </div>
      </section>
      <section className="kpi-strip" aria-label="治理驾驶舱关键指标">
        {[["可见学生", students.length, "按角色过滤"], ["可见预警", warnings.length, "按权限范围"], ["待确认", warnings.filter((w) => w.statusKey === "todo").length, "需推进"], ["帮扶中", warnings.filter((w) => w.statusKey === "active").length, "处理中"], ["已结案", warnings.filter((w) => w.statusKey === "done").length, "已沉淀"], ["工作权限", user?.role || "管理者", "当前账号"]].map(([label, value, note]) => (
          <div key={label} className="kpi-cell"><span>{label}</span><strong>{value}</strong><em>{note}</em></div>
        ))}
      </section>
      <section className="ops-board">
        <cp-panel eyebrow="治理关注点" title="今天要处理什么">
          <span slot="actions" className="role-pill">当前任务</span>
          <div className="student-queue">
            {["查看风险分布", "查看帮扶成效", "维护系统规则"].map((item, i) => (
              <div key={i} className="queue-row"><span className="rank">{i + 1}</span><span className="queue-person"><strong>{item}</strong><em>学院风险治理与系统配置。</em></span><b>{i + 1}</b></div>
            ))}
          </div>
        </cp-panel>
        <cp-panel eyebrow="当前关注对象" title={targetStudent ? targetStudent.name : "暂无可见学生"}>
          {targetStudent ? (
            <>
              {targetStudent?.riskKey && <cp-risk-pill level={targetStudent.riskLevel}></cp-risk-pill>}
              <div className="evidence-strip compact">
                {[["风险分数", targetStudent.riskScore], ["GPA", targetStudent.gpa], ["挂科", targetStudent.failed], ["出勤", `${targetStudent.attendance}%`], ["作业", `${targetStudent.assignment}%`]].map(([label, value]) => (
                  <span key={label}><em>{label}</em><strong>{value}</strong></span>
                ))}
              </div>
              <div className="agent-main"><strong>AI 建议</strong><p>{targetStudent.advice}</p></div>
            </>
          ) : (
            <div className="agent-main"><strong>暂无业务数据</strong><p>当前角色没有可见学生数据。</p></div>
          )}
          {targetWarning && (
            <div className="next-warning"><cp-status-pill status={targetWarning.status} status-key={targetWarning.statusKey}></cp-status-pill><strong>{targetWarning.code}</strong><p>{targetWarning.title}</p></div>
          )}
        </cp-panel>
      </section>
    </>
  );
}
