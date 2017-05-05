(function () {

    'use strict';

    var root = this, $ = root.jQuery;

    if (typeof VOA === 'undefined') {
        root.VOA = {};
    }

    root.VOA.DataTablePendingRequests = function (){

        var messages = VOA.messages.en,
        $table  = $('#dataTablePendingRequests'),
        results;

        VOA.helper.dataTableSettings($table);

        $table.DataTable({
            ajax: {
                data: function() {
                    var info = $table.DataTable().page.info();
                    $table.DataTable().ajax.url('/business-rates-property-linking/manage-clients/pending-requests/json?page=' + (info.page + 1) + '&pageSize='+ info.length +'&requestTotalRowCount=true');
                },
                dataSrc: 'propertyRepresentations',
                dataFilter: function(data) {
                    var json = jQuery.parseJSON(data);
                    json.recordsTotal = json.resultCount;
                    json.recordsFiltered = json.resultCount;
                    results = json.totalPendingRequests;
                    return JSON.stringify(json);
                }
            },
            columns: [
                {data: 'organisationName'},
                {data: 'address'},
                {data: null, defaultContent: '<ul><li></li><li></li></ul>'},
                {data: null},
                {data: null, defaultContent: '<ul><li></li><li></li></ul>'}
            ],
            fnRowCallback: function(nRow, aData, iDisplayIndex, iDisplayIndexFull) {
                $('td:eq(2) ul li:eq(0)', nRow).html( messages.labels.check + ': ' + messages.labels['status' + aData.checkPermission]);
                $('td:eq(2) ul li:eq(1)', nRow).html( messages.labels.challenge + ': ' + messages.labels['status' + aData.challengePermission]);
                $('td:eq(3)', nRow).text(moment(aData.createDatetime).format('LL'));
                $('td:eq(4) ul li:eq(0)', nRow).html('<a href="/business-rates-property-linking/representation-request/accept/' + aData.submissionId + '/' + results +'">' + messages.labels.accept + '</a>');
                $('td:eq(4) ul li:eq(1)', nRow).html('<a href="/business-rates-property-linking/representation-request/reject/' + aData.submissionId + '/' + results +'">' + messages.labels.reject + '</a>');
            }
        });

    };

}).call(this);
