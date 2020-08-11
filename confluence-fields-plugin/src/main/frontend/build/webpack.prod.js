const path = require('path');
const WRMPlugin = require('atlassian-webresource-webpack-plugin');
const ProvidedDependencies = require('./providedDependencies');

const PLUGIN_TARGET_DIR = path.join(__dirname, '..', '..', '..', '..', 'target');
const SRC_DIR = path.join(__dirname, '..', 'src');
const OUTPUT_PATH = path.join(PLUGIN_TARGET_DIR, 'classes');

const webpackConfig = {
  mode: 'production',
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
    jsonpFunction: 'confluenceFieldsFrontend'
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
    ]
  },
  plugins: [
    new WRMPlugin({
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
      providedDependencies: ProvidedDependencies
    })
  ]
};

module.exports = webpackConfig;
