import { KingdeeElement, api } from "@kdcloudjs/kwc";

export default class MetricCard extends KingdeeElement {
  @api label = "";
  @api value = "";
  @api note = "";
  @api tone = "blue";
  @api icon = "";

  get cardClass() {
    return `metric-card tone-${this.tone}`;
  }
}
