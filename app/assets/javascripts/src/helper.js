(function (VOA, $) {
    'use strict';

    VOA.helper = (function () {
        function Helper() {

            this.lang = function() {
                return $('html').attr('lang');
            };

            this.init = function () {
                return this;
            };

            return this.init();
        }

        return new Helper();
    }());


}(window.VOA = window.VOA || {}, jQuery));