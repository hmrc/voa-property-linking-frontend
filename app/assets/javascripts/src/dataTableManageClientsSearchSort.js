(function () {

    'use strict';

    var root = this, $ = root.jQuery;

    if (typeof VOA === 'undefined') {
        root.VOA = {};
    }

    root.VOA.DataTableManageClientsSearchSort = function (){

        var messages = VOA.messages.en,
        $table  = $('#dataTableManageClientsSearchSort');

        VOA.helper.dataTableSettings($table);

        var dataTable = $table.DataTable({
            searching: true,
            ordering: true,
            orderCellsTop: true,
            ajax: {
                data: function() {
                    var queryParameters = '';
                    queryParameters += '&baref=' + $('#baref').val();
                    queryParameters += '&client=' + $('#client').val();
                    queryParameters += '&address=' + $('#address').val();
                    queryParameters += '&status=' + $('#status').val();

                    var info = $table.DataTable().page.info();
                    $table.DataTable().ajax.url('/business-rates-property-linking/manage-clients-search-sort/json?page=' + (info.page + 1) + '&pageSize='+ info.length +'&requestTotalRowCount=true' + queryParameters);
                },
                dataSrc: 'authorisations',
                dataFilter: function(data) {
                    var json = jQuery.parseJSON(data);
                    json.recordsTotal = json.total;
                    json.recordsFiltered = json.filterTotal;
                    return JSON.stringify(json);
               }
            },
            columns: [
                {data: 'address', name: 'address'},
                {data: 'localAuthorityRef', name: 'baref'},
                {data: null, defaultContent: '<ul class="list"><li></li><li></li></ul>', name: 'status'},
                {data: 'client.organisationName', name: 'client'},
                {data: null, defaultContent: '<ul class="list"><li></li><li></li></ul>', 'bSortable': false}
            ],
            fnRowCallback: function(nRow, aData, iDisplayIndex, iDisplayIndexFull) {
                $('td:eq(2) ul li:eq(0)', nRow).text( messages.labels['status' + (aData.status.split('_').join('').toLowerCase())] );
                if(aData.status.toLowerCase() === 'pending'){
                    $('td:eq(2) ul li:eq(1)', nRow).html('<span class="submission-id">' + messages.labels.submissionId+ ': ' + aData.submissionId + '</span>' );
                }
                $('td:eq(4) ul li:eq(0)', nRow).html('<a href="/business-rates-property-linking/client-properties/' + aData.client.organisationId + '/revoke/' + aData.authorisationId +'">' + messages.labels.revokeClient + '</a>');
                if (aData.status.toLowerCase() === 'pending' || aData.status.toLowerCase() === 'approved') {
                    $('td:eq(4) ul li:eq(1)', nRow).html('<a href="/business-rates-property-linking/property-link/' + aData.authorisationId + '/assessments' + '">' + messages.labels.viewValuations + '</a>');
                } else {
                    $('td:eq(4) ul li:eq(1)', nRow).html('<a href="/business-rates-property-linking/summary/' + aData.uarn + '">' + messages.labels.viewValuations + '</a>');
                }
            },
            fnServerParams: function(data) {
                data['order'].forEach(function(items, index) {
                    data.sortfield = data['columns'][items.column]['name'];
                    data.sortorder = data['order'][index].dir;
                });
            }

        });

        $( '#dataTableManageClientsSearchSort th button').on('click', function () {
            dataTable.draw();
        } );

        $( '#dataTableManageClientsSearchSort input, select').bind('keyup', function(e) {
            if(e.keyCode === 13) {
                dataTable.draw();
            }
        });

        $( 'th .clear').on( 'click', function () {
            $('#dataTableManageClientsSearchSort th').find('input:text').val('');
            $('#dataTableManageClientsSearchSort th').find('#status').val('');
            dataTable.draw();
        } );

    };

}).call(this);