(function () {

    'use strict';

    var root = this, $ = root.jQuery;

    if (typeof VOA === 'undefined') {
        root.VOA = {};
    }

    var JqueryFiler = function (){

        $('.filer_input').filer({
            afterRender: function(){
                $('.jFiler').each(function(){
                    $(this).before('<span class="file-error"></span>');
                });
                $('.jFiler-theme-govuk .button-secondary').click(function(e){
                    e.preventDefault();
                });
            },
            limit: 1,
            maxSize: 10,
            fileMaxSize: 10,
            extensions: ['jpg', 'jpeg', 'pdf'],
            showThumbs: true,
            addMore: true,
            setIndex: function(){
                $('.jFiler-items-list .jFiler-item').each(function(){
                    var $element = $(this).closest('.jFiler-item');
                    var i = $(this).index();
                    //console.log(i);
                    //$element.attr('data-jfiler-index', i);
                    $element.find('.typeOfDoc label').attr('for' , 'typeOfDoc_'+i);
                    $element.find('.typeOfDoc select').attr('id' , 'typeOfDoc_'+i).attr('name' , 'typeOfDoc_'+i);
                });
            },
            onSelect: function() {
                $('.file-error').text('');
                $('.file-input-group').removeClass('form-grouped-error');
                this.setIndex();
                //$('.jFiler-theme-govuk .button-secondary').insertAfter('.jFiler-items');

            },
            onRemove: function(){
                $('.file-error').text('');
                $('.file-input-group').removeClass('form-grouped-error');
                //this.setIndex();
            },
            dialogs: {
                alert: function(text) {
				   //return alert(text);
                   $('.file-error').text(text);
                   $('.file-input-group').addClass('form-grouped-error');
			    },
    			confirm: function (text, callback) {
                    callback();
    			}
		    },
            theme: 'govuk',
            changeInput: '<a href="#" class="button-secondary">Choose a file to upload</a>',
            templates: {
                box: '<ul class="jFiler-items-list jFiler-items-default"></ul>',
                item: '<li class="jFiler-item">'+
                        '<div class="jFiler-item-container">'+
                            '<div class="jFiler-item-inner">'+
                                '<div class="jFiler-item-info pull-left">'+
                                    '<div class="jFiler-item-title" title="{{fi-name}}">{{fi-name | limitTo:40}}</div>'+
                                    '<div class="jFiler-item-others">'+
                                        '<span>&nbsp;({{fi-size2}})</span>'+
                                        '<span class="jFiler-item-status">{{fi-progressBar}}</span>'+
                                    '</div>'+
                                    '<div class="jFiler-item-assets">'+
                                        '<ul class="list-inline">'+
                                            '<li><a class="jFiler-item-trash-action" href="#">Remove</a></li>'+
                                        '</ul></div></div></div></div></li>',
                progressBar: '<div class="bar"></div>'
                },
                captions: {
    			button: 'Choose a file',
    			feedback: 'Choose a file to upload',
    			feedback2: 'files were chosen',
    			drop: 'Drop file here to upload',
    			removeConfirmation: false,
    			errors: {
    				filesLimit: 'Only {{fi-limit}} file allowed to be uploaded.',
    				filesType: 'Only JPG and PDF files are allowed to be uploaded.',
    				filesSize: 'Max file size is 10MB.',
    				filesSizeAll: 'Files you\'ve choosen are too large! Please upload files up to {{fi-maxSize}} MB.'
    			}
		    }
        });

    };

    root.VOA.JqueryFiler = JqueryFiler;

    }).call(this);
