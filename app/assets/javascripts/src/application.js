(function (VOA, $) {
    'use strict';

    $(document).ready(function() {
        VOA.VoaModules();
    });

    VOA.VoaModules = function(){
        new VOA.RadioToggleFields();
        new VOA.postcodeLookup();
        new VOA.ErrorSummary();
        new VOA.viewMessage();
        new VOA.FileUpload();
        new VOA.UpdateEvidenceType();
        new VOA.UpdateOtherEvidencePage();
    };

}(window.VOA = window.VOA|| {}, jQuery));
