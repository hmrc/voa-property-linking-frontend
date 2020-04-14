'use strict';
// Dependencies
var gulp = require('gulp'),
	requireDir = require('require-dir');
// Require all files in the 'tasks' folder
requireDir('./tasks', { recurse: true });
// Execute default task
gulp.task('default', ['copy', 'scripts']);