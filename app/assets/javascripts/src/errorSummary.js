(function () {

    'use strict';

    var root = this, $ = root.jQuery;

    if (typeof VOA === 'undefined') {
        root.VOA = {};
    }

    var ErrorSummary = function (){

        if ($('.error-summary')) {
            $('.error-summary').focus();
        }
        
    };

    root.VOA.ErrorSummary = ErrorSummary;

}).call(this);
