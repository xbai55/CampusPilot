import React from "react";
import { useAuth } from "../../context/AuthContext";

export default function Workflow() {
  const { data } = useAuth();
  if (!data) return null;

  const groupedLogs = data.workflowLogs.reduce((map, log) => {
    if (!map[log.code]) map[log.code] = [];
    map[log.code].push(log);
    return map;
  }, {});

  return (
    <>
      <section className="content-grid">
        <cp-panel eyebrow="处理过程记录" title="RW2026001 闭环轨迹">
          <span slot="actions" className="status-pill active">帮扶中</span>
          <ul className="timeline">
            {data.workflow.map((w, i) => (
              <li key={i} className="timeline-item"><strong>{w.step}</strong><div className="student-meta">{w.time} · {w.owner}</div><p>{w.detail}</p></li>
            ))}
          </ul>
        </cp-panel>
        <cp-panel eyebrow="升级路径" title="从单字段到正式流程">
          <div className="setting-stack">
            {["当前版本：处理状态字段 + 处理过程记录", "轻量增强：新增风险处理日志对象", "正式版本：接入 BPMN 流程与审批轨迹", "展示目标：待确认、帮扶中、学生反馈、复评、结案全链路可追踪"].map((m) => (
              <div key={m} className="module-card"><strong>{m}</strong></div>
            ))}
          </div>
        </cp-panel>
      </section>
      <cp-panel eyebrow="业务日志对象" title="风险处理日志">
        <span slot="actions" className="role-pill">除 Agent 外已本地闭环</span>
        <div className="log-board">
          {Object.entries(groupedLogs).map(([code, logs]) => (
            <article key={code}>
              <strong>{code}</strong>
              <ul className="timeline mini">
                {logs.map((l, i) => (
                  <li key={i} className="timeline-item"><strong>{l.action}</strong><div className="student-meta">{l.time} · {l.actor}</div><p>{l.detail}</p></li>
                ))}
              </ul>
            </article>
          ))}
        </div>
      </cp-panel>
    </>
  );
}
