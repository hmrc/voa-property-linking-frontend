(function (VOA, $) {
    'use strict';

    $(document).ready(function() {
        VOA.GdsModules();
        VOA.VoaModules();
    });

    VOA.GdsModules = function() {
        new GOVUK.SelectionButtons($('.block-label input:radio, .block-label input:checkbox'));
    };

    VOA.VoaModules = function(){
        new VOA.RadioToggleFields();
        new VOA.JqueryFiler();
        new VOA.postcodeLookup();
        new VOA.InPageFeedbackOverride();
    };

}(window.VOA = window.VOA|| {}, jQuery));
