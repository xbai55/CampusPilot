import { KingdeeElement, api } from "@kdcloudjs/kwc";

export default class DonutChart extends KingdeeElement {
  @api distribution = [];

  get total() {
    return (this.distribution || []).reduce((s, d) => s + (d.value || 0), 0);
  }

  get stops() {
    const dist = this.distribution || [];
    const t = this.total;
    if (t === 0) return "transparent 0% 100%";
    let cursor = 0;
    return dist
      .map((d) => {
        const start = cursor;
        const end = cursor + (d.value / t) * 100;
        cursor = end;
        return `${d.color} ${start}% ${end}%`;
      })
      .join(", ");
  }

  get donutStyle() {
    return `background: conic-gradient(${this.stops})`;
  }

  get chartItems() {
    const t = this.total;
    return (this.distribution || []).map((d) => {
      const pct = t > 0 ? Math.round((d.value / t) * 100) : 0;
      return {
        ...d,
        pct,
        countText: `${d.value} 人 · ${pct}%`,
        barStyle: `--risk-color:${d.color};--width:${pct}%`,
      };
    });
  }
}
