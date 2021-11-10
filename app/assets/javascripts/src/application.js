(function (VOA, $) {
    'use strict';

    $(document).ready(function() {
        VOA.VoaModules();
    });

    VOA.VoaModules = function(){
        new VOA.RadioToggleFields();
        new VOA.JqueryFiler();
        new VOA.postcodeLookup();
        new VOA.ErrorSummary();
        new VOA.viewMessage();
        new VOA.FileUpload();
    };

}(window.VOA = window.VOA|| {}, jQuery));
