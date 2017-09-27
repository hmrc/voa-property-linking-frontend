// Show Dialog
(function () {

    'use strict';

    var root = this, $ = root.jQuery;

    if (typeof VOA === 'undefined') {
        root.VOA = {};
    }

    root.VOA.ShowDialog = function () {

        if ($('.dialog').length > 0) {
            var dialogData = {
                lastFocus: null
            };

            $(document).on('click', 'a[data-toggle=dialog]', function (e) {

                e.preventDefault();
                e.stopPropagation();

                var anchor = $(this);
                var data = '#' + anchor.attr('data-target');

                openDialog(data, anchor); // Pass data value into function
                modalDialog(data);
            });
        }

        // Open dialog
        function openDialog(data, anchor) {

            dialogData.lastFocus = anchor;

            var dialog = $(data);

            dialog.attr('aria-hidden', 'false')
                .find('.dialog-content').focus()
                .attr('tabindex', '-1');

            dialog.trap();
        }

        // Close dialog only if visible
        function closeDialog() {


            var dialog = $('.dialog[aria-hidden=false]');


            dialog.attr('aria-hidden', 'true')
                .find('.dialog-content').blur()
                .attr('tabindex', '0');


            dialog.untrap();

            dialogData.lastFocus.focus();
            dialogData.lastFocus.blur();
        }

        // Stop bubbling

        //$('.dialog-holder').on('click', function (e) {
        $(document).on('click', '.dialog-holder', function (e) {
            e.stopPropagation();
        });


        //$('.dialog-close').on('click', function (e) {
        $(document).on('click', '.dialog-close', function (e) {
            e.preventDefault();
            e.stopPropagation();

            closeDialog();
        });


        //$('.dialog-cancel').on('click', function (e) {
        $(document).on('click', '.dialog-cancel', function (e) {
            e.preventDefault();
            e.stopPropagation();

            closeDialog();
        });

        // Document binding events
        function modalDialog(data) {
            $(data).bind({
                keyup: function (e) {
                    if (e.keyCode === 27) {
                        closeDialog();
                    }
                }
            });
        }
    };
}).call(this);
