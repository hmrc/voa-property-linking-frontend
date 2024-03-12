(function() {

    'use strict';

    var root = this, $ = root.jQuery;

    if (typeof VOA === 'undefined') {
        root.VOA = {};
    }

    // Check if the fileUploadForm element exists on the page
    var fileUploadFormElement = document.querySelector('#fileUploadForm');
    if (!fileUploadFormElement) {
        return; // If the form doesn't exist, exit the script
    }

    var FileUploadNew = function (){
        function makeGetRequest() {

            $.ajax({
                url: $("#fileUploadStatusUrl").text(),
                method: 'GET',
                dataType: 'json',
                success: function (responseBody, status, xmlHttpRequest) {
                    var fileStatus = responseBody[0];
                    var fileName = responseBody[1];
                    var fileDownloadLink = responseBody[2];
                    updateTag(fileStatus);
                    updateFormAction(fileStatus);
                    updateFileName(fileName, fileDownloadLink);
                    updateLiveRegion(fileStatus);
                    if (fileStatus === "READY") {
                        clearInterval(intervalId);
                    }
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

        // Function to update view with file name and download link
        function updateFileName(fileName, fileDownloadLink) {
            var element = document.querySelector('#main-content > div > div > dl > div > dt > a');
            if (element) {
                // Use the implicit Messages for the current language
                element.textContent = fileName;
                element.href = fileDownloadLink;
            }
        }

        var messages;

        // Fetch implicit messages & keys from server side
        $.ajax({
            url: $("#messageKeyUrl").text(), // Replace with the correct route
            method: 'GET',
            dataType: 'json',
            success: function(response) {
                messages = response;
                // Now you can use messages in your JavaScript logic
            },
            error: function() {
                console.error('Failed to fetch messages');
            }
        });

        // Use implicit messages response to update the status text
        function getStatusText(status) {
            switch (status) {
                case "UPLOADING":
                    return messages[0];
                case "READY":
                    return messages[1];
                case "FAILED":
                    return messages[2];
                default:
                    return "";
            }
        }

        function getAriaLiveText(status) {
            switch (status) {
                case "READY":
                    return messages[3];
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

        // Function to update the ARIA live region with the new status
        function updateLiveRegion(newStatus) {
            var liveRegion = document.getElementById('ariaLiveRegion');
            if (liveRegion) {
                liveRegion.textContent = getAriaLiveText(newStatus);
            }
        }

        // 2 second interval for update view
        var intervalId = setInterval(makeGetRequest, 2000);
    };

    root.VOA.FileUploadNew = FileUploadNew;

}).call(this);
