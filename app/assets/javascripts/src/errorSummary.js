(function () {

    'use strict';

    var root = this, $ = root.jQuery;

    if (typeof VOA === 'undefined') {
        root.VOA = {};
    }

    root.VOA.ErrorSummary = function (){

        if ($('.error-summary')) {
            $('.error-summary').focus();
        }

    };

}).call(this);
