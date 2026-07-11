import { KingdeeElement, api } from "@kdcloudjs/kwc";

export default class Panel extends KingdeeElement {
  @api eyebrow = "";
  @api title = "";

  get hasHeading() {
    return this.eyebrow || this.title;
  }
}
