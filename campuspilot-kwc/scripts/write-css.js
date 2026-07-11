const fs = require("fs");

const styles = {
  topbar:
    ":host{display:flex;align-items:flex-start;justify-content:space-between;gap:20px;padding:20px 32px;background:#fff;border-bottom:1px solid #e2e8f0;flex-wrap:wrap}" +
    ":host([data-authenticated=false]){display:none}" +
    ".meta{min-width:0}.eyebrow{font-size:12px;font-weight:800;text-transform:uppercase;letter-spacing:.07em;color:#60758d;margin:0}" +
    "h1{font-size:22px;font-weight:800;margin:2px 0 0;color:#1e2a3a;line-height:1.3}" +
    ".subtitle{margin:4px 0 0;font-size:13px;color:#60758d}" +
    ".actions{display:flex;align-items:center;gap:10px;flex-shrink:0}" +
    ".search{display:flex;align-items:center;gap:8px;padding:8px 14px;border-radius:999px;background:#f8fafc;border:1px solid #e2e8f0}" +
    ".search svg{width:16px;height:16px;fill:#60758d;flex-shrink:0}" +
    ".search input{border:none;background:none;outline:none;font-size:13px;min-width:160px;color:#1e2a3a}" +
    ".icon-btn{padding:8px;border-radius:8px;cursor:pointer;border:none;background:none;display:grid;place-items:center}" +
    ".icon-btn svg{width:20px;height:20px;fill:#60758d}" +
    ".icon-btn:hover{background:#f8fafc}" +
    ".user-btn{display:flex;align-items:center;gap:8px;padding:6px 12px 6px 6px;border-radius:999px;background:#f8fafc;cursor:pointer}" +
    ".avatar{display:grid;place-items:center;width:30px;height:30px;border-radius:50%;background:#1267e8;color:#fff;font-size:13px;font-weight:800}" +
    ".user-btn strong{display:block;font-size:13px;font-weight:700;color:#1e2a3a;line-height:1.2}" +
    ".user-btn small{font-size:11px;color:#60758d}" +
    ".auth-link{padding:8px 18px;border-radius:8px;font-size:14px;font-weight:600;color:#1e2a3a;background:#f8fafc;cursor:pointer;border:none}" +
    ".auth-link.primary{background:#1267e8;color:#fff}" +
    ".auth-link:hover{opacity:.85}",

  toast:
    ":host{position:fixed;bottom:32px;left:50%;transform:translateX(-50%);z-index:9999;pointer-events:none}" +
    ".toast{padding:12px 28px;border-radius:999px;background:#1e2a3a;color:#fff;font-size:14px;font-weight:600;box-shadow:0 8px 24px rgba(0,0,0,.18);opacity:0;transition:opacity .25s}" +
    ".toast.visible{opacity:1}",

  panel:
    ":host{display:block;background:#fff;border:1px solid #e2e8f0;border-radius:12px;padding:24px}" +
    ".head{display:flex;align-items:flex-start;justify-content:space-between;gap:16px;margin-bottom:20px}" +
    ".eyebrow{font-size:12px;font-weight:800;text-transform:uppercase;letter-spacing:.07em;color:#60758d;margin:0 0 4px}" +
    "h2{font-size:18px;font-weight:700;margin:0;color:#1e2a3a}",

  riskPill:
    ":host{display:inline-flex}" +
    ".pill{display:inline-flex;align-items:center;padding:2px 10px;border-radius:999px;font-size:12px;font-weight:700;line-height:1.6;text-transform:uppercase;letter-spacing:.04em;white-space:nowrap}" +
    ".high{background:rgba(212,63,58,.11);color:#a8241f}" +
    ".watch{background:rgba(217,131,20,.13);color:#9b5708}" +
    ".normal{background:rgba(36,150,109,.13);color:#16724f}" +
    ".improved{background:rgba(96,117,141,.14);color:#43566c}",

  statusPill:
    ":host{display:inline-flex}" +
    ".pill{display:inline-flex;align-items:center;padding:2px 10px;border-radius:999px;font-size:12px;font-weight:700;line-height:1.6;white-space:nowrap}" +
    ".todo{background:rgba(217,131,20,.13);color:#9b5708}" +
    ".active{background:rgba(18,103,232,.12);color:#0b4cb7}" +
    ".done{background:rgba(96,117,141,.14);color:#43566c}",

  scoreLine:
    ":host{display:block}" +
    ".row{display:flex;flex-direction:column;gap:6px}" +
    ".meta{display:flex;align-items:center;justify-content:space-between;font-size:13px}" +
    ".meta span{color:#1e2a3a}.meta strong{font-weight:700}" +
    ".track{overflow:hidden;border-radius:999px;background:#edf2f7;height:8px}" +
    ".fill{width:var(--width);height:100%;border-radius:inherit;background:var(--fill)}",

  metricCard:
    ":host{display:block;background:#fff;border:1px solid #e2e8f0;border-radius:12px;padding:18px}" +
    ".top{display:flex;align-items:center;justify-content:space-between;gap:8px;margin-bottom:12px}" +
    ".label{font-size:13px;font-weight:800;text-transform:uppercase;letter-spacing:.04em;color:#60758d}" +
    ".icon-wrap{display:grid;place-items:center;min-width:36px;height:36px;border-radius:8px;background:#edf5ff}" +
    ".icon-wrap svg{width:18px;height:18px;fill:#1267e8}" +
    ".value{display:block;margin-bottom:4px;font-size:32px;font-weight:800;line-height:1;color:#1e2a3a}" +
    ".note{font-size:12px;color:#60758d}" +
    ":host([tone=red]) .icon-wrap{background:rgba(212,63,58,.1)}" +
    ":host([tone=red]) .icon-wrap svg,:host([tone=red]) .value{fill:#d43f3a;color:#d43f3a}" +
    ":host([tone=orange]) .icon-wrap{background:rgba(217,131,20,.12)}" +
    ":host([tone=orange]) .icon-wrap svg,:host([tone=orange]) .value{fill:#d98314;color:#d98314}" +
    ":host([tone=green]) .icon-wrap{background:rgba(36,150,109,.12)}" +
    ":host([tone=green]) .icon-wrap svg,:host([tone=green]) .value{fill:#24966d;color:#24966d}" +
    ":host([tone=slate]) .icon-wrap{background:rgba(96,117,141,.13)}" +
    ":host([tone=slate]) .icon-wrap svg,:host([tone=slate]) .value{fill:#60758d;color:#60758d}",

  donutChart:
    ":host{display:block}" +
    ".wrap{position:relative;width:160px;height:160px;margin:0 auto 20px}" +
    ".chart{width:100%;height:100%;border-radius:50%}" +
    ".center{position:absolute;inset:28%;display:flex;flex-direction:column;align-items:center;justify-content:center;border-radius:50%;background:#fff}" +
    ".center strong{font-size:24px;font-weight:800;color:#1e2a3a}" +
    ".center span{font-size:11px;color:#60758d}" +
    ".bars{display:flex;flex-direction:column;gap:10px}" +
    ".bar-row{display:flex;flex-direction:column;gap:6px}" +
    ".bar-meta{display:flex;align-items:center;justify-content:space-between;font-size:13px}" +
    ".bar-name{display:flex;align-items:center;gap:8px;color:#1e2a3a}" +
    ".dot{display:inline-block;width:8px;height:8px;border-radius:50%;background:var(--risk-color)}" +
    ".bar-meta strong{font-weight:700}" +
    ".bar-track{overflow:hidden;border-radius:999px;background:#edf2f7;height:6px}" +
    ".bar-fill{width:var(--width);height:100%;border-radius:inherit;background:var(--risk-color)}",

  trendChart:
    ":host{display:block}" +
    ".chart{display:flex;align-items:flex-end;gap:16px;padding:8px 0 20px}" +
    ".week{flex:1;display:flex;flex-direction:column;align-items:center;gap:8px}" +
    ".week strong{font-size:11px;color:#60758d;font-weight:600}" +
    ".bars{display:flex;align-items:flex-end;gap:3px;height:100px;width:100%;justify-content:center}" +
    ".bars span{width:14px;border-radius:4px 4px 0 0;height:var(--height);min-height:4px}" +
    ".danger{background:#d43f3a}.warning{background:#d98314}.done{background:#24966d}" +
    ".legend{display:flex;gap:20px;justify-content:center;font-size:12px;color:#60758d}" +
    ".legend span{display:flex;align-items:center;gap:6px}" +
    ".legend i{display:inline-block;width:8px;height:8px;border-radius:2px}" +
    ".legend i.danger{background:#d43f3a}.legend i.warning{background:#d98314}.legend i.done{background:#24966d}",

  scatterChart:
    ":host{display:block}" +
    ".chart{position:relative;width:100%;height:260px;border-left:1px solid #e2e8f0;border-bottom:1px solid #e2e8f0}" +
    ".alabel{position:absolute;font-size:11px;color:#60758d;font-weight:600}" +
    ".alabel.y{top:-18px;left:-12px}.alabel.x{bottom:-18px;right:-12px}" +
    ".point{position:absolute;left:var(--x);bottom:var(--y);transform:translate(-50%,50%);width:20px;height:20px;display:grid;place-items:center;border-radius:50%;font-size:9px;font-weight:800;cursor:default}" +
    ".point.high{background:rgba(212,63,58,.18);color:#d43f3a}" +
    ".point.watch{background:rgba(217,131,20,.18);color:#d98314}" +
    ".point.normal{background:rgba(36,150,109,.18);color:#24966d}" +
    ".point.improved{background:rgba(96,117,141,.18);color:#60758d}",
};

Object.entries(styles).forEach(([name, css]) => {
  fs.writeFileSync(`src/kwc/${name}/${name}.scoped.css`, css);
  console.log(`${name}: ${css.length} bytes`);
});
console.log("Done!");
