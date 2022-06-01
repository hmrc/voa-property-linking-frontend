(function() {

    'use strict';

    var root = this, $ = root.jQuery;

    if (typeof VOA === 'undefined') {
        root.VOA = {};
    }

    var UpdateOtherEvidencePage = function (){

        $("#continue-button").click(function() {
          $('#continue').click();
        });

        var evidenceTypeOptions = 'input[type = "radio"][data-behaviour != "hide-file-upload-section"]';
        var cannotProvideEvidence = 'input[type = "radio"][data-behaviour = "hide-file-upload-section"]';

        $(evidenceTypeOptions).click(function() {
          $('#file-upload-form').removeClass("govuk-!-display-none");
        });

        $(cannotProvideEvidence).click(function() {
            $('#file-upload-form').addClass("govuk-!-display-none");
        });

        if($(evidenceTypeOptions).is(':checked')){
            $('#file-upload-form').removeClass("govuk-!-display-none");
        }

    };

    root.VOA.UpdateOtherEvidencePage = UpdateOtherEvidencePage;

}).call(this);