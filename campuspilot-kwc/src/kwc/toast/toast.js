import { KingdeeElement, api } from "@kdcloudjs/kwc";

let toastTimer = null;

export default class Toast extends KingdeeElement {
  @api message = "";
  @api visible = false;

  connectedCallback() {
    this._handler = (e) => this.show(e.detail);
    window.addEventListener("toast", this._handler);
  }

  disconnectedCallback() {
    window.removeEventListener("toast", this._handler);
    if (toastTimer) clearTimeout(toastTimer);
  }

  show(msg) {
    this.message = msg;
    this.visible = true;
    if (toastTimer) clearTimeout(toastTimer);
    toastTimer = setTimeout(() => {
      this.visible = false;
    }, 2800);
  }
}
