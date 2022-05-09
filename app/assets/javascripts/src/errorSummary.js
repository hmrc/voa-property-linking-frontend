(function () {

    'use strict';

    var root = this, $ = root.jQuery;

    if (typeof VOA === 'undefined') {
        root.VOA = {};
    }

    root.VOA.ErrorSummary = function (){

        if ($('.govuk-error-summary')) {
            $('.govuk-error-summary').focus();
        }

    };

}).call(this);
