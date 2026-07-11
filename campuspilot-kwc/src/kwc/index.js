import React from "react";
import ReactDOM from "react-dom/client";
import KwcApp from "../KwcApp";
import normalizeKData from "./normalizeKData";
import createKingdeeBridge from "./kingdeeBridge";
import "../styles.css";

const CONTROL_NAME = "campuspilot";
const CSS_PATH = "./css/campuspilot.css";

function loadControlCss(model, callback) {
  if (typeof window === "undefined" || !window.KDApi || typeof window.KDApi.loadFile !== "function") {
    callback();
    return;
  }
  window.KDApi.loadFile(CSS_PATH, model, callback);
}

function clearNode(node) {
  while (node.firstChild) node.removeChild(node.firstChild);
}

function CampusPilot(model) {
  this.model = model;
  this.mountNode = null;
  this.reactRoot = null;
  this.normalized = normalizeKData({});
  this.cssReady = false;
  this.bridge = createKingdeeBridge(model);
}

CampusPilot.prototype.init = function init(props) {
  this.normalized = normalizeKData(props);
  loadControlCss(this.model, () => {
    this.cssReady = true;
    this.render();
  });
};

CampusPilot.prototype.update = function update(props) {
  this.normalized = normalizeKData(props);
  this.render();
};

CampusPilot.prototype.render = function render() {
  if (!this.cssReady && typeof window !== "undefined" && window.KDApi?.loadFile) return;
  const container = this.model?.dom;
  if (!container) return;

  if (!this.mountNode) {
    clearNode(container);
    this.mountNode = document.createElement("div");
    this.mountNode.className = "campus-pilot-root";
    this.mountNode.setAttribute("data-kwc-control", CONTROL_NAME);
    container.appendChild(this.mountNode);
    this.reactRoot = ReactDOM.createRoot(this.mountNode);
  }

  const kwc = {
    ...this.normalized,
    props: this.normalized.raw,
    model: this.model,
    rootElement: this.mountNode,
    bridge: this.bridge,
  };

  this.reactRoot.render(<KwcApp kwc={kwc} />);
};

CampusPilot.prototype.destoryed = function destoryed() {
  if (this.reactRoot) this.reactRoot.unmount();
  if (this.mountNode) this.mountNode.remove();
  this.reactRoot = null;
  this.mountNode = null;
};

CampusPilot.prototype.destroyed = CampusPilot.prototype.destoryed;

if (typeof window !== "undefined" && window.KDApi && typeof window.KDApi.register === "function") {
  window.KDApi.register(CONTROL_NAME, CampusPilot);
} else if (typeof window !== "undefined") {
  window.CampusPilotKwcControl = CampusPilot;
}

export default CampusPilot;
