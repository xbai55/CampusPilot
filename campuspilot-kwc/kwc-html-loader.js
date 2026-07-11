/**
 * Webpack loader for KWC HTML template files.
 *
 * Compiles `.html` template files into JavaScript template functions
 * that can be consumed by the KWC engine at runtime.
 */
const compileTemplate = require("@kdcloudjs/kwc-template-compiler");

module.exports = function kwcHtmlLoader(source) {
  const filename = this.resourcePath;
  const callback = this.async();

  try {
    // compile is the default export of @kdcloudjs/kwc-template-compiler
    const compile =
      compileTemplate.default || compileTemplate.compile || compileTemplate;
    const result = compile(source, filename, {});

    callback(null, result.code, null);
  } catch (err) {
    callback(err);
  }
};
