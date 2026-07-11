import React from "react";
import { HashRouter } from "react-router-dom";
import { AuthProvider } from "./context/AuthContext";
import { KwcProvider } from "./kwc/KwcContext";
import App from "./App";

export default function KwcApp({ kwc }) {
  return (
    <React.StrictMode>
      <KwcProvider value={kwc}>
        <HashRouter>
          <AuthProvider kwcData={kwc?.data}>
            <App kwc={kwc} />
          </AuthProvider>
        </HashRouter>
      </KwcProvider>
    </React.StrictMode>
  );
}
