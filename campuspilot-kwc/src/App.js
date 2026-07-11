import React, { useEffect, useRef, useMemo, useCallback } from "react";
import "./kwc/riskPill/riskPill";
import "./kwc/statusPill/statusPill";
import "./kwc/scoreLine/scoreLine";
import "./kwc/metricCard/metricCard";
import "./kwc/panel/panel";
import "./kwc/donutChart/donutChart";
import "./kwc/trendChart/trendChart";
import "./kwc/scatterChart/scatterChart";
import "./kwc/toast/toast";
import "./kwc/sidebar/sidebar";
import "./kwc/topbar/topbar";
import "./kwc/appShell/appShell";
import { Routes, Route, Navigate, useLocation, useNavigate } from "react-router-dom";
import { useAuth } from "./context/AuthContext";
import Home from "./pages/Home/Home";
import Login from "./pages/Login/Login";
import Dashboard from "./pages/Dashboard/Dashboard";
import Students from "./pages/Students/Students";
import Courses from "./pages/Courses/Courses";
import Behavior from "./pages/Behavior/Behavior";
import Warnings from "./pages/Warnings/Warnings";
import Agent from "./pages/Agent/Agent";
import Workflow from "./pages/Workflow/Workflow";
import User from "./pages/User/User";
import Settings from "./pages/Settings/Settings";
import StudentHome from "./pages/StudentHome/StudentHome";
import TeacherHome from "./pages/TeacherHome/TeacherHome";
import CounselorHome from "./pages/CounselorHome/CounselorHome";
import AdminHome from "./pages/AdminHome/AdminHome";
import { showToast } from "./utils/toast";
import { routeMeta } from "./data/mockData";

