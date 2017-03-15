(function () {
    'use strict';

    var root = this, $ = root.jQuery;

    if (typeof VOA === 'undefined') {
        root.VOA = {};
    }

    root.VOA.postcodeLookup = function () {

        var messages = VOA.messages[VOA.helper.lang()];

        function showFields() {
            $('.address--fields').css('display', 'block');
            $('.postcode-lookup-fields').css('display', 'none');
            $('.address--fields input').attr('placeholder', '');
        }

        function clearFields(_this) {
            $(_this).closest('.postcode-lookup-group').find('.address--fields input').val('');
        }

        function errorCheck() {
            if($('#error-summary').length) {
                showFields();
            }
        }

        function showLookupError() {
            $('#postcodeSearchGroup .error-message').remove();
            $('#postcodeSearch').before('<p class="error-message">' + messages.errors.postcodeLookupError + '</p>').closest('.form-group').addClass('error');
            active = true;
        }

        function addressLine(s) {
            if (s) { return s + ', '; } else { return ''; }
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

                        },
                        400: function(res) {
                            $('#postcodeSearchGroup .error-message').text(res.responseText);
                        }
                    },
                    success: function(data) {
                        if (data.length > 0) {
                            $('.postcode-lookup-group').prepend('<label for="addressSelect" class="form-label-bold">Select address</label><select id="addressSelect" class="addressList form-control"></select>');
                            $('#addressSelect').append('<option value="" selected disabled>' + messages.labels.selectValue + '</option>');
                            $('.postcode-lookup-fields').css('display', 'none');
                            $('.lookupAddressCancel').css('display', 'inline-block');
                            $.each(data, function(i, item) {
                                var address = addressLine(item['line1']) + addressLine(item['line2']) + addressLine(item['line3']) + addressLine(item['line4']) + item['postcode'];
                                $('.addressList').append('<option value="' + i + '">' +  window.xssEscape(address) + '</option>');
                            });
                            $('#addressSelect').focus();
                            $('#addressSelect').change(function (e) {
                                $('[for="addressSelect"], .lookupAddressCancel').css('display', 'none');
                                $('.manualAddress').css('display', 'none');
                                var index = $(this).find('option:selected').index() - 1;
                                $('.address--fields').css('display', 'block');
                                $('.address--fields input:eq(0)').val(data[index]['addressUnitId']).attr('placeholder', '');
                                $('.address--fields input:eq(1)').val(data[index]['line1']).attr('placeholder', '');
                                $('.address--fields input:eq(2)').val(data[index]['line2']).attr('placeholder', '');
                                $('.address--fields input:eq(3)').val(data[index]['line3']).attr('placeholder', '');
                                $('.address--fields input:eq(4)').val(data[index]['line4']).attr('placeholder', '');
                                $('.address--fields input:eq(5)').val(data[index]['postcode']);
                                $(this).closest('.form-group').find('[for="addressSelect"]').remove();
                                $(this).remove();
                            });
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
            $('.manualAddress, .lookupAddressCancel, [for="addressSelect"], #addressSelect').css('display', 'none');
            showFields();
            clearFields(this);
        });

        $('.lookupAddress').click(function (e) {
            e.preventDefault();
            $('.address--fields').css('display', 'none');
            $('.postcode-lookup-fields').css('display', 'block');
            $('.manualAddress').css('display', 'inline-block');
            $('#postcodeSearchGroup').closest('.form-group').removeClass('error');
            $('#postcodeSearchGroup .error-message').remove();
            $('#postcodeSearch').val('');
            active = true;
        });

        $('.lookupAddressCancel').click(function (e) {
            e.preventDefault();
            $('.address--fields').css('display', 'none');
            $('.postcode-lookup-fields').css('display', 'block');
            $('.manualAddress').css('display', 'inline-block');
            $('#postcodeSearchGroup').closest('.form-group').removeClass('error');
            $('#postcodeSearchGroup .error-message').remove();
            $('#postcodeSearch').val('').focus();
            $('#addressSelect, [for="addressSelect"]').remove();
            $(this).css('display', 'none');
            active = true;
        });



    };

}).call(this);
