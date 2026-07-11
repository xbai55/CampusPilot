/**
 * Global toast utility – dispatches a "toast" custom event on window.
 * The <cp-toast> KWC component listens and displays the message.
 */
export function showToast(message) {
  window.dispatchEvent(new CustomEvent("toast", { detail: message }));
}
