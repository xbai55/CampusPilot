import React, { useState, useEffect } from "react";
import { useAuth } from "../../context/AuthContext";
import { scopeWarnings, canPerform } from "../../utils/permissions";
import { showToast } from "../../utils/toast";

export default function Warnings() {
  const { data, user, api, loadData } = useAuth();
  const [searchQuery, setSearchQuery] = useState("");
  const [selectedIndex, setSelectedIndex] = useState(0);
  const [confirmNote, setConfirmNote] = useState("");
  const [mentorPlan, setMentorPlan] = useState("");
  const [feedback, setFeedback] = useState("");
  const [closeNote, setCloseNote] = useState("复评通过，出勤、作业和课程补强情况达到阶段目标。");

  useEffect(() => {
    const handler = (e) => setSearchQuery(e.detail);
    window.addEventListener("search", handler);
    return () => window.removeEventListener("search", handler);
  }, []);

  if (!data) return null;

  const matchQuery = (item) => {
    if (!searchQuery.trim()) return true;
    return Object.values(item).join(" ").toLowerCase().includes(searchQuery.trim().toLowerCase());
  };

  const warnings = scopeWarnings(data.warnings, user).filter(matchQuery);
  const selected = warnings[Math.min(selectedIndex, warnings.length - 1)];

  const handleCreateWarning = async () => {
    const result = await api.createWarningSuggestion();
    const w = result.warning || result;
    showToast(`已生成预警单：${w.code || ""} ${w.student || w.student_name} · ${w.level || w.risk_level}`);
    await loadData();
  };

  const handleConfirm = async (e) => {
    e.preventDefault();
    if (!selected) return;
    await api.confirmWarning(selected.code, confirmNote || selected.counselorNote);
    showToast(`${selected.code} 已确认，辅导员意见已保存`);
    await loadData();
  };

  const handleMentorPlan = async (e) => {
    e.preventDefault();
    if (!selected) return;
    await api.saveMentorPlan(selected.code, mentorPlan || selected.mentorPlan);
    showToast(`${selected.code} 导师帮扶措施已保存`);
    await loadData();
  };

  const handleFeedback = async (e) => {
    e.preventDefault();
    if (!selected) return;
    await api.submitFeedback(selected.code, feedback || selected.studentFeedback);
    showToast(`${selected.code} 学生反馈已提交`);
    await loadData();
  };

  const handleClose = async (e) => {
    e.preventDefault();
    if (!selected) return;
    await api.closeWarning(selected.code, closeNote);
    showToast(`${selected.code} 已复评结案`);
    await loadData();
  };

  return (
    <>
      <section className="warning-workspace">
        <cp-panel
          eyebrow="预警单列表" title="按处理状态推进"
          className="list-panel"
        >
          {canPerform("createWarningSuggestion") ? <button slot="actions" className="text-button" type="button" onClick={handleCreateWarning}>生成预警单</button> : <span slot="actions" className="role-pill">{user?.role}视图</span>}
          <div className="table-wrap">
            <table className="data-table">
              <thead><tr><th>预警单</th><th>学生</th><th>风险等级</th><th>处理状态</th><th>建议处理人</th><th>复评时间</th></tr></thead>
              <tbody>
                {warnings.map((w, i) => (
                  <tr key={w.code} onClick={() => setSelectedIndex(i)} style={{ cursor: "pointer" }} className={i === selectedIndex ? "active-row" : ""}>
                    <td><strong>{w.code}</strong><br /><span className="student-meta">{w.title}</span></td>
                    <td>{w.student}</td>
                    <td><cp-risk-pill level={w.level}></cp-risk-pill></td>
                    <td><cp-status-pill status={w.status} status-key={w.statusKey}></cp-status-pill></td>
                    <td>{w.owner}</td>
                    <td>{w.reviewAt}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </cp-panel>

        {selected ? (
          <cp-panel className="detail-panel">
            <div className="warning-detail-shell">
              <div className="entity-top">
                <div><strong>{selected.title}</strong><div className="student-meta">{selected.code} · {selected.studentNo} · {selected.student}</div></div>
                <cp-status-pill status={selected.status} status-key={selected.statusKey}></cp-status-pill>
              </div>
              <div className="evidence-strip compact">
                {[["风险等级", selected.level], ["风险分数", selected.score], ["预警来源", selected.source], ["建议处理人", selected.owner], ["复评时间", selected.reviewAt]].map(([label, value]) => (
                  <span key={label}><em>{label}</em><strong>{value}</strong></span>
                ))}
              </div>
              <div className="warning-section-grid">
                <div><span>辅导员意见</span><p>{selected.counselorNote}</p></div>
                <div><span>导师帮扶措施</span><p>{selected.mentorPlan}</p></div>
                <div><span>学生反馈</span><p>{selected.studentFeedback}</p></div>
                <div><span>处理说明</span><p>当前状态字段显示为"{selected.status}"，时间线用于补充待确认、帮扶、反馈和复评过程。</p></div>
              </div>
              <div className="role-action-area">
                {canPerform("confirmWarning") && (
                  <form className="action-form" onSubmit={handleConfirm}>
                    <label className="field"><span>辅导员意见</span><textarea value={confirmNote || selected.counselorNote} onChange={(e) => setConfirmNote(e.target.value)} /></label>
                    <button className="primary-button" type="submit">确认风险预警单</button>
                  </form>
                )}
                {canPerform("saveMentorPlan") && (
                  <form className="action-form" onSubmit={handleMentorPlan}>
                    <label className="field"><span>导师帮扶措施</span><textarea value={mentorPlan || selected.mentorPlan} onChange={(e) => setMentorPlan(e.target.value)} /></label>
                    <button className="primary-button" type="submit">保存导师措施</button>
                  </form>
                )}
                {canPerform("submitFeedback") && (
                  <form className="action-form" onSubmit={handleFeedback}>
                    <label className="field"><span>学生反馈</span><textarea value={feedback || selected.studentFeedback} onChange={(e) => setFeedback(e.target.value)} /></label>
                    <button className="primary-button" type="submit">提交学生反馈</button>
                  </form>
                )}
                {canPerform("closeWarning") && selected.statusKey !== "done" && (
                  <form className="action-form" onSubmit={handleClose}>
                    <label className="field"><span>复评结案说明</span><textarea value={closeNote} onChange={(e) => setCloseNote(e.target.value)} /></label>
                    <button className="secondary-button" type="submit">复评结案</button>
                  </form>
                )}
                {!canPerform("confirmWarning") && !canPerform("saveMentorPlan") && !canPerform("submitFeedback") && !canPerform("closeWarning") && (
                  <div className="agent-main"><strong>只读视图</strong><p>当前角色可查看预警处理进度，写操作由对应业务角色完成。</p></div>
                )}
              </div>
            </div>
          </cp-panel>
        ) : (
          <cp-panel className="detail-panel"><div className="agent-main"><strong>未找到匹配预警单</strong><p>请换用预警单号、学生姓名或状态继续检索。</p></div></cp-panel>
        )}
      </section>

      <cp-panel eyebrow="处理轨迹" title={`${selected ? selected.code : "预警单"} 操作日志`}>
        <a className="text-button" href="#/workflow" slot="actions">查看完整流程</a>
        <ul className="timeline">
          {data.workflowLogs.filter((l) => l.code === selected?.code).map((l, i) => (
            <li key={i} className="timeline-item"><strong>{l.action}</strong><div className="student-meta">{l.time} · {l.actor}</div><p>{l.detail}</p></li>
          ))}
          {data.workflowLogs.filter((l) => l.code === selected?.code).length === 0 && <div className="empty-mini">该预警单暂无处理日志</div>}
        </ul>
      </cp-panel>
    </>
  );
}
