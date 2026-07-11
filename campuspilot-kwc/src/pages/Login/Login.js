import React, { useState, useEffect, useCallback } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../../context/AuthContext";
import { showToast } from "../../utils/toast";
import { roleConfig } from "../../utils/permissions";

const ROLES = [
  { role: "学生", title: "我的成长画像", desc: "查看个人学业状态、课程成绩、学习行为和 AI 学习建议，并提交帮扶反馈。", label: "学生端" },
  { role: "辅导员", title: "风险学生处置工作台", desc: "查看风险学生列表，确认预警单，填写辅导员意见并跟踪处理状态。", label: "辅导员端" },
  { role: "导师", title: "课程短板与帮扶措施", desc: "查看学生课程短板，制定导师帮扶计划，推荐课程和实践项目。", label: "导师端" },
  { role: "学院管理者", title: "学院风险治理驾驶舱", desc: "查看风险分布、预警处理进度、帮扶成效和系统治理配置。", label: "管理端" },
];

export default function Login() {
  const { login, isAuth } = useAuth();
  const navigate = useNavigate();
  const [tab, setTab] = useState("login");
  const [activeRole, setActiveRole] = useState(0);
  const [form, setForm] = useState({ account: "counselor@campus.edu", password: "campuspilot", role: "辅导员" });
  const [regForm, setRegForm] = useState({ name: "", email: "", role: "辅导员" });

  useEffect(() => {
    if (isAuth) {
      const cfg = roleConfig();
      navigate(`/${cfg.home}`, { replace: true });
    }
  }, [isAuth, navigate]);

  useEffect(() => {
    const timer = setInterval(() => setActiveRole((prev) => (prev + 1) % ROLES.length), 3500);
    return () => clearInterval(timer);
  }, []);

  const handleLogin = (e) => {
    e.preventDefault();
    const user = { name: form.account.includes("@") ? (form.role === "辅导员" ? "王老师" : form.role === "导师" ? "陈导师" : form.role === "学生" ? "张明远" : "学院管理者") : form.account, email: form.account, role: form.role, college: "信息工程学院" };
    login(user);
    showToast(`已以${form.role}身份登录`);
    // page-exit animation before navigation
    document.body.classList.add("page-exit");
    setTimeout(() => {
      document.body.classList.remove("page-exit");
      navigate(`/${roleConfig(form.role).home}`, { replace: true });
    }, 220);
  };

  const handleRegister = (e) => {
    e.preventDefault();
    if (!regForm.name || !regForm.email) { showToast("请填写姓名和邮箱"); return; }
    const user = { name: regForm.name, email: regForm.email, role: regForm.role, college: "信息工程学院" };
    login(user);
    showToast("账号已创建并登录");
    navigate(`/${roleConfig(regForm.role).home}`, { replace: true });
  };

  return (
    <main className="auth-shell">
      <section className="auth-hero" aria-label="产品说明">
        <Link className="brand auth-brand" to="/home">
          <span className="brand-mark" aria-hidden="true">
            <svg viewBox="0 0 32 32">
              <path d="M6 17.5 16 5l10 12.5-4.1 2.2L16 12.3l-5.9 7.4L6 17.5Z" />
              <path d="M8.8 22.5 16 18l7.2 4.5L16 27 8.8 22.5Z" />
            </svg>
          </span>
          <span><strong>启航智伴</strong><small>CampusPilot</small></span>
        </Link>
        <div className="auth-copy">
          <p className="eyebrow">AI 原生智慧校园平台</p>
          <h1>启航智伴 CampusPilot</h1>
          <p>面向高校学业风险识别、帮扶协同与治理分析的一体化工作系统。</p>
        </div>
        <div className="auth-role-carousel" aria-label="角色入口自动轮播">
          <div className="auth-role-track">
            {ROLES.map((r, i) => (
              <article key={r.role} className={`auth-role-slide ${i === activeRole ? "active" : ""}`}>
                <span>{r.label}</span>
                <strong>{r.title}</strong>
                <p>{r.desc}</p>
                <a href="#!" onClick={(e) => { e.preventDefault(); setForm((f) => ({ ...f, role: r.role })); setTab("login"); }}>以{r.role}身份进入</a>
              </article>
            ))}
          </div>
          <div className="auth-role-dots" aria-label="切换角色">
            {ROLES.map((_, i) => (
              <button key={i} className={i === activeRole ? "active" : ""} type="button" onClick={() => setActiveRole(i)} aria-label={`查看${ROLES[i].label}`} />
            ))}
          </div>
        </div>
      </section>

      <section className="auth-panel" aria-label="账号入口">
        <div className="auth-tabs" role="tablist" aria-label="账号操作">
          <button className={tab === "login" ? "active" : ""} type="button" onClick={() => setTab("login")}>登录</button>
          <button className={tab === "register" ? "active" : ""} type="button" onClick={() => setTab("register")}>注册</button>
        </div>

        {tab === "login" && (
          <form className="auth-form active" onSubmit={handleLogin}>
            <div className="form-heading"><h2>欢迎回来</h2><p>使用学校账号或统一身份认证进入 CampusPilot。</p></div>
            <label><span>账号</span><input value={form.account} onChange={(e) => setForm({ ...form, account: e.target.value })} autoComplete="username" /></label>
            <label><span>密码</span><input value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} type="password" autoComplete="current-password" /></label>
            <label><span>登录角色</span>
              <select value={form.role} onChange={(e) => setForm({ ...form, role: e.target.value })}>
                {ROLES.map((r) => <option key={r.role}>{r.role}</option>)}
              </select>
            </label>
            <div className="form-row">
              <label className="check-line"><input type="checkbox" defaultChecked /><span>记住此设备</span></label>
              <button className="link-button" type="button" onClick={() => showToast("请联系学校管理员重置密码")}>忘记密码</button>
            </div>
            <button className="primary-button" type="submit">进入工作台</button>
          </form>
        )}

        {tab === "register" && (
          <form className="auth-form active" onSubmit={handleRegister}>
            <div className="form-heading"><h2>申请试用账号</h2><p>注册后可按角色进入学校租户，使用统一账号入口、组织权限和风险治理工作台。</p></div>
            <label><span>姓名</span><input value={regForm.name} onChange={(e) => setRegForm({ ...regForm, name: e.target.value })} placeholder="请输入姓名" autoComplete="name" /></label>
            <label><span>学校邮箱</span><input value={regForm.email} onChange={(e) => setRegForm({ ...regForm, email: e.target.value })} type="email" placeholder="name@campus.edu" autoComplete="email" /></label>
            <label><span>角色</span>
              <select value={regForm.role} onChange={(e) => setRegForm({ ...regForm, role: e.target.value })}>
                {ROLES.map((r) => <option key={r.role}>{r.role}</option>)}
              </select>
            </label>
            <button className="primary-button" type="submit">创建账号</button>
          </form>
        )}
      </section>
    </main>
  );
}
