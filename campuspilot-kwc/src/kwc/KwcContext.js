import React, { createContext, useContext } from "react";

const noopBridge = {
  invoke: () => false,
  notify: () => false,
};

const defaultKwcContext = {
  props: {},
  data: null,
  configItems: [],
  cardRowData: null,
  themeColor: "",
  lock: false,
  model: null,
  rootElement: null,
  bridge: noopBridge,
};

const KwcContext = createContext(defaultKwcContext);

export function KwcProvider({ value, children }) {
  return <KwcContext.Provider value={{ ...defaultKwcContext, ...value }}>{children}</KwcContext.Provider>;
}

export function useKwcContext() {
  return useContext(KwcContext);
}

export default KwcContext;
