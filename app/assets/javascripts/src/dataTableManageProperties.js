(function () {

    'use strict';

    var root = this, $ = root.jQuery;

    if (typeof VOA === 'undefined') {
        root.VOA = {};
    }

    root.VOA.DataTableManageProperties = function (){

        var messages = VOA.messages.en;
        var $table  = $('#dataTableManageProperties');
        var service = '/business-rates-property-linking';
        var pageSize = 15;

        $.fn.dataTable.ext.errMode = 'none';

        $table.DataTable({
            serverSide: true,
            info: true,
            paging: true,
            processing: true,
            lengthChange: false,
            searching: false,
            ordering: false,
            pageLength: pageSize,
            ajax: {
                data: function() {
                    var info = $table.DataTable().page.info();
                    $table.DataTable().ajax.url(service + '/list-properties?page=' + (info.page + 1) + '&pageSize='+ pageSize +'&requestTotalRowCount=true');
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
                $('td:eq(2) ul li:eq(0)', nRow).text( messages.labels['status' + aData.pending + ''] );
                if(aData.pending){
                    $('td:eq(2) ul li:eq(1)', nRow).html('<span class="submission-id">' + messages.labels.submissionId+ ': ' + aData.submissionId + '</span>' );
                }
                if(aData.agents.length === 0){
                    $('td:eq(3)', nRow).text('None');
                }
                $('td:eq(4) ul li:eq(0)', nRow).html('<a href="' + service + '/appoint-agent/' + aData.authorisationId + '">'+ messages.labels.appointAgent + '</a>');
                $('td:eq(4) ul li:eq(1)', nRow).html('<a href="' + service + '/property-link/' + aData.authorisationId + '/assessments'+'">' + messages.labels.viewValuations + '</a>');
            },
            columns: [
                {data: 'address'},
                {data: 'assessment.0.billingAuthorityReference'},
                {data: null, defaultContent: '<ul><li></li><li></li></ul>'},
                {data: 'agents[, ].organisationName'},
                {data: null, defaultContent: '<ul><li></li><li></li></ul>', sClass: 'last'}
            ],
            language: {
                info: messages.labels.showing + ' _START_ ' + messages.labels.to + ' _END_ ' + messages.labels.of + ' _TOTAL_',
                processing: '<div class="loading-container"><span class="loading-icon"></span><span class="loading-label">' +  messages.labels.loading + '</span></div>',
                paginate: {
                    next: messages.labels.next + '<i class="next-arrow"></i>',
                    previous: '<i class="previous-arrow"></i>' +  messages.labels.previous
                }
            }
        }).on('error.dt', function (e, settings, techNote, message) {
            $table.find('tbody').empty().append('<tr><td class="text-centered" colspan="'+settings.aoColumns.length+'"><span class="heading-medium error-message">' + messages.errors.dataError + '</span></td></tr>');
        });

    };

}).call(this);
