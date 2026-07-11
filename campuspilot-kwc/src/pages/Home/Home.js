import React, { useState, useEffect, useCallback } from "react";
import { Link } from "react-router-dom";

const SLIDES = [
  ["01", "风险识别", "汇总成绩、出勤、作业和活跃度，形成学生风险分数与风险原因。", "课程低分", "出勤下降", "作业缺交"],
  ["02", "协同处置", "不同角色进入对应工作台，围绕同一张预警单推进处理。", "辅导员确认", "导师帮扶", "学生反馈"],
  ["03", "Agent 建议", "Agent 根据画像和预警上下文，输出风险解释与成长建议。", "画像解释", "预警建议", "成长计划"],
  ["04", "数据联动", "统一连接学生画像、课程成绩、学习行为和风险预警单，支撑跨角色协同与审计追踪。", "业务对象", "数据同步", "审计日志"],
];

export default function Home() {
  const [activeSlide, setActiveSlide] = useState(0);

  const showSlide = useCallback((index) => {
    setActiveSlide((index + SLIDES.length) % SLIDES.length);
  }, []);

  useEffect(() => {
    const timer = setInterval(() => showSlide(activeSlide + 1), 4300);
    return () => clearInterval(timer);
  }, [activeSlide, showSlide]);

  return (
    <>
      <section className="public-home">
        <div className="public-copy">
          <p className="eyebrow">CampusPilot Growth Intelligence</p>
          <h2>风险预警与帮扶工作台</h2>
          <p>围绕学生成长画像、学业表现、学习行为和风险预警单，提供辅导员、导师、学生与学院管理者共同使用的日常工作入口。</p>
          <div className="public-shortcuts">
            <Link to="/login">查看画像</Link>
            <Link to="/login">处理预警</Link>
            <Link to="/login">制定帮扶</Link>
            <Link to="/login">治理分析</Link>
          </div>
          <Link className="public-start-button" to="/login">开启体验</Link>
        </div>
        <div className="public-carousel" aria-label="平台能力轮播">
          <div className="public-carousel-head">
            <span>平台能力</span>
            <strong>业务流程</strong>
          </div>
          <div className="public-carousel-track">
            {SLIDES.map(([index, title, text, tagA, tagB, tagC], i) => (
              <article key={index} className={`public-carousel-slide ${i === activeSlide ? "active" : ""}`}>
                <div className="slide-index"><span>{index}</span><small>/ 04</small></div>
                <strong>{title}</strong>
                <p>{text}</p>
                <div className="slide-tags">
                  <em>{tagA}</em><em>{tagB}</em><em>{tagC}</em>
                </div>
              </article>
            ))}
          </div>
          <div className="public-carousel-list" aria-hidden="true">
            <span>画像</span><i /><span>预警</span><i /><span>帮扶</span><i /><span>复评</span>
          </div>
          <div className="public-carousel-dots" aria-label="切换平台能力">
            {SLIDES.map((_, i) => (
              <button key={i} className={i === activeSlide ? "active" : ""} type="button" onClick={() => showSlide(i)} aria-label={`查看第 ${i + 1} 项能力`} />
            ))}
          </div>
        </div>
      </section>
      <section className="public-status-strip" aria-label="平台能力">
        {[
          ["统一入口", "画像 / 预警 / 帮扶 / 治理"],
          ["核心流程", "识别 - 确认 - 帮扶 - 反馈 - 复评"],
          ["智能能力", "画像解释 / 预警建议 / 成长方案"],
          ["部署形态", "校内租户 / API 接入 / 审计留痕"],
        ].map(([title, text]) => (
          <div key={title}><strong>{title}</strong><p>{text}</p></div>
        ))}
      </section>
    </>
  );
}
