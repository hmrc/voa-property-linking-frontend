(function () {

    'use strict';

    var root = this, $ = root.jQuery;

    if (typeof VOA === 'undefined') {
        root.VOA = {};
    }

    var DataTableManageProperties = function (){

        var $table = $('#dataTableManageProperties');

        var showLinks = '<ul><li><a href=\"/dashboard/1/appoint-agent\">Appoint agent</a></li><li><a href=\"\">View valuations</a></li></ul>';

        $table.DataTable({
            serverSide: true,
            info: true,
            paging: true,
            lengthChange: false,
            searching: false,
            ordering: false,
            lengthMenu: [[10, 25, 50, 100],[10, 25, 50, 100]],
            processing: true,
            ajax: {
                data: function() {
                    var info = $table.DataTable().page.info();
                    $table.DataTable().ajax.url('http://localhost:9523/business-rates-property-linking/list-properties?page=' + (info.page + 1) + '&pageSize=10&requestTotalRowCount=true');
                },
                dataSrc: 'propertyLinks',
                dataFilter: function(data) {
                    var json = jQuery.parseJSON(data);
                    json.recordsTotal = json.resultCount;
                    json.recordsFiltered = json.resultCount;
                    return JSON.stringify(json);
                },
                error: function() {
                    console.log('error'); //todo
                }
            },
            fnRowCallback: function(nRow, aData, iDisplayIndex, iDisplayIndexFull) {
                $('td:eq(4) ul li:eq(0) a', nRow).attr('href', '/business-rates-property-linking/appoint-agent/' + aData.authorisationId + '');
                $('td:eq(4) ul li:eq(1) a', nRow).attr('href', '/business-rates-property-linking/property-link/' + aData.authorisationId + '/assessments');
            },
            columns: [
                {width: '300px', data: 'address'},
                {width: '208px', data: 'assessment.0.billingAuthorityReference'},
                {width: 'auto', data: 'pending'},
                {width: 'auto', data: 'agents[, ].organisationName'},
                {width: '140px', data: null, defaultContent: showLinks, sClass: 'last', sortable: false}
            ],
            language: {
                info: 'Showing page _PAGE_ of _PAGES_',
                processing: '<div class="loading-container"><span class="loading-icon"></span><span class="loading-label">Loading...</span></div>',
                paginate: {
                    next: 'Next<i class="next-arrow"></i>',
                    previous: '<i class="previous-arrow"></i>Previous'
                }
            }
        });

    };

    root.VOA.DataTableManageProperties = DataTableManageProperties;

}).call(this);
