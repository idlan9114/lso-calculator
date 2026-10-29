/* Browser previews only. The submission service remains authoritative for every final figure. */
(function (scope) {
    "use strict";
    function round(n) { return Math.floor(n + 0.5); }
    function sum(values) { return values.reduce(function (a, b) { return a + b; }, 0); }
    // Keep previously edited percentages fixed and distribute the remainder over untouched fields.
    function allocate(current, keys, touched, changed, value) {
        var locked = touched.filter(function (key) { return key !== changed && keys.indexOf(key) >= 0; });
        var free = keys.filter(function (key) { return key !== changed && locked.indexOf(key) < 0; });
        if (!free.length && locked.length) { free = [locked.shift()]; }
        var result = {}, lockedTotal = 0;
        locked.forEach(function (key) { result[key] = current[key] || 0; lockedTotal += result[key]; });
        var chosen = Math.min(Math.max(0, Math.round(Number(value) || 0)), Math.max(0, 100 - lockedTotal));
        result[changed] = keys.length === 1 ? 100 : chosen;
        var rest = 100 - lockedTotal - result[changed];
        free.forEach(function (key, i) { result[key] = Math.floor(rest / free.length) + (i < rest % free.length ? 1 : 0); });
        return { values: result, touched: locked.concat([changed]) };
    }
    function percentages(values) {
        var total = sum(values);
        if (!total) { return values.map(function () { return 0; }); }
        var raw = values.map(function (v) { return v / total * 100; });
        var result = raw.map(Math.floor);
        var order = raw.map(function (v, i) { return i; }).sort(function (a, b) {
            return (raw[b] - result[b]) - (raw[a] - result[a]) || a - b;
        });
        var left = 100 - sum(result);
        for (var i = 0; i < left; i++) { result[order[i % order.length]]++; }
        return result;
    }
    function periods(s) { return s.frequency === "monthly" ? 12 : 1; }
    function totalPayments(s, payment) {
        if (!integer(s.years, 1, 100) || (s.stepUp && (!integer(s.rate, 1, 100) || !integer(s.interval, 1, 100)))) { return 0; }
        var total = 0;
        for (var y = 0; y < s.years; y++) {
            total += payment * periods(s) * (s.stepUp ? Math.pow(1 + s.rate / 100, Math.floor(y / s.interval)) : 1);
        }
        return total;
    }
    function firstPayment(s, pot) {
        var denominator = totalPayments(s, 1);
        return pot > 0 && denominator > 0 ? round(pot / denominator) : 0;
    }
    function needs(d) {
        return d.parts.map(function (part) {
            if (part === "lump") { return d.lumpAmount || 0; }
            if (part === "structured") { return totalPayments(d.structured, d.structured.amount || 0); }
            return sum(d.milestones.selected.map(function (id) { return d.milestones.amounts[id] || 0; }));
        });
    }
    function preview(a, rules) {
        var unsure = a.amountMode === "unsure";
        var totals = a.people.map(function (p) { return unsure ? sum(needs(p.distribution)) : 0; });
        var shares = unsure ? percentages(totals) : a.people.map(function (p) { return p.share; });
        var legacy = unsure ? Math.max(round(sum(totals)), rules.legacyMinimum) : a.legacyAmount;
        return { legacyAmount: legacy, needsTotal: round(sum(totals)), putAside: a.putAside,
            gap: Math.max(legacy - a.putAside, 0), people: a.people.map(function (p, i) {
                return { id: p.id, share: shares[i], amount: round(legacy * shares[i] / 100) };
            }) };
    }
    function validName(value) { return !!value.trim() && value.trim().length <= 100 && !/[\x00-\x1f\x7f<>]/.test(value); }
    function integer(n, min, max) { return Number.isInteger(n) && n >= min && n <= max; }
    function distributionError(a, p, rules) {
        var d = p.distribution, unsure = a.amountMode === "unsure", total = 0;
        if (!unsure && !p.share) { return ""; }
        if (!d.parts.length) { return "Choose at least one way for them to receive their gift."; }
        if (!unsure && d.parts.length > 1) {
            if (d.parts.some(function (part) { return !integer(d.split[part], 1, 100); }) || sum(d.parts.map(function (part) { return d.split[part]; })) !== 100) {
                return "Give each selected way at least 1%. The split must add up to 100%.";
            }
        }
        if (unsure && d.parts.indexOf("lump") >= 0 && !integer(d.lumpAmount, 1, 1000000000)) { return "Enter a lump sum between RM1 and RM1 billion."; }
        if (d.parts.indexOf("structured") >= 0) {
            var s = d.structured;
            if (!integer(s.years, 1, 100)) { return "Choose a payment duration between 1 and 100 years."; }
            if (s.stepUp && (!integer(s.rate, 1, 100) || !integer(s.interval, 1, 100))) { return "Enter a growth rate and interval between 1 and 100."; }
            if (s.start === "age" && !integer(s.startAge, 1, 120)) { return "Enter a starting age between 1 and 120."; }
            var minimum = s.frequency === "monthly" ? rules.monthlyPaymentMinimum : rules.yearlyPaymentMinimum;
            var pot = a.legacyAmount * p.share / 100 * (d.parts.length > 1 ? d.split.structured / 100 : 1);
            var payment = unsure ? s.amount : firstPayment(s, pot);
            if (!integer(payment, minimum, 1000000000)) { return "Payments must be at least RM" + minimum.toLocaleString("en-MY") + ". Try fewer years or a larger portion."; }
            if (!unsure && payment * periods(s) > pot) { return "This portion cannot cover a full year of payments."; }
        }
        if (d.parts.indexOf("milestones") >= 0) {
            var m = d.milestones;
            if (!m.selected.length || m.selected.length > rules.maxMilestones) { return "Choose between 1 and " + rules.maxMilestones + " life moments."; }
            if (!integer(m.within, 1, 50)) { return "Choose a life-moment period between 1 and 50 years."; }
            if (m.selected.indexOf("age") >= 0 && !integer(m.ages.age, 1, 120)) { return "Enter a special age between 1 and 120."; }
            for (var i = 0; i < m.selected.length; i++) {
                var value = unsure ? m.amounts[m.selected[i]] : m.allocation[m.selected[i]];
                if (!integer(value, unsure ? 1 : 0, unsure ? 1000000000 : 100)) { return "Enter a valid allocation for each selected life moment."; }
                total += value;
            }
            if (!unsure && total !== 100) { return "Life-moment allocations must add up to 100%."; }
        }
        if (unsure && sum(a.people.map(function (person) { return sum(needs(person.distribution)); })) > 1000000000) { return "The combined needs cannot exceed RM1 billion."; }
        return "";
    }
    function contactErrors(c, rules) {
        var errors = {};
        if (!validName(c.fullName)) { errors.fullName = "Enter a name of up to 100 characters, without < or >."; }
        if (c.email.trim().length > 254 || !/^[^\s@,;]+@[^\s@,;]+\.[^\s@,;]{2,}$/.test(c.email.trim())) { errors.email = "Enter a valid email address."; }
        var digits = c.mobile.replace(/\D/g, "");
        if (c.countryCode === "60") { digits = digits.replace(/^60/, "").replace(/^0+/, ""); }
        if (rules.countryCodes.indexOf(c.countryCode) < 0) { errors.countryCode = "Select a country code."; }
        if (!(c.countryCode === "60" ? /^1\d{8,9}$/.test(digits) : digits.length >= 6 && digits.length <= 14)) { errors.mobile = "Enter a valid mobile number."; }
        if (c.nric.replace(/\D/g, "").length !== 12) { errors.nric = "Enter a valid 12-digit NRIC number."; }
        if (rules.states.indexOf(c.state) < 0) { errors.state = "Select your state."; }
        if (!/^[A-Za-z0-9]{0,8}$/.test(c.referralCode)) { errors.referralCode = "Use up to 8 letters or numbers."; }
        if (!c.consent) { errors.consent = "Please read and agree to the privacy notice to continue."; }
        return errors;
    }
    scope.LsoCalculatorMath = { round: round, sum: sum, allocate: allocate, percentages: percentages, totalPayments: totalPayments,
        firstPayment: firstPayment, preview: preview, validName: validName, integer: integer,
        distributionError: distributionError, contactErrors: contactErrors };
}(typeof window === "undefined" ? globalThis : window));
