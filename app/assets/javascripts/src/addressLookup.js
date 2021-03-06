(function () {
    'use strict';

    var root = this, $ = root.jQuery;

    if (typeof VOA === 'undefined') {
        root.VOA = {};
    }

    root.VOA.postcodeLookup = function () {

        var messages = VOA.messages.en;

        function showFields() {
            $('.address--fields').css('display', 'block');
            $('.postcode-lookup-fields, .manualAddress').css('display', 'none');
            $('.address--fields input').attr('placeholder', '');
        }

        function clearFields(_this) {
            $(_this).closest('.postcode-lookup-group').find('.address--fields input').val('');
        }

        function errorCheck() {
            if($('#error-summary').length || $('#addressline1Group').find('.error-message').length || $('#addresspostcodeGroup').find('.error-message').length) {
                showFields();
            }
        }

        function showLookupError() {
            $('#postcodeSearchGroup').find('.error-message').remove();
            $('#postcodeSearch').before('<p class="error-message">' + messages.errors.postcodeLookupError + '</p>').closest('.form-group').addClass('error');
            active = true;
        }

        function addressLine(s) {
            if (s) { return s + ', '; } else { return ''; }
        }

        function clearId() {
            $('.postcode-lookup-group input').change(function(){
                $('#address_addressId_text').val('');
            });
        }

        errorCheck();

        var active = true;

        $('#postcodeLookupButton').click(function (e) {
            e.preventDefault();
            $(this).closest('.postcode-lookup-group').find('#addressSelect, [for="addressSelect"]').remove();

            var postcode = $('#postcodeSearch').val();

            if(postcode !== '' && active) {
                active = false;
                $.ajax({
                    type: 'GET',
                    url: '/business-rates-property-linking/lookup?postcode=' + postcode.toUpperCase(),
                    statusCode: {
                        404: function(res) {
                            $('#postcodeSearchGroup').find('.error-message').text(messages.errors.postcodeLookupError);
                        }
                    },
                    success: function(data) {
                        if (data.length > 0) {
                            $('.postcode-lookup-group').prepend('<label for="addressSelect" class="form-label-bold">'+ messages.labels.selectValue +'</label><span class="form-hint" id="addressHelp">' + messages.labels.addressHelp + '</span><select id="addressSelect" class="addressList form-control"></select>');
                            $('#addressSelect').append('<option value="" selected disabled>' + messages.labels.selectValue + '</option>');
                            $('.postcode-lookup-fields').css('display', 'none');
                            $('.lookupAddressCancel').css('display', 'inline-block');


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
                                $("#textAddressData").empty();
                                $('[for="addressSelect"], .lookupAddressCancel').css('display', 'none');
                                var index = $(this).find('option:selected').index() - 1;
                                $('.address--fields').css('display', 'none');
                                $('#textAddressDiv').css('display', 'block');
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

                                $(this).closest('.form-group').find('[for="addressSelect"], #addressHelp').remove();
                                $(this).remove();
                            });
                            clearId();
                            active = true;
                        } else {
                            showLookupError();
                        }
                    },
                    error: function(error) {
                        showLookupError();
                    }
                });
            } else {
                showLookupError();
            }
        });


        $('.manualAddress').click(function (e) {
            e.preventDefault();
            $('#textAddressDiv').css('display', 'none');
            $('#textAddressData').html('');
            $('#text-form-group input:eq(0)').val('');
            $('#selectedAddress').attr('value', '');
            $('.manualAddress, .lookupAddressCancel, [for="addressSelect"], #addressSelect, #addressHelp').css('display', 'none');
            showFields();
            clearFields(this);
        });

        $('.lookupAddress').click(function (e) {
            e.preventDefault();
            $('.address--fields').css('display', 'none');
            $('#textAddressDiv').css('display', 'none');
            $('.postcode-lookup-fields').css('display', 'block');
            $('.manualAddress').css('display', 'inline-block');
            $('#postcodeSearchGroup').closest('.form-group').removeClass('error');
            $('#postcodeSearchGroup').find('.error-message').remove();
            $('#postcodeSearch').val('');
            active = true;
        });

        $('.lookupAddressCancel').click(function (e) {
            e.preventDefault();
            $('.address--fields').css('display', 'none');
            $('.postcode-lookup-fields').css('display', 'block');
            $('.manualAddress').css('display', 'inline-block');
            $('#postcodeSearchGroup').closest('.form-group').removeClass('error');
            $('#postcodeSearchGroup').find('.error-message').remove();
            $('#postcodeSearch').val('').focus();
            $('#addressSelect, [for="addressSelect"], #addressHelp').remove();
            $(this).css('display', 'none');
            active = true;
        });

        if($('#text-form-group input:eq(0)').val() != ""){
            $('.address--fields').css('display', 'none');
            $('.postcode-lookup-fields').css('display', 'none');
            $('.manualAddress').css('display', 'inline-block');
            $('#textAddressDiv').css('display', 'block');
            $('#textAddressData').append($("#selectedAddress").val());
        }



    };

}).call(this);
