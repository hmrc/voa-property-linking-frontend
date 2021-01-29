(function (VOA, $) {
    'use strict';

    $(document).ready(function() {
        VOA.VoaModules();
        VOA.GdsModules();
    });

    VOA.GdsModules = function() {
        new GOVUK.SelectionButtons($('.block-label input:radio, .block-label input:checkbox'));
    };

    VOA.VoaModules = function(){
        new VOA.RadioToggleFields();
        new VOA.JqueryFiler();
        new VOA.postcodeLookup();
        new VOA.ErrorSummary();
        new VOA.ShowDialog();
        new VOA.viewMessage();
        new VOA.FileUpload();
        new VOA.TimeOutReminder();
    };

}(window.VOA = window.VOA|| {}, jQuery));
