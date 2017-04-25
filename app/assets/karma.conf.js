// karma.conf.js
module.exports = function(config) {
  config.set({
    frameworks: ['jasmine'],
    reporters: ['spec'],
    browsers: ['PhantomJS'],
    files: [
      './node_modules/jquery/dist/jquery.min.js',
      './tests/vendor/jasmine-jquery.js',
      './node_modules/govuk_frontend_toolkit/javascripts/govuk/selection-buttons.js',
      './javascripts/src/*.js',
      './javascripts/vendor/jquery.filer.min.js',
      './javascripts/vendor/jquery.dataTables.min.js',
      './tests/spec/*.js',
      {
        pattern: './tests/fixtures/*.html',
        include: false
      }
    ]
  });
};
