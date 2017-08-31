(function () {

    'use strict';

    var root = this, $ = root.jQuery;

    if (typeof VOA === 'undefined') {
        root.VOA = {};
    }

    root.VOA.DataTableManagePropertiesSearchSort = function (){

        var messages = VOA.messages.en,
            $table  = $('#dataTableManagePropertiesSearchSort');

        VOA.helper.dataTableSettings($table);

        var dataTable = $table.DataTable({
            searching: true,
            ordering: true,
            orderCellsTop: true,
            ajax: {
                data: function() {
                    var info = $table.DataTable().page.info();
                    var queryParameters = "";
                    queryParameters += "&baref=" + $('#baref').val();
                    queryParameters += "&agent=" + $('#agent').val();
                    queryParameters += "&address=" + $('#address').val();
                    queryParameters += "&status=" + $('#status').val();

                    $table.DataTable().ajax.url('/business-rates-property-linking/properties-search-sort/json?page=' + (info.page + 1) + '&pageSize='+ info.length +'&requestTotalRowCount=true' + queryParameters);
                },
                dataSrc: 'propertyLinks',
                dataFilter: function(data) {
                    var json = jQuery.parseJSON(data);
                    json.recordsTotal = json.resultCount;
                    json.recordsFiltered = json.resultCount;
                    return JSON.stringify(json);
                }
            },
            columns: [
                {data: 'address', name: 'address'},
                {data: 'assessments.0.billingAuthorityReference', defaultContent:'-', name: 'baref'},
                {data: null, defaultContent: '<ul class="list"><li></li><li></li></ul>', name: 'status'},
                {data: 'agents[, ].organisationName', name: 'agent'},
                {data: null, defaultContent: '<ul class="list"><li></li><li></li></ul>'}
            ],
            fnRowCallback: function(nRow, aData, iDisplayIndex, iDisplayIndexFull) {
                $('td:eq(2) ul li:eq(0)', nRow).text( messages.labels['status' + aData.pending + ''] );
                if(aData.pending){
                    $('td:eq(2) ul li:eq(1)', nRow).html('<span class="submission-id">' + messages.labels.submissionId+ ': ' + aData.submissionId + '</span>' );
                }
                if(aData.agents.length === 0){
                    $('td:eq(3)', nRow).text('None');
                }
                $('td:eq(4) ul li:eq(0)', nRow).html('<a href="/business-rates-property-linking/appoint-agent/' + aData.authorisationId + '">'+ messages.labels.appointAgent + '</a>');
                $('td:eq(4) ul li:eq(1)', nRow).html('<a href="/business-rates-property-linking/property-link/' + aData.authorisationId + '/assessments'+'">' + messages.labels.viewValuations + '</a>');
            },
            fnServerParams: function(data) {
                data['order'].forEach(function(items, index) {
                    data.sortfield = data['columns'][items.column]['name'];
                    data.sortorder = data['order'][index].dir
                });
            }
        });

        $( '#dataTableManagePropertiesSearchSort th button').on( 'click', function () {
            dataTable.draw();
        } );


    };

}).call(this);
