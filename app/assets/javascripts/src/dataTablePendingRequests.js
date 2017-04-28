(function () {

    'use strict';

    var root = this, $ = root.jQuery;

    if (typeof VOA === 'undefined') {
        root.VOA = {};
    }

    root.VOA.DataTablePendingRequests = function (){

        var messages = VOA.messages.en;
        var $table  = $('#dataTablePendingRequests');
        var service = '/business-rates-property-linking';

        $.fn.dataTable.ext.errMode = 'none';

        $table.DataTable({
            serverSide: true,
            info: true,
            paging: true,
            processing: true,
            lengthChange: true,
            searching: false,
            ordering: false,
            lengthMenu: [[15, 25, 50, 100], [15, 25, 50, 100]],
            ajax: {
                data: function() {
                    var info = $table.DataTable().page.info();
                    $table.DataTable().ajax.url(service + '/manage-clients/pending-requests/json?page=' + (info.page + 1) + '&pageSize='+ info.length +'&requestTotalRowCount=true');
                },
                dataSrc: 'propertyRepresentations',
                dataFilter: function(data) {
                    var json = jQuery.parseJSON(data);
                    json.recordsTotal = json.resultCount;
                    json.recordsFiltered = json.resultCount;
                    return JSON.stringify(json);
                }
            },
            fnRowCallback: function(nRow, aData, iDisplayIndex, iDisplayIndexFull) {
                $('td:eq(2) ul li:eq(0)', nRow).html( messages.labels.check + ': ' + messages.labels['status' + aData.checkPermission]);
                $('td:eq(2) ul li:eq(1)', nRow).html( messages.labels.challenge + ': ' + messages.labels['status' + aData.challengePermission]);
                $('td:eq(3)', nRow).text(aData.createDatetime));
                $('td:eq(4) ul li:eq(0)', nRow).html('<a href="' + service + '/representation-request/accept/' + aData.submissionId + '">' + messages.labels.accept + '</a>');
                $('td:eq(4) ul li:eq(1)', nRow).html('<a href="' + service + '/representation-request/reject/' + aData.submissionId + '">' + messages.labels.reject + '</a>');
            },
            columns: [
                {data: 'organisationName'},
                {data: 'address'},
                {data: null, defaultContent: '<ul><li></li><li></li></ul>'},
                {data: null},
                {data: null, defaultContent: '<ul><li></li><li></li></ul>', sClass: 'last'}
            ],
            language: {
                info: messages.labels.showing + ' _START_ ' + messages.labels.to + ' _END_ ' + messages.labels.of + ' _TOTAL_',
                processing: '<div class="loading-container"><span class="loading-icon"></span><span class="loading-label">' +  messages.labels.loading + '</span></div>',
                paginate: {
                    next: messages.labels.next + '<i class="next-arrow"></i>',
                    previous: '<i class="previous-arrow"></i>' +  messages.labels.previous
                },
                lengthMenu: messages.labels.show + ' _MENU_ ' + messages.labels.rows
            }
        }).on('error.dt', function (e, settings, techNote, message) {
            $table.find('tbody').empty().append('<tr><td class="text-centered" colspan="'+settings.aoColumns.length+'"><span class="heading-medium error-message">' + messages.errors.dataError + '</span></td></tr>');
        });

    };

}).call(this);
