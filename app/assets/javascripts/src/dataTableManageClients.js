(function () {

    'use strict';

    var root = this, $ = root.jQuery;

    if (typeof VOA === 'undefined') {
        root.VOA = {};
    }

    root.VOA.DataTableManageClients = function (){

        var messages = VOA.messages.en,
        $table  = $('#dataTableManageClients');

        VOA.helper.dataTableSettings($table);

        $table.DataTable({
            ajax: {
                data: function() {
                    var info = $table.DataTable().page.info();
                    $table.DataTable().ajax.url('/business-rates-property-linking/manage-clients/json?page=' + (info.page + 1) + '&pageSize='+ info.length +'&requestTotalRowCount=true');
                },
                dataSrc: 'propertyRepresentations',
                dataFilter: function(data) {
                   var json = jQuery.parseJSON(data);
                   json.recordsTotal = json.resultCount;
                   json.recordsFiltered = json.resultCount;
                   return JSON.stringify(json);
               }
            },
            columns: [
                {data: 'organisationName'},
                {data: 'address'},
                {data: 'billingAuthorityReference'},
                {data: null, defaultContent: '<ul><li></li><li></li></ul>'},
                {data: null, defaultContent: '<ul><li></li><li></li></ul>'}
            ],
            fnRowCallback: function(nRow, aData, iDisplayIndex, iDisplayIndexFull) {
                $('td:eq(3) ul li:eq(0)', nRow).html( messages.labels.check + ': ' + messages.labels['status' + aData.checkPermission]);
                $('td:eq(3) ul li:eq(1)', nRow).html( messages.labels.challenge + ': ' + messages.labels['status' + aData.challengePermission]);
                $('td:eq(4) ul li:eq(0)', nRow).html('<a href="/business-rates-property-linking/client-properties/' + aData.organisationId + '/revoke/' + aData.authorisationId +'">' + messages.labels.revokeClient + '</a>');
                $('td:eq(4) ul li:eq(1)', nRow).html('<a href="/business-rates-property-linking/property-link/' + aData.authorisationId + '/assessments' + '">' + messages.labels.viewValuations + '</a>');
            }

        });

    };

}).call(this);
