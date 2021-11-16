(function () {

    'use strict';

    var root = this, $ = root.jQuery;

    if (typeof VOA === 'undefined') {
        root.VOA = {};
    }

    root.VOA.DataTablePendingRequests = function (){
    };

    // TODO move the following code onto the pending agent request page
    $('#par-select-all-top, #par-select-all-bottom').click(function(event) {
        var allselected = true;
        // determine if all checkboxes are currently selected
        $('.selection-button-checkbox').each(function () {
            allselected = allselected && $(this).hasClass('selected');
        });
        // set checked state of all checkboxes (dependent on current state)
        $('.selection-button-checkbox').each(function () {
            $(this).removeClass('selected');
            if (!allselected) {
                $(this).addClass('selected');
            }
        });
        // set underlying input fields
        $('input[type="checkbox"]').each(function () {
            $(this).prop('checked', !allselected);
        });

        if ( $(this).text() == $('#selectAll').text() ) {
           $('#par-select-all-top, #par-select-all-bottom').text($('#deselectAll').text())
        }else{
           $('#par-select-all-top, #par-select-all-bottom').text($('#selectAll').text())
        }

        return false;
    });


}).call(this);

