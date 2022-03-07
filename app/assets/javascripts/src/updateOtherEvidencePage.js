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

        var options = 'input[type="radio"][data-behaviour!="hide-file-upload-section"]';

        $(options).click(function() {
          $('#file-upload-form').removeClass("govuk-visually-hidden");
        });

        if($(options).is(':checked')){
            $('#file-upload-form').removeClass("govuk-visually-hidden");
        }

        $('input[type="radio"][data-behaviour="hide-file-upload-section"]').click(function() {
          $('#file-upload-form').addClass("govuk-visually-hidden");
        });

    };

    root.VOA.UpdateOtherEvidencePage = UpdateOtherEvidencePage;

}).call(this);