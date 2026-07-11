import { KingdeeElement, api } from "@kdcloudjs/kwc";

const STATUS_CLASS = { todo: "todo", active: "active", done: "done" };

export default class StatusPill extends KingdeeElement {
  @api status = "";
  @api statusKey = "todo";

  get pillClass() {
    return `status-pill ${STATUS_CLASS[this.statusKey] || "todo"}`;
  }
}
