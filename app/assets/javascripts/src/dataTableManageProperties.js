(function () {

    'use strict';

    var root = this, $ = root.jQuery;

    if (typeof VOA === 'undefined') {
        root.VOA = {};
    }

    root.VOA.DataTableManageProperties = function (){

        var messages = VOA.messages.en,
        $table  = $('#dataTableManageProperties');

        VOA.helper.dataTableSettings($table);

        $table.DataTable({
            ajax: {
                data: function() {
                    var info = $table.DataTable().page.info();
                    $table.DataTable().ajax.url('/business-rates-property-linking/properties/json?page=' + (info.page + 1) + '&pageSize='+ info.length +'&requestTotalRowCount=true');
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
                {data: 'address'},
                {data: 'assessment.0.billingAuthorityReference'},
                {data: null, defaultContent: '<ul class="list"><li></li><li></li></ul>'},
                {data: 'agents[, ].organisationName'},
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
            }
        });

    };

}).call(this);
