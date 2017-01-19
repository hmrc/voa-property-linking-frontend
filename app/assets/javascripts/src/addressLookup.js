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
        }

        function errorCheck() {
            if($('#error-summary').length) {
                showFields();
            }
        }

        function showLookupError() {
            $('.error-message').remove();
            $('#postcodeSearch_text').before('<p class="error-message">' + messages.errors.postcodeLookupError + '</p>').closest('.form-group').addClass('error');
        }

        function addressLine(s) {
            if (s) { return s + ', '; } else { return ''; }
        }

        errorCheck();

        $('#postcodeLookupButton').click(function (e) {
            e.preventDefault();

            var postcode = $('#postcodeSearch_text').val();

            if(postcode !== '') {
                $.ajax({
                    type: 'GET',
                    url: '/business-rates-property-linking/lookup?postcode=' + postcode.toUpperCase(),
                    success: function(data) {
                        if (data.length > 0) {
                            $('.postcode-lookup-group').prepend('<label for="addressSelect" class="form-label-bold">Select address</label><select id="addressSelect" class="addressList form-control"></select>');
                            $('#addressSelect').append('<option value="" selected disabled>' + messages.labels.selectValue + '</option>');
                            $('.postcode-lookup-fields').css('display', 'none');
                            $.each(data, function(i, item) {
                                var address = addressLine(item['line1']) + addressLine(item['line2']) + addressLine(item['line3']) + addressLine(item['line4']) + item['postcode'];
                                $('.addressList').append('<option value="' + i + '">' + address + '</option>');
                            });
                            $('#addressSelect').change(function (e) {
                                $('[for="addressSelect"]').css('display', 'none');
                                $(this).css('display', 'none');
                                $('.manualAddress').css('display', 'none');
                                var index = $(this).find('option:selected').index() - 1;
                                $('.address--fields').css('display', 'block');
                                $('.address--fields input:eq(0)').val(data[index]['addressUnitId']);
                                $('.address--fields input:eq(1)').val(data[index]['line1']);
                                $('.address--fields input:eq(2)').val(data[index]['line2']);
                                $('.address--fields input:eq(3)').val(data[index]['line3']);
                                $('.address--fields input:eq(4)').val(data[index]['line4']);
                                $('.address--fields input:eq(5)').val(data[index]['postcode']);
                            });
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
            $('.manualAddress').css('display', 'none');
            $('[for="addressSelect"]').css('display', 'none');
            $('#addressSelect').css('display', 'none');
            showFields();
        });

        $('.lookupAddress').click(function (e) {
            e.preventDefault();
            $('.address--fields').css('display', 'none');
            $('.postcode-lookup-fields').css('display', 'block');
            $('.manualAddress').css('display', 'block');
        });
    };

}).call(this);