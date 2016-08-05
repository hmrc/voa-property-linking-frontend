(function ($) {
    'use strict';

    VoaCommon.addMultiButtonState = function(){
            var $element = $('.add-multi-fields');
            var $group = $('.multi-fields-group');
            var l = parseInt($element.closest('fieldset').attr('data-limit'), 10);
            if($group.length === l){
                $element.hide();
            }else{
                $element.show();
            }
    };

})(jQuery);
