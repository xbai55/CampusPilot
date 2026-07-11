import React from "react";
import { Link } from "react-router-dom";
import { useAuth } from "../../context/AuthContext";
import { scopeAuditLogs } from "../../utils/permissions";

export default function User() {
  const { user, data, permissions, logout } = useAuth();
  if (!data || !user) return null;

  const logs = scopeAuditLogs(data.auditLogs, user);
  const activeInfo = data.roles.find((r) => r.role === user.role) || data.roles[1];

  return (
    <>
      <section className="content-grid">
        <cp-panel eyebrow="当前账号" title={user.name}>
          <span slot="actions" className="role-pill">{user.role}</span>
          <p className="muted">{user.email} · {user.college}</p>
          <section className="role-scenario">
            <div className="agent-main">
              <strong>{activeInfo.role}账号已绑定</strong>
              <p>{activeInfo.duty}</p>
              <p>一个登录账号只对应一个角色。如需更换角色，请退出当前账号后重新登录对应账号。</p>
            </div>
            <div className="role-path">
              {data.roles.map((r) => (
                <div key={r.role} className={`role-path-card ${r.role === user.role ? "active" : ""}`}>
                  <strong>{r.role}</strong>
                  <p>{r.role === user.role ? "当前账号角色。" : "需退出当前账号后，用对应账号重新登录。"}</p>
                </div>
              ))}
            </div>
          </section>
        </cp-panel>
        <cp-panel eyebrow="操作记录" title="最近活动">
          <ul className="timeline">
            {logs.map((l, i) => (
              <li key={i} className="timeline-item"><strong>{l.action}</strong><div className="student-meta">{l.time} · {l.actor}</div></li>
            ))}
          </ul>
        </cp-panel>
      </section>
      <cp-panel eyebrow="权限范围" title="当前租户权限">
        <Link slot="actions" className="text-button" to="/login">切换账号</Link>
        <div className="grid-3">
          {permissions.labels.map((l) => (
            <div key={l} className="module-card"><strong>{l}</strong><p>{user.role}角色已获得此业务权限。</p></div>
          ))}
        </div>
      </cp-panel>
    </>
  );
}
