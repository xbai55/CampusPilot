import { KingdeeElement, api } from "@kdcloudjs/kwc";

export default class TrendChart extends KingdeeElement {
  @api trend = [];

  get maxValue() {
    if (!this.trend || this.trend.length === 0) return 1;
    return Math.max(...this.trend.flatMap((t) => [t.high, t.watch, t.done]), 1);
  }

  get chartItems() {
    const max = this.maxValue;
    return (this.trend || []).map((t) => ({
      week: t.week,
      high: t.high,
      watch: t.watch,
      done: t.done,
      highH: `--height:${Math.max(12, (t.high / max) * 100)}%`,
      watchH: `--height:${Math.max(12, (t.watch / max) * 100)}%`,
      doneH: `--height:${Math.max(12, (t.done / max) * 100)}%`,
    }));
  }
}
