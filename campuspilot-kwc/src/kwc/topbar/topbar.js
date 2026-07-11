import { KingdeeElement, api } from "@kdcloudjs/kwc";

function getShellTarget(el) {
  return el.closest(".campus-pilot-root") || document.body;
}

function syncShellAttrs(el, attrs) {
  const target = getShellTarget(el);
  attrs.forEach(a => el.setAttribute(a, target.getAttribute(a) || ''));
}

export default class Topbar extends KingdeeElement {
  @api eyebrow = "AI 原生智慧校园平台";
  @api title = "CampusPilot 学业风险驾驶舱";
  @api subtitle = "";
  @api userName = "";
  @api userRole = "";
  @api userInitial = "访";
  @api isAuth = false;

  connectedCallback() {
    const attrs = ['data-authenticated','data-page','data-role'];
    const target = getShellTarget(this);
    syncShellAttrs(this, attrs);
    this._obs = new MutationObserver(() => syncShellAttrs(this, attrs));
    this._obs.observe(target, {attributes:true,attributeFilter:attrs});
  }
  disconnectedCallback() {
    if (this._obs) this._obs.disconnect();
    if (this.searchTimer) clearTimeout(this.searchTimer);
  }

  searchTimer = null;

  handleSearchInput(e) {
    const value = e.target.value;
    if (this.searchTimer) clearTimeout(this.searchTimer);
    this.searchTimer = setTimeout(() => {
      window.dispatchEvent(
        new CustomEvent("search", { detail: value })
      );
    }, 160);
  }

  handleRefresh() {
    this.dispatchEvent(
      new CustomEvent("cp-refresh", { bubbles: true, composed: true })
    );
  }

  handleLogout() {
    this.dispatchEvent(
      new CustomEvent("cp-logout", { bubbles: true, composed: true })
    );
  }

  handleNavigate(e) {
    const route = e.currentTarget.dataset.route;
    if (route) {
      this.dispatchEvent(
        new CustomEvent("cp-navigate", {
          detail: { route },
          bubbles: true,
          composed: true,
        })
      );
    }
  }

}
