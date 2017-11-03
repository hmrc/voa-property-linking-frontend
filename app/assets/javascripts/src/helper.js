(function (VOA, $) {
    'use strict';

    VOA.helper = (function () {
        function Helper() {

            this.lang = function() {
                return $('html').attr('lang');
            };

            this.dataTableSettings = function(table){

                var messages = VOA.messages.en;

                $.fn.dataTable.ext.errMode = 'none';

                function settings(table) {
                    return $.extend( true, $.fn.dataTable.defaults, {
                        serverSide: true,
                        info: true,
                        paging: true,
                        processing: true,
                        lengthChange: false,
                        searching: false,
                        ordering: false,
                        pageLength: 15,
                        drawCallback: function(settings) {
                            var current = this.api().page.len();
                            $(".page-size-option-current").removeClass().addClass("page-size-option");
                            $("#page-size-option-"+current).removeClass().addClass("page-size-option-current");
                        },
                        language: {
                            info: messages.labels.showing + ' _START_ ' + messages.labels.to + ' _END_ ' + messages.labels.of + ' _TOTAL_',
                            processing: '<div class="loading-container"><span class="loading-icon"></span><span class="loading-label">' +  messages.labels.loading + '</span></div>',
                            paginate: {
                                next: messages.labels.next + '<i class="next-arrow"></i>',
                                previous: '<i class="previous-arrow"></i>' +  messages.labels.previous
                            }
                        },
                        initComplete: function(settings, json) {
                            $(this).closest('.dataTables_wrapper').find('.dataTables_paginate').toggle(settings._iRecordsTotal > settings._iDisplayLength);
                            $(this).closest('.dataTables_wrapper').find('.dataTables_length').toggle(settings._iRecordsTotal > settings._iDisplayLength);
                        }
                    });
                }

                function errors(table) {
                    return table.on('error.dt', function (e, settings, techNote, message) {
                        $(this).find('tbody').empty().append('<tr><td class="text-centered" colspan="'+settings.aoColumns.length+'"><span class="heading-medium error-message">' +  messages.errors.dataError + '</span></td></tr>');
                    });
                }

                return  [settings(table), errors(table)];
            };

            this.init = function () {
                return this;
            };

            return this.init();
        }

        return new Helper();
    }());


}(window.VOA = window.VOA || {}, jQuery));
