module.exports = function(grunt) {
	'use strict';
    grunt.initConfig({
        // Clean
        clean: {
            //remove any unwanted files
            end: [
                'public/javascripts/application.js',
                'public/javascripts/common.js',
                'public/javascripts/feedback.js',
                'public/javascripts/messages.js',
                'public/javascripts/postcodeLookup.js',
                'public/javascripts/radioToggle.js',
                'public/javascripts/voaFor.js',
                'public/javascripts/intelAlerts.js'
            ]
        },
        // Builds Sass
        sass: {
            dev: {
                options: {
                    loadPath: [
                        'govuk_modules/govuk_template/assets/stylesheets',
                        'govuk_modules/govuk_frontend_toolkit/stylesheets'
                    ],
                    lineNumbers: true,
                    style: 'expanded'
                },
                files: [{
                    expand: true,
                    cwd: 'frontend/sass',
                    src: ['*.scss'],
                    dest: 'public/stylesheets/',
                    ext: '.css'
                }]
            },
            prod: {
                options: {
                    loadPath: [
                        'govuk_modules/govuk_template/assets/stylesheets',
                        'govuk_modules/govuk_frontend_toolkit/stylesheets'
                    ],
                    lineNumbers: false,
                    style: 'compressed',
                    sourcemap: false
                },
                files: [{
                    expand: true,
                    cwd: 'frontend/sass',
                    src: ['*.scss'],
                    dest: 'public/stylesheets/',
                    ext: '.min.css'
                }]
            }
        },

        // Copies templates and assets from external modules and dirs
        copy: {
            govukElements: {
                files: [{
                    expand: true,
                    cwd: 'node_modules/govuk-elements-sass/public/sass',
                    src: ['**', '!_govuk-elements.scss'],
                    dest: 'frontend/sass'
                }]
            },
            assets: {
                files: [{
                    expand: true,
                    cwd: 'frontend/',
                    src: ['**/*', '!sass/**', '!jasmine/**', '!javascripts/polyfills/**'],
                    dest: 'public/'
                }]
            },
            govuk: {
                files: [{
                    expand: true,
                    cwd: 'node_modules/govuk_frontend_toolkit',
                    src: '**',
                    dest: 'govuk_modules/govuk_frontend_toolkit/'
                }, {
                    expand: true,
                    cwd: 'node_modules/govuk_template_mustache/',
                    src: '**',
                    dest: 'govuk_modules/govuk_template/'
                }]
            },
            govukModules: {
                files: [{
                    expand: true,
                    cwd: 'govuk_modules/govuk_template/assets/images',
                    src: '**.*',
                    dest: 'public/images/'
                }, {
                    expand: true,
                    cwd: 'govuk_modules/govuk_template/assets/stylesheets',
                    src: '**',
                    dest: 'public/stylesheets/'
                }, {
                    expand: true,
                    cwd: 'govuk_modules/govuk_frontend_toolkit/images',
                    src: '**',
                    dest: 'public/images/'
                }]
            },
            cutstomImages: {
                files: [{
                    expand: true,
                    cwd: 'frontend/images',
                    src: '**',
                    dest: 'public/images/'
                }]
            }

        },
        cssmin: {
            target: {
                files: [{
                    expand: true,
                    cwd: 'public/stylesheets',
                    src: ['*.css', '!*.min.css'],
                    dest: 'public/stylesheets',
                    ext: '.min.css'
                }]
            }
        },

        // workaround for libsass
        replace: {
            fixSass: {
                src: ['govuk_modules/govuk_template/**/*.scss', 'govuk_modules/govuk_frontend_toolkit/**/*.scss'],
                overwrite: true,
                replacements: [{
                    from: /filter:chroma(.*);/g,
                    to: 'filter:unquote("chroma$1");'
                }]
            }
        },
        jshint: {
            options: grunt.file.readJSON('.jshintrc'),
            javascripts: {
                src: [
                    'Gruntfile.js',
                    'frontend/javascripts/*.js'
                ]
            }
        },
        uglify: {

            app: {
                options: {
                    mangle: true,
                    beautify: false
                },
                files: {
                    'public/javascripts/app.min.js': [
                        'frontend/javascripts/**.js'
                    ]
                }
            },
            gds: {
                files: {
                    'public/javascripts/selection-buttons.min.js': [
                        'node_modules/govuk_frontend_toolkit/javascripts/govuk/selection-buttons.js'
                    ]
                }
            },
            govukTemplate: {
                files: {
                    'public/javascripts/govuk-template.min.js': [
                        'govuk_modules/govuk_template/assets/javascripts/govuk-template.js'
                    ]
                }
            },
            ie: {
                files: {
                    'public/javascripts/ie.min.js': [
                        'govuk_modules/govuk_template/assets/javascripts/ie.js'
                    ]
                }
            },
            webfont: {
                files: {
                    'public/javascripts/vendor/goog/webfont-debug.min.js': [
                        'govuk_modules/govuk_template/assets/javascripts/vendor/goog/webfont-debug.js'
                    ]
                }
            },
            bind: {
                files: {
                    'public/javascripts/polyfills/bind.min.js': [
                        'govuk_modules/govuk_frontend_toolkit/javascripts/vendor/polyfills/bind.js'
                    ]
                }
            },
            detailsPolyfill: {
                files: {
                    'public/javascripts/polyfills/details.polyfill.min.js': [
                        'frontend/javascripts/polyfills/details.polyfill.js'
                    ]
                }
            },
            toISOStringPolyfill: {
                files: {
                    'public/javascripts/polyfills/toISOString.polyfill.min.js': [
                        'frontend/javascripts/polyfills/toISOString.polyfill.js'
                    ]
                }
            },
            dev: {
                options: {
                    mangle: false,
                    beautify: true
                },
                files: {
                    'public/javascripts/app.js': [
                        'frontend/javascripts/**.js'
                    ]
                }
            }
        },
        // Watches assets and sass for changes
        watch: {
            css: {
                files: ['app/assets/sass/**/*.scss'],
                tasks: ['sass'],
                options: {
                    spawn: false
                }
            },
            assets: {
                files: ['app/assets/**/*', '!app/assets/sass/**'],
                tasks: ['copy:assets'],
                options: {
                    spawn: false
                }
            }
        }
    });

    [
        'grunt-contrib-copy',
        'grunt-contrib-watch',
        'grunt-contrib-clean',
        'grunt-sass',
        'grunt-contrib-jshint',
        'grunt-contrib-uglify',
        'grunt-text-replace',
        'grunt-contrib-jasmine',
        'grunt-contrib-sass',
        'grunt-contrib-cssmin'
    ].forEach(function(task) {
        grunt.loadNpmTasks(task);
    });

    grunt.registerTask('generate-assets', [
        //'clean:start',
        'jshint',
        'copy',
        'uglify',
        'replace',
        'sass',
        'cssmin',
        'clean:end'
    ]);

    grunt.registerTask('default', [
        'generate-assets'
    ]);

    grunt.event.on('watch', function(action, filepath, target) {
        if (target === 'assets') {
            grunt.config('copy.assets.files.0.src', filepath.replace('app/assets/', ''));
        }
    });
};