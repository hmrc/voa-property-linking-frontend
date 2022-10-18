(function () {
    'use strict';
    var root = this, $ = root.jQuery;
    if (typeof VOA === 'undefined') {
        root.VOA = {};
    }
    root.VOA.postcodeLookup = function () {
        function showFields() {
            show($('.address--fields'));
            hide($('.postcode-lookup-fields'));
            $('.address--fields input').attr('placeholder', '');
        }
        function clearFields(_this) {
            $(_this).closest('.postcode-lookup-group').find('.address--fields input').val('');
        }
        function errorCheck() {
            if($('.govuk-error-summary').length) {
                showFields();
            }
        }
        function clearLookup() {
            clearLookupError();
            $('#postcodeSearch').val('');
            $('#selectedAddress').attr('value', '');
            hide($('.manualAddress, .postcode-lookup-fields'));
        }
        function clearLookupError() {
            $("#error-summary, #invalidPostcode").remove();
            $('#postcodeSearch').closest('.govuk-form-group').removeClass('govuk-form-group--error');
        }
        function clearSelectAnAddress() {
            $('#addressSelect, #addressHint, [for="addressSelect"]').remove();
            hide($('.lookupAddressCancel .manualAddress'));
        }
        function clearSelectedAddress() {
            clearId();
            $('#selectedAddress').attr('value', '');
            $('#textAddressData').empty();
            hide($('.lookupAddressCancel, .manualAddress, #textAddressDiv'));
        }
        function clearManualAddress() {
            $('#postcodeSearch').val('');
            hide($('.address--fields'));
        }

        function clearId() {$('#address_addressId_text').val('');}

        function showLookupError() {

            var errorTitlePrefix = $('#accessibility-error-label').text();
            var errorHeading = $('#error-common-title').text();
            var errorPostcodeMissing = $('#error-postcode-missing').text();
            var errorPostcodeLookup = $('#error-postcode-lookup').text();

            var title = $(document).prop('title');
            var errorMessages = '<div id="error-summary" class="govuk-error-summary" aria-labelledby="error-summary-heading" role="alert" tabindex="-1" data-module="govuk-error-summary">'+
                        '<h2 class="govuk-error-summary__title" id="error-summary-heading">' + errorHeading + '</h2>'+
                        '<ul class="govuk-list govuk-error-summary__list"><li><a href="#postcodeSearch">'+ errorPostcodeLookup +'</a></li></ul></div>';
             if(title.startsWith(errorTitlePrefix)){
                 $(document).prop('title', title);
             } else {
                 $(document).prop('title', errorTitlePrefix + ' ' + title);
             }
             if(!$('.govuk-error-summary__list').length){
                 $("#page-error-summary").append(errorMessages);
                 $('#error-summary').focus();
             }else{
                 if(!$('#page-error-summary').text().indexOf(errorPostcodeMissing)){
                    $('#page-error-summary #error-summary .govuk-error-summary__list').append('<li><a href="#postcodeSearch">'+ errorPostcodeLookup +'</a></li>');
                 }
             }

            var isError = $('#postcodeSearchOnly').text().indexOf(errorPostcodeMissing) > -1;
            if(isError){
                 $('span[id^="invalidPostcode"]').remove();
            }

            $('#postcodeSearchGroup').find('.govuk-error-message').remove();
            $('#postcodeSearchGroup').before('<span class="govuk-form-group govuk-form-group--error">'
                + '</span>').closest('.govuk-form-group').addClass('govuk-form-group--error');
            $('#postcodeSearch').before('<span id="invalidPostcode" class="govuk-error-message">'
            + errorPostcodeLookup + '</span>').closest('.govuk-form-group').addClass('govuk-form-group--error');
        }
        function addressLine(s) {
            if (s) { return s + ', '; } else { return ''; }
        }
        errorCheck();
        var lookupActive = true;
        $('#postcodeLookupButton').click(function (e) {
            e.preventDefault();
            $(this).closest('.postcode-lookup-group').find('#addressSelect, [for="addressSelect"]').remove();
            var postcode = $('#postcodeSearch').val();
            if(postcode !== '' && lookupActive) {
                lookupActive = false;
                $.ajax({
                    type: 'GET',
                    url: '/business-rates-property-linking/lookup?postcode=' + postcode.toUpperCase(),
                    statusCode: {
                        404: function(res) {
                            $('#postcodeSearchGroup').find('.govuk-error-message').text(errorPostcodeLookup);
                        }
                    },
                    success: function(data) {
                        $("#error-summary").remove();
                        if (data.length > 0) {
                            $('.postcode-lookup-group').prepend('<label for="addressSelect" class="govuk-label--m govuk-!-display-block">'+
                            $("#selectValue").text() +'</label><span class="govuk-hint govuk-!-display-block" id="addressHint">' +
                            $("#addressHelp").text() + '</span><select id="addressSelect" class="addressList govuk-select"></select>');
                            $('#addressSelect').append('<option value="" selected disabled>' + $("#selectValue").text() + '</option>');
                            hide($('.postcode-lookup-fields'));
                            show($('.lookupAddressCancel'));
                            $.each(data, function(i, item) {
                                var organisationName = item['organisationName'];
                                var departmentName = item['departmentName'];
                                var subBuildingName = item['subBuildingName'];
                                var buildingNumber = item['buildingNumber'];
                                var buildingName = item['buildingName'];
                                var dependentThoroughfareName = item['dependentThoroughfareName'];
                                var thoroughfareName = item['thoroughfareName'];
                                var doubleDependentLocality = item['doubleDependentLocality'];
                                var dependentLocality = item['dependentLocality'];
                                var postTown = item['postTown'];
                                var postcode = item['postcode'];
                                var address =
                                    addressLine(departmentName) +
                                    addressLine(organisationName) +
                                    addressLine(subBuildingName) +
                                    addressLine(buildingNumber) +
                                    addressLine(buildingName) +
                                    addressLine(dependentThoroughfareName) +
                                    addressLine(thoroughfareName) +
                                    addressLine(doubleDependentLocality) +
                                    addressLine(dependentLocality) +
                                    addressLine(postTown) +
                                    postcode;
                                $('.addressList').append('<option value="' + i + '">' +  window.xssEscape(address) + '</option>');
                            });
                            $('#addressSelect').focus();
                            $('#addressSelect').change(function (e) {
                                clearSelectAnAddress();
                                var index = $(this).find('option:selected').index() - 1;
                                show($('#textAddressDiv'));
                                $('#text-form-group input:eq(0)').val(data[index]['addressUnitId']).attr('placeholder', '');
                                if(data[index]['departmentName'] != undefined) {
                                    $('#textAddressData').append("<span>" + data[index]['departmentName']+ "</span><br>");
                                }
                                if(data[index]['organisationName'] != undefined) {
                                    $('#textAddressData').append("<span>" + data[index]['organisationName'] + "</span><br>");
                                }
                                if(data[index]['subBuildingName'] != undefined) {
                                    $('#textAddressData').append("<span>" + data[index]['subBuildingName']+ "</span><br>");
                                }
                                if(data[index]['buildingNumber'] != undefined) {
                                    $('#textAddressData').append("<span>" + data[index]['buildingNumber'] + " " + "</span>");
                                }
                                if(data[index]['buildingName'] != undefined) {
                                    $('#textAddressData').append("<span>" + data[index]['buildingName']+ "</span><br>");
                                }else{
                                    $('#textAddressData').append("<br>");
                                }
                                if(data[index]['dependentThoroughfareName'] != undefined) {
                                    $('#textAddressData').append("<span>" + data[index]['dependentThoroughfareName']+ "</span><br>");
                                }
                                if(data[index]['thoroughfareName'] != undefined) {
                                    $('#textAddressData').append("<span>" + data[index]['thoroughfareName']+ "</span><br>");
                                }
                                if(data[index]['doubleDependentLocality'] != undefined) {
                                    $('#textAddressData').append("<span>" + data[index]['doubleDependentLocality']+ "</span><br>");
                                }
                                if(data[index]['dependentLocality'] != undefined) {
                                    $('#textAddressData').append("<span>" + data[index]['dependentLocality']+ "</span><br>");
                                }
                                if(data[index]['postTown'] != undefined) {
                                    $('#textAddressData').append("<span>" + data[index]['postTown']+ "</span><br>");
                                }
                                if(data[index]['postcode'] != undefined) {
                                    $('#textAddressData').append("<span>" + data[index]['postcode']+ "</span>");
                                }
                                $('#selectedAddress').attr('value', $('#textAddressData').html());
                            });
                            lookupActive = false;
                        } else {
                            showLookupError();
                            lookupActive = true;
                        }
                    },
                    error: function(error) {
                        showLookupError();
                        lookupActive = true;
                    }
                });
            } else {
                showLookupError();
            }
        });
        $('.manualAddress').click(function (e) {
            e.preventDefault();
            clearLookup();
            clearSelectAnAddress();
            clearSelectedAddress();
            showFields();
            clearFields(this);
        });
        $('.lookupAddress').click(function (e) {
            e.preventDefault();
            show($('.postcode-lookup-fields, .manualAddress'));
        });
        $('#backLookup').click(function (e) {
            e.preventDefault();
            clearLookup();
            clearManualAddress();
            show($('.postcode-lookup-fields, .manualAddress'))
            $('#postcodeSearch').focus();
            lookupActive = true;
        });
        $('.lookupAddressCancel').click(function (e) {
            e.preventDefault();
            clearLookup();
            clearSelectAnAddress();
            clearSelectedAddress();
            show($('.postcode-lookup-fields, .manualAddress'));
            $('#postcodeSearch').focus();
            lookupActive = true;
        });
        if($('#text-form-group input:eq(0)').val() != ""){
            hide($('.address--fields, .postcode-lookup-fields'));
            hide($('.postcode-lookup-fields'));
            show($('.manualAddress, #textAddressDiv'));
            $('#textAddressData').append($("#selectedAddress").val());
        }

        function add(selector, clazz) {selector.addClass(clazz)}
        function clear(selector, clazz) {selector.removeClass(clazz)}

        function show(selector) {
            clear(selector, 'govuk-!-display-none')
            add(selector, 'govuk-!-display-block')
        }
        function hide(selector) {
            clear(selector, 'govuk-!-display-block')
            add(selector, 'govuk-!-display-none')
        }
    };
}).call(this);

