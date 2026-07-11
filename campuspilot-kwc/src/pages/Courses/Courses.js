import React, { useState, useEffect } from "react";
import { useAuth } from "../../context/AuthContext";
import { scopeCourses } from "../../utils/permissions";

export default function Courses() {
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

  const rows = scopeCourses(data.courses, user).filter(matchQuery);

  return (
    <section className="content-grid">
      <cp-panel eyebrow="成绩数据" title="课程成绩明细">
        <div className="table-wrap">
          <table className="data-table">
            <thead><tr><th>学生</th><th>课程</th><th>类型</th><th>成绩</th><th>状态</th><th>建议</th></tr></thead>
            <tbody>
              {rows.map((c, i) => (
                <tr key={i}><td>{c.student}</td><td>{c.course}</td><td>{c.type}</td><td><strong>{c.score}</strong></td><td>{c.status}</td><td>{c.advice}</td></tr>
              ))}
            </tbody>
          </table>
        </div>
      </cp-panel>
      <cp-panel eyebrow="短板识别" title="核心课程补强建议">
        <div className="setting-stack">
          <div className="agent-main"><strong>张明远</strong><p>高等数学 58 分，数据结构 61 分，与 AI 算法工程师目标存在明显基础短板。</p></div>
          <div className="module-card"><strong>补强计划</strong><p>每周 2 次课程辅导，结合算法题训练与阶段测验进行复评。</p></div>
          <div className="module-card"><strong>接口预留</strong><p>后续对接 query_course_score 工具，按学生编号实时读取课程数据。</p></div>
        </div>
      </cp-panel>
    </section>
  );
}
