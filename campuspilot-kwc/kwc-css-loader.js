/**
 * Webpack loader for KWC CSS files.
 *
 * Handles both:
 *   *.css         → regular (unscoped) stylesheets – compiled with scoped:false
 *   *.scoped.css  → scoped stylesheets               – compiled with scoped:true
 *
 * The KWC template compiler imports both variants; the query param
 * `?scoped=true` on *.scoped.css imports is used to toggle compilation mode.
 */
const { transform: transformCss } = require("@kdcloudjs/kwc-style-compiler");

module.exports = function kwcCssLoader(source) {
  const isScoped =
    this.resourceQuery === "?scoped=true" ||
    /\.scoped\.css$/.test(this.resourcePath);

  const callback = this.async();

  try {
    const result = transformCss(source, this.resourcePath, {
      scoped: isScoped,
    });

    callback(null, result.code);
  } catch (err) {
    callback(err);
  }
};
