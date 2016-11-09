(function () {

    'use strict';

    var root = this, $ = root.jQuery;

    if (typeof VOA === 'undefined') {
        root.VOA = {};
    }

    var JqueryFiler = function (){

        $('.filer_input').filer({
            limit: 1,
            maxSize: 5,
            extensions: ['jpg', 'jpeg', 'pdf', 'doc', 'docm', 'docx',  'txt'],
            showThumbs: true,
            addMore: false,
            onSelect: function() {
                $('.error-message').remove();
                $('.jFiler').removeClass('error');
                /*
                $('.typeOfDoc').each(function(){
                    var index = $(this).closest('.jFiler-items-list .jFiler-item').attr('data-jfiler-index');
                    $(this).find('label').attr('for', 'typeOfDoc-'+index);
                    $(this).find('select').attr('id', 'typeOfDoc-'+index);
                    $(this).find('select').attr('name', 'typeOfDoc-'+index);
                });
                */

            },
            dialogs: {
                alert: function(text) {
                   //return alert(text);
                   $('.error-message').remove();
                   $('.jFiler').addClass('error').prepend('<span class=\'error-message\'>'+text+'</span>');
                },
                confirm: function (text, callback) {
                    callback();
                }
            },
            theme: 'govuk',
            templates: {
                box: '<ul class=\'jFiler-items-list jFiler-items-default\'></ul>',
                //item: '<li class=\'jFiler-item\'><div class=\'jFiler-item-container\'><div class=\'jFiler-item-inner\'><div class=\'jFiler-item-icon pull-left\'>{{fi-icon}}</div><div class=\'jFiler-item-info pull-left\'><div class=\'jFiler-item-title\' title=\'{{fi-name}}\'>{{fi-name | limitTo:30}}</div><div class=\'jFiler-item-others\'><span>size: {{fi-size2}}</span><span>type: {{fi-extension}}</span><span class=\'jFiler-item-status\'>{{fi-progressBar}}</span></div><div class=\'jFiler-item-assets\'><ul class=\'list-inline\'><li class=\'typeOfDoc\'><div><label class=\'visuallyhidden\'>Type of document</label><select class=\'form-control form-control-1\' ><option>Type of document</option><option value=\'Floor plan\'>Floor plan</option><option value=\'Photographs\'>Photographs</option><option value=\'Email / letters\'>Email / letters </option><option value=\'Planning permission\'>Planning permission</option><option value=\'Other\'>Other</option></select></div></li><li><a class=\'jFiler-item-trash-action\'>Remove</a></li></ul></div></div></div></div></li>',
                item: '<li class=\'jFiler-item\'><div class=\'jFiler-item-container\'><div class=\'jFiler-item-inner\'><div class=\'jFiler-item-icon pull-left\'>{{fi-icon}}</div><div class=\'jFiler-item-info pull-left\'><div class=\'jFiler-item-title\' title=\'{{fi-name}}\'>{{fi-name | limitTo:30}}</div><div class=\'jFiler-item-others\'><span>size: {{fi-size2}}</span><span>type: {{fi-extension}}</span><span class=\'jFiler-item-status\'>{{fi-progressBar}}</span></div><div class=\'jFiler-item-assets\'><ul class=\'list-inline\'><li><a class=\'jFiler-item-trash-action\'>Remove</a></li></ul></div></div></div></div></li>',
                progressBar: '<div class=\'bar\'></div>'
            },
            captions: {
            button: 'Choose a file',
            feedback: 'Choose a file to upload',
            feedback2: 'file chosen',
            removeConfirmation: 'Are you sure you want to remove this file?',
            errors: {
                filesLimit: 'Only {{fi-limit}} files are allowed to be uploaded.',
                filesType: 'File types must be JPEG, PDF or Word are allowed to be uploaded.',
                filesSize: '{{fi-name}} is too large! Please upload file up to {{fi-maxSize}} MB.',
                filesSizeAll: 'Files you\'ve choosed are too large! Please upload files up to {{fi-maxSize}} MB.'
                }


            }
        });

    };

    root.VOA.JqueryFiler = JqueryFiler;

    }).call(this);
