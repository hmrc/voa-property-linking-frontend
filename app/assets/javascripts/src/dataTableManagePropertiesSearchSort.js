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

        var declinedHelpLink = '<a class="help" href="#declinedHelp" data-toggle="dialog" data-target="declinedHelp-dialog"><i><span class="visuallyhidden">' +
            messages.labels.declinedHelp +
            '</span></i></a>';

        var dataTable = $table.DataTable({
            searching: false,
            ordering: true,
            orderCellsTop: true,
            ajax: {
                data: function() {
                    var info = $table.DataTable().page.info();
                    var queryParameters = '';
                    queryParameters += '&baref=' + $('#baref').val();
                    queryParameters += '&agent=' + $('#agent').val();
                    queryParameters += '&address=' + $('#address').val();
                    queryParameters += '&status=' + $('#status').val();
                    $table.DataTable().ajax.url('/business-rates-property-linking/properties/json?page=' + (info.page + 1) + '&pageSize='+ info.length +'&requestTotalRowCount=true' + queryParameters);
                },
                dataSrc: 'authorisations',
                dataFilter: function(data) {
                    try{
                        var json = jQuery.parseJSON(data);
                        json.recordsTotal = json.total;
                        json.recordsFiltered = json.filterTotal;
                        return JSON.stringify(json);
                    }catch(e){
                        window.location.reload(true);
                    }
                },
                error: function (x, status, error) {
                    window.location.reload(true);
                }
            },
            columns: [
                {data: 'address', name: 'address'},
                {data: 'localAuthorityRef', defaultContent:'-', name: 'baref'},
                {data: null, defaultContent: '<ul class="list"><li></li><li></li></ul>', name: 'status'},
                {data: 'agents[, ].organisationName', name: 'agent'},
                {data: null, defaultContent: '<ul class="list"><li></li><li></li></ul>', 'bSortable': false}
            ],
            fnRowCallback: function(nRow, aData, iDisplayIndex, iDisplayIndexFull) {
                var $status = $('td:eq(2) ul li:eq(0)', nRow);

                $status.text( messages.labels['status' + (aData.status.split('_').join('').toLowerCase())] );

                if(aData.status.toLowerCase() === 'declined') {
                    $status.after(declinedHelpLink);
                }

                if(aData.status.toLowerCase() === 'pending'){
                    $('td:eq(2) ul li:eq(1)', nRow).html('<span class="submission-id">' + messages.labels.submissionId+ ': ' + aData.submissionId + '</span>' );
                }
                if(!aData.agents){
                    $('td:eq(3)', nRow).text('');
                }

                var liNum=0;
                if (!dataTable.ajax.json().config.agentMultipleAppointEnabled) {
                    if (aData.status.toLowerCase() === 'approved' || aData.status.toLowerCase() === 'pending') {
                        $('td:eq(4) ul li:eq('+liNum+')', nRow).html('<a href="/business-rates-property-linking/appoint-agent/' + aData.authorisationId + '">' + messages.labels.appointAgent + '</a>');
                    }
                    liNum++;
                }

                if (aData.status.toLowerCase() === 'approved') {
                    $('td:eq(4) ul li:eq('+liNum+')', nRow).html('<a href="/business-rates-property-linking/property-link/' + aData.authorisationId + '/assessments' + '">' + messages.labels.viewValuations + '</a>');
                } else {
                    $('td:eq(4) ul li:eq('+liNum+')', nRow).html('<a href="/business-rates-property-linking/summary/' + aData.uarn + '">' + messages.labels.viewValuations + '</a>');
                }
            },
            fnServerParams: function(data) {
                data['order'].forEach(function(items, index) {
                    data.sortfield = data['columns'][items.column]['name'];
                    data.sortorder = data['order'][index].dir;
                });
            }
        });

        $( '#dataTableManageProperties th button').on( 'click', function () {
            dataTable.draw();
        } );

        $( '#dataTableManageProperties input').bind('keyup', function(e) {
            if(e.keyCode === 13) {
                dataTable.draw();
            }
        });

        $( '#dataTableManageProperties select').bind('keyup', function(e) {
            if(e.keyCode === 13) {
                dataTable.draw();
            }
        });

        $( 'th .clear').on( 'click', function () {
            $('#dataTableManageProperties th').find('input:text').val('');
            $('#dataTableManageProperties th').find('#status').val('');

            dataTable.draw();
        } );



    };

}).call(this);
