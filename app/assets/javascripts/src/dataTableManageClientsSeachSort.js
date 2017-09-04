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
                    var queryParameters = "";
                    queryParameters += "&baref=" + $('#baref').val();
                    queryParameters += "&client=" + $('#client').val();
                    queryParameters += "&address=" + $('#address').val();

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
                {data: 'client.organisationName', name: 'client'},
                {data: null, defaultContent: '<ul class="list"><li></li><li></li></ul>', "bSortable": false}
            ],
            fnRowCallback: function(nRow, aData, iDisplayIndex, iDisplayIndexFull) {
                $('td:eq(3) ul li:eq(0)', nRow).html('<a href="/business-rates-property-linking/client-properties/' + aData.client.organisationId + '/revoke/' + aData.id +'">' + messages.labels.revokeClient + '</a>');
                $('td:eq(3) ul li:eq(1)', nRow).html('<a href="/business-rates-property-linking/property-link/' + aData.id + '/assessments' + '">' + messages.labels.viewValuations + '</a>');
            },
            fnServerParams: function(data) {
                data['order'].forEach(function(items, index) {
                    data.sortfield = data['columns'][items.column]['name'];
                    data.sortorder = data['order'][index].dir
                });
            }

        });

        $( '#dataTableManageClientsSearchSort th button').on( 'click', function () {
            dataTable.draw();
        } );

        $( 'th .clear').on( 'click', function () {
            $('#dataTableManageClientsSearchSort th').find('input:text').val('');
            dataTable.draw();
        } );

    };

}).call(this);