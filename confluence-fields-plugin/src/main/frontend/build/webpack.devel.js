const path = require('path');
const merge = require('webpack-merge');
const webpack = require('webpack');
const os = require('os');
const WRMPlugin = require('atlassian-webresource-webpack-plugin');
const providedDependencies = require('./providedDependencies');

const PLUGIN_TARGET_DIR = path.join(__dirname, '..', '..', '..', '..', 'target');
const SRC_DIR = path.join(__dirname, '..', 'src');
const OUTPUT_PATH = path.join(PLUGIN_TARGET_DIR, 'classes');

// https://developer.atlassian.com/server/jira/platform/web-resource/#WebResourcePluginModule-contextsforjira
const getWrmPlugin = (watch = false, watchPrepare = false) => {
  return  new WRMPlugin({
    pluginKey: 'com.mesilat:confluence-fields',
    xmlDescriptors: path.join(OUTPUT_PATH, 'META-INF', 'plugin-descriptors', 'wr-webpack-bundles.xml'),
    contextMap: {
      'confluence-fields-portal': ['customerportal'],
      'confluence-fields-frontend': [
        'jira.view.issue',
        'jira.edit.issue',
        'jira.create.issue',
        'jira.inline.dialog',
        'gh-rapid-detailsview'
      ],
      'confluence-fields-changehistory': ['jira.view.issue'],
      'confluence-fields-inline': ['jira.view.issue'],
      'confluence-fields-settings': ['jira.admin'],
      'confluence-fields-project': ['jira.admin.conf']
    },
    providedDependencies: providedDependencies,
    watch: watch,
    watchPrepare: watchPrepare,
  });
};

const webpackConfig = {
  mode: 'development',
  entry: {
    'confluence-fields-portal': path.join(SRC_DIR, 'portal.js'),
    'confluence-fields-frontend': path.join(SRC_DIR, 'view-edit.js'),
    'confluence-fields-changehistory': path.join(SRC_DIR, 'changehistory.js'),
    'confluence-fields-inline': path.join(SRC_DIR, 'inline-edit.js'),
    'confluence-fields-settings': path.join(SRC_DIR, 'common-settings.js'),
    'confluence-fields-project': path.join(SRC_DIR, 'project-settings.js')
  },
  module: {
    rules: [
      {
        test: /\.jpe?g$/,
        use: [
          {
            loader: 'file-loader',
          },
        ],
      },
      {
        test: /\.less$/,
        use: [
          {
            loader: 'style-loader',
          },
          {
            loader: 'css-loader',
            options: {
              modules: true,
            },
          },
          {
            loader: 'less-loader',
          },
        ],
      },
    ]
  },
  output: {
    path: OUTPUT_PATH,
    filename: '[name].[chunkhash].js',
    chunkFilename: '[name].[chunkhash].js',
    jsonpFunction: 'confluenceFieldsFrontend',
  },
  optimization: {
    splitChunks: false,
    runtimeChunk: false,
  },
  devtool: 'cheap-module-source-map',
  resolve: {
    modules: [
      'node_modules',
      SRC_DIR,
    ],
  },
  plugins: [new webpack.NamedChunksPlugin()]
};

const hostname = os.hostname();
const devServerPort = '3333';

const watchPrepareConfig = {
  output: {
    publicPath: `http://${hostname}:${devServerPort}/`,
    filename: '[name].js',
    chunkFilename: '[name].chunk.js',
  },
  plugins: [
    getWrmPlugin(true, true)
  ],
};

const watchConfig = {
  output: {
    publicPath: `http://${hostname}:${devServerPort}/`,
    filename: '[name].js',
    chunkFilename: '[name].chunk.js',
  },
  devServer: {
    host: hostname,
    port: devServerPort,
    overlay: true,
    hot: true,
    headers: { 'Access-Control-Allow-Origin': '*' },
    disableHostCheck: true
  },
  plugins: [
    new webpack.NamedModulesPlugin(),
    new webpack.HotModuleReplacementPlugin(),
    getWrmPlugin(true),
  ]
};

const devConfig = {
  optimization: {
    splitChunks: {
      minSize: 0,
      chunks: 'all',
      maxInitialRequests: Infinity,
    },
    runtimeChunk: true,
  },
  plugins: [
    getWrmPlugin(),
  ],
}

module.exports = (env) => {
  if (env === "watch:prepare") {
    return merge([webpackConfig, watchPrepareConfig]);
  }

  if (env === "watch") {
    return merge([webpackConfig, watchConfig, watchPrepareConfig]);
  }

  return merge([webpackConfig, devConfig]);
};
