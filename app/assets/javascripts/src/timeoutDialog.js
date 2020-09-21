(function() {

    'use strict';

    var root = this, $ = root.jQuery;

    if (typeof VOA === 'undefined') {
        root.VOA = {};
    }


    var TimeOutReminder = function () {
        var signOutUrl = $('#signOut').attr('data-url');
        var timeout = $('#signOut').attr('data-timeout');
        var countdown = $('#signOut').attr('data-countdown');
        var keepAliveUrl = $('#signOut').attr('data-keep-alive-url');

        if (window.GOVUK.timeoutDialog && signOutUrl) {
            window.GOVUK.timeoutDialog({
                timeout: timeout,
                countdown: countdown,
                keepAliveUrl: keepAliveUrl,
                signOutUrl: signOutUrl
            });
        }
    };

root.VOA.TimeOutReminder = TimeOutReminder;

}).call(this);