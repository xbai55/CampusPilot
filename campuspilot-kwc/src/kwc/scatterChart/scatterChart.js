import { KingdeeElement, api } from "@kdcloudjs/kwc";

function riskClass(key) {
  return (
    { high: "high", watch: "watch", normal: "normal", improved: "improved" }[
      key
    ] || "normal"
  );
}

export default class ScatterChart extends KingdeeElement {
  @api students = [];

  get points() {
    return (this.students || []).map((s) => ({
      no: s.no,
      nameLabel: s.name ? s.name.slice(-1) : "?",
      tooltip: `${s.name}：GPA ${s.gpa}，出勤率 ${s.attendance}%`,
      pointClass: `scatter-point ${riskClass(s.riskKey)}`,
      pointStyle: `--x:${((s.gpa || 0) / 4) * 100}%;--y:${s.attendance || 0}%`,
    }));
  }
}
