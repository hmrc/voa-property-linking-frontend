'use strict';

// Dependencies
var gulp = require('gulp'),
    watch = require('gulp-watch');

// Watch task
gulp.task('watch', function () {
    gulp.watch('sass/**/*.scss', ['sass']);
    gulp.watch('javascripts/**/*.js', ['scripts']);
});