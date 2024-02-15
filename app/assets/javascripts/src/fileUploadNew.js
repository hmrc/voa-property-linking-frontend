(function() {

    'use strict';

    var root = this, $ = root.jQuery;

    if (typeof VOA === 'undefined') {
        root.VOA = {};
    }

    var FileUploadNew = function (){
        function makeGetRequest() {

            var csrfToken = $("#fileUploadForm input[name='csrfToken']").val();

            $.ajax({
                url: $("#fileUploadStatusUrl").text(),
                method: 'GET',
                dataType: 'json',
                success: function (responseBody, status, xmlHttpRequest) {
                    updateTag(responseBody);
                    updateFormAction(responseBody)
                },
                error: function (xmlHttpRequest, status, error) {
                }
            });
        }

        // Function to update the tag with the new status
        function updateTag(newStatus) {
            var ddElement = document.querySelector('.govuk-summary-list__value strong');
            if (ddElement) {
                ddElement.className = getStatusClass(newStatus);
                ddElement.textContent = getStatusText(newStatus);
            }
        }

        // Function to get the status text based on the provided status
        function getStatusText(status) {
            switch (status) {
                case "UPLOADING":
                    return "uploading";
                case "READY":
                    return "uploaded";
                case "FAILED":
                    return "failed";
                default:
                    return "";
            }
        }

        // Function to get the status class based on the provided status
        function getStatusClass(status) {
            switch (status) {
                case "UPLOADING":
                    return "govuk-tag govuk-tag--yellow";
                case "READY":
                    return "govuk-tag govuk-tag--green";
                case "FAILED":
                    return "govuk-tag govuk-tag--red";
                default:
                    return "";
            }
        }

        // Function to update the form action with the new status
        function updateFormAction(newStatus) {
            var formElement = document.querySelector('#fileUploadForm');
            if (formElement) {
                var submitUrlPrefix = document.getElementById('submitUrlPrefix').value;
                formElement.action = submitUrlPrefix + newStatus;
            }
        }

        var intervalId = setInterval(makeGetRequest, 5000);
    };

    root.VOA.FileUploadNew = FileUploadNew;

}).call(this);
