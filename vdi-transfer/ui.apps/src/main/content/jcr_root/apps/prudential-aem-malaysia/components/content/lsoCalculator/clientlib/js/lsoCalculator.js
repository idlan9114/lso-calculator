/* Legacy Blueprint journey. Authored content and the existing submission contract come from AEM. */
(function () {
    "use strict";
    var M = window.LsoCalculatorMath;
    var ASSETS = "/etc.clientlibs/prudential-aem-malaysia/components/content/lsoCalculator/clientlib/resources/";
    var STEP_LABELS = ["Priorities", "Peace of mind", "About you", "People", "Legacy", "Distribution", "Put aside", "Review"];
    var IMAGE_PANELS = [
        { tag: "Your Legacy Planner", title: "Your legacy.\nYour masterplan.", description: "Think about what you want your legacy to achieve, and we’ll help you explore how to bring it to life." },
        { tag: "Part 1 of 4", title: "Start with what\nmatters most.", description: "" },
        { tag: "Part 2 of 4", title: "Plan for the\npeople you love.", description: "" },
        { tag: "Part 3 of 4", title: "Decide how your legacy lives on.", description: "" },
        { tag: "Part 4 of 4", title: "Your Plan", description: "One final look before we bring your plan to life." }
    ];
    function node(tag, attrs, children) {
        var n = document.createElement(tag);
        Object.keys(attrs || {}).forEach(function (key) {
            var v = attrs[key];
            if (v === false || v === null || v === undefined) { return; }
            if (key === "text") { n.textContent = v; }
            else if (key === "html") { n.innerHTML = v; } // Only server-filtered authored RTE.
            else { n.setAttribute(key, v === true ? "" : String(v)); }
        });
        (children || []).forEach(function (child) { if (child !== null && child !== undefined) { n.appendChild(typeof child === "string" ? document.createTextNode(child) : child); } });
        return n;
    }
    function button(text, fn, secondary) {
        var b = node("button", { type: "button", "class": secondary ? "lb-button lb-button--secondary" : "lb-button", text: text });
        b.addEventListener("click", fn); return b;
    }
    function money(n) { return "RM " + Math.round(n || 0).toLocaleString("en-MY"); }
    // Exact SVG markup extracted from the supplied v10 prototype.
    var ICONS = {
        "everyday": "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"18\" height=\"18\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"#DA291C\" stroke-width=\"2\" stroke-linecap=\"round\" stroke-linejoin=\"round\"><path d=\"M3 11l9-7 9 7\"></path><path d=\"M5 10v10h14V10\"></path><path d=\"M10 20v-6h4v6\"></path></svg>",
        "education": "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"18\" height=\"18\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"#DA291C\" stroke-width=\"2\" stroke-linecap=\"round\" stroke-linejoin=\"round\"><path d=\"M2 9l10-5 10 5-10 5z\"></path><path d=\"M6 11v5c0 1.5 3 3 6 3s6-1.5 6-3v-5\"></path></svg>",
        "care": "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"18\" height=\"18\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"#DA291C\" stroke-width=\"2\" stroke-linecap=\"round\" stroke-linejoin=\"round\"><path d=\"M20.8 5.6a5 5 0 0 0-7.1 0L12 7.3l-1.7-1.7a5 5 0 0 0-7.1 7.1L12 21.5l8.8-8.8a5 5 0 0 0 0-7.1z\"></path><path d=\"M8 12h2l1-2 2 4 1-2h2\"></path></svg>",
        "family": "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"18\" height=\"18\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"#DA291C\" stroke-width=\"2\" stroke-linecap=\"round\" stroke-linejoin=\"round\"><circle cx=\"9\" cy=\"14\" r=\"5\"></circle><circle cx=\"15\" cy=\"14\" r=\"5\"></circle><path d=\"M10 4h4l-2 3z\"></path></svg>",
        "retirement": "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"18\" height=\"18\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"#DA291C\" stroke-width=\"2\" stroke-linecap=\"round\" stroke-linejoin=\"round\"><circle cx=\"12\" cy=\"12\" r=\"4\"></circle><path d=\"M12 2v2M12 20v2M4.9 4.9l1.4 1.4M17.7 17.7l1.4 1.4M2 12h2M20 12h2M4.9 19.1l1.4-1.4M17.7 6.3l1.4-1.4\"></path></svg>",
        "enough": "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"18\" height=\"18\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"#DA291C\" stroke-width=\"2\" stroke-linecap=\"round\" stroke-linejoin=\"round\"><path d=\"M12 3l8 3v6c0 4.5-3.4 8.2-8 9-4.6-.8-8-4.5-8-9V6z\"></path><path d=\"M9 12l2 2 4-4\"></path></svg>",
        "timing": "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"18\" height=\"18\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"#DA291C\" stroke-width=\"2\" stroke-linecap=\"round\" stroke-linejoin=\"round\"><circle cx=\"12\" cy=\"12\" r=\"9\"></circle><path d=\"M12 7v5l3 2\"></path></svg>",
        "flex": "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"18\" height=\"18\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"#DA291C\" stroke-width=\"2\" stroke-linecap=\"round\" stroke-linejoin=\"round\"><path d=\"M16 3h5v5\"></path><path d=\"M4 20L21 3\"></path><path d=\"M21 16v5h-5\"></path><path d=\"M15 15l6 6\"></path><path d=\"M4 4l5 5\"></path></svg>",
        "wishes": "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"18\" height=\"18\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"#DA291C\" stroke-width=\"2\" stroke-linecap=\"round\" stroke-linejoin=\"round\"><path d=\"M14 3H6a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V9z\"></path><path d=\"M14 3v6h6\"></path><path d=\"M8 15l2 2 4-4\"></path></svg>",
        "unsure": "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"18\" height=\"18\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"#DA291C\" stroke-width=\"2\" stroke-linecap=\"round\" stroke-linejoin=\"round\"><circle cx=\"12\" cy=\"12\" r=\"9\"></circle><path d=\"M15.5 8.5l-2 5-5 2 2-5z\"></path></svg>",
        "introHeart": "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"15\" height=\"15\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"#DA291C\" stroke-width=\"2\" stroke-linecap=\"round\" stroke-linejoin=\"round\"><path d=\"M20.8 5.6a5 5 0 0 0-7.1 0L12 7.3l-1.7-1.7a5 5 0 0 0-7.1 7.1L12 21.5l8.8-8.8a5 5 0 0 0 0-7.1z\"></path></svg>",
        "introShield": "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"15\" height=\"15\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"#DA291C\" stroke-width=\"2\" stroke-linecap=\"round\" stroke-linejoin=\"round\"><path d=\"M12 3l8 3v6c0 4.5-3.4 8.2-8 9-4.6-.8-8-4.5-8-9V6z\"></path><path d=\"M9 12l2 2 4-4\"></path></svg>",
        "age": "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"13\" height=\"13\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"#DA291C\" stroke-width=\"2\" stroke-linecap=\"round\" stroke-linejoin=\"round\"><path d=\"M4 21h16v-8H4z\"></path><path d=\"M4 16c2 0 2-1.5 4-1.5s2 1.5 4 1.5 2-1.5 4-1.5 2 1.5 4 1.5\"></path><path d=\"M12 13V9\"></path><path d=\"M12 6.5a1.5 1.5 0 0 1-1-2.5L12 3l1 1a1.5 1.5 0 0 1-1 2.5z\"></path></svg>",
        "relationship": "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"13\" height=\"13\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"#DA291C\" stroke-width=\"2\" stroke-linecap=\"round\" stroke-linejoin=\"round\"><circle cx=\"9\" cy=\"8\" r=\"3.5\"></circle><path d=\"M2.5 20c0-3.6 2.9-6 6.5-6s6.5 2.4 6.5 6\"></path><path d=\"M16 4.6a3.5 3.5 0 0 1 0 6.8\"></path><path d=\"M18 14.4c2 .7 3.5 2.7 3.5 5.6\"></path></svg>",
        "person": "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"17\" height=\"17\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"#DA291C\" stroke-width=\"2\" stroke-linecap=\"round\" stroke-linejoin=\"round\"><circle cx=\"12\" cy=\"8\" r=\"4\"></circle><path d=\"M4 21c0-4.4 3.6-7 8-7s8 2.6 8 7\"></path></svg>",
        "lump": "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"17\" height=\"17\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"#DA291C\" stroke-width=\"2\" stroke-linecap=\"round\" stroke-linejoin=\"round\"><path d=\"M20 12v9H4v-9\"></path><path d=\"M2 7h20v5H2z\"></path><path d=\"M12 22V7\"></path><path d=\"M12 7H7.5a2.5 2.5 0 0 1 0-5C11 2 12 7 12 7z\"></path><path d=\"M12 7h4.5a2.5 2.5 0 0 0 0-5C13 2 12 7 12 7z\"></path></svg>",
        "structured": "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"17\" height=\"17\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"#DA291C\" stroke-width=\"2\" stroke-linecap=\"round\" stroke-linejoin=\"round\"><rect x=\"3\" y=\"4\" width=\"18\" height=\"17\" rx=\"2\"></rect><path d=\"M16 2v4M8 2v4M3 10h18\"></path></svg>",
        "milestones": "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"17\" height=\"17\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"#DA291C\" stroke-width=\"2\" stroke-linecap=\"round\" stroke-linejoin=\"round\"><path d=\"M5 22V3\"></path><path d=\"M5 4h12l-2 4 2 4H5\"></path></svg>",
        "thought": "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"18\" height=\"18\" viewBox=\"0 0 24 24\" fill=\"#DA291C\"><path d=\"M12 2l1.9 5.6L19.5 9.5l-5.6 1.9L12 17l-1.9-5.6L4.5 9.5l5.6-1.9z\"></path><path d=\"M19 14l.9 2.6 2.6.9-2.6.9L19 21l-.9-2.6-2.6-.9 2.6-.9z\"></path></svg>",
        "reviewHeart": "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"12\" height=\"12\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"#DA291C\" stroke-width=\"2\" stroke-linecap=\"round\" stroke-linejoin=\"round\"><path d=\"M20.8 5.6a5 5 0 0 0-7.1 0L12 7.3l-1.7-1.7a5 5 0 0 0-7.1 7.1L12 21.5l8.8-8.8a5 5 0 0 0 0-7.1z\"></path></svg>",
        "reviewPeace": "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"12\" height=\"12\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"#DA291C\" stroke-width=\"2\" stroke-linecap=\"round\" stroke-linejoin=\"round\"><path d=\"M12 3l8 3v6c0 4.5-3.4 8.2-8 9-4.6-.8-8-4.5-8-9V6z\"></path><path d=\"M9 12l2 2 4-4\"></path></svg>",
        "reviewPriorities": "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"12\" height=\"12\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"#DA291C\" stroke-width=\"2\" stroke-linecap=\"round\" stroke-linejoin=\"round\"><path d=\"M12 3l2.7 5.6 6.1.9-4.4 4.3 1 6.1L12 17l-5.4 2.9 1-6.1-4.4-4.3 6.1-.9z\"></path></svg>",
        "reviewAbout": "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"12\" height=\"12\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"#DA291C\" stroke-width=\"2\" stroke-linecap=\"round\" stroke-linejoin=\"round\"><circle cx=\"12\" cy=\"8\" r=\"4\"></circle><path d=\"M4 21c0-4.4 3.6-7 8-7s8 2.6 8 7\"></path></svg>",
        "reviewPeople": "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"12\" height=\"12\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"#DA291C\" stroke-width=\"2\" stroke-linecap=\"round\" stroke-linejoin=\"round\"><circle cx=\"9\" cy=\"8\" r=\"3.5\"></circle><path d=\"M2.5 20c0-3.6 2.9-6 6.5-6s6.5 2.4 6.5 6\"></path><path d=\"M16 4.6a3.5 3.5 0 0 1 0 6.8\"></path><path d=\"M18 14.4c2 .7 3.5 2.7 3.5 5.6\"></path></svg>",
        "reviewLegacy": "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"12\" height=\"12\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"#DA291C\" stroke-width=\"2\" stroke-linecap=\"round\" stroke-linejoin=\"round\"><ellipse cx=\"12\" cy=\"6\" rx=\"7\" ry=\"3\"></ellipse><path d=\"M5 6v6c0 1.7 3.1 3 7 3s7-1.3 7-3V6\"></path><path d=\"M5 12v6c0 1.7 3.1 3 7 3s7-1.3 7-3v-6\"></path></svg>",
        "reviewPutAside": "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"12\" height=\"12\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"#DA291C\" stroke-width=\"2\" stroke-linecap=\"round\" stroke-linejoin=\"round\"><path d=\"M19 10c0-3.3-3.1-6-7-6-3 0-5.5 1.6-6.5 3.8L3 7v4l1.5.6c.3 1.6 1.3 3 2.5 4V19h3v-2h4v2h3v-3.4c1.2-.9 2-2.1 2-3.6h2v-2z\"></path><circle cx=\"15.5\" cy=\"9.5\" r=\".6\"></circle></svg>",
        "reviewDistribution": "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"12\" height=\"12\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"#DA291C\" stroke-width=\"2\" stroke-linecap=\"round\" stroke-linejoin=\"round\"><path d=\"M12 3v18\"></path><path d=\"M12 8l-7 4\"></path><path d=\"M12 8l7 4\"></path><circle cx=\"5\" cy=\"14\" r=\"2\"></circle><circle cx=\"19\" cy=\"14\" r=\"2\"></circle></svg>",
        "modeKnown": "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"15\" height=\"15\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"#DA291C\" stroke-width=\"2\" stroke-linecap=\"round\" stroke-linejoin=\"round\"><circle cx=\"12\" cy=\"12\" r=\"9\"></circle><circle cx=\"12\" cy=\"12\" r=\"5\"></circle><circle cx=\"12\" cy=\"12\" r=\"1\"></circle></svg>",
        "modeUnsure": "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"15\" height=\"15\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"#DA291C\" stroke-width=\"2\" stroke-linecap=\"round\" stroke-linejoin=\"round\"><circle cx=\"12\" cy=\"12\" r=\"9\"></circle><path d=\"M15.5 8.5l-2 5-5 2 2-5z\"></path></svg>",
        "help1": "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"16\" height=\"16\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"#DA291C\" stroke-width=\"2\" stroke-linecap=\"round\" stroke-linejoin=\"round\"><path d=\"M12 3v18\"></path><path d=\"M12 8l-7 4\"></path><path d=\"M12 8l7 4\"></path><circle cx=\"5\" cy=\"14\" r=\"2\"></circle><circle cx=\"19\" cy=\"14\" r=\"2\"></circle></svg>",
        "help2": "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"16\" height=\"16\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"#DA291C\" stroke-width=\"2\" stroke-linecap=\"round\" stroke-linejoin=\"round\"><path d=\"M12 3l8 3v6c0 4.5-3.4 8.2-8 9-4.6-.8-8-4.5-8-9V6z\"></path><path d=\"M9 12l2 2 4-4\"></path></svg>",
        "help3": "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"16\" height=\"16\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"#DA291C\" stroke-width=\"2\" stroke-linecap=\"round\" stroke-linejoin=\"round\"><path d=\"M20.6 13.4l-7.2 7.2a2 2 0 0 1-2.8 0L3 13V3h10l7.6 7.6a2 2 0 0 1 0 2.8z\"></path><circle cx=\"7.5\" cy=\"7.5\" r=\"1.3\"></circle></svg>",
        "help4": "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"16\" height=\"16\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"#DA291C\" stroke-width=\"2\" stroke-linecap=\"round\" stroke-linejoin=\"round\"><path d=\"M3 17l6-6 4 4 8-8\"></path><path d=\"M15 7h6v6\"></path></svg>",
        "contactMail": "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"21\" height=\"21\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"#DA291C\" stroke-width=\"2\" stroke-linecap=\"round\" stroke-linejoin=\"round\"><rect x=\"3\" y=\"5\" width=\"18\" height=\"14\" rx=\"2\"></rect><path d=\"M3 7l9 6 9-6\"></path></svg>",
        "putAside": "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"21\" height=\"21\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"#DA291C\" stroke-width=\"2\" stroke-linecap=\"round\" stroke-linejoin=\"round\"><path d=\"M19 10c0-3.3-3.1-6-7-6-3 0-5.5 1.6-6.5 3.8L3 7v4l1.5.6c.3 1.6 1.3 3 2.5 4V19h3v-2h4v2h3v-3.4c1.2-.9 2-2.1 2-3.6h2v-2z\"></path><circle cx=\"15.5\" cy=\"9.5\" r=\".6\"></circle></svg>",
        "introThought": "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"16\" height=\"16\" viewBox=\"0 0 24 24\" fill=\"#DA291C\"><path d=\"M12 2l1.9 5.6L19.5 9.5l-5.6 1.9L12 17l-1.9-5.6L4.5 9.5l5.6-1.9z\"></path><path d=\"M19 14l.9 2.6 2.6.9-2.6.9L19 21l-.9-2.6-2.6-.9 2.6-.9z\"></path></svg>"
    };
    function icon(name) {
        var svg = ICONS[name] || ICONS.person;
        var size = svg.match(/width="(\d+)"/)[1];
        return node("span", { "class": "lb-icon lb-icon--" + name, "aria-hidden": "true" }, [node("img", { src: "data:image/svg+xml," + encodeURIComponent(svg), alt: "", width: size, height: size })]);
    }
    function safeUrl(value) { return /^(https?:\/\/|\/(?!\/))/.test(value || "") ? value : ""; }
    function init(root) {
        if (root.dataset.lsoReady) { return; }
        var config;
        try { config = JSON.parse(root.getAttribute("data-config")); } catch (e) { return; }
        if (!config || !config.rules || !M) { return; }
        root.dataset.lsoReady = "true";
        var app = root.querySelector(".lso-calculator__app"), rules = config.rules, settings = config.settings || {};
        var uid = root.id || "lb-" + Math.random().toString(36).slice(2);
        var storeKey = "legacy-blueprint-v8:" + location.pathname + ":" + root.getAttribute("data-endpoint");
        var state = { screen: "intro", person: 0, sending: false, result: null, errors: {}, notice: "", saved: null, returnToReview: false, shareTouched: [], allocationTouched: {} };
        var draftName = "", draftRelationship = "", editing = null;
        var bindings = [];
        function boundText(read, className) {
            var element = node("span", { "class": className || null, text: read() });
            bindings.push(function () { element.textContent = read(); }); return element;
        }
        function shortMoney(value) { return value >= 1000000 ? "RM " + value / 1000000 + "M" : value >= 1000 ? "RM " + value / 1000 + "K" : money(value); }
        function currencyField(labelText, value, setter, opts) {
            opts = opts || {}; var id = uid + "-money-" + Math.random().toString(36).slice(2);
            var field = node("input", { id: id, type: "text", inputmode: "numeric", autocomplete: "off", value: value ? Number(value).toLocaleString("en-MY") : "", placeholder: opts.placeholder || "0", maxlength: 20 });
            field.addEventListener("input", function () { var digits = field.value.replace(/[^0-9]/g, ""); setter(digits ? Number(digits) : 0); save(); updatePreviews(); });
            field.addEventListener("blur", function () { var digits = field.value.replace(/[^0-9]/g, ""); field.value = digits ? Number(digits).toLocaleString("en-MY") : ""; });
            return node("div", { "class": "lb-field" }, [labelText ? node("label", { "for": id, text: labelText }) : null,
                node("div", { "class": "lb-currency-field" }, [node("span", { text: "RM" }), field])]);
        }
        function unitField(labelText, value, setter, unit, min, max, attributes) {
            var field = number(labelText, value, setter, min, max), control = field.querySelector("input");
            Object.keys(attributes || {}).forEach(function (key) { control.setAttribute(key, attributes[key]); });
            field.removeChild(control); field.appendChild(node("div", { "class": "lb-unit-field" }, [control, node("span", { text: unit })])); return field;
        }
        function shortPresets(values, current, setter) {
            return choices(values.map(function (value) { return { value: value, label: shortMoney(value) }; }), current, function (value) { setter(value); save(); render(); });
        }
        function detailRow(labelText, value) { return node("div", { "class": "lb-detail-row" }, [node("span", { text: labelText }), node("strong", { text: value })]); }
        function needParts(person) {
            var d = person.distribution;
            return { lump: d.parts.indexOf("lump") >= 0 ? d.lumpAmount || 0 : 0,
                structured: d.parts.indexOf("structured") >= 0 ? M.totalPayments(d.structured, d.structured.amount || 0) : 0,
                milestones: d.parts.indexOf("milestones") >= 0 ? M.sum(d.milestones.selected.map(function (id) { return d.milestones.amounts[id] || 0; })) : 0 };
        }
        function needsTotal(person) { var values = needParts(person); return values.lump + values.structured + values.milestones; }
        function displayPerson(person) {
            var preview = M.preview(a, rules), result = preview.people.filter(function (p) { return p.id === person.id; })[0];
            return result || { share: 0, amount: 0 };
        }
        function partAmount(person, part) {
            if (a.amountMode === "unsure") { return needParts(person)[part]; }
            return a.legacyAmount * person.share / 100 * (person.distribution.parts.length > 1 ? person.distribution.split[part] / 100 : 1);
        }
        function fresh() {
            return { importance: null, preparedness: null, needs: [], peaceOfMind: null, ageRange: null, relationshipStatus: null,
                people: [], amountMode: "known", legacyAmount: rules.legacyMinimum, putAside: 0,
                contact: { fullName: "", email: "", countryCode: "60", mobile: "", nric: "", state: "", referralCode: "", consent: false } };
        }
        var a = fresh();
        function displayCopy(value) {
            // Remove the test-content marker from visible text only; retain HTML attributes and URLs.
            return String(value).split(/(<[^>]*>)/g).map(function (part) {
                return part.charAt(0) === "<" ? part : part.replace(/[ \t]*\btest\b/gi, "").replace(/[ \t]+([.,!?;:])/g, "$1");
            }).join("").trim();
        }
        function copy(tab, key, fallback) {
            var v = (config[tab] || {})[key];
            var value = v === undefined || v === null || v === "" ? (fallback || "") : String(v);
            return /(?:Path|Image|Icon|GaLabel)$/.test(key) ? value : displayCopy(value);
        }
        function fill(text, values) { return text.replace(/\{(\w+)\}/g, function (match, key) { return values[key] === undefined ? match : values[key]; }); }
        function rte(tab, key, fallback) { return node("div", { "class": "lb-copy", html: copy(tab, key, fallback) }); }
        function title(text) { return node("h2", { "class": "lb-heading", text: text }); }
        function note(text) { return node("p", { "class": "lb-muted", text: text }); }
        function card(children) { return node("div", { "class": "lb-card" }, children); }
        function options(ids, tab, prefix) { return ids.map(function (id, i) { return { value: id, label: copy(tab, prefix + (i + 1) + "Label", id) }; }); }
        var needs = options(rules.needs, "priorities", "option"), peace = options(rules.peaceOfMind, "priorities", "peace");
        var ages = options(rules.ageRanges, "aboutPeople", "age"), statuses = options(rules.relationshipStatuses, "aboutPeople", "status");
        var relationships = ((config.aboutPeople || {}).relationshipOptions || []).map(function (option) {
            return { value: option.value, label: displayCopy(option.label || option.value) };
        });
        function relationshipKey(value) {
            var key = String(value || "").trim().toLowerCase();
            if (key === "spouse" || key === "parent") { return key; }
            var option = relationships.filter(function (o) { return o.value === value; })[0];
            return option ? option.label.trim().toLowerCase() : key;
        }
        var moments = options(rules.milestones, "distribution", "moment");
        var parts = ["lump", "structured", "milestones"];
        function label(list, value) { var found = list.filter(function (o) { return o.value === value; })[0]; return found ? found.label : value || "—"; }
        function stepInfo() {
            var s = state.screen;
            var steps = { intro: [0, "Main page"], s1: [1, "Priorities"], s2: [2, "Peace of mind"], s3: [3, "About you"], s4: [4, "People"],
                s5: [5, "Legacy"], shares: [6.1, "Distribution"], distribution: [6.2, "Distribution"], calc: [6.2, "Distribution"],
                s7: [7, "Put aside"], s8: [7, "Review"], contact: [7, "Contact Us"], blueprint: [7, "Blueprint Ready"] };
            return steps[s] || steps.intro;
        }
        function track(event, action) {
            if (!settings.gaEnabled || settings.editorPreview) { return; }
            var step = stepInfo();
            window.dataLayer = window.dataLayer || [];
            window.dataLayer.push({ event: event, funnel_name: "legacy blueprint", funnel_type: "$calculator",
                step_number: step[0], step_name: state.saved && state.screen === "intro" ? "Returning User" : step[1], step_action: action || "view" });
        }
        // Resume only the planning answers; contact details and consent stay in memory.
        function hasProgress(answers) {
            return !!(answers && (answers.importance || answers.preparedness ||
                (Array.isArray(answers.needs) && answers.needs.length) || (Array.isArray(answers.people) && answers.people.length)));
        }
        function save() {
            if (settings.editorPreview || state.screen === "blueprint") { return; }
            if (!hasProgress(a)) { clearSaved(); return; }
            try {
                var planAnswers = JSON.parse(JSON.stringify(a)); delete planAnswers.contact;
                localStorage.setItem(storeKey, JSON.stringify({ version: 1, updated: Date.now(), answers: planAnswers, screen: state.screen, person: state.person }));
            } catch (e) { /* Storage can be disabled. The journey still works. */ }
        }
        function clearSaved() { try { localStorage.removeItem(storeKey); } catch (e) { /* optional */ } }
        try {
            var saved = JSON.parse(localStorage.getItem(storeKey));
            if (!settings.editorPreview && saved && saved.version === 1 && Date.now() - saved.updated < 30 * 86400000 && saved.answers && Array.isArray(saved.answers.people) && hasProgress(saved.answers)) { state.saved = saved; }
        } catch (e) { /* optional */ }
        function go(screen, person) {
            state.screen = screen; state.person = person || 0; state.notice = ""; state.errors = {};
            save(); render(true);
            if (screen !== "blueprint") { track(screen === "intro" && !state.saved ? "funnel_begin" : "funnel_step"); }
            var heading = app.querySelector(".lb-heading"); if (heading) { heading.setAttribute("tabindex", "-1"); heading.focus({ preventScroll: true }); }
            root.scrollIntoView({ behavior: window.matchMedia && window.matchMedia("(prefers-reduced-motion: reduce)").matches ? "auto" : "smooth", block: "start" });
        }
        function edit(screen, person) { state.returnToReview = true; track("funnel_navigation", "edit"); go(screen, person); }
        function restart() { clearSaved(); a = fresh(); state.saved = null; state.result = null; state.returnToReview = false; state.shareTouched = []; state.allocationTouched = {}; draftName = ""; draftRelationship = ""; editing = null; go("intro"); }
        function choices(list, selected, onSelect, descriptions, kind) {
            return node("div", { "class": descriptions ? "lb-options" : "lb-chips" }, list.map(function (o, i) {
                var active = Array.isArray(selected) ? selected.indexOf(o.value) >= 0 : selected === o.value;
                var b = button(o.label, function () { onSelect(o.value); }, true);
                b.classList.add("lb-choice"); b.classList.toggle("is-selected", active);
                b.setAttribute("aria-pressed", String(active));
                if (kind) {
                    b.textContent = "";
                    b.appendChild(icon(o.value));
                    b.appendChild(node("span", { "class": "lb-choice__body" }, [node("span", { "class": "lb-choice__title", text: o.label }),
                        descriptions && descriptions[i] ? node("span", { "class": "lb-choice__description", text: descriptions[i] }) : null]));
                    b.appendChild(node("span", { "class": "lb-choice__mark lb-choice__mark--" + kind, "aria-hidden": "true", text: kind === "rank" && active ? selected.indexOf(o.value) + 1 : kind === "multi" && active ? "✓" : "" }));
                } else if (descriptions && descriptions[i]) { b.appendChild(node("span", { "class": "lb-choice__description", text: descriptions[i] })); }
                return b;
            }));
        }
        function input(labelText, value, onChange, opts) {
            opts = opts || {};
            var id = uid + "-" + (opts.key || Math.random().toString(36).slice(2));
            var element = node("input", { id: id, type: opts.type || "text", value: value === null ? "" : value, min: opts.min, max: opts.max,
                step: opts.type === "number" ? "1" : null, maxlength: opts.maxlength, inputmode: opts.numeric ? "numeric" : null,
                autocomplete: opts.autocomplete, placeholder: opts.placeholder, "aria-invalid": state.errors[opts.key] ? "true" : null,
                "aria-describedby": state.errors[opts.key] ? id + "-error" : null, required: opts.required });
            element.addEventListener("input", function () { onChange(opts.type === "number" ? (element.value === "" ? null : Number(element.value)) : element.value); save(); updatePreviews(); });
            var field = node("div", { "class": "lb-field" }, [node("label", { "for": id, text: labelText }), element]);
            if (state.errors[opts.key]) { field.appendChild(node("p", { id: id + "-error", "class": "lb-field-error", text: state.errors[opts.key] })); }
            return field;
        }
        function number(labelText, value, setter, min, max) { return input(labelText, value, setter, { type: "number", numeric: true, min: min, max: max }); }
        function updatePreviews() {
            bindings.forEach(function (update) { update(); });
            var preview = M.preview(a, rules);
            var legacyReaction = app.querySelector("[data-legacy-reaction]");
            if (legacyReaction && a.amountMode === "known") {
                legacyReaction.textContent = fill(copy("legacy", "reactionShortRte").replace(/<[^>]*>/g, ""), { gap: money(Math.max(a.legacyAmount - a.putAside, 0)) });
            }
            Array.prototype.forEach.call(app.querySelectorAll("[data-preview]"), function (n) { n.textContent = (n.getAttribute("data-prefix") || "") + money(preview[n.getAttribute("data-preview")]); });
            var shareTotal = app.querySelector("[data-share-total]");
            if (shareTotal) { shareTotal.textContent = "Total: " + M.sum(a.people.map(function (p) { return p.share || 0; })) + "%"; }
        }
        function select(labelText, list, value, setter, key) {
            var id = uid + "-" + (key || Math.random().toString(36).slice(2));
            var field = node("select", { id: id, "aria-invalid": state.errors[key] ? "true" : null, "aria-describedby": state.errors[key] ? id + "-error" : null },
                list.map(function (o) { return node("option", { value: o.value, text: o.label, selected: o.value === value }); }));
            field.addEventListener("change", function () { setter(field.value); save(); render(); });
            return node("div", { "class": "lb-field" }, [node("label", { "for": id, text: labelText }), field,
                state.errors[key] ? node("p", { id: id + "-error", "class": "lb-field-error", text: state.errors[key] }) : null]);
        }
        function evenShares() { var n = a.people.length; state.shareTouched = []; a.people.forEach(function (p, i) { p.share = Math.floor(100 / n) + (i >= n - 100 % n ? 1 : 0); }); }
        function newDistribution() {
            return { parts: [], split: { lump: 0, structured: 0, milestones: 0 }, lumpAmount: 0,
                structured: { frequency: "monthly", years: 10, stepUp: false, rate: 1, interval: 1, start: "now", startAge: 21, amount: rules.monthlyPaymentMinimum },
                milestones: { selected: [], allocation: {}, amounts: {}, ages: { age: rules.defaultMilestoneAge }, within: rules.defaultWithinYears } };
        }
        function activePeople() { return a.people.filter(function (p) { return a.amountMode === "unsure" || p.share > 0; }); }
        function problem(screen) {
            var counts = {};
            if (screen === "intro" && !(a.importance && a.preparedness)) { return "Move both sliders to choose your answers."; }
            if (screen === "s1" && (a.needs.length !== rules.needs.length || rules.needs.some(function (id) { return a.needs.indexOf(id) < 0; }))) { return "Rank all five priorities in the order that matters to you."; }
            if (screen === "s2" && !a.peaceOfMind) { return "Choose what would give you peace of mind."; }
            if (screen === "s3" && !(a.ageRange && a.relationshipStatus)) { return "Choose your age range and relationship status."; }
            if (screen === "s4") {
                if (!a.people.length) { return "Add at least one person to continue."; }
                if (editing !== null || draftName.trim()) { return "Save the person you are adding or editing before continuing."; }
                for (var i = 0; i < a.people.length; i++) {
                    var p = a.people[i], key = relationshipKey(p.relationship); counts[key] = (counts[key] || 0) + 1;
                    if (!M.validName(p.name) || !relationships.some(function (r) { return r.value === p.relationship; })) { return "Check each person's name and relationship."; }
                    if (({ spouse: 1, parent: 2 }[key] || 0) && counts[key] > { spouse: 1, parent: 2 }[key]) { return "You can add at most one spouse and two parents."; }
                }
            }
            if (screen === "s5" && a.amountMode === "known" && !M.integer(a.legacyAmount, rules.legacyMinimum, 1000000000)) { return "Choose a whole-ringgit amount between " + money(rules.legacyMinimum) + " and RM1 billion."; }
            if (screen === "shares" && (a.people.some(function (p) { return !M.integer(p.share, 0, 100); }) || M.sum(a.people.map(function (p) { return p.share; })) !== 100)) { return "Shares must be whole percentages and add up to 100%."; }
            if (screen === "distribution") { return M.distributionError(a, activePeople()[state.person], rules); }
            if (screen === "s7" && !M.integer(a.putAside, 0, 1000000000)) { return "Enter an amount between RM0 and RM1 billion."; }
            return "";
        }
        function notify(text) { state.notice = text; render(); var status = app.querySelector(".lb-status"); if (status) { status.focus(); } }
        function next() {
            var reason = problem(state.screen); if (reason) { notify(reason); return; }
            track(state.screen === "intro" ? "funnel_navigation" : "funnel_navigation", state.screen === "intro" ? "begin" : "continue");
            if (state.returnToReview) { state.returnToReview = false; go("s8"); return; }
            var s = state.screen;
            if (s === "intro") { state.saved = null; go("s1"); }
            else if (/^s[1-4]$/.test(s)) { go("s" + (Number(s.slice(1)) + 1)); }
            else if (s === "s5") { go(a.amountMode === "known" && a.people.length > 1 ? "shares" : "distribution"); }
            else if (s === "shares") { go("distribution"); }
            else if (s === "distribution") {
                if (state.person + 1 < activePeople().length) { go("distribution", state.person + 1); }
                else { go(a.amountMode === "unsure" ? "calc" : "s7"); }
            } else if (s === "calc") { go("s7"); }
            else if (s === "s7") { go("s8"); }
            else if (s === "s8") { track("funnel_cta", "create my blueprint"); go("contact"); }
        }
        function back() {
            track("funnel_navigation", "back");
            var s = state.screen;
            if (s === "contact") { go("s8"); }
            else if (s === "s8") { go("s7"); }
            else if (s === "s7") { go(a.amountMode === "unsure" ? "calc" : "distribution", activePeople().length - 1); }
            else if (s === "calc") { go("distribution", activePeople().length - 1); }
            else if (s === "distribution") {
                if (state.person) { go("distribution", state.person - 1); }
                else { go(a.amountMode === "known" && a.people.length > 1 ? "shares" : "s5"); }
            } else if (s === "shares") { go("s5"); }
            else if (/^s[2-5]$/.test(s)) { go("s" + (Number(s.slice(1)) - 1)); }
            else { go("intro"); }
        }
        function intro(view) {
            view.appendChild(node("div", { "class": "lb-wordmark" }, [node("span", { text: "Your " }), node("strong", { text: "Legacy Planner" })]));
            view.appendChild(title(copy("intro", "headline", "If life changed tomorrow, would the people you love be okay?")));
            view.appendChild(rte("intro", "subHeadlineRte"));
            if (state.saved) {
                var actions = node("div", { "class": "lb-welcome-actions" });
                actions.appendChild(button(copy("intro", "resumeLabel", "Pick up where I left off"), function () {
                    track("funnel_navigation", "resume");
                    var saved = state.saved;
                    try {
                        a = Object.assign(fresh(), saved.answers); a.contact = fresh().contact;
                        a.people.forEach(function (p) { if (!p.distribution || !Array.isArray(p.distribution.parts)) { throw new Error("Old draft"); } });
                        state.saved = null;
                        var screen = ["intro", "s1", "s2", "s3", "s4", "s5", "shares", "distribution", "calc", "s7", "s8", "contact"].indexOf(saved.screen) >= 0 ? saved.screen : "s1";
                        if (screen === "distribution" && !activePeople().length) { screen = "s4"; }
                        go(screen, Math.min(saved.person || 0, Math.max(activePeople().length - 1, 0)));
                    } catch (e) { restart(); }
                }));
                actions.appendChild(button(copy("intro", "startOverLabel", "Start over"), function () { track("funnel_navigation", "start over"); restart(); }, true));
                view.appendChild(actions);
                return;
            }
            var sliders = card([]);
            ["importance", "preparedness"].forEach(function (key, i) {
                var prefix = "q" + (i + 1), id = uid + "-" + key;
                var display = node("output", { "for": id, text: a[key] ? a[key] + " / 5" : "— / 5" });
                var range = node("input", { id: id, type: "range", min: 1, max: 5, step: 1, value: a[key] || 5, "aria-valuetext": a[key] ? String(a[key]) : "Choose your answer" });
                var reactionText = node("p", { text: a[key] ? copy("intro", prefix + "Reaction" + (a[key] <= 2 ? "Low" : a[key] === 3 ? "Mid" : "High")) : "" });
                var reaction = node("div", { "class": "lb-intro-reaction", hidden: !a[key] }, [icon("introThought"), node("div", {}, [node("p", {"class":"lb-eyebrow",text:copy("labels","reactionHeading","A quick thought")}), reactionText])]);
                range.addEventListener("input", function () { a[key] = Number(range.value); display.textContent = ""; display.appendChild(node("strong", {text:range.value})); display.appendChild(node("span", {text:" / 5"})); range.setAttribute("aria-valuetext", range.value);
                    reaction.hidden=false; reactionText.textContent = copy("intro", prefix + "Reaction" + (a[key] <= 2 ? "Low" : a[key] === 3 ? "Mid" : "High")); save(); updatePreviews(); });
                display.textContent = ""; display.appendChild(node("strong", {text:a[key] || "\u2013"})); display.appendChild(node("span", {text:" / 5"}));
                sliders.appendChild(node("div", { "class": "lb-slider" }, [node("label", { "for": id, "class": "lb-icon-label" }, [icon(i === 0 ? "introHeart" : "introShield"), node("span", { text: copy("intro", prefix + "Question") })]), display, range,
                    node("div", { "class": "lb-range-labels" }, [node("span", { text: copy("intro", prefix + "LabelMin") }), node("span", { text: copy("intro", prefix + "LabelMax") })]), reaction]));
            });
            view.appendChild(sliders);
        }
        function priorities(view) {
            var question = rte("priorities", "s1QuestionRte", "<p>What matters most for your loved ones?</p>");
            question.classList.add("lb-question"); view.appendChild(question);
            view.appendChild(node("div", { "class": "lb-priority-helper" }, [rte("priorities", "s1HelperRte"),
                node("strong", { text: fill(copy("priorities", "selectionCounter", "{count} of 5 ranked"), { count: a.needs.length }) })]));
            view.appendChild(choices(needs, a.needs, function (id) {
                var index = a.needs.indexOf(id); if (index < 0) { a.needs.push(id); } else { a.needs.splice(index, 1); } save(); render();
            }, needs.map(function (o, i) { return copy("priorities", "option" + (i + 1) + "Description"); }), "rank"));
        }
        function people(view) {
            var maxPeople = Math.min(Number(rules.maxPeople) || 5, 5);
            function relationshipFull(value) {
                var key = relationshipKey(value), limit = { spouse: 1, parent: 2 }[key];
                return !!limit && a.people.filter(function (p) {
                    return relationshipKey(p.relationship) === key && p.id !== editing;
                }).length >= limit;
            }
            view.appendChild(title(copy("aboutPeople", "s4Title", "Who are you doing this for?")));
            view.appendChild(rte("aboutPeople", "s4DescriptionRte"));
            function editorForm(person) {
                var editor = node("div", { "class": "lb-person-editor" + (person ? " is-editing" : "") }, [input(copy("aboutPeople", "nameLabel", "Their name"), draftName, function (v) { draftName = v; }, { maxlength: 100, placeholder: copy("aboutPeople", "namePlaceholder") }),
                    node("h3", { text: copy("aboutPeople", "relationshipQuestion", "How are they related to you?") })]);
                var rels = choices(relationships, draftRelationship, function (v) { draftRelationship = v; render(); });
                Array.prototype.forEach.call(rels.children, function (b, i) {
                    var r = relationships[i].value;
                    b.disabled = relationshipFull(r);
                    if (b.disabled && relationshipKey(r) === "spouse") { b.textContent = copy("aboutPeople", "relationshipSpouseAdded", "Spouse (added)"); }
                    if (b.disabled && relationshipKey(r) === "parent") { b.textContent = copy("aboutPeople", "relationshipParentsAdded", "Parent (2 added)"); }
                });
                editor.appendChild(rels);
                var actions = node("div", { "class": "lb-person-editor__actions" });
                if (person) { actions.appendChild(button(copy("aboutPeople", "cancelLabel", "Cancel"), function () { editing = null; draftName = ""; draftRelationship = ""; render(); }, true)); }
                var savePerson = button(person ? copy("aboutPeople", "saveLabel", "Save") : "+ " + copy("aboutPeople", "addLabel", "Add them"), function () {
                    if (!M.validName(draftName) || !draftRelationship) { notify("Enter their name and choose their relationship to you."); return; }
                    if (relationshipFull(draftRelationship)) { notify("This relationship has reached its limit."); return; }
                    if (!person && a.people.length >= maxPeople) { notify("You can add up to five people."); return; }
                    if (person) { person.name = draftName.trim(); person.relationship = draftRelationship; }
                    else { a.people.push({ id: "person-" + Date.now().toString(36) + Math.random().toString(36).slice(2, 6), name: draftName.trim(), relationship: draftRelationship, share: 0, distribution: newDistribution() }); evenShares(); }
                    draftName = ""; draftRelationship = ""; editing = null; state.notice = ""; save(); render();
                });
                function updatePersonButton() {
                    savePerson.disabled = !M.validName(draftName) || !draftRelationship || relationshipFull(draftRelationship) || (!person && a.people.length >= maxPeople);
                }
                bindings.push(updatePersonButton); updatePersonButton(); actions.appendChild(savePerson);
                editor.appendChild(actions); return editor;
            }
            a.people.forEach(function (p) {
                if (editing === p.id) { view.appendChild(editorForm(p)); return; }
                view.appendChild(node("div", { "class": "lb-person-card" }, [node("div", { "class": "lb-person" }, [icon("person"),
                    node("div", {}, [node("strong", { text: p.name }), note(label(relationships, p.relationship))]),
                    button(copy("aboutPeople", "editLabel", "Edit"), function () { editing = p.id; draftName = p.name; draftRelationship = p.relationship; render(); }, true),
                    button(copy("aboutPeople", "removeLabel", "Remove"), function () { a.people = a.people.filter(function (person) { return person.id !== p.id; }); evenShares(); save(); render(); }, true)])]));
            });
            if (editing === null && a.people.length < maxPeople) { view.appendChild(editorForm(null)); }
            if (a.people.length >= maxPeople) { view.appendChild(rte("aboutPeople", "limitReachedRte")); }
        }
        function legacy(view) {
            view.classList.add("lb-legacy-card");
            view.appendChild(title(copy("legacy", "s5Title", "How much would you like to leave for them?")));
            view.appendChild(rte("legacy", "s5DescriptionRte"));
            var modes = node("div", { "class": "lb-legacy-modes" });
            ["known", "unsure"].forEach(function (mode) {
                var modeButton = button("", function () { a.amountMode = mode; save(); render(); }, true);
                modeButton.classList.add("lb-legacy-mode"); modeButton.classList.toggle("is-selected", a.amountMode === mode);
                modeButton.setAttribute("aria-pressed", String(a.amountMode === mode));
                modeButton.appendChild(icon(mode === "known" ? "modeKnown" : "modeUnsure"));
                modeButton.appendChild(node("span", { "class": "lb-legacy-mode__title", text: copy("legacy", mode + "Title", mode === "known" ? "I have an amount in mind" : "I'm not sure yet") }));
                modeButton.appendChild(node("span", { "class": "lb-legacy-mode__description", text: copy("legacy", mode + "Description", mode === "known" ? "Enter an amount and choose how to share it." : "We'll shape the amount around how you'd like to support each person.") }));
                modes.appendChild(modeButton);
            }); view.appendChild(modes);
            if (a.amountMode === "known") {
                var id = uid + "-legacy-amount";
                var amount = node("input", { id: id, type: "text", inputmode: "numeric", value: a.legacyAmount === null ? "" : Number(a.legacyAmount).toLocaleString("en-MY"), autocomplete: "off", maxlength: 20 });
                amount.addEventListener("input", function () {
                    var digits = amount.value.replace(/[^0-9]/g, ""); a.legacyAmount = digits ? Number(digits) : null;
                    save(); updatePreviews();
                });
                amount.addEventListener("blur", function () { amount.value = a.legacyAmount === null ? "" : a.legacyAmount.toLocaleString("en-MY"); });
                view.appendChild(node("div", { "class": "lb-field" }, [node("label", { "for": id, text: copy("legacy", "amountLabel", "The amount you'd like to leave behind") }),
                    node("div", { "class": "lb-currency-field" }, [node("span", { text: "RM" }), amount])]));
                view.appendChild(choices(rules.legacyAmountChips.map(function (v, i) {
                    return { value: v, label: copy("legacy", "chip" + (i + 1) + "Label", "RM " + (v / 1000000) + "M") };
                }), a.legacyAmount, function (v) { a.legacyAmount = v; save(); render(); }));
            } else {
                view.appendChild(node("div", { "class": "lb-legacy-explainer" }, [node("span", { "class": "lb-legacy-explainer__mark", "aria-hidden": "true", text: "?" }), rte("legacy", "unsureExplainerRte")]));
            }
        }
        function shares(view) {
            view.classList.add("lb-share-card");
            view.appendChild(title(copy("distribution", "shareTitle", "How would you like to share it?")));
            view.appendChild(note(fill(copy("distribution", "shareDescription", "Choose each person's share of {amount}."), { amount: money(a.legacyAmount) })));
            function changeShare(person, value) {
                var current = {}; a.people.forEach(function (p) { current[p.id] = p.share; });
                var allocation = M.allocate(current, a.people.map(function (p) { return p.id; }), state.shareTouched, person.id, value);
                state.shareTouched = allocation.touched; a.people.forEach(function (p) { p.share = allocation.values[p.id]; });
                Array.prototype.forEach.call(view.querySelectorAll("[data-share-input]"), function (field) { field.value = allocation.values[field.getAttribute("data-share-input")]; });
                Array.prototype.forEach.call(view.querySelectorAll("[data-share-range]"), function (field) { field.value = allocation.values[field.getAttribute("data-share-range")]; });
                save(); updatePreviews();
            }
            a.people.forEach(function (p) {
                var field = node("input", { type: "number", min: 0, max: 100, step: 1, value: p.share, "class": "lb-percent-input", "aria-label": p.name + " share percentage", "data-share-input": p.id });
                field.addEventListener("input", function () { changeShare(p, Number(field.value)); });
                var range = node("input", { type: "range", min: 0, max: 100, step: 1, value: p.share, "aria-label": p.name + " share", "data-share-range": p.id });
                range.addEventListener("input", function () { changeShare(p, Number(range.value)); });
                view.appendChild(node("div", { "class": "lb-share-row" }, [node("div", { "class": "lb-review-row" }, [node("strong", { text: p.name }), boundText(function () { return money(a.legacyAmount * p.share / 100); }, "lb-muted")]),
                    node("div", { "class": "lb-share-controls" }, [range, field, node("span", { text: "%" })])]));
            });
            var totalLabel=node("strong", { "data-share-total": true });
            var totalBar=node("span");
            function updateTotal() {
                var total=M.sum(a.people.map(function (p) { return p.share || 0; })),color=total===100?"#22252A":"#A71E13";
                totalLabel.textContent="Total: "+total+"%";totalLabel.setAttribute("style","color:"+color);
                totalBar.setAttribute("style","width:"+Math.max(0,Math.min(total,100))+"%;background:"+color);
            }
            bindings.push(updateTotal);updateTotal();
            view.appendChild(node("div", { "class": "lb-review-row lb-share-footer" }, [button(copy("labels", "splitEvenlyLabel", "Split evenly"), function () { evenShares(); save(); render(); }, true),totalLabel]));
            view.appendChild(node("div", { "class": "lb-allocation-track lb-share-total-track" }, [totalBar]));
        }
        function distribution(view) {
            var list = activePeople(), p = list[state.person];
            if (!p) { view.appendChild(note("Add a person and allocate a share first.")); return; }
            var d = p.distribution, unsure = a.amountMode === "unsure", multi = d.parts.length > 1;
            view.classList.add("lb-distribution-card");
            view.appendChild(node("p", { "class": "lb-eyebrow", text: fill(copy("distribution", "personEyebrow", "PERSON {index} OF {total}"), { index: state.person + 1, total: list.length }) }));
            view.appendChild(title(fill(copy("distribution", "s6Title", "How should {name} receive their gift?"), { name: p.name })));
            view.appendChild(node("div", { "class": "lb-share-intro" }, [node("span", { text: unsure ? "So far this adds up to" : "Their share is" }), node("strong", {}, [boundText(function () { return money(unsure ? needsTotal(p) : a.legacyAmount * p.share / 100); })]), unsure ? null : node("span", { text: "(" + p.share + "% of your legacy)" })]));
            view.appendChild(note(copy("distribution", "methodHelper", "Choose one, two or all three options.")));
            view.appendChild(choices(parts.map(function (part) { return { value: part, label: copy("distribution", part + "Title", part) }; }), d.parts, function (part) {
                var selected = d.parts.indexOf(part) < 0 ? d.parts.concat([part]) : d.parts.filter(function (key) { return key !== part; });
                d.parts = parts.filter(function (key) { return selected.indexOf(key) >= 0; });
                parts.forEach(function (key) { d.split[key] = 0; });
                d.parts.forEach(function (key, i) { d.split[key] = Math.floor(100 / d.parts.length) + (i >= d.parts.length - 100 % d.parts.length ? 1 : 0); });
                state.allocationTouched[p.id + "-split"] = []; save(); render();
            }, parts.map(function (part) { return copy("distribution", part + "Description"); }), "multi"));
            if (!d.parts.length) { view.appendChild(note(copy("distribution", "nothingTicked", "Tick at least one above and we'll show you the details."))); return; }
            function evenly(keys, values) { keys.forEach(function (key, i) { values[key] = Math.floor(100 / keys.length) + (i >= keys.length - 100 % keys.length ? 1 : 0); }); }
            if (!unsure && multi) {
                var split = node("div", { "class": "lb-split-panel" }, [node("div", { "class": "lb-review-row" }, [node("h3", { "class": "lb-eyebrow", text: copy("distribution", "mixStep1Heading", "STEP 1 \u00b7 SPLIT THEIR SHARE") }), boundText(function () { return M.sum(d.parts.map(function (key) { return d.split[key]; })) + "% of 100%"; })])]);
                var splitHeader = split.firstChild, splitBar = node("div", { "class": "lb-split-bar" });
                splitHeader.classList.add("lb-split-header");
                d.parts.forEach(function (part) {
                    var segment = node("span", { "class": "lb-part-color--" + part });
                    segment.setAttribute("style", "width:" + (d.split[part] || 0) + "%");
                    bindings.push(function () { segment.setAttribute("style", "width:" + (d.split[part] || 0) + "%"); });
                    splitBar.appendChild(segment);
                });
                splitHeader.appendChild(splitBar);
                var splitGrid = node("div", { "class": "lb-split-grid" });
                d.parts.forEach(function (part) {
                    var field = unitField(copy("distribution", part + "Title", part), d.split[part], function (value) {
                        var key = p.id + "-split", allocation = M.allocate(d.split, d.parts, state.allocationTouched[key] || [], part, value);
                        state.allocationTouched[key] = allocation.touched; parts.forEach(function (id) { d.split[id] = allocation.values[id] || 0; });
                        Array.prototype.forEach.call(split.querySelectorAll("[data-split-part]"), function (element) { element.value = d.split[element.getAttribute("data-split-part")]; });
                    }, "%", 1, 100, { "data-split-part": part });
                    field.classList.add("lb-split-field--" + part);
                    splitGrid.appendChild(node("div", {}, [field, boundText(function () { return money(partAmount(p, part)); }, "lb-muted")]));
                });
                split.appendChild(splitGrid); split.appendChild(button(copy("labels", "splitEvenlyLabel", "Split evenly"), function () { evenly(d.parts, d.split); state.allocationTouched[p.id + "-split"] = []; save(); render(); }, true)); view.appendChild(split);
            }
            function payoutPanel(part, readSummary) {
                var index = d.parts.indexOf(part) + (unsure ? 1 : 2);
                var outer = node("div", { "class": "lb-payout-panel" + (multi ? " lb-payout-panel--mix" : "") });
                if (multi) {
                    view.appendChild(node("p", { "class": "lb-payout-step", text: "STEP " + index + " \u00b7 " + copy("distribution", "mix" + part.charAt(0).toUpperCase() + part.slice(1) + "Heading", copy("distribution", part + "Title", part)).toUpperCase() }));
                    var status = boundText(function () {
                        var person = JSON.parse(JSON.stringify(p));
                        person.distribution.parts = [part];
                        if (!unsure) { person.share = p.share * d.split[part] / 100; }
                        var valid = !M.distributionError(a, person, rules);
                        if (status) { status.classList.toggle("lb-payout-status--incomplete", !valid); }
                        return valid ? "\u2713 All set" : "Needs your input";
                    }, "lb-payout-status");
                    status.classList.toggle("lb-payout-status--incomplete", status.textContent === "Needs your input");
                    outer.appendChild(node("div", { "class": "lb-payout-person" }, [node("span", { "class": "lb-step-number lb-part-color--" + part, text: index }), node("div", {}, [node("h3", { text: copy("distribution", part + "Title", part) }), boundText(function () { return (unsure ? "" : d.split[part] + "% \u00b7 ") + money(partAmount(p, part)); }, "lb-muted")]), status]));
                }
                if (!multi) { outer.appendChild(node("div", { "class": "lb-payout-summary" }, [node("p", { "class": "lb-eyebrow", text: copy("distribution", "summaryHeading", "HOW IT WORKS OUT") }), boundText(readSummary, "lb-payout-summary__value")])); }
                var body = node("div", { "class": "lb-payout-body" });
                if (multi && part === "structured") { body.appendChild(node("div", { "class": "lb-combo-summary" }, [boundText(readSummary), boundText(function () { return (d.structured.years || 0) + " years"; }, "lb-duration")])); }
                outer.appendChild(body); view.appendChild(outer); return body;
            }
            function control(body, children) { var row = node("div", { "class": "lb-payout-control" }, children); body.appendChild(row); return row; }
            if (d.parts.indexOf("lump") >= 0) {
                var lump = payoutPanel("lump", function () { return fill(copy("distribution", "lumpSummary", "{name} receives {amount} in one go."), { name: p.name, amount: money(partAmount(p, "lump")) }); });
                var lumpContent = multi ? lump : lump.parentNode.querySelector(".lb-payout-summary");
                if (!multi) { lumpContent.appendChild(note(copy("distribution", "lumpNote", "Nothing else to set up, they can use it however they need."))); }
                if (unsure) { control(lumpContent, [currencyField(copy("distribution", multi ? "lumpAmountComboLabel" : "lumpAmountLabel", "How much would you like to give them?"), d.lumpAmount, function (v) { d.lumpAmount = v; }), shortPresets(rules.lumpAmountChips, d.lumpAmount, function (v) { d.lumpAmount = v; })]); }
                else if (multi) { control(lump, [note("Paid in one go.")]); }
            }
            if (d.parts.indexOf("structured") >= 0) {
                var s = d.structured;
                function payment() { return unsure ? s.amount || 0 : M.firstPayment(s, partAmount(p, "structured")); }
                var regular = payoutPanel("structured", function () { return money(payment()) + (s.frequency === "monthly" ? " every month" : " every year") + (s.stepUp ? ", rising " + s.rate + "% every " + s.interval + " years" : "") + ", starting " + (s.start === "age" ? "at age " + s.startAge : "right away") + (multi ? (unsure ? " \u00b7 for " : " \u00b7 lasts about ") : ""); });
                if (!multi) {
                    var durationLine = node("div", {"class":"lb-structured-duration"}, [boundText(function () { return unsure ? "For " + (s.years || 0) + " years \u00b7 adds up to " : "Their share lasts about "; }), node("strong", {}, [boundText(function () { return unsure ? money(M.totalPayments(s, s.amount || 0)) : (s.years || 0) + " years"; })])]);
                    regular.parentNode.querySelector(".lb-payout-summary").appendChild(durationLine);
                }
                control(regular, [unitField(copy("distribution", "yearsLabel", "For how many years?"), s.years, function (v) { s.years = v; }, copy("distribution", "yearsUnit", "years"), 1, 100), choices((rules.yearChips || [5,10,15,20,25]).map(function (v) { return { value: v, label: v + " years" }; }), s.years, function (v) { s.years = v; save(); render(); })]);
                var frequency = choices([{ value: "monthly", label: copy("distribution", "frequencyMonthly", "Every month") }, { value: "yearly", label: copy("distribution", "frequencyYearly", "Every year") }], s.frequency, function (v) {
                    if (unsure && s.frequency !== v) { s.amount = v === "yearly" ? s.amount * 12 : Math.round(s.amount / 12); } s.frequency = v; save(); render();
                }); frequency.classList.add("lb-toggle-pair");
                control(regular, [node("h3", { text: copy("distribution", "frequencyLabel", "How often?") }), frequency]);
                if (unsure) {
                    control(regular, [currencyField(copy("distribution", "paymentLabel", "How much each time?"), s.amount, function (v) { s.amount = v; }), note("Minimum " + money(s.frequency === "monthly" ? rules.monthlyPaymentMinimum : rules.yearlyPaymentMinimum) + (s.frequency === "monthly" ? " per month" : " per year")), shortPresets(s.frequency === "monthly" ? [2000,3000,5000,10000] : [24000,36000,60000,120000], s.amount, function (v) { s.amount = v; })]);
                } else {
                    control(regular, [node("h3", { text: copy("distribution", "paymentCalcLabel", "Each payment") }), node("div", { "class": "lb-payment-result" }, [boundText(function () { return money(payment()) + (s.frequency === "monthly" ? " / month" : " / year"); }), boundText(function () { return "Worked out from their share over " + (s.years || 0) + " years"; }, "lb-muted")]), boundText(function () { var minimum = s.frequency === "monthly" ? rules.monthlyPaymentMinimum : rules.yearlyPaymentMinimum; return "Minimum " + money(minimum) + (s.frequency === "monthly" ? " per month" : " per year") + (payment() >= minimum ? " \u2713" : ""); }, "lb-muted")]);
                }
                var growth = choices([{ value: false, label: copy("distribution", "growthNo", "No, keep it steady") }, { value: true, label: copy("distribution", "growthYes", "Yes, increase it") }], s.stepUp, function (v) { s.stepUp = v; save(); render(); }); growth.classList.add("lb-toggle-pair");
                var growthRow = control(regular, [node("h3", { text: copy("distribution", "growthLabel", "Should it grow over time?") }), growth]);
                if (s.stepUp) { growthRow.appendChild(node("div", { "class": "lb-two-columns" }, [unitField(copy("distribution", "stepUpRateLabel", "Increase by"), s.rate, function (v) { s.rate = v; }, "%", 1, 100), unitField(copy("distribution", "stepUpIntervalLabel", "Every"), s.interval, function (v) { s.interval = v; }, "years", 1, 100)])); }
                var start = choices([{ value: "now", label: copy("distribution", "startNow", "Right away") }, { value: "age", label: copy("distribution", "startAtAge", "At a certain age") }], s.start, function (v) { s.start = v; save(); render(); }); start.classList.add("lb-toggle-pair");
                var startRow = control(regular, [node("h3", { text: copy("distribution", "startLabel", "When should payments begin?") }), start]);
                if (s.start === "age") {
                    startRow.appendChild(unitField(copy("distribution", "startAgeLabel", "Starting age"), s.startAge, function (v) { s.startAge = v; }, "years old", 1, 120));
                    startRow.appendChild(choices((rules.startAgeChips || [18,21,25,55,60]).map(function (v) { return { value:v,label:String(v) }; }),s.startAge,function (v) { s.startAge=v;save();render(); }));
                }
            }
            if (d.parts.indexOf("milestones") >= 0) {
                var m = d.milestones;
                var milestone = payoutPanel("milestones", function () { return m.selected.length ? "Paid across " + m.selected.length + " life moments" : copy("distribution", "milestonesNote", "Pick the moments you'd like to be part of below."); });
                var selection = control(milestone, [node("div", { "class": "lb-moment-heading" }, [node("h3", { text: copy("distribution", "momentsQuestion", "Which moments would you like to be part of?") }), note("Pick up to " + rules.maxMilestones + " \u00b7 " + m.selected.length + "/" + rules.maxMilestones)])]);
                var momentChoices = choices(moments, m.selected, function (id) {
                    var index = m.selected.indexOf(id);
                    if (index >= 0) { m.selected.splice(index,1); } else if (m.selected.length < rules.maxMilestones) { m.selected.push(id); m.amounts[id] = m.amounts[id] || 0; }
                    var key = p.id + "-milestones", touched = (state.allocationTouched[key] || []).filter(function (v) { return m.selected.indexOf(v) >= 0; });
                    if (touched.length >= m.selected.length) { touched = []; }
                    var free = m.selected.filter(function (v) { return touched.indexOf(v) < 0; }), remaining = 100 - M.sum(touched.map(function (v) { return m.allocation[v] || 0; }));
                    free.forEach(function (v,i) { m.allocation[v] = Math.floor(remaining / free.length) + (i >= free.length - remaining % free.length ? 1 : 0); });
                    state.allocationTouched[key] = touched; save(); render();
                });
                Array.prototype.forEach.call(momentChoices.children,function (b,i) { b.disabled = m.selected.indexOf(moments[i].value) < 0 && m.selected.length >= rules.maxMilestones; }); selection.appendChild(momentChoices);
                if (m.selected.length) {
                    var allocations = control(milestone, [node("h3", { text: copy("distribution", multi ? "momentAmountComboLabel" : "momentAmountLabel", "How much for each milestone?") })]);
                    m.selected.forEach(function (id) {
                        var row = node("div", { "class": "lb-milestone-row" }, [node("strong", { text: label(moments,id) })]);
                        if (unsure) {
                            row.appendChild(currencyField("",m.amounts[id],function (v) { m.amounts[id] = v; }));
                            var guide=copy("distribution","moment"+(rules.milestones.indexOf(id)+1)+"Guide");if(guide){row.appendChild(note(guide));}
                            if ((rules.milestoneGuidePicks || {})[id]) { row.appendChild(shortPresets(rules.milestoneGuidePicks[id],m.amounts[id],function (v) { m.amounts[id] = v; })); }
                        } else {
                            row.appendChild(boundText(function () { return money(partAmount(p,"milestones") * (m.allocation[id] || 0) / 100); },"lb-muted"));
                            row.appendChild(unitField("Allocation",m.allocation[id],function (v) {
                                var key = p.id + "-milestones", allocation = M.allocate(m.allocation,m.selected,state.allocationTouched[key] || [],id,v);
                                m.allocation = allocation.values; state.allocationTouched[key] = allocation.touched;
                                Array.prototype.forEach.call(milestone.querySelectorAll("[data-milestone-allocation]"),function (field) { field.value = m.allocation[field.getAttribute("data-milestone-allocation")]; });
                            },"%",0,100,{ "data-milestone-allocation": id }));
                        }
                        if (id === "age") { row.appendChild(unitField(copy("distribution","momentAgeLabel","At age"),m.ages.age,function (v) { m.ages.age = v; },"years old",1,120)); }
                        allocations.appendChild(row);
                    });
                    if (!unsure) { allocations.appendChild(node("div", { "class": "lb-review-row" }, [button(copy("labels","splitEvenlyLabel","Split evenly"),function () { evenly(m.selected,m.allocation);state.allocationTouched[p.id+"-milestones"]=[];save();render(); },true),boundText(function () { return "Left to assign " + (100 - M.sum(m.selected.map(function (id) { return m.allocation[id] || 0; }))) + "%"; },"lb-muted")])); }
                }
                control(milestone,[unitField(copy("distribution","withinLabel","Within how many years should all these moments be covered?"),m.within,function (v) { m.within = v; },"years",1,50),note(copy("distribution","withinNote")),choices((rules.withinYearChips || [5,10,15,20]).map(function (v) { return { value:v,label:v+" years" }; }),m.within,function (v) { m.within=v;save();render(); })]);
            }
        }
        function summaryNumbers(view, plan) {
            view.appendChild(card([node("div", { "class": "lb-stat" }, [node("span", { text: copy("review", "summaryLegacyLabel", "Amount you'd love to leave behind") }), node("strong", { "data-preview": state.screen === "blueprint" ? null : "legacyAmount", text: money(plan.legacyAmount) })]),
                node("div", { "class": "lb-stat" }, [node("span", { text: copy("review", "summaryPutAsideLabel", "Already put aside") }), node("strong", { "data-preview": state.screen === "blueprint" ? null : "putAside", "data-prefix": "− ", text: "− " + money(plan.putAside) })]),
                node("div", { "class": "lb-stat lb-stat--accent" }, [node("span", { text: copy("review", "summaryGapLabel", "Still to plan for") }), node("strong", { "data-preview": state.screen === "blueprint" ? null : "gap", text: money(plan.gap) })])]));
        }
        function calculatorSummary(view) {
            view.classList.add("lb-calculator-card");
            var plan = M.preview(a, rules);
            view.appendChild(node("p", { "class": "lb-eyebrow", text: copy("legacy", "calcEyebrow", "WE'VE DONE THE MATHS") }));
            view.appendChild(title(copy("legacy", "calcTitle", "Here's what your legacy could add up to")));
            view.appendChild(note(copy("legacy", "calcDescription", "Based on how you'd like to support each person. You can go back and make changes anytime.")));
            var rows = node("div", { "class": "lb-calculator-people" });
            a.people.forEach(function (person) {
                rows.appendChild(node("div", { "class": "lb-calculator-person" }, [node("div", {}, [node("strong", { text: person.name + " (" + label(relationships, person.relationship) + ")" }), note(person.distribution.parts.map(function (part) { return copy("distribution", part + "Title", part); }).join(" + "))]), node("strong", { text: money(needsTotal(person)) })]));
            }); view.appendChild(rows);
            rows.appendChild(node("div", { "class": "lb-calculator-total" }, [node("strong", { text: copy("legacy", "calcTotalLabel", "Amount you'd love to leave behind") }), node("strong", { text: money(plan.legacyAmount) })]));
            if (plan.needsTotal < rules.legacyMinimum) { view.appendChild(node("div", { "class": "lb-minimum-note" }, [node("span", { "aria-hidden": "true", text: "!" }), note(copy("legacy", "calcBelowMinimum"))])); }
        }
        function putAside(view) {
            view.classList.add("lb-putaside-card"); view.appendChild(icon("putAside"));
            view.appendChild(title(copy("review", "s7Title", "How much have you already set aside for them?"))); view.appendChild(rte("review", "s7DescriptionRte"));
            view.appendChild(currencyField(copy("review", "putAsideLabel", "Already put aside"), a.putAside, function (v) { a.putAside = v; }));
            view.appendChild(choices(rules.putAsideChips.map(function (value,i) { return { value: value, label: copy("review", "putAsideChip" + (i+1) + "Label", value ? shortMoney(value) : "None yet") }; }), a.putAside, function (v) { a.putAside=v;save();render(); }));
            summaryNumbers(view,M.preview(a,rules));
        }
        function review(view) {
            view.classList.add("lb-review-card");
            var plan = M.preview(a, rules);
            view.appendChild(title(copy("review", "s8Title", "Your plan is taking shape"))); view.appendChild(rte("review", "s8DescriptionRte"));
            function row(heading, text, screen) {
                var reviewIcon = { s1: "reviewPriorities", s2: "reviewPeace", s3: "reviewAbout", s4: "reviewPeople", s5: "reviewLegacy", s7: "reviewPutAside" }[screen];
                view.appendChild(card([node("div", { "class": "lb-review-row" }, [node("h3", { "class": "lb-icon-label" }, [reviewIcon ? icon(reviewIcon) : null, node("span", { text: heading })]), button(copy("review", "sectionEditLabel", "Edit"), function () { edit(screen); }, true)]), note(text)]));
            }
            var starting = card([node("h3", { "class": "lb-icon-label" }, [icon("reviewHeart"), node("span", { text: copy("review", "section1Label", "Where you're starting from") })])]);
            ["importance", "preparedness"].forEach(function (key, index) {
                var id = uid + "-review-" + key;
                var output = node("output", { "for": id, text: a[key] + "/5" });
                var slider = node("input", { id: id, type: "range", min: 1, max: 5, step: 1, value: a[key] });
                slider.addEventListener("input", function () { a[key] = Number(slider.value); output.textContent = slider.value + "/5"; save(); });
                starting.appendChild(node("div", { "class": "lb-review-rating" }, [node("label", { "for": id, text: copy("review", index === 0 ? "rowImportance" : "rowPreparedness", index === 0 ? "How much a legacy matters to you" : "Whether what you have now is enough") }), slider, output]));
            });
            view.appendChild(starting);
            row(copy("review", "section2Label", "Peace of mind"), label(peace, a.peaceOfMind), "s2");
            row(copy("review", "section3Label", "Priorities"), a.needs.map(function (id) { return label(needs,id); }).join(", ") + "\nYour ranking: " + a.needs.map(function (id,i) { return (i+1)+". "+label(needs,id); }).join(" → "), "s1");
            row(copy("review", "section4Label", "About you"), label(ages, a.ageRange) + " · " + label(statuses, a.relationshipStatus), "s3");
            row(copy("review", "section5Label", "People"), a.people.map(function (p) { return p.name + " (" + label(relationships, p.relationship) + ")"; }).join(", "), "s4");
            row(copy("review", "section6Label", "Legacy amount"), money(plan.legacyAmount), "s5");
            row(copy("review", "section8Label", "Already put aside"), money(a.putAside) + " · still to plan for " + money(plan.gap), "s7");
            var distributionGroup = node("div", { "class": "lb-review-distribution" }, [node("div", { "class": "lb-review-row" }, [node("h3", { "class": "lb-icon-label" }, [icon("reviewDistribution"),node("span", {text:copy("labels","step6Name","Distribution")})]),
                a.amountMode === "known" && a.people.length > 1 ? button(copy("review", "editSplitLabel", "Edit split") + " →", function () { edit("shares"); }, true) : null])]);
            plan.people.forEach(function (p) {
                var person = a.people.filter(function (v) { return v.id === p.id; })[0];
                var index = activePeople().indexOf(person);
                distributionGroup.appendChild(node("div", { "class": "lb-review-person" }, [
                    node("div", {}, [node("div", { "class": "lb-review-person-title" }, [node("strong", { text: person.name }), node("span", { text: " \u00b7 " + p.share + "% \u00b7 " + money(p.amount) })]),
                        note(p.share > 0 ? person.distribution.parts.map(function (part) { return copy("distribution", part + "Title", part); }).join(" + ") : "No share allocated")]),
                    button(p.share ? copy("review", "personEditLabel", "Edit") : "Give a share", function () { edit(index >= 0 ? "distribution" : "shares", index); }, true)]));
            });
            view.appendChild(distributionGroup);
            var create = button(copy("review", "createLabel", "Create my blueprint"), next);
            create.classList.add("lb-review-create"); view.appendChild(create);
        }
        function contact(view) {
            view.classList.add("lb-contact-card");
            view.appendChild(icon("contactMail"));
            view.appendChild(node("p", { "class": "lb-eyebrow", text: copy("contact","eyebrow","ALMOST THERE") }));
            view.appendChild(title(copy("contact", "title", "Your plan is ready. Where should we send it?"))); view.appendChild(rte("contact", "descriptionRte"));
            var form = node("form", { novalidate: true, "class": "lb-contact-form" });
            function contactField(key, fallback, type, autocomplete, max) {
                var field = input(copy("contact",key+"Label",fallback),a.contact[key],function (v) {
                    if (key === "nric") { var digits=v.replace(/\D/g,"").slice(0,12);v=digits.length>8?digits.slice(0,6)+"-"+digits.slice(6,8)+"-"+digits.slice(8):digits.length>6?digits.slice(0,6)+"-"+digits.slice(6):digits;if(control)control.value=v; }
                    a.contact[key]=v;
                },{ key:key,type:type,autocomplete:autocomplete,maxlength:max,numeric:key==="nric",placeholder:copy("contact",key+"Placeholder"),required:true });
                var control=field.querySelector("input"), error=field.querySelector(".lb-field-error");
                if(!error){error=node("p",{id:control.id ? control.id+"-error" : control.getAttribute("id")+"-error","class":"lb-field-error",hidden:true});field.appendChild(error);}
                control.addEventListener("blur",function(){var message=M.contactErrors(a.contact,rules)[key];error.textContent=message||"";error.hidden=!message;control.setAttribute("aria-invalid",message?"true":"false");control.setAttribute("aria-describedby",error.getAttribute("id"));});
                return field;
            }
            form.appendChild(contactField("fullName","Full name","text","name",100));
            form.appendChild(contactField("email","Email address","email","email",254));
            form.appendChild(node("div",{"class":"lb-contact-phone"},[select(copy("contact","countryCodeLabel","Country code"),rules.countryCodes.map(function(v,i){return{value:v,label:copy("contact","country"+(i+1)+"Label","+"+v)};}),a.contact.countryCode,function(v){a.contact.countryCode=v;},"countryCode"),contactField("mobile","Mobile number","tel","tel-national",30)]));
            form.appendChild(contactField("nric","NRIC number","text","off",14));
            form.appendChild(select(copy("contact","stateLabel","State"),[{value:"",label:copy("contact","statePlaceholder","Select your state")}].concat(rules.states.map(function(v){return{value:v,label:v};})),a.contact.state,function(v){a.contact.state=v;},"state"));
            var consent=node("input",{id:uid+"-consent",type:"checkbox",checked:a.contact.consent,"aria-invalid":state.errors.consent?"true":null,"aria-describedby":state.errors.consent?uid+"-consent-error":null});
            consent.addEventListener("change",function(){a.contact.consent=consent.checked;updatePreviews();});
            form.appendChild(node("div",{"class":"lb-consent"},[consent,node("label",{"for":uid+"-consent",html:copy("contact","consentRte","I agree to receive my plan by email.")})]));
            if(state.errors.consent){form.appendChild(node("p",{id:uid+"-consent-error","class":"lb-field-error",text:state.errors.consent}));}
            var send=node("button",{type:"submit","class":"lb-button lb-contact-send",text:state.sending?copy("contact","sendingLabel","Sending\u2026"):copy("contact","sendLabel","Send Me My Plan")});
            function canSend(){send.disabled=!!(state.sending||settings.editorPreview||(state.result&&state.result.apiSubmitted)||Object.keys(M.contactErrors(a.contact,rules)).length);}
            bindings.push(canSend);canSend();form.appendChild(send);
            form.addEventListener("submit",function(event){event.preventDefault();submit();});
            if(state.sending){Array.prototype.forEach.call(form.querySelectorAll("input,select,button"),function(field){field.disabled=true;});}
            view.appendChild(form);
            var backButton=button("\u2190 "+copy("contact","backToReviewLabel","Back to review"),back,true);backButton.classList.add("lb-back-to-review");backButton.disabled=state.sending;view.appendChild(backButton);
        }
        function checkAll() {
            var screens = ["intro", "s1", "s2", "s3", "s4", "s5", "s7"];
            if (a.amountMode === "known") { screens.push("shares"); }
            for (var i = 0; i < screens.length; i++) { var error = problem(screens[i]); if (error) { return { screen: screens[i], error: error }; } }
            var people = activePeople();
            for (var p = 0; p < people.length; p++) { var message = M.distributionError(a, people[p], rules); if (message) { return { screen: "distribution", person: p, error: message }; } }
            return null;
        }
        function submit() {
            if (state.sending || settings.editorPreview || (state.result && state.result.apiSubmitted)) { return; }
            var invalid = checkAll();
            if (invalid) { state.returnToReview = false; go(invalid.screen, invalid.person); notify(invalid.error); return; }
            state.errors = M.contactErrors(a.contact, rules);
            if (Object.keys(state.errors).length) { notify("Please check the highlighted contact details."); var bad = app.querySelector('[aria-invalid="true"]'); if (bad) { bad.focus(); } return; }
            state.sending = true; state.notice = ""; render(); track("funnel_cta", "send plan");
            var payload = JSON.parse(JSON.stringify(a));
            // Use AEM's existing referral field; the external agentCode mapping belongs to the backend.
            var agentCode = new URLSearchParams(location.search).get("agentCode");
            if (!payload.contact.referralCode && /^[A-Za-z0-9]{1,8}$/.test(agentCode || "")) {
                payload.contact.referralCode = agentCode;
            }
            fetch("/libs/granite/csrf/token.json", { credentials: "same-origin" }).then(function (r) {
                if (!r.ok) { throw new Error("CSRF unavailable"); } return r.json();
            }).then(function (token) {
                var endpoint = root.getAttribute("data-endpoint");
                if (new URLSearchParams(location.search).get("wcmmode") === "disabled") {
                    endpoint += (endpoint.indexOf("?") >= 0 ? "&" : "?") + "wcmmode=disabled";
                }
                return fetch(endpoint, { method: "POST", credentials: "same-origin", headers: { "Content-Type": "application/json", "Accept": "application/json", "CSRF-Token": token.token }, body: JSON.stringify(payload) });
            }).then(function (response) {
                return response.json().then(function (body) { return { http: response.status, body: body }; });
            }).then(function (response) {
                state.sending = false;
                var body = response.body;
                if (!body || typeof body !== "object" || Array.isArray(body)) { throw new Error("Invalid submission response"); }
                // As in the supplied frontend, email delivery determines the visitor's success screen.
                // API acceptance is independent and never inferred from emailSent.
                if (response.http === 200 && body.emailSent === true && body.plan) {
                    state.result = body; clearSaved(); go("blueprint");
                    // Count a completed lead only when the API confirms acceptance as well.
                    if (body.apiSubmitted === true) { track("funnel_complete", "complete"); }
                    return;
                }
                if (response.http === 200 && body.apiSubmitted === true) {
                    state.result = body; clearSaved();
                    notify("Your request was received, but the email could not be sent. Please contact your wealth planner before submitting again.");
                    return;
                }
                if (response.http === 400) { notify("Some answers could not be accepted. Return to review and check your plan and contact details before trying again."); return; }
                notify(copy("contact", "submissionFailedRte", "We couldn't send your blueprint. Please try again.").replace(/<[^>]*>/g, ""));
            }).catch(function () {
                state.sending = false;
                notify("We couldn't confirm your submission. Check your email before trying again to avoid a duplicate request.");
            });
        }
        function payoutSchedule(schedule, frequency) {
            var container=node("div",{"class":"lb-schedule"}),mode="yearly";
            function draw(){
                container.textContent="";
                var toggles=choices([{value:"monthly",label:copy("blueprint","tableMonthly","Monthly")},{value:"yearly",label:copy("blueprint","tableYearly","Yearly")}],mode,function(value){mode=value;draw();track("funnel_cta","view payout schedule");});toggles.classList.add("lb-schedule-tabs");
                container.appendChild(node("div",{"class":"lb-review-row"},[node("strong",{text:copy("blueprint","tableHeading","Payout breakdown")}),toggles]));
                var monthly=mode==="monthly",max=Math.max.apply(null,[1].concat(schedule.map(function(row){return row.paid;})));
                var legend=monthly?copy("blueprint","tablePaidMonthLabel","Paid each month"):copy("blueprint","tablePaidLabel","Paid that year");
                var chartBox=node("div",{"class":"lb-chart-box"},[node("div",{"class":"lb-chart-legend"},[node("span",{"aria-hidden":"true"}),node("span",{text:legend})])]);
                var chart=node("div",{"class":"lb-payout-chart",role:"img","aria-label":"Payouts by year. Exact values are in the table below."});
                var years=node("div",{"class":"lb-chart-years"});
                schedule.forEach(function(row,index){
                    var amount=monthly?row.paid/12:row.paid;
                    chart.appendChild(node("div",{"class":"lb-chart-column",title:"Year "+row.year+": "+money(amount)},[node("span",{"class":"lb-chart-bar",style:"height:"+Math.max(2,row.paid/max*100)+"%"})]));
                    years.appendChild(node("span",{text:schedule.length<=10||index===0||index===schedule.length-1?String(row.year):""}));
                });
                chartBox.appendChild(chart);chartBox.appendChild(years);chartBox.appendChild(node("p",{"class":"lb-chart-axis",text:"Year"}));container.appendChild(chartBox);
                var table=node("table",{"class":"lb-table"},[node("caption",{text:copy("blueprint","tableHeading","Payout breakdown")}),node("thead",{},[node("tr",{},[copy("blueprint","tableYearLabel","Year"),frequency==="monthly"?"Each month":"Each payment",monthly?"Paid each month":"Paid that year",copy("blueprint","tableLeftLabel","Left after")].map(function(text){return node("th",{scope:"col",text:text});}))])]);
                table.appendChild(node("tbody",{},schedule.map(function(row){return node("tr",{},[String(row.year),money(row.payment),money(monthly?row.paid/12:row.paid),money(row.remaining)].map(function(text){return node("td",{text:text});}));})));
                container.appendChild(node("div",{"class":"lb-table-scroll",tabindex:0,"aria-label":"Payment schedule"},[table]));
            }draw();return container;
        }
        function blueprint(view) {
            var result=state.result,plan=result&&result.plan;
            if(!plan){view.appendChild(note("The final blueprint appears here after a successful submission."));return;}
            var colors=["#DA291C","#22252A","#8A9199","#A71E13","#F2A0A8"];
            function original(person){return a.people.filter(function(p){return p.id===person.id;})[0]||{name:"Loved one",relationship:person.relationship};}
            function methods(person){return(person.parts||[]).map(function(part){return copy("distribution",part+"Title",part);}).join(" + ");}
            function giftHeading(titleText, amount) { return node("h3", {"class":"lb-gift-heading"}, [node("span", {text:titleText}), node("strong", {text:money(amount)})]); }
            function section(key,fallback){var element=card([node("h3",{"class":"lb-eyebrow",text:copy("blueprint",key,fallback)})]);view.appendChild(element);return element;}
            var prepared=copy("blueprint","bannerPreparedFor","Prepared for {name}").split("{name}");
            view.appendChild(node("div",{"class":"lb-report-hero"},[node("p",{"class":"lb-eyebrow",text:copy("blueprint","bannerEyebrow","Your blueprint is ready")}),node("p",{"class":"lb-prepared-for"},[node("span",{text:prepared[0]}),node("strong",{text:a.contact.fullName}),node("span",{text:prepared.slice(1).join("{name}")})]),title(fill(copy("blueprint",a.people.length===1?"bannerHeadlineOne":"bannerHeadlineMany","{amount} of love, planned for {count} people who matter most."),{amount:money(plan.legacyAmount),count:a.people.length}))]));
            var about=section("aboutHeading","ABOUT YOU");
            [["aboutNameLabel","Name",a.contact.fullName],["aboutAgeLabel","Age",label(ages,a.ageRange)],["aboutRelationshipStatusLabel","Relationship status",label(statuses,a.relationshipStatus)],["aboutEmailLabel","Email",a.contact.email],["aboutMobileLabel","Mobile number","+"+a.contact.countryCode+" "+a.contact.mobile],["aboutNricLabel","NRIC number",a.contact.nric],["aboutStateLabel","State",a.contact.state]].forEach(function(row){about.appendChild(detailRow(copy("blueprint",row[0],row[1]),row[2]));});
            var starting=section("section1Label","01 \u00b7 WHERE YOU'RE STARTING FROM");starting.classList.add("lb-report-starting");
            [["importance","rowImportance","How much a legacy matters to you"],["preparedness","rowPreparedness","Whether what you have now is enough"]].forEach(function(row){starting.appendChild(node("div",{},[node("div",{"class":"lb-review-row"},[note(copy("blueprint",row[1],row[2])),node("strong",{text:a[row[0]]+"/5"})]),node("div",{"class":"lb-allocation-track"},[node("span",{style:"width:"+(a[row[0]]*20)+"%;background:#da291c"})])]));});starting.appendChild(detailRow(copy("blueprint","rowPeace","What gives you peace of mind"),label(peace,a.peaceOfMind)));
            starting.appendChild(note(copy("intro","q2Reaction"+(a.preparedness<=2?"Low":a.preparedness===3?"Mid":"High"))));
            var peopleSection=section("section2Label","02 \u00b7 THE PEOPLE YOU'RE LOOKING AFTER");
            [1,2].filter(function(i){return i===1 || plan.people.some(function(person){return !person.share;});}).forEach(function(i){peopleSection.appendChild(node("div",{"class":"lb-badge-legend"},[node("span",{"class":"lb-badge"+(i===2?" lb-badge--empty":""),text:(i===1?"\u2713 ":"! ")+copy("blueprint","badge"+i+"Label",i===1?"Looked after":"Plan for them")}),note(copy("blueprint","badge"+i+"Legend"))]));});
            plan.people.forEach(function(person,i){var p=original(person);peopleSection.appendChild(node("div",{"class":"lb-review-row lb-report-person"},[node("span", {}, [node("strong", {text:p.name}), node("span", {"class":"lb-muted",text:" ("+label(relationships,p.relationship)+")"})]),node("span",{"class":"lb-badge"+(person.share?"":" lb-badge--empty"),text:(person.share?"\u2713 ":"! ")+copy("blueprint",person.share?"badge1Label":"badge2Label",person.share?"Looked after":"Plan for them")})]));});
            var prioritySection=section("topPrioritiesLabel","YOUR TOP PRIORITIES");prioritySection.appendChild(note(copy("blueprint","topPrioritiesDescription","What you told us matters most, in the order you chose.")));
            a.needs.forEach(function(id,index){prioritySection.appendChild(node("div",{"class":"lb-report-priority"+(index===0?" is-first":"")},[node("span",{"class":"lb-report-rank",text:index+1}),node("div",{},[node("strong",{text:label(needs,id)}),note(copy("priorities","option"+(rules.needs.indexOf(id)+1)+"Description"))])]));});
            var gifts=section("section3Label","03 \u00b7 HOW EACH GIFT WILL REACH THEM");gifts.classList.add("lb-report-gifts");
            if(plan.people.length>1){plan.people.forEach(function(person,i){gifts.appendChild(node("div",{"class":"lb-allocation-row"},[node("div",{"class":"lb-review-row"},[node("strong",{text:original(person).name}),node("span",{text:person.share+"% \u00b7 "+money(person.amount)})]),node("div",{"class":"lb-allocation-track"},[node("span",{style:"width:"+Math.max(0,Math.min(100,person.share))+"%;background:"+colors[i%colors.length]})])]));});}
            plan.people.forEach(function(person,index){
                var p=original(person),details=node("details",{"class":"lb-beneficiary lb-report-accordion",open:index===0},[node("summary",{},[node("span",{"class":"lb-color-dot",style:"background:"+colors[index%colors.length]}),node("span",{"class":"lb-accordion-person"},[node("span",{"class":"lb-accordion-name"},[node("strong",{text:p.name}),node("span",{text:" \u00b7 "+person.share+"%"})]),note(methods(person))]),node("strong",{text:money(person.amount)}),node("span",{"class":"lb-accordion-chevron","aria-hidden":"true"},[node("img",{src:"data:image/svg+xml,"+encodeURIComponent('<svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#7A1010" stroke-width="2.6" stroke-linecap="round" stroke-linejoin="round"><path d="M6 9l6 6 6-6"/></svg>'),alt:"",width:14,height:14})])])]);
                details.addEventListener("toggle",function(){if(details.open){track("funnel_cta","view beneficiary");Array.prototype.forEach.call(gifts.querySelectorAll("details"),function(other){if(other!==details)other.open=false;});}});
                var body=node("div",{"class":"lb-report-accordion__body"});details.appendChild(body);
                if(!person.share){body.appendChild(detailRow(copy("blueprint","detailNoShare","Share"),copy("blueprint","detailNoShareValue","0% \u2014 nothing to receive yet")));}
                if(person.lumpAmount!==undefined&&person.lumpAmount!==null){body.appendChild(giftHeading(copy("distribution","lumpTitle","All at once"),person.lumpAmount));body.appendChild(detailRow(copy("blueprint","detailPaid","Paid"),copy("blueprint","detailInOneGo","In one go")));}
                if(person.structured){var s=person.structured;body.appendChild(giftHeading(copy("distribution","structuredTitle","Little by little"),person.structuredAmount));
                    [["detailHowOften","How often",s.frequency==="monthly"?"Every month":"Every year"],["detailEachPayment","Each payment",money(s.payment)],["detailGrows","Grows over time",s.stepUp?"Yes, +"+s.rate+"% every "+s.interval+" years":"No, stays the same"],["detailBegins","Payments begin",s.start==="age"?"At age "+s.startAge:"Right away"],["detailLasts","Lasts about",s.years+" years"]].forEach(function(row){body.appendChild(detailRow(copy("blueprint",row[0],row[1]),row[2]));});
                    if(s.schedule&&s.schedule.length){body.appendChild(payoutSchedule(s.schedule,s.frequency));}}
                if(person.milestones&&person.milestones.length){body.appendChild(giftHeading(copy("distribution","milestonesTitle","At big life moments"),person.milestonesAmount));person.milestones.forEach(function(moment){body.appendChild(detailRow(label(moments,moment.id)+(moment.age?" ("+moment.age+")":""),moment.percent+"% \u00b7 "+money(moment.amount)));});body.appendChild(detailRow(copy("blueprint","detailWithin","All moments covered within"),person.withinYears+" years"));}
                gifts.appendChild(details);
            });
            var legacy=section("section4Label","04 \u00b7 WHAT YOU'D LOVE TO LEAVE BEHIND"),covered=plan.legacyAmount>0?Math.min(plan.putAside/plan.legacyAmount,1)*100:0;
            legacy.classList.add("lb-report-legacy");
            legacy.appendChild(node("p",{"class":"lb-amount",text:money(plan.legacyAmount)}));legacy.appendChild(note("You've already set aside "+money(plan.putAside)+", with "+money(plan.gap)+" left to plan for."));
            legacy.appendChild(node("div",{"class":"lb-coverage-bar",role:"img","aria-label":money(plan.putAside)+" set aside, "+money(plan.gap)+" still to plan for"},[node("span",{style:"width:"+covered+"%;background:#5b6670"}),node("span",{style:"width:"+(100-covered)+"%;background:#da291c"})]));
            legacy.appendChild(node("div",{"class":"lb-review-row lb-muted"},[node("span",{text:"RM 0"}),node("span",{text:money(plan.legacyAmount)})]));
            [["legacyPutAsideLabel","Already put aside",plan.putAside,"#5b6670"],["legacyGapLabel","Still to plan for",plan.gap,"#da291c"]].forEach(function(row) { legacy.appendChild(node("div", {"class":"lb-coverage-legend"}, [node("span", {"class":"lb-coverage-swatch",style:"background:"+row[3]}),node("span", {text:copy("blueprint",row[0],row[1])}),node("strong",{style:row[0]==="legacyGapLabel"?"color:#da291c":null,text:money(row[2])})])); });
            legacy.appendChild(detailRow(copy("blueprint","legacyAmountLabel","Your legacy goal"),money(plan.legacyAmount)));
            legacy.appendChild(node("div",{"class":"lb-recommendation"},[detailRow(copy("blueprint","recommendedPlanLabel","Recommended plan amount"),money(plan.recommendedPlanAmount)),note(fill(copy("blueprint",plan.gapBelowMinimum?"planNoteBelowMinimum":"planNoteSteps"),{gap:money(plan.gap),plan:money(plan.recommendedPlanAmount)}))]));
            if (plan.legacyAmount > 0 && plan.legacyAmount < rules.legacyMinimum) { legacy.appendChild(node("p", {"class":"lb-report-minimum",text:"Below the RM 1,000,000 minimum for an individual legacy plan \u2014 worth reviewing with a wealth planner."})); }
            var help=section("section5Label","05 \u00b7 HOW WE CAN HELP MAKE IT HAPPEN"),helpGrid=node("div",{"class":"lb-help-grid"});
            help.classList.add("lb-report-help");
            [1,2,3,4].forEach(function(i){helpGrid.appendChild(node("div",{"class":"lb-help-item"},[icon("help"+i),node("div",{},[node("h3",{text:copy("blueprint","help"+i+"Title")}),note(copy("blueprint","help"+i+"Description"))])]));});help.appendChild(helpGrid);
            var products=section("plansLabel","06 \u00b7 PLANS DESIGNED FOR YOUR LEGACY");products.classList.add("lb-products-section");products.appendChild(note(copy("blueprint","plansDescription")));
            [1,2].forEach(function(i){var link=i===1?"https://www.prudential.com.my/en/products/wealth-and-legacy/pruprestige-legacy/":"https://www.prudential.com.my/en/products/wealth-and-legacy/pruwealth-enrich-2/";
                var product=node("div",{"class":"lb-product"},[node("img",{src:ASSETS+"plan"+i+".jpg",alt:copy("blueprint","plan"+i+"ImageAlt",i===1?"PRUPrestige Legacy":"PRUWealth Enrich 2"),"class":"lb-product-image"})]),content=node("div",{"class":"lb-product-content"});
                [1,2,3].forEach(function(line){content.appendChild(node("div",{"class":"lb-product-feature"},[node("span",{"aria-hidden":"true",text:"\u221a"}),node("span",{text:copy("blueprint","plan"+i+"Line"+line)})]));});
                var anchor=node("a",{href:link,target:"_blank",rel:"noopener noreferrer","class":"lb-product-link",text:copy("blueprint","plan"+i+"LearnMoreLabel","Learn more")+" \u2192"});anchor.addEventListener("click",function(){track("funnel_cta","learn more");});content.appendChild(anchor);product.appendChild(content);products.appendChild(product);
            });
            var completed=copy("blueprint","completedRte","You've completed your Legacy Planner. A copy will be sent to {email}, and one of our Wealth Planners will be in touch to help you explore the next steps towards achieving your legacy goals.").replace(/<[^>]*>/g,"");
            var sentenceEnd=completed.indexOf("."),heading=sentenceEnd>=0?completed.slice(0,sentenceEnd+1):"You've completed your Legacy Planner.",message=sentenceEnd>=0?completed.slice(sentenceEnd+1).trim():completed;
            var emailParts=message.split("{email}");
            view.appendChild(node("div",{"class":"lb-completion"},[node("p",{"class":"lb-completion-heading",text:heading}),node("p",{"class":"lb-completion-message"},[node("span",{text:emailParts[0]}),node("strong",{text:emailParts.length>1?a.contact.email:""}),node("span",{text:emailParts.slice(1).join("{email}")})])]));
            var restartButton=button(copy("blueprint","startNewLabel","Start a new plan")+" \u2192",function(){track("funnel_navigation","start new plan");restart();},true);restartButton.classList.add("lb-report-restart");view.appendChild(restartButton);
        }
        function panel() {
            var s = state.screen, part = ["s1", "s2", "s3"].indexOf(s) >= 0 ? 1 : s === "s4" ? 2 : ["s5", "shares", "distribution", "calc"].indexOf(s) >= 0 ? 3 : 4;
            var introScreen = s === "intro", tab = introScreen ? "intro" : "panels", prefix = "part" + part;
            var panelCopy = IMAGE_PANELS[introScreen ? 0 : part];
            var authoredImage = safeUrl(copy(tab, introScreen ? "welcomeImage" : prefix + "Image"));
            var imageName = introScreen ? "welcome" : prefix;
            var image = authoredImage || ASSETS + imageName + ".jpg";
            var picture = node("picture", {}, [!authoredImage && (introScreen || part === 1 || part === 3) ? node("source", { media: "(max-width: 1199px)", srcset: ASSETS + imageName + "-mobile.jpg" }) : null,
                node("img", { src: image, alt: copy(tab, introScreen ? "welcomeImageAlt" : prefix + "ImageAlt"), "class": "lb-panel__image" })]);
            var aside = node("aside", { "class": "lb-panel", "aria-label": "Your legacy planner" }, [picture,
                node("div", { "class": "lb-panel__shade" }), node("div", { "class": "lb-panel__copy" }, [node("span", { "class": "lb-tag", text: panelCopy.tag }),
                    node("h2", { text: panelCopy.title }),
                    panelCopy.description && (!introScreen || state.saved) ? note(panelCopy.description) : null])]);
            aside.addEventListener("wheel", function (event) {
                if (!window.matchMedia || !window.matchMedia("(min-width: 1200px)").matches || event.ctrlKey || !event.deltaY) { return; }
                var right = app.querySelector(".lb-view");
                if (right && right.scrollHeight > right.clientHeight) {
                    var distance = event.deltaY * (event.deltaMode === 1 ? 16 : event.deltaMode === 2 ? right.clientHeight : 1);
                    right.scrollBy({ top: distance, behavior: window.matchMedia("(prefers-reduced-motion: reduce)").matches ? "auto" : "smooth" });
                    event.preventDefault();
                }
            }, { passive: false });
            return aside;
        }
        function reaction(view) {
            var text = "", s = state.screen;
            if (s === "s1" && a.needs.length) { text = fill(copy("priorities", a.needs.length === rules.needs.length ? "priorityReactionComplete" : "priorityReactionPartial", "Got it. {first} comes first."), { first: label(needs, a.needs[0]) }); }
            if (s === "s2" && a.peaceOfMind) { text = copy("priorities", "peace" + (rules.peaceOfMind.indexOf(a.peaceOfMind) + 1) + "Reaction"); }
            if (s === "s4" && a.people.length) { text = copy("aboutPeople", a.people.length <= 2 ? "peopleReaction1Rte" : "peopleReaction2Rte", "Let's take good care of the people who matter most.").replace(/<[^>]*>/g, ""); }
            if (s === "s5") { text = copy("legacy", a.amountMode === "unsure" ? "reactionUnsureRte" : "reactionShortRte").replace(/<[^>]*>/g, ""); text = fill(text, { gap: money(Math.max(a.legacyAmount - a.putAside, 0)) }); }
            if (s === "distribution") {
                var person = activePeople()[state.person];
                if (person && person.distribution.parts.length) { text = fill(copy("distribution", person.distribution.parts.length > 1 ? "mixReaction" + person.distribution.parts.length : person.distribution.parts[0] + "Reaction"), { name: person.name }); }
            }
            if (s === "calc") { text = copy("legacy", "calcReaction", "You've turned your love for them into a plan."); }
            if (s === "s8") { text = fill(copy("review", a.people.length === 1 ? "reviewReactionOne" : "reviewReactionMany", "You've planned for {count} people you love."), { count: a.people.length }); }
            if (s === "contact") { text = copy("contact", "reassuranceRte", "Nearly there. We'll keep your details private and only use them to email your plan and follow up with you.").replace(/<[^>]*>/g, ""); }
            if (text) { view.appendChild(node("div", { "class": "lb-reaction" }, [icon("thought"), node("div", {}, [node("p", { "class": "lb-eyebrow", text: copy("labels", "reactionHeading", "A quick thought") }), node("p", { "data-legacy-reaction": s === "s5" && a.amountMode === "known" ? true : null, text: text })])])); }
        }
        function render(resetScroll) {
            var previousView = app.querySelector(".lb-view");
            var scrollTop = !resetScroll && previousView ? previousView.scrollTop : 0;
            var pageX = window.scrollX, pageY = window.scrollY;
            bindings = [];
            var isBlueprint = state.screen === "blueprint";
            var layout = node("div", { "class": "lb-layout" + (isBlueprint ? " lb-layout--report" : "") + (settings.editorPreview ? " lb-layout--editor" : "") });
            if (!isBlueprint) { layout.appendChild(panel()); }
            var outer = node("div", { "class": "lb-view" + (state.screen === "intro" ? " lb-view--intro" : ""), "aria-busy": state.sending ? "true" : "false" });
            var view = isBlueprint || state.screen === "intro" ? outer : node("div", { "class": "lb-flow-card" });
            if (view !== outer) { outer.appendChild(view); }
            if (state.screen !== "intro" && !isBlueprint) {
                var step = stepInfo(), labelIndex = state.screen === "contact" || state.screen === "s8" ? 8 : Math.floor(step[0]);
                var header = node("div", { "class": "lb-step-heading" }, [node("div", { "class": "lb-step-header" }, [node("strong", { text: STEP_LABELS[labelIndex - 1] }), node("span", { text: labelIndex + " / 8" })]),
                    node("div", { "class": "lb-progress", role: "progressbar", "aria-valuemin": 0, "aria-valuemax": 8, "aria-valuenow": labelIndex, "aria-label": "Journey progress" }, [node("span", { style: "width:" + (labelIndex / 8 * 100) + "%" })])]);
                outer.insertBefore(header, view);
            }
            var screen = state.screen;
            view.classList.add("lb-screen--" + screen);
            if (screen === "intro") { intro(view); }
            if (screen === "s1") { priorities(view); }
            if (screen === "s2") { var peaceQuestion = rte("priorities", "s2QuestionRte"); peaceQuestion.classList.add("lb-question"); view.appendChild(peaceQuestion);
                view.appendChild(choices(peace, a.peaceOfMind, function (v) { a.peaceOfMind = v; save(); render(); }, peace.map(function (v, i) { return copy("priorities", "peace" + (i + 1) + "Description"); }), "single")); }
            if (screen === "s3") {
                view.appendChild(title(copy("aboutPeople", "s3Title", "Tell us a little about you")));
                view.appendChild(rte("aboutPeople", "s3DescriptionRte"));
                view.appendChild(node("h3", { "class": "lb-icon-label" }, [icon("age"), node("span", { text: copy("aboutPeople", "ageQuestion", "How old are you?") })]));
                view.appendChild(choices(ages, a.ageRange, function (v) { a.ageRange = v; save(); render(); }));
                view.appendChild(node("h3", { "class": "lb-icon-label" }, [icon("relationship"), node("span", { text: copy("aboutPeople", "statusQuestion", "What's your relationship status?") })]));
                view.appendChild(choices(statuses, a.relationshipStatus, function (v) { a.relationshipStatus = v; save(); render(); }));
            }
            if (screen === "s4") { people(view); }
            if (screen === "s5") { legacy(view); }
            if (screen === "shares") { shares(view); }
            if (screen === "distribution") { distribution(view); }
            if (screen === "calc") { calculatorSummary(view); }
            if (screen === "s7") { putAside(view); }
            if (screen === "s8") { review(view); }
            if (screen === "contact") { contact(view); }
            if (screen === "blueprint") { blueprint(view); }
            reaction(outer);
            if (screen === "s8") {
                var reviewBack = button("\u2190 " + copy("review", "backToDistributionLabel", "Back to distribution"), function () {
                    track("funnel_navigation", "back"); go("distribution", Math.max(0, activePeople().length - 1));
                }, true);
                reviewBack.classList.add("lb-review-back"); outer.appendChild(reviewBack);
            }
            if (state.notice && screen !== "s1") { view.appendChild(node("div", { "class": "lb-status", role: "alert", tabindex: -1, text: state.notice })); }
            if (screen !== "contact" && screen !== "s8" && !isBlueprint && !(screen === "intro" && state.saved)) {
                var nav = node("div", { "class": "lb-navigation" });
                if (screen !== "intro") { nav.appendChild(button("← " + copy("labels", "backLabel", "Back"), back, true)); }
                var continueButton = button(state.returnToReview ? copy("labels", "returnToReviewLabel", "Save & back to summary") : screen === "intro" ? copy("intro", "beginLabel", "Let's begin") : screen === "s8" ? copy("review", "createLabel", "Create my blueprint") : copy("labels", "continueLabel", "Continue"), next);
                function updateContinue() { continueButton.disabled = !!problem(screen); }
                bindings.push(updateContinue); updateContinue(); nav.appendChild(continueButton);
                outer.appendChild(nav);
                if (screen === "s1" && a.needs.length < rules.needs.length) {
                    outer.appendChild(node("p", { "class": "lb-priority-validation", role: "status", "aria-live": "polite" }, [
                        node("span", { "class": "lb-validation-icon", "aria-hidden": "true", text: "!" }),
                        node("span", { text: fill(copy("priorities", "priorityValidation", "Pick {count} more to continue. Rank all 5 priorities."), { count: rules.needs.length - a.needs.length }) })]));
                }
                if (screen !== "intro" && screen !== "s1" && screen !== "s8") {
                    var hint = node("p", { "class": "lb-priority-validation", role: "status", "aria-live": "polite" });
                    function updateHint() { hint.textContent = problem(screen); hint.hidden = !hint.textContent; }
                    bindings.push(updateHint); updateHint(); outer.appendChild(hint);
                }
                if (screen === "intro") {
                    outer.appendChild(node("p", { "class": "lb-begin-note", text: copy("intro", "beginNote", "About 10 minutes, we will save your progress as you go.") }));
                }
            }
            // Build before replacing the current layout so the page does not collapse mid-render.
            layout.appendChild(outer);
            app.textContent = ""; app.appendChild(layout);
            outer.scrollTop = scrollTop;
            // On mobile the document scrolls instead of the right-hand panel.
            if (!resetScroll && (window.scrollX !== pageX || window.scrollY !== pageY)) {
                window.scrollTo({ left: pageX, top: pageY, behavior: "instant" });
            }
        }
        if (settings.editorPreview) {
            a.importance = 4; a.preparedness = 3; a.needs = rules.needs.slice(); a.peaceOfMind = rules.peaceOfMind[0]; a.ageRange = rules.ageRanges[1]; a.relationshipStatus = rules.relationshipStatuses[1];
            a.people = [{ id: "preview-person", name: "Beth", relationship: relationships.length ? relationships[0].value : "", share: 100, distribution: newDistribution() }]; a.people[0].distribution.parts = ["lump"];
            var requested = settings.previewScreen || "intro";
            state.screen = requested === "s6" ? "distribution" : requested;
            if (["intro", "s1", "s2", "s3", "s4", "s5", "distribution", "s7", "s8", "contact", "blueprint"].indexOf(state.screen) < 0) { state.screen = "intro"; }
            if (state.screen === "blueprint") {
                a.contact.fullName = "Sample Visitor"; a.contact.email = "sample@example.com";
                var samplePlan = M.preview(a, rules);
                samplePlan.recommendedPlanAmount = (Math.floor(samplePlan.gap / rules.planStep) + 1) * rules.planStep;
                samplePlan.gapBelowMinimum = samplePlan.gap < rules.legacyMinimum;
                samplePlan.people[0].lumpAmount = samplePlan.legacyAmount;
                samplePlan.people[0].parts = ["lump"];
                state.result = { plan: samplePlan, emailSent: false, apiSubmitted: false };
            }
        }
        render(); track("page_view"); track(state.saved ? "funnel_step" : "funnel_begin");
        // Size against the visible browser, not AEM's content-driven iframe height.
        function sizePanels() {
            if (!root.getBoundingClientRect || !root.style || !document.documentElement) { return; }
            root.style.marginLeft = ""; root.style.width = "";
            var rect = root.getBoundingClientRect(), width = document.documentElement.clientWidth;
            root.style.marginLeft = -rect.left + "px"; root.style.width = width + "px";
            var height = window.innerHeight || 720;
            try {
                if (window.frameElement && window.parent !== window) { height = window.parent.innerHeight - Math.max(0, window.frameElement.getBoundingClientRect().top); }
            } catch (e) { /* Cross-origin embedding uses the local viewport. */ }
            height = Math.max(320, height - Math.max(0, rect.top));
            root.style.setProperty("--lb-panel-height", height + "px");
        }
        sizePanels();
        if (window.addEventListener) { window.addEventListener("resize", sizePanels); }
        // Reloads must show the beginning of the welcome panel, rather than a restored mid-page offset.
        function positionWelcome() {
            if (!settings.editorPreview && state.screen === "intro") { root.scrollIntoView({ behavior: "auto", block: "start" }); }
        }
        if (window.requestAnimationFrame) { window.requestAnimationFrame(positionWelcome); }
        else { positionWelcome(); }
        if (window.addEventListener) { window.addEventListener("pageshow", positionWelcome); }
    }
    function boot() { Array.prototype.forEach.call(document.querySelectorAll('[data-cmp-is="lsoCalculator"]'), init); }
    if (document.readyState === "loading") { document.addEventListener("DOMContentLoaded", boot); } else { boot(); }
    document.addEventListener("foundation-contentloaded", boot);
}());
