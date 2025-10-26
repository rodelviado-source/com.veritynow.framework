(function () {
    "use strict";

    const FormVuAPI = {};

    /**
     * Takes a JSON input in the format of formdata.json and updates the relevant
     * HTML fields values to match the provided values.
     *
     * @param {String|Object} formValues The values to be inserted into the HTML
     * @param {boolean} resetForm Whether a form reset should be called before inserting the values
     * @returns {boolean} true on method completion, false on invalid input
     */
    FormVuAPI.insertFormValues = function (formValues, resetForm) {
        if (typeof formValues === "string") {
            formValues = JSON.parse(formValues);
        } else if (!(formValues instanceof Object)) {
            console.error('Form values provided to insertFormValues is not an Object or JSON String');
            return false;
        }

        if (resetForm) {
            idrform.doc.resetForm();
        }

        for (let key of Object.keys(formValues)) {
            let val = formValues[key];
            if (val.inputType) {
                switch (val.inputType) {
                    case "radio button":
                        _handleRadioButtonInsert(val);
                        break;
                    case "checkbox":
                    case "checkbox group":
                        _handleCheckboxInsert(val);
                        break;
                    case "combobox":
                        _handleComboboxInsert(val);
                        break;
                    case "listbox":
                    case "listbox multi":
                        _handleListboxInsert(val);
                        break;
                    case "multiline text":
                        _handleMultilineTextInsert(val);
                        break;
                    default:
                        _handleGenericInputInsert(val);
                        break;
                }
            }
        }

        return true;
    };

    /**
     * Escapes the provided string for use as a CSS selector
     * Backslashes need to be escaped in order to be used in CSS selectors
     * (otherwise they will fail)
     *
     * @param {String} string the string to be escaped
     * @returns {String} an escaped string ready for use as a CSS selector
     * @private
     */
    let escapeForCssSelector = function(string) {
        return string.replaceAll('\\','\\\\');
    }

    /**
     * Selects the relevant radio button of the provided Form Field JSON object
     * Refreshes the AP images so the change is displayed
     * If the radio button is not found, a warning will be logged to the console
     *
     * @param {Object} jsonObj an object representation of a Form Field
     * @private
     */
    let _handleRadioButtonInsert = function(jsonObj) {
        let domRadioButtons = document.querySelectorAll('input[type=radio][data-field-name="' + escapeForCssSelector(jsonObj.fieldName) + '"][data-objref]');
        if (!domRadioButtons) {
            console.warn("Failed to find <input type=\"radio\"> " + jsonObj.fieldName);
            return;
        }
        domRadioButtons.forEach(element => {
            if (element.dataset.objref == jsonObj.objref) {
                element.checked = jsonObj.value;
                element.value = jsonObj.value;
            } else {
                element.checked = false;
            }
            if ("refreshApImage" in window && element.dataset.imageIndex) refreshApImage(parseInt(element.dataset.imageIndex));
        });
    }

    /**
     * Ticks the relevant checkbox of the provided Form Field JSON object
     * Refreshes the AP images so the change is displayed
     * If the checkbox is not found, a warning will be logged to the console
     *
     * @param {Object} jsonObj an object representation of a Form Field
     * @private
     */
    let _handleCheckboxInsert = function(jsonObj) {
        let domCheckboxes = document.querySelectorAll('input[type=checkbox][data-field-name="' + escapeForCssSelector(jsonObj.fieldName) + '"][data-objref]');
        if (!domCheckboxes) {
            console.warn("Failed to find <input type=\"checkbox\"> " + jsonObj.fieldName);
            return;
        }
        domCheckboxes.forEach(element => {
            if (element.dataset.objref == jsonObj.objref) {
                element.checked = jsonObj.value;
                element.value = jsonObj.value;
            } else {
                element.checked = false;
            }
            if ("refreshApImage" in window && element.dataset.imageIndex) refreshApImage(parseInt(element.dataset.imageIndex));
        });
    };

    /**
     * Selects the relevant combobox option from the provided Form Field JSON object
     * If a value is not an option, a new option will be added with the provided value
     * If the combobox is not found, a warning will be logged to the console
     *
     * @param {Object} jsonObj an object representation of a Form Field
     * @private
     */
    let _handleComboboxInsert = function(jsonObj) {
        const domCombobox = document.querySelector('[data-field-name="' + escapeForCssSelector(jsonObj.fieldName) + '"][data-objref="' + jsonObj.objref + '"]');
        if (!domCombobox) {
            console.warn("Failed to find " + jsonObj.fieldName);
            return;
        }

        if (domCombobox.dataset.editableCombo === "input") {
            const options = [...domCombobox.parentElement.querySelectorAll("[data-editable-combo=\"select\"]")]
                    .map(option => [option.value, option.innerText]);

            const option = options.find(option => option[0] === jsonObj.value);

            if (option) {
                domCombobox.value = option[1];
                domCombobox.dataset.realValue = option[0];
            } else {
                domCombobox.value = jsonObj.value;
                delete domCombobox.dataset.realValue;
            }

        } else {
            let options = domCombobox.children;
            let optionFound = false;
            for (let i = 0, ii = options.length; i < ii; i++) {
                let option = options[i];
                if (option.value === jsonObj.value) {
                    option.setAttribute("selected", "selected");
                    domCombobox.selectedIndex = i;
                    optionFound = true;
                } else {
                    option.removeAttribute("selected");
                }
            }

            if (!optionFound) {
                const newOpt = document.createElement("option");
                newOpt.text = jsonObj.value;
                newOpt.value = jsonObj.value;
                newOpt.setAttribute("selected", "selected");
                domCombobox.appendChild(newOpt);
            }
            domCombobox.value = jsonObj.value;
        }
    }

    /**
     * Selects the relevant listbox option from the provided Form Field JSON object
     * Unselects any other options
     * If the listbox is not found, a warning will be logged to the console
     *
     * @param {Object} jsonObj an object representation of a Form Field
     * @private
     */
    let _handleListboxInsert = function(jsonObj) {
        let domListbox = document.querySelector('select[data-field-name="' + escapeForCssSelector(jsonObj.fieldName) + '"][data-objref="' + jsonObj.objref + '"]');
        if (!domListbox) {
            console.warn("Failed to find <select> " + jsonObj.fieldName);
            return;
        }
        let options = domListbox.children;
        for (const option of options) {
            if (option.value === jsonObj.value || jsonObj.value instanceof Array && jsonObj.value.includes(option.value)) {
                option.selected = true;
                option.setAttribute("selected", "selected");
            } else {
                option.removeAttribute("selected");
            }
        }
    }

    /**
     * Inserts the multiline text from the provided Form Field JSON object
     * If the textarea is not found, a warning will be logged to the console
     *
     * @param {Object} jsonObj an object representation of a Form Field
     * @private
     */
    let _handleMultilineTextInsert = function(jsonObj) {
        let domTextArea = document.querySelector('textarea[data-field-name="' + escapeForCssSelector(jsonObj.fieldName) + '"][data-objref="' + jsonObj.objref + '"]');
        if (!domTextArea) {
            console.warn("Failed to find <textarea> " + jsonObj.fieldName);
            return;
        }
        domTextArea.value = jsonObj.value;
    }

    /**
     * Inserts the value of the provided Form Field JSON object into the relevant HTML input
     * Can also insert into textareas if not using single-line text output
     * If the input/textarea is not found, a warning will be logged to the console
     *
     * @param {Object} jsonObj an object representation of a Form Field
     * @private
     */
    let _handleGenericInputInsert = function(jsonObj) {
        let domInput = document.querySelector(':is(input,textarea)[data-field-name="' + escapeForCssSelector(jsonObj.fieldName) + '"][data-objref="' + jsonObj.objref + '"]');
        if (!domInput) {
            console.warn("Failed to find <input> or <textarea>" + jsonObj.fieldName);
            return;
        }
        domInput.value = jsonObj.value;
    }

    /**
     * @typedef SubmitParams Parameters used for submissions
     * @property {function(Response)} [success] A method to call on successful submission
     * @property {function(*)} [failure] A method to call on failed submission
     * @property {string} url The url to send the submission to
     */

    /**
     * @typedef MailParams parameters used for the mail submission
     * @extends SubmitParams
     * @property {"formdata"|"pdf"|"xml"} format The format to save the formdata as
     */


    /**
     * Populate a {@link XMLHttpRequest} object with handlers from the params.
     * @param {XMLHttpRequest} xhr The {@link XMLHttpRequest} object to populate
     * @param {SubmitParams} params The submit parameters containing the handlers to bind
     */
    let setRequestEventHandlers = function(xhr, params) {
        xhr.onreadystatechange = function(event) {
            if (xhr.readyState === XMLHttpRequest.DONE) {
                if (xhr.status === 200) {
                    if (params.success) {
                        params.success(event);
                    }
                } else {
                    if (params.failure) {
                        params.failure(event);
                    } else {
                        console.log(event.target.response);
                    }
                }
            }
        };
    };

    /**
     * Submit the formdata as mail
     * @param {string|MailParams} params The parameters for the submission, or the url
     */
    FormVuAPI.submitFormAsMail = function(params) {
        if (typeof params !== 'object') {
            params = {url: params, format: "formdata"};
        }

        if (!params.url.startsWith('mailto:')) {
            return;
        }

        switch (params.format) {
            case 'formdata':
                alert('The file will be saved in your machine, please attach it to the email');
                downloadFormDataValues(idrform.doc._getFormData());
                openMailToLink(params.url);
                break;
            case 'pdf':
                alert('The file will be saved in your machine, please attach it to the email');
                idrform.app.execMenuItem('SaveAs');
                openMailToLink(params.url);
                break;
            case 'xml':
                alert('The file will be saved in your machine, please attach it to the email');
                downloadXmlValues(idrform.doc._getFormData());
                break;
            default:
                alert('Unsupported submission format. Submission failed.');
                break;
        }
    };

    /**
     * A utility function to make the user's browser download a file
     * @param fileContents The contents of the file to download
     * @param fileName The name of the file
     * @param mimetype The mimetype of the file
     * @private
     */
    function _downloadFile(fileContents, fileName, mimetype) {
        let fileDL = document.createElement('a');
        fileDL.setAttribute('href', `data:${mimetype};charset=utf-8,${encodeURIComponent(fileContents)}`);
        fileDL.setAttribute('download', fileName + '.xml');
        fileDL.style.display = 'none';

        document.body.appendChild(fileDL);
        fileDL.click();
        document.body.removeChild(fileDL);
    }

    /**
     * Download the form data as a file
     * @param {Object.<string, string>} formValues the values to download
     */
    let downloadFormDataValues = function(formValues) {
        let formValuesString = idrform.doc._urlEncodeFormData(formValues)
        let fileDL = document.createElement('a');
        let pdfName = document.getElementById("FDFXFA_PDFName").textContent;

        _downloadFile(formValuesString, pdfName, "text/plain")
    };

    /**
     * Download the form data as a file
     * @param {Object.<string, string>} formValues the values to download
     */
    let downloadXmlValues = function(formValues) {
        let formValuesString = idrform.doc._buildXMLString(formValues)
        let pdfName = document.getElementById("FDFXFA_PDFName").textContent;

        _downloadFile(formValuesString, pdfName, "text/xml");
    };

    /**
     * Open a mailto link
     * @param {string} target The target url to mail to
     */
    let openMailToLink = function(target) {
        let mailto = document.createElement('a');
        mailto.setAttribute('href', target);
        mailto.setAttribute('target', "_blank");
        mailto.style.display = 'none';
        document.body.appendChild(mailto);
        mailto.click();
        document.body.removeChild(mailto);
    };

    /**
     * Submit the form data as json
     * @param {string|SubmitParams} params The params for the submission
     * @return {XMLHttpRequest} The {@link XMLHttpRequest} object used for the submission
     */
    FormVuAPI.submitFormAsJSON = function (params) {
        let url = typeof params === 'object' ? params.url : params;

        let formValues = {data: idrform.doc._getFormData()};
        let xhr = new XMLHttpRequest();
        if (xhr.upload) {
            setRequestEventHandlers(xhr, params);
            xhr.open('POST', url, true);
            xhr.setRequestHeader('Content-type', 'application/json');
            xhr.send(JSON.stringify(formValues));
            return xhr;
        }
    };
	
		
    /**
     * Submit the form data as XML
     * @param {string|SubmitParams} params The params for the submission
     * @return {XMLHttpRequest} The {@link XMLHttpRequest} object used for the submission
     */
    FormVuAPI.submitFormAsXML = function (params) {
        let url = typeof params === 'object' ? params.url : params;

        let xhr = new XMLHttpRequest();
        if (xhr.upload) {
            setRequestEventHandlers(xhr, params);
            xhr.open('POST', url, true);
            xhr.setRequestHeader('Content-type', 'application/xml');
            xhr.send(idrform.doc._buildXMLString(idrform.doc._getFormData()));
            return xhr;
        }
    }

    /**
     * Submit the form data as {@link FormData}
     * @param {string|SubmitParams} params The params for the submission
     * @return {XMLHttpRequest} The {@link XMLHttpRequest} object used for the submission
     */
    FormVuAPI.submitFormAsFormData = function (params) {
        let url = typeof params === 'object' ? params.url : params;

        let formValues = idrform.doc._getFormData();
        let xhr = new XMLHttpRequest();
        if (xhr.upload) {
            setRequestEventHandlers(xhr, params);
            xhr.open('POST', url, true);

            let formData = new FormData();
            for (const [key, value] of Object.entries(formValues)) {
                if (value === undefined) continue;

                if (Array.isArray(value)) {
                    value.forEach(v => formData.append(`${encodeURIComponent(key)}[]`, v));
                } else {
                    formData.append(encodeURIComponent(key), value);
                }
            }
            xhr.send(formData);
            return xhr;
        }
    };

    /**
     * Submit the form data as a PDF file
     * @param {string|SubmitParams} params The params for the submission
     * @return {XMLHttpRequest} The {@link XMLHttpRequest} object used for the submission
     */
    FormVuAPI.submitFormAsPDF = function (params) {
        let url, submitType="formData";
        if (typeof params === 'object') {
            url = params.url;
            submitType = params.submitType || "formData";
        } else {
            url = params;
        }

        const xhr = new XMLHttpRequest();
        if (xhr.upload) {
            setRequestEventHandlers(xhr, params);
            xhr.open('POST', url, true);
            const file = idrform.getCompletedFormPDF();
            const fileName = document.querySelector("#FDFXFA_PDFName").textContent;

            if (submitType === "raw") {
                xhr.setRequestHeader("Content-Disposition", `filename="${fileName}"`)
                xhr.setRequestHeader("Content-Type", "application/pdf");

                xhr.send(file);
            } else if (submitType === "formData") {
                const formData = new FormData();
                formData.append("file", file, fileName);
                xhr.send(formData);
            }
            return xhr;
        }
    }

    window.FormVuAPI = FormVuAPI;

}());