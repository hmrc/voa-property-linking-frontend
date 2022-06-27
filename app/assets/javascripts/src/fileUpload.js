(function() {

    'use strict';

    var root = this, $ = root.jQuery;

    if (typeof VOA === 'undefined') {
        root.VOA = {};
    }

    var FileUpload = function (){
        var $element = $('#newFile');
        var errorHeading = $('#error-common-title').text();
        var errorMessages = '<div id="error-summary" class="govuk-error-summary" aria-labelledby="error-summary-heading" role="alert" tabindex="-1" data-module="govuk-error-summary">'+
                            '<h2 class="govuk-error-summary__title" id="error-summary-heading">' + errorHeading + '</h2>' +
                            '<ul class="govuk-list govuk-error-summary__list"><li></li></ul></div>';

        $element.change(function(){
            var file = this.files[0];

            if(!file) {
                $('#error-summary').remove();
                $('button.govuk-button').attr('disabled','disabled');
                return;
            }

            clearErrors();
            $('#error-summary').remove();
            $('#newFile').attr('disabled','disabled');
            $('#newFileButton').css('display', 'none');
            $('button.govuk-button').attr('disabled','disabled');

            $('#message-warning').removeClass('govuk-!-display-none');

            function resolveMimeType(upload) {
                var defaultErrorText = $('#supportingDocuments-defaultError').text()
                var extension = upload.name.substr( (upload.name.lastIndexOf('.') +1) ).toLowerCase();
                if(extension === "csv"){
                    return defaultErrorText;
                }

                if(file.type){
                   return file.type;
                }

                var mime;
                switch(extension){
                            case "xls":
                            mime = "application/vnd.ms-excel";
                            break;

                            case "xlsb":
                            mime = "application/vnd.ms-excel.sheet.binary.macroEnabled.12";
                            break;

                            case "xlsx":
                            mime = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
                            break;

                            case "pdf":
                            mime = "application/pdf";
                            break;

                            case "docx":
                            mime = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
                            break;

                            case "doc":
                            mime = "application/msword";
                            break;

                            case "jpg":
                            mime = "image/jpeg";
                            break;

                            case "png":
                            mime = "image/png";
                            break;

                    default:
                        mime = file.type ? file.type : defaultErrorText;
                        break;
                }
                return mime;
            }

            var resolvedMimeType = resolveMimeType(file);
            var csrfToken = $("#uploadForm input[name='csrfToken']").val();
            var fileName = file.name.replace(/[^0-9A-Za-z. -]/g,' ');
            var submissionId = $("#submissionId").text();
            var evidenceType = ($('input[type="radio"][name="evidenceType"]').length) ? $('input[name="evidenceType"]:checked').val() : $("#evidenceType").text();
            $.ajax({
                url: $("#businessRatesAttachmentsInitiateUploadURL").text(),
                method: "POST",
                contentType: "application/json",
                data: JSON.stringify({
                    "fileName": submissionId + "-" + fileName,
                    "mimeType": resolvedMimeType,
                    "evidenceType": evidenceType,
                    "csrfToken": csrfToken
                }),
                headers: {
                    'Csrf-Token': csrfToken
                },
                crossDomain: false,
                cache: false
            }).fail(function (jqXHR) {
                if (jqXHR.status === 400) {
                    addError(jqXHR.responseText);
                } else if (jqXHR.status === 413) {
                    $('#message-warning').addClass('govuk-!-display-none');
                    addError($('#errorsFileSizeTooLarge').text());
                } else if (jqXHR.status > 400) {
                    $('#message-warning').addClass('govuk-!-display-none');
                    addError($('#errorsUpscan').text());
                } else if (!jqXHR.status) {
                    var continueUrl = $("#startClaimUrl").text();
                    window.location.href =
                        $("#signInPageUrl").text()+"?continue=" + encodeURIComponent(continueUrl)+ "&origin=voa-property-linking-frontend";
                }
                $('#newFile').removeAttr('disabled');
                $('#newFileButton').css('display', '');
                $('button.govuk-button').removeAttr('disabled');
            }).done(function(data, statusText, resObject) {
                fileUpload(resObject.responseJSON, file, csrfToken);
                $('#message-warning').addClass('govuk-!-display-none');
            });

            $(this).closest('.form-group').removeClass('error');
            $(this).closest('.form-group').find('.error-message').remove();
        });

        $element.attr({'tabindex':'-1', 'style': 'position: absolute; left: -9999px; top: -9999px; z-index: -9999'});

        $(document).on('click', '#newFileButton', function(e){
            e.preventDefault();
            $element.trigger('click');
        });

        var errorTitlePrefix = $('#accessibility-error-label').text();
        var title = $(document).prop('title');
        function addError(message){
            if(title.startsWith(errorTitlePrefix)){
                $(document).prop('title', title);
            } else {
                $(document).prop('title', errorTitlePrefix + ' ' + title);
            }
            $('#errorsList').html(errorMessages.replace('<li></li>', '<li><a href="#newFileGroup">'+ message +'</a></li>'));
            $('<span id="file-upload-1-error" class="govuk-error-message"><span class="govuk-!-display-none">'+ errorTitlePrefix +'</span>'+ message +'</span>').insertBefore('#newFileButton');
            $('#newFileGroup').addClass('govuk-form-group--error');
            $('#message-warning').addClass('govuk-!-display-none');
            $('#error-summary').focus();
        }

        function clearErrors(){
            $(document).prop('title', title.replace(errorTitlePrefix,''));
            $('#errorsList').html("");
            $('.govuk-error-summary').remove();
            $('#file-upload-1-error').remove();
            $('#newFileGroup').removeClass('govuk-form-group--error');
        }

        function fileUpload(form, file, csrfToken){
            $('#uploadForm').attr('action', form.uploadRequest.href);

            $('input[name ="csrfToken"]').remove();

            Object.keys(form.uploadRequest.fields).map(function(k) {
                $('#initiateFields').append('<input class="label-span govuk-!-display-none" name="' + k + '" value="' + form.uploadRequest.fields[k] + '">')
            })

            $('#newFile').removeAttr('disabled');
            $('#uploadForm').submit();
        };

    };



    root.VOA.FileUpload = FileUpload;

}).call(this);
