import { KingdeeElement, api } from "@kdcloudjs/kwc";

export default class ScoreLine extends KingdeeElement {
  @api label = "";
  @api value = 0;
  @api color = "#1267e8";

  get fillStyle() {
    return `--width:${this.value}%;--fill:${this.color}`;
  }
}
