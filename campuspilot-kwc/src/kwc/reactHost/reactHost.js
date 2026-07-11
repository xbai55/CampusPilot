import { KingdeeElement } from "@kdcloudjs/kwc";
import React from "react";
import ReactDOM from "react-dom/client";
import { HashRouter } from "react-router-dom";
import { AuthProvider } from "../../context/AuthContext";
import App from "../../App";
import "../../styles.css";

export default class ReactHost extends KingdeeElement {
  renderedCallback() {
    if (this._reactRoot) return;

    const host = this.template.host;
    this._mount = document.createElement("div");
    this._mount.className = "kwc-react-mount";
    host.appendChild(this._mount);

    this._reactRoot = ReactDOM.createRoot(this._mount);
    this._reactRoot.render(
      React.createElement(
        React.StrictMode,
        null,
        React.createElement(
          HashRouter,
          null,
          React.createElement(
            AuthProvider,
            null,
            React.createElement(App)
          )
        )
      )
    );
  }

  disconnectedCallback() {
    if (this._reactRoot) {
      this._reactRoot.unmount();
      this._reactRoot = null;
    }
    if (this._mount?.parentNode) {
      this._mount.parentNode.removeChild(this._mount);
    }
    this._mount = null;
  }
}
