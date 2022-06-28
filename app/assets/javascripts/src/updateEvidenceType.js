(function() {

    'use strict';

    var root = this, $ = root.jQuery;

    if (typeof VOA === 'undefined') {
        root.VOA = {};
    }

    var UpdateEvidenceType = function (){
        var errorHeading = $('#error-common-title').text();
        var errorMessages = '<div id="error-summary" class="govuk-error-summary" aria-labelledby="error-summary-title" role="alert" tabindex="-1" data-module="govuk-error-summary">'+
                '<h2 class="govuk-error-summary__title" id="error-summary-heading">' + errorHeading + '</h2>'+
                '<ul class="govuk-list govuk-error-summary__list"><li></li></ul></div>';


        var options = '#upload-evidence-options input[type="radio"]';

        $(options).click(function() {
          submitEvidenceType(this.value);
        });

       function submitEvidenceType(evidenceType){
                    if (evidenceType !== "") {
                        var csrfToken = $("#uploadForm input[name='csrfToken']").val();
                        clearErrors("evidenceType");
                        $.ajax({
                            url: $("#updateEvidenceTypeURL").text(),
                            method: "POST",
                            contentType: "application/json",
                            data: JSON.stringify({
                                "evidenceType": evidenceType,
                                "csrfToken": csrfToken
                            }),
                            headers: {
                                'Csrf-Token': csrfToken
                            },
                            crossDomain: false,
                            cache: false
                        }).fail(function (jqXHR) {
                            addError(jqXHR.responseText, "evidenceType");
                        }).done(function (data, statusText, resObject) {
                            $('#message-warning').addClass('govuk-!-display-none');
                        });
                    }
               }

        var errorTitlePrefix = $('#accessibility-error-label').text();
        var title = $(document).prop('title');
        function addError(message, evidenceTypeId){
            if(title.startsWith(errorTitlePrefix)){
                $(document).prop('title', title);
            } else {
                $(document).prop('title', errorTitlePrefix + title);
            }
            $('#errorsList').html(errorMessages.replace('<li></li>', '<li><a href="#'+evidenceTypeId+'">'+ message +'</a></li>'));
            $('<span id="file-upload-1-error" class="govuk-error-message"><span class="govuk-!-display-none">' + errorTitlePrefix + '</span>'+ message +'</span>').insertBefore('#'.evidenceTypeId);
            $('#'.evidenceTypeId).addClass('govuk-form-group--error');
            $('#message-warning').addClass('govuk-!-display-none');
            $('#error-summary').focus();
        }
        function clearErrors(evidenceTypeId){
            $(document).prop('title', title.replace(errorTitlePrefix,''));
            $('#errorsList').html("");
            $('.govuk-error-summary').remove();
            $('#file-upload-1-error').remove();
            $('#'.evidenceTypeId).removeClass('govuk-form-group--error');
        }
    };

    root.VOA.UpdateEvidenceType = UpdateEvidenceType;

}).call(this);