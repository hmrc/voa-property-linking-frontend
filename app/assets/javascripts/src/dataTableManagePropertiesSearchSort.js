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

                    var pageNumber = $('#current').data("dt-idx");
                    var pageSize = $('#page_size').find(":selected").text();
                    $table.DataTable().ajax.url('/business-rates-property-linking/properties-search-sort/json?page=' + pageNumber + '&pageSize='+ pageSize +'&requestTotalRowCount=true' + queryParameters);
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
                $('td:eq(4) ul li:eq(0)', nRow).html('<a href="/business-rates-property-linking/appoint-agent/' + aData.authorisationId + '">'+ messages.labels.appointAgent + '</a>');
                if (aData.status.toLowerCase() === 'approved' || aData.status.toLowerCase() === 'pending') {
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

        $(document).on("click", '.pX', function(){
            var value = parseInt($(this).data("dt-idx"));
            var last = parseInt($('#propertiesTableBusiness_paginate > :nth-last-child(2)').data("dt-idx"));
            var isNearMax = value >= (last - 2);
            var isMax = value == last;
            $('#current').removeAttr("id");
            previous(value);
            minValues(value);
            middleValues(value, isNearMax, last);
            maxValues(last, isNearMax);
            next(value, isMax);
            $('#propertiesTableBusiness_paginate').children('a').each(function (){
            var childValue = parseInt($(this).data("dt-idx") | 0);
            if($(this).hasClass('value') && value == childValue){
                $(this).attr("id", "current");
            }
            });
            dataTable.draw();
        });

        $('#page_size').change(function (){
            dataTable.draw();
        } );
        $( '#dataTableManagePropertiesSearchSort th button').on( 'click', function () {
            dataTable.draw();
        } );

        $( '#dataTableManagePropertiesSearchSort input').bind('keyup', function(e) {
            if(e.keyCode === 13) {
                dataTable.draw();
            }
        });

        $( '#dataTableManagePropertiesSearchSort select').bind('keyup', function(e) {
            if(e.keyCode === 13) {
                dataTable.draw();
            }
        });

        $( 'th .clear').on( 'click', function () {
            $('#dataTableManagePropertiesSearchSort th').find('input:text').val('');
            $('#dataTableManagePropertiesSearchSort th').find('#status').val('');

            dataTable.draw();
        } );


        function minValues(value) {
          if(value <= 4){
            $('#propertiesTableBusiness_paginate > :nth-child(3)').data("dt-idx", 2).addClass("pX").addClass("value").text(2);
          } else {
            $('#propertiesTableBusiness_paginate > :nth-child(3)').removeAttr("data-dt-idx").text("...").removeClass("pX").removeClass("value");
          }
        };

        function maxValues(max, isNearMax){
            if(isNearMax){
               $('#propertiesTableBusiness_paginate > :nth-last-child(3)').data("dt-idx", max - 1).addClass("pX").addClass("value").text(max - 1);
            } else {
               $('#propertiesTableBusiness_paginate > :nth-last-child(3)').removeAttr("data-dt-idx").text("...").removeClass("pX").removeClass("value");
            }
        };

        function middleValues(value, isNearMax, max){
          if(parseInt(value) <= 4){
            $('#propertiesTableBusiness_paginate > :nth-child(4)').data("dt-idx", 3).text(3);
            $('#propertiesTableBusiness_paginate > :nth-child(5)').data("dt-idx", 4).text(4);
            $('#propertiesTableBusiness_paginate > :nth-child(6)').data("dt-idx", 5).text(5);
          } else if(isNearMax) {
            $('#propertiesTableBusiness_paginate > :nth-last-child(4)').data("dt-idx", parseInt(max) - 2).text(parseInt(max) - 2);
            $('#propertiesTableBusiness_paginate > :nth-last-child(5)').data("dt-idx", parseInt(max) - 3).text(parseInt(max) - 3);
            $('#propertiesTableBusiness_paginate > :nth-last-child(6)').data("dt-idx", parseInt(max) - 4).text(parseInt(max) - 4);
          } else {
            $(this).removeAttr("data-dt-idx");
            $('#propertiesTableBusiness_paginate > :nth-child(4)').data("dt-idx", parseInt(value) - 1).text(parseInt(value) - 1);
            $('#propertiesTableBusiness_paginate > :nth-child(5)').data("dt-idx", value).text(value);
            $('#propertiesTableBusiness_paginate > :nth-child(6)').data("dt-idx", parseInt(value) + 1).text(parseInt(value) + 1);
          }
        };

        function previous(value) {
          if(value == 1){
            $('#propertiesTableBusiness_paginate > :nth-child(1)').addClass("disabled").removeAttr("data-dt-idx");
          } else {
            $('#propertiesTableBusiness_paginate > :nth-child(1)').removeClass("disabled").data("dt-idx", parseInt(value) - 1);
          }
        };

        function next(value, isMax) {
         if(isMax){
            $('#propertiesTableBusiness_paginate > :nth-last-child(1)').addClass("disabled").removeAttr("data-dt-idx");
         } else {
            $('#propertiesTableBusiness_paginate > :nth-last-child(1)').removeClass("disabled").attr("data-dt-idx", parseInt(value) + 1);
         }
        };
    };

}).call(this);
