(function() {

    'use strict';

    var root = this, $ = root.jQuery;

    if (typeof VOA === 'undefined') {
        root.VOA = {};
    }

    var FileUpload = function (){
        var $element = $('#newFile');

        var errorMessages = '<div id="error-summary" class="error-summary error-summary--show" role="group" aria-labelledby="error-summary-heading" tabindex="-1">'+
            '<h2 class="heading-medium error-summary-heading" id="error-summary-heading">There&rsquo;s been a problem</h2>'+
            '<p>Check the following</p>'+
            '<ul class="error-summary-list"><li></li></ul></div>';

        $element.change(function(){
            $('#error-summary').remove();
            $('#uploadFile').attr('disabled','disabled');
            var file = this.files[0];


            if(file.type){
                $(this).after('<div class="message-warning" id="message-warning"><p>Please wait whilst your file is uploading. This may take some time.</p></div>');
            }

            function resolveMimeType(upload) {
                if(file.type){
                    return file.type;
                }
                var extension = upload.name.substr( (upload.name.lastIndexOf('.') +1) );
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
                            mime = file.type ? file.type : 'Unknown/Extension missing';
                            break;

                        }
                 return mime;
            }
            var resolvedMimeType = resolveMimeType(file);
            var csrfToken = $("#uploadForm input[name='csrfToken']").val();
            var fileName = file.name.replace(/[^0-9A-Za-z. -]/g,' ');
            var submissionId = $("#submissionId").text();
            //$(this).closest('form').submit();
            $.ajax({
                url: $("#businessRatesAttachmentsInitiateUploadURL").text(),
                method: "POST",
                contentType: "application/json",
                data: JSON.stringify({
                    "fileName": submissionId + "-" + fileName,
                    "mimeType": resolvedMimeType,
                    "csrfToken": csrfToken
                }),
                headers: {
                    'Csrf-Token': csrfToken
                },
                crossDomain: false,
                cache: false
            }).error(function (jqXHR, textStatus, errorThrown) {
                if (jqXHR.status === 400) {
                    $('#errorsList').html(errorMessages.replace('<li></li>', '<li>' + $('#errorsBusinessRatesAttachmentUnsupportedFiles').text() + '</li>'));
                    $('#message-warning').remove();
                } else if (jqXHR.status === 413) {
                    $('#errorsList').html(errorMessages.replace('<li></li>', '<li>' + $('#errorsFileSizeTooLarge').text() + '</li>'));
                    $('#message-warning').remove();
                } else if (jqXHR.status > 400) {
                    $('#errorsList').html(errorMessages.replace('<li></li>', '<li>' + $('#errorsUpscan').text() + '</li>'));
                    $('#message-warning').remove();
                } else if (!jqXHR.satus) {
                    var continueUrl = $("#startClaimUrl").text();
                    window.location.href =
                        $("#signInPageUrl").text()+"?continue=" + encodeURIComponent(continueUrl)+ "&origin=voa-property-linking-frontend";
                }

            }).done(function(data, statusText, resObject) {
                fileUpload(resObject.responseJSON, file, csrfToken);
                $('#message-warning').remove();
            });

            $(this).closest('.form-group').removeClass('error');
            $(this).closest('.form-group').find('.error-message').remove();
            //$('#newFileButton').remove();
        });

        $element.attr({'tabindex':'-1', 'style': 'position: absolute; left: -9999px; top: -9999px; z-index: -9999'});
        $('[for="newFile"] .label-span').addClass('visuallyhidden');

        $(document).on('click', '#newFileButton', function(e){
            e.preventDefault();
            $element.trigger('click');
        });


        function fileUpload(form, file, csrfToken){
            $('#uploadForm').attr('action', form.uploadRequest.href);

            $('input[name ="csrfToken"]').remove();

            Object.keys(form.uploadRequest.fields).map(function(k) {
                $('#initiateFields').append('<input class="label-span visuallyhidden" name="' + k + '" value="' + form.uploadRequest.fields[k] + '">')
            })

            $('#uploadForm').submit();
        };

    };



    root.VOA.FileUpload = FileUpload;

}).call(this);