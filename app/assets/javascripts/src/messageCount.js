(function () {

    'use strict';

    var root = this, $ = root.jQuery;

    if (typeof VOA === 'undefined') {
        root.VOA = {};
    }

    root.VOA.messageCount = function () {

        $.getJSON('/business-rates-property-linking/message-count/json', function (data) {
            if (data.unread === 0) {
                $('#unread-message-count').html('<i class="icon icon-messageread"></i> You have no unread messages');
            } else {
                $('#unread-message-count').html('<i class="icon icon-messageunread"></i> You have <a href="/business-rates-property-linking/messages">(' + data.unread + ') unread messages</a>');
                $('#messages').html('Messages (' + data.unread + ')');
            }
        });

    };

}).call(this);
