'use strict';

// Dependencies
var gulp = require('gulp'),
    copy = require('gulp-copy');

// Copy jQuery
gulp.task('copy:jquery', function () {
    return gulp.src(['./node_modules/jquery/dist/jquery.min.js'])
        .pipe(gulp.dest('../../public/javascripts/vendor'));
});


// Copy css
gulp.task('copy:css', function () {
    return gulp.src(['../assets/css/*.css'])
        .pipe(gulp.dest('../../public/stylesheets'));
});


// Copy public html
gulp.task('copy:publicHtml', function () {
    return gulp.src(['./public-folder/*.*'])
        .pipe(gulp.dest('../../public/html'));
});


// Run copy task
gulp.task('copy', ['copy:jquery', 'copy:publicHtml', 'copy:css']);
