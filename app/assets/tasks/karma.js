'use strict';

// Dependencies
var gulp = require('gulp'),
	Server = require('karma').Server;

// Test suite task
gulp.task('test', function (done) {
  return new Server({
    configFile: __dirname + '/../karma.conf.js',
    singleRun: true
  }, done).start();
});

// Watch
gulp.task('test:watch', function (done) {
  new Server({
    configFile: __dirname + '/../karma.conf.js'
  }, done).start();
});