'use strict';

// Dependencies
var gulp = require('gulp'),
    cssmin = require('gulp-cssmin'),
    rename = require('gulp-rename'),
	sass = require('gulp-sass');

// Sass task
gulp.task('sass', function () {
    return gulp.src('./sass/**/*')
        .pipe(sass({
            style: 'expanded',
            sourceComments: 'normal'
        }).on('error', sass.logError))
        .pipe(gulp.dest('../../public/stylesheets'));
});

// Minify govuk_template css
gulp.task('minify-css:govuk_template', function () {
    gulp.src('./node_modules/govuk_template_mustache/assets/stylesheets/*.css')
        .pipe(cssmin())
        .pipe(rename({suffix: '.min'}))
        .pipe(gulp.dest('../../public/stylesheets'));
});

// Run sass task
gulp.task('build-sass', [ 'sass','minify-css:govuk_template']);