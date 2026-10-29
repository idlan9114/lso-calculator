/*
 * Dialog validators for lsoCalculator. The server enforces the same rules; these just give authors
 * feedback before they save.
 */
(function (window, $) {
    "use strict";

    var TEMPLATE_ROOT = "/content/dam/pamb/email-templates/legacy-blueprint/";
    var COMPONENT_ID = /^[A-Za-z][A-Za-z0-9_-]{0,63}$/;
    var registry = $(window).adaptTo("foundation-registry");

    // Email Template: an .html file inside the legacy-blueprint template folder.
    registry.register("foundation.validation.validator", {
        selector: "[data-validation='lso-email-template']",
        validate: function (el) {
            var value = (el.value || "").trim();
            if (!value) {
                return;
            }
            if (value.indexOf(TEMPLATE_ROOT) !== 0 || value.indexOf("..") !== -1 || !/\.html$/i.test(value)) {
                return "Select an HTML file from " + TEMPLATE_ROOT;
            }
        }
    });

    // Component ID: a valid HTML id (no '#').
    registry.register("foundation.validation.validator", {
        selector: "[data-validation='lso-component-id']",
        validate: function (el) {
            var value = (el.value || "").trim();
            if (value && !COMPONENT_ID.test(value)) {
                return "Use letters, numbers, - and _ only, starting with a letter (no #).";
            }
        }
    });

    // Multifield minimum item count (data-lso-min-items).
    registry.register("foundation.validation.validator", {
        selector: "coral-multifield[data-validation='lso-multifield-min']",
        validate: function (el) {
            var min = parseInt(el.getAttribute("data-lso-min-items"), 10) || 1;
            if (el.items && el.items.length < min) {
                return "Add at least " + min + " option" + (min === 1 ? "" : "s") + ".";
            }
        }
    });
})(window, Granite.$);