const NAV_GROUPS = [
  { label: "工作台", items: [
    { route: "student-home", icon: "M12 12a4 4 0 1 0 0-8 4 4 0 0 0 0 8Zm-7 8a7 7 0 0 1 14 0H5Z", label: "我的工作台" },
    { route: "teacher-home", icon: "M5 4h14v3H5V4Zm2 5h10v11H7V9Zm2 2v2h6v-2H9Zm0 4v2h4v-2H9Z", label: "教学工作台" },
    { route: "counselor-home", icon: "M12 3 2.8 19h18.4L12 3Zm1 12h-2v2h2v-2Zm0-6h-2v5h2V9Z", label: "处置工作台" },
    { route: "admin-home", icon: "M4 5h16v4H4V5Zm0 6h7v8H4v-8Zm9 0h7v8h-7v-8Z", label: "治理驾驶舱" },
  ] },
  { label: "业务模块", items: [
    { route: "dashboard", icon: "M4 5.5A1.5 1.5 0 0 1 5.5 4h4A1.5 1.5 0 0 1 11 5.5v4A1.5 1.5 0 0 1 9.5 11h-4A1.5 1.5 0 0 1 4 9.5v-4Zm9 0A1.5 1.5 0 0 1 14.5 4h4A1.5 1.5 0 0 1 20 5.5v4a1.5 1.5 0 0 1-1.5 1.5h-4A1.5 1.5 0 0 1 13 9.5v-4ZM4 14.5A1.5 1.5 0 0 1 5.5 13h4a1.5 1.5 0 0 1 1.5 1.5v4A1.5 1.5 0 0 1 9.5 20h-4A1.5 1.5 0 0 1 4 18.5v-4Zm9 0a1.5 1.5 0 0 1 1.5-1.5h4a1.5 1.5 0 0 1 1.5 1.5v4a1.5 1.5 0 0 1-1.5 1.5h-4a1.5 1.5 0 0 1-1.5-1.5v-4Z", label: "风险驾驶舱" },
    { route: "students", icon: "M12 12a4 4 0 1 0 0-8 4 4 0 0 0 0 8Zm-7 8a7 7 0 0 1 14 0H5Z", label: "学生画像" },
    { route: "courses", icon: "M5 4h13a1 1 0 0 1 1 1v15H6.5A2.5 2.5 0 0 1 4 17.5V5a1 1 0 0 1 1-1Zm1 13.5a.5.5 0 0 0 .5.5H17V6H6v11.5ZM8 8h7v2H8V8Zm0 4h7v2H8v-2Z", label: "课程成绩" },
    { route: "behavior", icon: "M5 12h3v7H5v-7Zm5-7h3v14h-3V5Zm5 4h3v10h-3V9Z", label: "学习行为" },
    { route: "warnings", icon: "M12 3 2.8 19h18.4L12 3Zm1 12h-2v2h2v-2Zm0-6h-2v5h2V9Z", label: "风险预警单" },
  ] },
  { label: "智能闭环", items: [
    { route: "agent", icon: "M12 2a1 1 0 0 1 1 1v1.1A7.5 7.5 0 0 1 19.5 12v3.5A3.5 3.5 0 0 1 16 19h-1.8l-1.5 2.3a.8.8 0 0 1-1.4 0L9.8 19H8a3.5 3.5 0 0 1-3.5-3.5V12A7.5 7.5 0 0 1 11 4.1V3a1 1 0 0 1 1-1Zm-3 10.2a1.2 1.2 0 1 0 0-2.4 1.2 1.2 0 0 0 0 2.4Zm6 0a1.2 1.2 0 1 0 0-2.4 1.2 1.2 0 0 0 0 2.4Zm-6.5 3.1c1.9 1.2 5.1 1.2 7 0l-.8-1.3c-1.4.8-4 .8-5.4 0l-.8 1.3Z", label: "AI 成长助手" },
    { route: "workflow", icon: "M7 5a3 3 0 1 1 2.8 4H14a3 3 0 0 1 3 3v3.2a3 3 0 1 1-2 0V12a1 1 0 0 0-1-1H9.8A3 3 0 1 1 7 5Z", label: "处理闭环" },
  ] },
  { label: "组织与系统", items: [
    { route: "user", icon: "M12 12a4 4 0 1 0 0-8 4 4 0 0 0 0 8Zm-7 8a7 7 0 0 1 14 0H5Z", label: "用户中心" },
    { route: "settings", icon: "m19.4 13.5 1.2.9-2 3.4-1.4-.6a7.4 7.4 0 0 1-1.8 1l-.2 1.5h-4l-.2-1.5a7.4 7.4 0 0 1-1.8-1l-1.4.6-2-3.4 1.2-.9a6.7 6.7 0 0 1 0-2.1l-1.2-.9 2-3.4 1.4.6a7.4 7.4 0 0 1 1.8-1l.2-1.5h4l.2 1.5a7.4 7.4 0 0 1 1.8 1l1.4-.6 2 3.4-1.2.9a6.7 6.7 0 0 1 0 2.1ZM13 15.5a3 3 0 1 0 0-6 3 3 0 0 0 0 6Z", label: "系统设置" },
  ] },
];
function ProtectedRoute({ children, route }) {
  const { isAuth, permissions } = useAuth();
  if (route === "home") return children;
  if (!isAuth) return <Navigate to="/home" replace />;
  if (permissions?.routes.includes(route)) return children;
  if (permissions) return <Navigate to={`/${permissions.home}`} replace />;
  return <Navigate to="/home" replace />;
}

