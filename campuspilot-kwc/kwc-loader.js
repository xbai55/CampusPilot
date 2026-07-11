/**
 * Webpack loader for KWC (Kingdee Web Components) files.
 *
 * Intercepts .js files under src/kwc/ and passes them through the KWC compiler
 * (transformSync), which handles:
 *   - HTML template compilation (companion .html -> JS template function)
 *   - CSS stylesheet scoping (companion .css -> scoped stylesheets)
 *   - Decorator transformation (@api, @track, @wire)
 *   - KWC internal component metadata registration
 *
 * Component naming convention:
 *   src/kwc/<name>/<name>.js -> registered as <cp-name> custom element
 *
 * The root host is intentionally not registered here. It is created through
 * KWC's public createElement(tagName, { is }) API in src/index.js; registering
 * it before createElement would make KWC reject the tag as already defined.
 */
const { transformSync } = require("@kdcloudjs/kwc-compiler");
const path = require("path");
const fs = require("fs");

module.exports = function kwcLoader(source) {
  const filename = this.resourcePath;
  const dir = path.dirname(filename);
  const basename = path.basename(filename, ".js");

  // Register HTML / CSS file dependencies so webpack watches them.
  const htmlPath = path.join(dir, `${basename}.html`);
  const cssPath = path.join(dir, `${basename}.css`);
  if (fs.existsSync(htmlPath)) this.addDependency(htmlPath);
  if (fs.existsSync(cssPath)) this.addDependency(cssPath);

  // src/kwc/... uses the cp namespace in this project.
  const namespace = "cp";
  const callback = this.async();

  const className = basename.charAt(0).toUpperCase() + basename.slice(1);
  const kebabName = basename.replace(/([a-z0-9])([A-Z])/g, "$1-$2").toLowerCase();
  const tagName = `${namespace}-${kebabName}`;

  try {
    const result = transformSync(source, filename, {
      name: basename,
      namespace,
      scopedStyles: true,
      enableStaticContentOptimization: true,
    });

    // React-rendered KWC tags need browser registration. The root host is
    // created by KWC createElement(), so it must remain unregistered until then.
    const shouldDefineCustomElement = basename !== "reactHost";
    const code = shouldDefineCustomElement
      ? result.code +
        `\nif (!customElements.get("${tagName}")) customElements.define("${tagName}", ${className}.CustomElementConstructor);\n`
      : result.code;

    callback(null, code, null);
  } catch (err) {
    callback(err);
  }
};
