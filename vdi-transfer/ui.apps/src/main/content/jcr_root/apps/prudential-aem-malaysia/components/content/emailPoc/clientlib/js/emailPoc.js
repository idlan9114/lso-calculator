(function () {
    "use strict";

    var SELECTOR = '[data-cmp-is="emailPoc"]';
    var CSRF_TOKEN_URL = "/libs/granite/csrf/token.json";

    function getCsrfToken() {
        return fetch(CSRF_TOKEN_URL, { credentials: "same-origin" })
            .then(function (response) {
                return response.ok ? response.json() : {};
            })
            .then(function (json) {
                return json.token || "";
            })
            .catch(function () {
                return "";
            });
    }

    function showStatus(statusEl, message, isError) {
        statusEl.textContent = message;
        statusEl.classList.toggle("email-poc__status--error", !!isError);
    }

    function init(el) {
        var form = el.querySelector(".email-poc__form");
        var input = el.querySelector(".email-poc__input");
        var button = el.querySelector(".email-poc__button");
        var statusEl = el.querySelector(".email-poc__status");
        if (!form || !input || !button || !statusEl) return;

        form.addEventListener("submit", function (event) {
            event.preventDefault();
            if (!form.reportValidity()) return;

            button.disabled = true;
            showStatus(statusEl, "Sending...", false);

            getCsrfToken()
                .then(function (token) {
                    return fetch(el.dataset.endpoint, {
                        method: "POST",
                        credentials: "same-origin",
                        headers: {
                            "Accept": "application/json",
                            "CSRF-Token": token
                        },
                        body: new URLSearchParams({ email: input.value.trim() })
                    });
                })
                .then(function (response) {
                    return response.json().catch(function () {
                        return { sent: false, message: "Email could not be sent (HTTP " + response.status + ")." };
                    });
                })
                .then(function (result) {
                    showStatus(statusEl, result.message, !result.sent);
                    if (result.sent) form.reset();
                })
                .catch(function () {
                    showStatus(statusEl, "Email could not be sent. Please try again later.", true);
                })
                .then(function () {
                    button.disabled = false;
                });
        });
    }

    function onReady() {
        document.querySelectorAll(SELECTOR).forEach(init);
    }

    if (document.readyState !== "loading") {
        onReady();
    } else {
        document.addEventListener("DOMContentLoaded", onReady);
    }
})();
