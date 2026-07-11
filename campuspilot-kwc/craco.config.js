const path = require("path");

module.exports = {
  webpack: {
    configure: (webpackConfig) => {
      const oneOfRule = webpackConfig.module.rules.find(
        (r) => r.oneOf && Array.isArray(r.oneOf)
      );
      const target = oneOfRule ? oneOfRule.oneOf : webpackConfig.module.rules;
      const kwcRoots = [
        path.resolve(__dirname, "src/kwc"),
        path.resolve(__dirname, "src/modules"),
      ];

      target.unshift({
        test: /\.js$/,
        include: kwcRoots,
        use: [{ loader: path.resolve(__dirname, "kwc-loader.js") }],
      });

      target.unshift({
        test: /\.html$/,
        include: kwcRoots,
        use: [{ loader: path.resolve(__dirname, "kwc-html-loader.js") }],
      });

      target.unshift({
        test: /\.css$/,
        include: kwcRoots,
        use: [{ loader: path.resolve(__dirname, "kwc-css-loader.js") }],
      });

      webpackConfig.resolve.extensions.push(".html");

      return webpackConfig;
    },
  },
};
