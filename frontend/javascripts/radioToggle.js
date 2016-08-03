(function ($) {
    'use strict';

    VoaRadioToggle.radioDataShowField = function () {
        function radioDataShowFieldElement(that) {
            var classname = $(that).attr('name').replace(/(:|\.|\[|\])/g,'\\$1');
            if ($(that).closest('[data-show-field="true"]').is(':checked')) {
                var element = '.' + classname + '';
                $(element).removeClass('hidden');

                //show full address fields
                if (VoaCommon.showAddressfieldsCondition(element) === true) {
                    VoaCommon.showAddressfields(element);
                }

                if ($('input:radio[name="' + $(that).attr('name') + '"]').is(':radio')) {
                    var checkedName = $('.' + $(that).attr('name') + ' input:radio').attr('name');
                    $('[name="' + checkedName + '"][checked]').prop('checked', true);
                }

                //show error if yes selected
                if($('#propertyIsSublet_true').is(':checked') || $('#propertyAlterations_false').is(':checked')){
                    $('.form-error').removeClass('hidden');
                }

            } else {

                if($(that).closest('fieldset').find('input:radio, input:checkbox').is('[data-show-fields]') || $(that).closest('fieldset').find('input:radio, input:checkbox').is('[data-show-field]')){
                    $('.' + classname + '').addClass('hidden');
                    //hide error if no selected
                    if($('#propertyIsSublet_false').is(':checked') || $('#propertyAlterations_false').is(':checked')){
                        $('.form-error').addClass('hidden');
                    }

                }

                $('.' + $(that).attr('name') + ' input:radio')
                    .prop('checked', false)
                    .closest('label')
                    .removeClass('selected');
            }

            if ($('[data-show-details="true"]').is(':checked')) {
                $('.data-show-details').removeClass('hidden');

            } else {
                $('.data-show-details').addClass('hidden');
            }

        }

        //data-show-field data attribute on load
        var selected = [];
        $('input:radio:checked').not('[name="overseas"]').each(function () {
            radioDataShowFieldElement(this);
            selected.push($(this).attr('name'));
            //toggle the add multi fields button visibility
            VoaCommon.addMultiButtonState();
        });

        //data-show-field data attribute on change
        $(document).on('change', 'input:radio', function () {
            radioDataShowFieldElement(this);
            $('[name="' + $(this).attr('name') + '"]').removeAttr('checked');
            if (selected) {
                $(this).prop('checked', true);
            }
            $(this).attr('checked', 'checked');
            $.each(selected, function (index, value) {
                $('[name="' + value + '"][checked]').prop('checked', true);
            });
            //toggle the add multi fields button visibility
            VoaCommon.addMultiButtonState();
        });

    };


    VoaRadioToggle.radioDataShowFields = function () {
        function radioDataShowFieldsElement(that) {
            var fieldsToShow = $(that).attr('data-show-fields').split(','),
                hiddenGroup = $(that).attr('name');

            //hide all inputs
            $('[data-show-fields-group="' + hiddenGroup + '"] input').closest('.form-group').addClass('hidden');
            $('[data-show-fields-group="' + hiddenGroup + '"] input').closest('fieldset').addClass('hidden');

            $.each(fieldsToShow, function (index, value) {
                //only show selected fields with the data-show-fields data attribute
                var element = $('[data-show-fields-group="' + hiddenGroup + '"] [name="' + value + '"]');
                var elementFind = element.closest('.postcode-lookup-group');
                element.closest('.form-group').removeClass('hidden');
                element.closest('fieldset').removeClass('hidden');
                //show full address fields
                elementFind.find('.form-group-lookup').css('display', 'none');
                elementFind.find('.showHide-group').css('display', 'inline-block');
                elementFind.find('.showHide-group .form-group').removeClass('hidden');
                elementFind.find('.manual-address').text('todo.findPostcode.label');
            });
        }

        //data-show-fields data attribute on load
        $('[type="radio"][data-show-fields]:checked').each(function () {
            radioDataShowFieldsElement(this);
        });
        //data-show-fields data attribute on change
        $('[type="radio"][data-show-fields]').change(function () {
            radioDataShowFieldsElement(this);

            //make sure postcode lookup fields are open
            var hiddenGroup = $(this).attr('name');
            var element = $('[data-show-fields-group="' + hiddenGroup + '"] .postcode-lookup-group');
            element.find('.form-group-lookup').css('display', 'block');
            element.find('.showHide-group').css('display', 'none');
            element.find('.manual-address').text('todo.enterManual.label');
        });
    };

})(jQuery);