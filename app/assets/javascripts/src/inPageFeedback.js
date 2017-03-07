(function() {

    'use strict';

    var root = this, $ = root.jQuery;

    if (typeof VOA === 'undefined') {
        root.VOA = {};
    }

    var InPageFeedbackOverride = function (){
        //inpage feedback overrides


        $(document).ajaxComplete(function() {

        	var textbox = $(document.createElement('textarea')).attr('id', 'report-error')
                .attr('class', 'form-control')
                .attr('maxlength', '1000')
                .attr('name', 'report-error')
                .attr('data-rule-required','true')
                .attr('data-msg-required','Please enter details of what went wrong.');

            $('#report-error').replaceWith(textbox);

            $('.report-error__content h2').text('Form help');

            $('.report-error__content').prepend('<div class="form-help-content">'+
            '<div class="panel panel-border-wide">'+
            '<p>If you’d like to speak to somebody about this service, please contact us.</p>'+
            '<p>Email'+
                '<br>'+
                '<a href="matilto:ccaservice@voa.gsi.gov.uk">ccaservice@voa.gsi.gov.uk</a>'+
            '</p>'+
            '<p>Telephone'+
                '<br>'+
                '<strong>03000 501 501</strong>'+
            '</p>'+
            '<p>Opening hours: 8:30am to 5:00pm Monday to Friday. Closed on bank holidays.</p>'+
            '</div>'+
            '<p>Alternatively you can use the form below.</p>'+
            '<strong>It can take up to 1 minute for the form to submit your data and display a confirmation page.</strong>'+
            '</div>');

            if($('.form--feedback').length < 1){
                $('.form-help-content').remove();
            }

            $('#feedback-thank-you-header').next().next()
                .text('Someone will get back to you within 5 working days.');

        });



    };

    root.VOA.InPageFeedbackOverride = InPageFeedbackOverride;

}).call(this);
