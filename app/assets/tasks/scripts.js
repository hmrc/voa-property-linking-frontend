'use strict';

// Dependencies
var gulp = require('gulp'),
	jshint = require('gulp-jshint'),
    uglify = require('gulp-uglify'),
    concat = require('gulp-concat');

// JS lint
gulp.task('jshint', function() {
  return gulp.src(['./javascripts/src/**/*'])
    .pipe(jshint())
    .pipe(jshint.reporter('jshint-stylish'))
    .pipe(jshint.reporter('fail'));
});

// Concat/Minify JS
gulp.task('javascripts', function(callback) {
  return gulp.src([
    './javascripts/src/*.js',
    './node_modules/govuk_frontend_toolkit/javascripts/vendor/polyfills/bind.js',
    './node_modules/govuk_frontend_toolkit/javascripts/govuk/selection-buttons.js'
    ])
    .pipe(concat('all.min.1.js'))
    .pipe(gulp.dest('../../public/javascripts'))
    .pipe(uglify())
    .pipe(gulp.dest('../../public/javascripts'));
});

// Run scripts task
gulp.task('scripts', ['javascripts']);
