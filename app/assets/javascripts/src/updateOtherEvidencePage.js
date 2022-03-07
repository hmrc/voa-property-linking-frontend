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

        var options = "input[value='lease'],input[value='license'],input[value='serviceCharge'],input[value='stampDutyLandTaxForm'],input[value='waterRateDemand'],input[value='otherUtilityBill'],input[value='landRegistryTitle']";

        $(options).click(function() {
          $('#file-upload-form').removeClass("govuk-visually-hidden");
        });

        if($(options).is(':checked')){
            $('#file-upload-form').removeClass("govuk-visually-hidden");
        }

        $("#evidenceType-9").click(function() {
          $('#file-upload-form').addClass("govuk-visually-hidden");
        });

    };

    root.VOA.UpdateOtherEvidencePage = UpdateOtherEvidencePage;

}).call(this);