export default function App({ kwc = null }) {
  const { isAuth, user, permissions, logout, loadData } = useAuth();
  const location = useLocation();
  const navigate = useNavigate();
  const currentRoute = location.pathname.replace("/", "") || "home";
  const meta = routeMeta[currentRoute] || routeMeta.dashboard;
  const isLoginPage = currentRoute === "login";
  const shellStateTarget = kwc?.rootElement || document.body;

  const navGroups = useMemo(() => {
    return NAV_GROUPS.map((g) => ({
      label: g.label,
      items: g.items
        .filter((it) => isAuth ? permissions?.routes?.includes(it.route) : it.route === "home")
        .map((it) => ({ ...it, itemClass: currentRoute === it.route ? "nav-item active" : "nav-item" })),
    })).filter((g) => g.items.length > 0);
  }, [isAuth, permissions, currentRoute]);

  const handleShellEvent = useCallback((e) => {
    switch (e.type) {
      case "cp-navigate": navigate(`/${e.detail.route}`); break;
      case "cp-logout":   logout(); navigate("/home"); break;
      case "cp-refresh":  loadData().then(() => showToast("数据已刷新")); break;
    }
  }, [navigate, logout, loadData]);

  useEffect(() => {
    const target = shellStateTarget;
    target.setAttribute("data-page", isLoginPage ? "auth" : "app");
    target.setAttribute("data-authenticated", String(isAuth && !isLoginPage));
    target.setAttribute("data-role", user?.role || "鍖垮悕");
    target.setAttribute("data-lock", String(Boolean(kwc?.lock)));
    target.classList.toggle("agent-route", currentRoute === "agent" && !isLoginPage);
    if (kwc?.themeColor) target.style.setProperty("--theme-color", kwc.themeColor);
    return () => target.classList.remove("agent-route");
  }, [isAuth, user, currentRoute, isLoginPage, shellStateTarget, kwc?.themeColor, kwc?.lock]);

  const shellRef = useRef(null);
  useEffect(() => {
    const el = shellRef.current; if (!el) return;
    el.addEventListener("cp-navigate", handleShellEvent);
    el.addEventListener("cp-logout", handleShellEvent);
    el.addEventListener("cp-refresh", handleShellEvent);
    return () => { el.removeEventListener("cp-navigate", handleShellEvent); el.removeEventListener("cp-logout", handleShellEvent); el.removeEventListener("cp-refresh", handleShellEvent); };
  }, [handleShellEvent]);

  const sidebarRef = useRef(null);
  const topbarRef = useRef(null);
  useEffect(() => {
    const sb = sidebarRef.current;
    if (sb) { sb.navGroups = navGroups; sb.currentRoute = currentRoute; }
    const tb = topbarRef.current;
    if (tb) {
      tb.eyebrow = meta?.eyebrow || "AI 原生智慧校园平台";
      tb.title = meta?.title || "CampusPilot 学业风险驾驶舱";
      tb.subtitle = meta?.subtitle || "";
      tb.userName = user?.name || "";
      tb.userRole = user?.role || "";
      tb.userInitial = user?.name?.slice(0, 1) || "访";
      tb.isAuth = isAuth;
    }
  });

  return (
    <div ref={shellRef}>
      <Routes>
        <Route path="/login" element={<Login />} />
        <Route path="/agent" element={<ProtectedRoute route="agent"><Agent /></ProtectedRoute>} />
        <Route path="*" element={
          <cp-app-shell>
            <cp-sidebar ref={sidebarRef} slot="sidebar" current-route={currentRoute}></cp-sidebar>
            <cp-topbar ref={topbarRef} slot="topbar"></cp-topbar>
            <Routes>
                  <Route path="/home" element={<ProtectedRoute route="home"><Home /></ProtectedRoute>} />
                  <Route path="/student-home" element={<ProtectedRoute route="student-home"><StudentHome /></ProtectedRoute>} />
                  <Route path="/teacher-home" element={<ProtectedRoute route="teacher-home"><TeacherHome /></ProtectedRoute>} />
                  <Route path="/counselor-home" element={<ProtectedRoute route="counselor-home"><CounselorHome /></ProtectedRoute>} />
                  <Route path="/admin-home" element={<ProtectedRoute route="admin-home"><AdminHome /></ProtectedRoute>} />
                  <Route path="/dashboard" element={<ProtectedRoute route="dashboard"><Dashboard /></ProtectedRoute>} />
                  <Route path="/students" element={<ProtectedRoute route="students"><Students /></ProtectedRoute>} />
                  <Route path="/courses" element={<ProtectedRoute route="courses"><Courses /></ProtectedRoute>} />
                  <Route path="/behavior" element={<ProtectedRoute route="behavior"><Behavior /></ProtectedRoute>} />
                  <Route path="/warnings" element={<ProtectedRoute route="warnings"><Warnings /></ProtectedRoute>} />
                  <Route path="/workflow" element={<ProtectedRoute route="workflow"><Workflow /></ProtectedRoute>} />
                  <Route path="/user" element={<ProtectedRoute route="user"><User /></ProtectedRoute>} />
                  <Route path="/settings" element={<ProtectedRoute route="settings"><Settings /></ProtectedRoute>} />
                  <Route path="/" element={<Navigate to={isAuth ? `/${meta.home || "dashboard"}` : "/home"} replace />} />
            </Routes>
          </cp-app-shell>
        } />
      </Routes>
      <cp-toast></cp-toast>
    </div>
  );
}
