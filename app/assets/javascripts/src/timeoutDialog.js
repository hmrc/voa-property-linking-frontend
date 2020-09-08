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
        if (window.GOVUK.timeoutDialog && signOutUrl) {
            window.GOVUK.timeoutDialog({
                timeout: timeout,
                countdown: countdown,
                keepAliveUrl: window.location,
                signOutUrl: signOutUrl
            });
        }
    };

root.VOA.TimeOutReminder = TimeOutReminder;

}).call(this);