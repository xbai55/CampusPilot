process.env.NODE_ENV = process.env.NODE_ENV || "production";
process.env.BABEL_ENV = process.env.BABEL_ENV || "production";

const fs = require("fs");
const path = require("path");
const webpack = require("webpack");
const MiniCssExtractPlugin = require("mini-css-extract-plugin");
const postcss = require("postcss");

const projectRoot = path.resolve(__dirname, "..");
const outputRoot = path.join(projectRoot, "dist", "campuspilot");
const jsOutput = path.join(outputRoot, "js");
const cssOutput = path.join(outputRoot, "css", "campuspilot.css");
const srcRoot = path.join(projectRoot, "src");
const kwcRoots = [path.join(srcRoot, "kwc"), path.join(srcRoot, "modules")];

function isInside(file, roots) {
  return roots.some((root) => file.startsWith(root));
}

function isKwcComponentFile(file) {
  if (!isInside(file, kwcRoots)) return false;
  const basename = path.basename(file, ".js");
  return path.basename(path.dirname(file)) === basename;
}

function scopeSelector(selector) {
  let scoped = selector
    .replace(/:root\b/g, ".campus-pilot-root")
    .replace(/\bhtml\b/g, ".campus-pilot-root")
    .replace(/\bbody\b/g, ".campus-pilot-root");

  if (scoped.includes(".campus-pilot-root")) return scoped;
  if (/^(from|to|\d+%)/.test(scoped.trim())) return scoped;
  return `.campus-pilot-root ${scoped}`;
}

async function scopeCss() {
  if (!fs.existsSync(cssOutput)) return;
  const css = fs.readFileSync(cssOutput, "utf8");
  const root = postcss.parse(css);
  root.walkRules((rule) => {
    if (rule.parent?.type === "atrule" && /keyframes$/i.test(rule.parent.name)) return;
    rule.selectors = rule.selectors.map(scopeSelector);
  });
  fs.writeFileSync(cssOutput, root.toString(), "utf8");
}

function runWebpack() {
  fs.rmSync(outputRoot, { recursive: true, force: true });

  const compiler = webpack({
    mode: "production",
    target: ["web", "es5"],
    entry: path.join(srcRoot, "kwc", "index.js"),
    output: {
      path: jsOutput,
      filename: "campuspilot.js",
      publicPath: "../",
      clean: true,
    },
    devtool: false,
    resolve: {
      extensions: [".js", ".jsx", ".html"],
      fallback: { fs: false, path: false },
    },
    module: {
      rules: [
        {
          test: /\.js$/,
          include: srcRoot,
          use: (info) => {
            const loaders = [
              {
                loader: require.resolve("babel-loader"),
                options: {
                  babelrc: false,
                  configFile: false,
                  presets: [require.resolve("babel-preset-react-app")],
                  cacheDirectory: true,
                },
              },
            ];
            if (isKwcComponentFile(info.resource)) {
              loaders.push({ loader: path.join(projectRoot, "kwc-loader.js") });
            }
            return loaders;
          },
        },
        {
          test: /\.html$/,
          include: kwcRoots,
          use: [{ loader: path.join(projectRoot, "kwc-html-loader.js") }],
        },
        {
          test: /\.css$/,
          include: kwcRoots,
          use: [{ loader: path.join(projectRoot, "kwc-css-loader.js") }],
        },
        {
          test: /\.css$/,
          exclude: kwcRoots,
          use: [MiniCssExtractPlugin.loader, { loader: require.resolve("css-loader"), options: { importLoaders: 0 } }],
        },
        {
          test: /\.(png|jpe?g|gif|svg|webp|woff2?)$/i,
          type: "asset/resource",
          generator: { filename: "../assets/[name][ext]" },
        },
      ],
    },
    plugins: [
      new MiniCssExtractPlugin({ filename: "../css/campuspilot.css" }),
      new webpack.DefinePlugin({ "process.env.NODE_ENV": JSON.stringify("production") }),
    ],
    optimization: {
      splitChunks: false,
      runtimeChunk: false,
    },
    performance: false,
  });

  return new Promise((resolve, reject) => {
    compiler.run((err, stats) => {
      compiler.close(() => {});
      if (err) return reject(err);
      if (stats.hasErrors()) {
        return reject(new Error(stats.toString({ all: false, errors: true, warnings: true })));
      }
      process.stdout.write(stats.toString({ colors: true, chunks: false, modules: false, assets: true, warnings: true }) + "\n");
      resolve();
    });
  });
}

runWebpack()
  .then(scopeCss)
  .then(() => {
    const js = path.join(jsOutput, "campuspilot.js");
    const content = fs.readFileSync(js, "utf8");
    if (!content.includes("KDApi") || !content.includes("register") || !content.includes("campuspilot")) {
      throw new Error("KDApi.register('campuspilot', CampusPilot) was not found in bundle output.");
    }
    console.log(`KWC package ready: ${outputRoot}`);
  })
  .catch((err) => {
    console.error(err);
    process.exit(1);
  });
