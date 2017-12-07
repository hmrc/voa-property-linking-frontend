(function () {

    'use strict';

    var root = this, $ = root.jQuery;

    if (typeof VOA === 'undefined') {
        root.VOA = {};
    }

    root.VOA.viewMessage = function () {

        $('.message-link').click(function (e) {
            //prevent the non-js behaviour
            e.preventDefault();
        });

        $('.message--row').click(function (e) {
            markMessageAsRead($(this));

            $('#dialog-message').remove();

            var viewMessageUrl = $(this).find('a').first().attr('href');
            window.open(viewMessageUrl, '_self');
            //This has been commented as it will be needed once a way to display the javascript has been decided
        //     $.get(viewMessageUrl).then(function (viewMessagePage) {
        //         console.log(viewMessageUrl)
        //         var modal = createModal(viewMessagePage, viewMessageUrl);
        //
        //         $(document).find('.content_body').after(modal);
        //
        //         $('#print-message').click(function() {
        //             $('.modal-content-div').printThis();
        //         });
        //     });
        });

        function markMessageAsRead(row) {
            if(row.hasClass('message--row__unread')) {
                row.removeClass('message--row__unread').addClass('message--row__read');

                var readStatus = row.find('i');
                readStatus.removeClass('icon-messageunread').addClass('icon-messageread');
                readStatus.find('span').text('Read');

                var messageTab = $('ul[role="tablist"] li.active a');
                messageTab.text(decrementMessageCount);

                var unreadCount = $('#unread-message-count').find('a');
                unreadCount.text(decrementMessageCount);
            }
        }

        function decrementMessageCount(idx, text) {
            return text.replace(/(\d+)/, function(d) {
                return d !== 0 ? d - 1 : d;
            });
        }

        //This has been commented as it will be needed once a way to display the javascript has been decided
        // function createModal(message, viewMessageUrl) {
        //     var content = $(message).find('#messageContent');
        //
        //     var pdfLink = '<p><a id="message-pdf" href="' + viewMessageUrl + '/pdf' + '" target="_blank">Download this message as a PDF</a></p>';
        //
        //     var printLink = '<p><a id="print-message" href="#">Print this message</a></p>';
        //
        //     var closeIcon =
        //         '<button class="dialog-close" aria-label="close" type="button">' +
        //             '<span class="icon icon-close">x</span>' +
        //         '</button>';
        //
        //     var closeButton = '<a href="#" class="margin-top-10 dialog-cancel button-secondary" aria-label="close" role="button">Close</a>';
        //
        //     return '' +
        //         '<div id="dialog-message" class="dialog dialog-scroll" aria-hidden="false">' +
        //             '<div class="dialog-holder">' +
        //                 '<div id="dialog-content-message" class="dialog-content">' +
        //                     closeIcon +
        //                     '<div class="modal-content-div">' +
        //                         content.html() +
        //                     '</div>' +
        //                     pdfLink +
        //                     printLink +
        //                     closeButton +
        //                 '</div>' +
        //             '</div>' +
        //         '</div>';
        // }
    };

}).call(this);