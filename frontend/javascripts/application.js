/* overides jshint */
/*******************/
/* jshint -W079 */
/* jshint -W009 */
/* jshint -W098 */
/******************/

// set namespaces (remember to add new namespaces to .jshintrc)
var VoaRadioToggle = {};
var VoaCommon = {};
var ref;

(function ($) {
    'use strict';
    $(document).ready(function () {

       //common.js
       VoaCommon.addMultiButtonState();

       //radioToggle.js
       VoaRadioToggle.radioDataShowField();
       VoaRadioToggle.radioDataShowFields();

    });

})(jQuery);