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
    };

}(window.VOA = window.VOA|| {}, jQuery));
