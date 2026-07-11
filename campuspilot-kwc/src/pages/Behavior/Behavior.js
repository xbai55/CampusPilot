import React, { useState, useEffect } from "react";
import { useAuth } from "../../context/AuthContext";
import { scopeBehaviors } from "../../utils/permissions";

export default function Behavior() {
  const { data, user } = useAuth();
  const [searchQuery, setSearchQuery] = useState("");

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

  const rows = scopeBehaviors(data.behaviors, user).filter(matchQuery);

  return (
    <>
      <section className="grid-2">
        {rows.map((b) => (
          <article key={b.student} className="entity-card">
            <div className="entity-top"><strong>{b.student}</strong><span className="role-pill">最近登录 {b.lastLogin}</span></div>
            <cp-score-line label="出勤率" value={b.attendance} color={b.attendance < 80 ? "#d43f3a" : "#24966d"}></cp-score-line>
            <cp-score-line label="作业完成率" value={b.assignment} color={b.assignment < 70 ? "#d43f3a" : "#24966d"}></cp-score-line>
            <cp-score-line label="平台活跃度" value={b.activity} color="#1267e8"></cp-score-line>
            <p className="card-note">{b.note}</p>
          </article>
        ))}
        {rows.length === 0 && <cp-panel eyebrow="行为数据" title="未找到匹配行为数据"><div className="agent-main"><p>请换用学生姓名继续检索。</p></div></cp-panel>}
      </section>
      <cp-panel eyebrow="行为风险规则" title="学习行为判断依据">
        <div className="grid-3">
          {["出勤率低于 0.85 进入关注", "出勤率低于 0.8 且作业低于 0.7 进入高风险", "平台活跃度下降触发辅导员提醒"].map((r) => (
            <div key={r} className="module-card"><strong>{r}</strong><p>规则同步到 Agent 提示词与知识库。</p></div>
          ))}
        </div>
      </cp-panel>
    </>
  );
}
