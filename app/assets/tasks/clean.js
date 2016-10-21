'use strict';

// Dependencies
var gulp = require('gulp'),
    del = require('del');

// Clean public folder task
gulp.task('clean:public', function () {
  return del(['../../public'], {force: true});
});

// Clean node_modules folder task
gulp.task('clean:node_modules', function () {
  return del(['node_modules'], {force: true});
});

// Run clean all task
gulp.task('clean', [ 'clean:public','clean:node_modules']);