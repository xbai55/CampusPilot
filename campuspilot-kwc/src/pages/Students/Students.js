import React, { useState, useEffect, useMemo } from "react";
import { useAuth } from "../../context/AuthContext";
import { scopeStudents, scopeCourses, scopeBehaviors } from "../../utils/permissions";

export default function Students() {
  const { data, user } = useAuth();
  const [searchQuery, setSearchQuery] = useState("");
  const [selectedIndex, setSelectedIndex] = useState(0);

  useEffect(() => {
    const handler = (e) => setSearchQuery(e.detail);
    window.addEventListener("search", handler);
    return () => window.removeEventListener("search", handler);
  }, []);

  if (!data) return <div className="agent-main" style={{ padding: 40 }}><strong>加载中...</strong></div>;

  const matchQuery = (item) => {
    if (!searchQuery.trim()) return true;
    return Object.values(item).join(" ").toLowerCase().includes(searchQuery.trim().toLowerCase());
  };

  const students = scopeStudents(data.students, user).filter(matchQuery);
  const selected = students[Math.min(selectedIndex, students.length - 1)];

  if (!selected) {
    return (
      <cp-panel eyebrow="学生画像" title="未找到匹配学生">
        <div className="agent-main"><p>请换用学生姓名、编号、专业或风险等级继续检索。</p></div>
      </cp-panel>
    );
  }

  const courses = scopeCourses(data.courses, user).filter((c) => c.student === selected.name);
  const behavior = scopeBehaviors(data.behaviors, user).find((b) => b.student === selected.name);

  return (
    <>
      <section className="split-workspace">
        <cp-panel eyebrow="学生队列" title="按风险分数排序" className="list-panel">
          <span slot="actions" className="role-pill">{user?.role}</span>
          <div className="student-list-table">
            {students.map((s, i) => (
              <button key={s.no} className={`student-row ${i === selectedIndex ? "active" : ""}`} type="button" onClick={() => setSelectedIndex(i)}>
                <span className="avatar small">{s.name.slice(-1)}</span>
                <strong>{s.name}</strong>
                <span>{s.major}</span>
                <cp-risk-pill level={s.riskLevel}></cp-risk-pill>
                <b>{s.riskScore}</b>
              </button>
            ))}
          </div>
        </cp-panel>

        <cp-panel className="detail-panel">
          <div className="entity-top">
            <div className="person-line">
              <span className="avatar">{selected.name.slice(-2)}</span>
              <div><strong>{selected.name}</strong><div className="student-meta">{selected.no} · {selected.college} · {selected.className}</div></div>
            </div>
            <cp-risk-pill level={selected.riskLevel}></cp-risk-pill>
          </div>
          <div className="evidence-strip">
            {[["风险分数", selected.riskScore], ["GPA", selected.gpa], ["挂科数", selected.failed], ["出勤率", `${selected.attendance}%`], ["作业完成率", `${selected.assignment}%`], ["职业目标", selected.goal]].map(([label, value]) => (
              <span key={label}><em>{label}</em><strong>{value}</strong></span>
            ))}
          </div>
          <div className="detail-columns">
            <div>
              <h3>风险证据</h3>
              <p className="card-note">{selected.reason}</p>
              <cp-score-line label="GPA" value={Math.round((selected.gpa / 4) * 100)} color="#1267e8"></cp-score-line>
              <cp-score-line label="出勤率" value={selected.attendance} color={selected.attendance < 80 ? "#d43f3a" : "#24966d"}></cp-score-line>
              <cp-score-line label="作业完成率" value={selected.assignment} color={selected.assignment < 70 ? "#d43f3a" : "#24966d"}></cp-score-line>
            </div>
            <div>
              <h3>课程与行为</h3>
              <div className="compact-list">
                {courses.map((c) => (
                  <a key={c.course} href="#/courses"><strong>{c.course}</strong><span>{c.score} 分 · {c.status} · {c.advice}</span></a>
                ))}
                {behavior && (
                  <a href="#/behavior"><strong>学习行为</strong><span>出勤 {behavior.attendance}% · 作业 {behavior.assignment}% · {behavior.note}</span></a>
                )}
              </div>
            </div>
          </div>
          <div className="agent-main"><strong>AI 建议</strong><p>{selected.advice}</p></div>
        </cp-panel>
      </section>
      <cp-panel eyebrow="字段设计" title="学生画像业务对象字段">
        <span slot="actions" className="role-pill">Kingdee BOS Object</span>
        <div className="field-row">
          {["学院 / 专业 / 年级 / 班级", "职业目标 / 兴趣方向", "GPA / 学分完成率 / 挂科数", "出勤率 / 作业完成率", "风险等级 / 风险分数", "风险原因 / AI 建议 / 当前状态"].map((f) => <span key={f}>{f}</span>)}
        </div>
      </cp-panel>
    </>
  );
}
