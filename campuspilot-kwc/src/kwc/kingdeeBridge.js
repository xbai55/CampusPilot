export function createKingdeeBridge(model) {
  const dom = model?.dom || null;

  function notify(type, detail = {}) {
    if (!dom || typeof window === "undefined") return false;
    dom.dispatchEvent(
      new CustomEvent(`campuspilot:${type}`, {
        detail,
        bubbles: true,
        composed: true,
      })
    );
    return true;
  }

  function invoke(eventName, payload = {}) {
    notify(eventName, payload);
    if (!model || typeof model.invoke !== "function") return false;
    model.invoke(eventName, payload);
    return true;
  }

  return { invoke, notify };
}

export default createKingdeeBridge;
