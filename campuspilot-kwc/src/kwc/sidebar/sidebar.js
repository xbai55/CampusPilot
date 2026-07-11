import { KingdeeElement, api } from "@kdcloudjs/kwc";

function getShellTarget(el) {
  return el.closest(".campus-pilot-root") || document.body;
}

function syncShellAttrs(el, attrs) {
  const target = getShellTarget(el);
  attrs.forEach(a => el.setAttribute(a, target.getAttribute(a) || ''));
}

export default class Sidebar extends KingdeeElement {
  @api currentRoute = "";
  @api navGroups = [];

  connectedCallback() {
    const attrs = ['data-authenticated','data-page','data-role'];
    const target = getShellTarget(this);
    syncShellAttrs(this, attrs);
    this._obs = new MutationObserver(() => syncShellAttrs(this, attrs));
    this._obs.observe(target, {attributes:true,attributeFilter:attrs});
  }
  disconnectedCallback() { if (this._obs) this._obs.disconnect(); }

  handleClick(e) {
    const route = e.currentTarget.dataset.route;
    if (route) {
      this.dispatchEvent(new CustomEvent("cp-navigate", {detail:{route},bubbles:true,composed:true}));
    }
  }

  handleAgentContext() { getShellTarget(this).classList.toggle("agent-nav-open"); }

  get isAgentRoute() { return this.currentRoute === "agent"; }
}
