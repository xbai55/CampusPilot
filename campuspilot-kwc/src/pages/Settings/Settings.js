import React, { useState } from "react";
import { useAuth } from "../../context/AuthContext";
import { canPerform } from "../../utils/permissions";
import { showToast } from "../../utils/toast";

function SettingSwitch({ label, on }) {
  return <div className="module-card switch-line"><strong>{label}</strong><span className={`switch ${on ? "on" : ""}`} aria-hidden="true" /></div>;
}

export default function Settings() {
  const { data } = useAuth();
  const [apiBase, setApiBase] = useState(localStorage.getItem("campuspilot:apiBase") || "http://10.0.160.250:8080/ierp");

  if (!data) return null;

  const handleSave = () => {
    if (!canPerform("saveSettings")) { showToast("当前角色无权执行该操作"); return; }
    localStorage.setItem("campuspilot:apiBase", apiBase.trim());
    showToast("设置已保存到当前环境");
  };

  const integration = data.integrationStatus;
  const thirdPartyApp = integration.thirdPartyApp || { appId: "campuspilot_isv", auth: "AccessToken / API 授权 / IP 白名单", status: "待配置真实密钥" };

  return (
    <>
      <section className="content-grid">
        <cp-panel eyebrow="接口配置" title="数据源与 Agent 工具">
          <button slot="actions" className="text-button" type="button" onClick={handleSave}>保存设置</button>
          <div className="form-grid">
            <label className="field"><span>金蝶环境地址</span><input value={apiBase} onChange={(e) => setApiBase(e.target.value)} /></label>
            <label className="field"><span>默认租户</span><input defaultValue="CampusPilot 校园租户" /></label>
            <label className="field"><span>学生画像对象编码</span><input defaultValue="cp_student_profile" /></label>
            <label className="field"><span>风险预警单对象编码</span><input defaultValue="cp_warning_order" /></label>
            <label className="field"><span>Agent 名称</span><input defaultValue="CampusPilot 学业成长助手" /></label>
            <label className="field"><span>工具调用模式</span>
              <select defaultValue="知识库优先 + 工具增强"><option>知识库优先 + 工具增强</option><option>工具优先 + 知识库兜底</option></select>
            </label>
          </div>
        </cp-panel>
        <cp-panel eyebrow="系统开关" title="运行与审计">
          <div className="setting-stack">
            <SettingSwitch label="启用驾驶舱统计" on={true} />
            <SettingSwitch label="启用 Agent 结构化输出" on={true} />
            <SettingSwitch label="启用处理过程记录" on={true} />
            <SettingSwitch label="记录用户操作审计" on={true} />
            <SettingSwitch label="自动生成预警单" on={false} />
          </div>
        </cp-panel>
      </section>
      <section className="content-grid">
        <cp-panel eyebrow="第三方应用" title="金蝶 OpenAPI 接入边界">
          <span slot="actions" className="status-pill todo">{thirdPartyApp.status}</span>
          <div className="integration-summary">
            <strong>{thirdPartyApp.appId}</strong>
            <p>{thirdPartyApp.auth}</p>
            <p>本地前后端只负责业务界面、数据闭环和 Agent API 调用入口；Agent 本体、RAG 和工具编排在金蝶平台低代码配置。</p>
          </div>
        </cp-panel>
        <cp-panel eyebrow="低代码模型" title="业务对象清单">
          <div className="object-grid">
            {integration.objects.map((o) => (
              <div key={o.code}><strong>{o.name}</strong><span>{o.code}</span><em>{o.status} · {o.fields} 字段</em></div>
            ))}
          </div>
        </cp-panel>
      </section>
      <section className="content-grid">
        <cp-panel eyebrow="低代码开发" title="表单、流程、报表与集成蓝图">
          <div className="object-grid">
            {(data.lowcodeBlueprint?.objects || []).map((o) => (
              <div key={o.code}><strong>{o.name}</strong><span>{o.code} · {o.type}</span><em>{o.purpose}</em></div>
            ))}
          </div>
        </cp-panel>
        <cp-panel eyebrow="苍穹能力" title="平台能力落点">
          <div className="setting-stack">
            {(data.lowcodeBlueprint?.platformCapabilities || []).map((item) => (
              <div key={item.name} className="module-card"><strong>{item.name}</strong><p>{item.status} · {item.detail}</p></div>
            ))}
          </div>
        </cp-panel>
      </section>

      <section className="content-grid">
        <cp-panel eyebrow="Agent 智能体" title={data.agentWorkflow?.agentName || "CampusPilot 学业成长助手"}>
          <div className="integration-summary"><strong>{data.agentWorkflow?.modelRoute}</strong><p>RAG 知识库、工具调用和多步任务流共同生成可落表的预警建议。</p></div>
          <div className="agent-pipeline">
            {(data.agentWorkflow?.workflow || []).map((step, index) => (
              <div key={step.step}><span>{index + 1}</span><strong>{step.step}</strong><p>{step.detail}</p></div>
            ))}
          </div>
        </cp-panel>
        <cp-panel eyebrow="工具调用" title="可由 Agent 编排的业务工具">
          <div className="setting-stack">
            {(data.agentWorkflow?.tools || []).map((tool) => (
              <div key={tool.name} className="module-card"><strong>{tool.name}</strong><p>{tool.method} · {tool.purpose}</p></div>
            ))}
          </div>
        </cp-panel>
      </section>

      <section className="content-grid">
        <cp-panel eyebrow="数据可视化分析" title="报表中心">
          <div className="setting-stack">
            {(data.reportCenter || []).map((report) => (
              <div key={report.name} className="module-card"><strong>{report.name}</strong><p>{report.metric} · {report.decision}</p></div>
            ))}
          </div>
        </cp-panel>
        <cp-panel eyebrow="云原生微服务" title="部署单元">
          <div className="setting-stack">
            {(data.cloudNative || []).map((item) => (
              <div key={item.layer} className="module-card"><strong>{item.layer} · {item.unit}</strong><p>{item.deploy}</p></div>
            ))}
          </div>
        </cp-panel>
      </section>

      <cp-panel eyebrow="多模态大模型" title="可扩展输入与输出设计">
        <div className="grid-3">
          {(data.multimodal || []).map((item) => (
            <div key={item.input} className="module-card"><strong>{item.input}</strong><p>{item.model} · {item.output}</p><em>{item.status}</em></div>
          ))}
        </div>
      </cp-panel>
      <cp-panel eyebrow="风险规则" title="标准化判断条件">
        <div className="grid-3">
          {["高风险：GPA < 2.0 或挂科数 >= 2", "高风险：核心课程明显不及格", "高风险：出勤 < 0.8 且作业 < 0.7", "需要关注：GPA 2.0 到 2.8", "需要关注：出勤低于 0.85", "正常：无挂科且行为稳定"].map((r) => (
            <div key={r} className="module-card"><strong>{r}</strong><p>同步到 Agent 提示词、知识库和项目说明。</p></div>
          ))}
        </div>
      </cp-panel>
    </>
  );
}
