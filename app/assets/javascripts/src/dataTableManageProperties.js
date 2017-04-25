(function () {

    'use strict';

    var root = this, $ = root.jQuery;

    if (typeof VOA === 'undefined') {
        root.VOA = {};
    }

    var DataTableManageProperties = function (){

        var $table  = $('#dataTableManageProperties');
        var service = '/business-rates-property-linking';
        var action = '<ul><li><a href=\"/dashboard/1/appoint-agent\">Appoint agent</a></li><li><a href=\"\">View valuations</a></li></ul>';

        $.fn.dataTable.ext.errMode = 'none';

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
                    $table.DataTable().ajax.url(service + '/list-properties?page=' + (info.page + 1) + '&pageSize=10&requestTotalRowCount=true');
                },
                dataSrc: 'propertyLinks',
                dataFilter: function(data) {
                    var json = jQuery.parseJSON(data);
                    json.recordsTotal = json.resultCount;
                    json.recordsFiltered = json.resultCount;
                    return JSON.stringify(json);
                }
            },
            fnRowCallback: function(nRow, aData, iDisplayIndex, iDisplayIndexFull) {
                $('td:eq(4) ul li:eq(0) a', nRow).attr('href', service + '/appoint-agent/' + aData.authorisationId );
                $('td:eq(4) ul li:eq(1) a', nRow).attr('href', service + '/property-link/' + aData.authorisationId + '/assessments');
            },
            columns: [
                {width: '300px', data: 'address'},
                {width: '208px', data: 'assessment.0.billingAuthorityReference'},
                {width: 'auto', data: 'pending'},
                {width: 'auto', data: 'agents[, ].organisationName'},
                {width: '140px', data: null, defaultContent: action, sClass: 'last', sortable: false}
            ],
            language: {
                info: 'Showing page _PAGE_ of _PAGES_',
                processing: '<div class="loading-container"><span class="loading-icon"></span><span class="loading-label">Loading...</span></div>',
                paginate: {
                    next: 'Next<i class="next-arrow"></i>',
                    previous: '<i class="previous-arrow"></i>Previous'
                }
            }
        }).on('error.dt', function (e, settings, techNote, message) {
            $table.find('tbody').empty().append('<tr><td class="text-centered" colspan="'+settings.aoColumns.length+'"><span class="heading-medium error-message">An error occurred</span></td></tr>');
        });

    };

    root.VOA.DataTableManageProperties = DataTableManageProperties;

}).call(this);
