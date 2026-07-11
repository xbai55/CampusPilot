import { KingdeeElement, api } from "@kdcloudjs/kwc";

const RISK_CLASS = { high: "high", watch: "watch", normal: "normal", improved: "improved" };

export default class RiskPill extends KingdeeElement {
  @api level = "normal";

  get pillClass() {
    return `risk-pill ${RISK_CLASS[this.level] || "normal"}`;
  }

  get label() {
    return this.level;
  }
}
