import { formatEur, formatKilograms, formatNumber, formatXof, NO_VALUE } from "./format";

/*
 * The scenario screen and its API, kept apart from the components so their
 * rules can be tested: the body of the scenario form, the checks that mirror
 * the API's 400 rules, the parameters of a variant, and how the figures of an
 * analysis are written. Nothing here computes a result: every figure comes
 * from the API, and only its form changes here (sign, currency, unit).
 */

/** The currencies a scenario can be entered in, as the API names them. */
export const SCENARIO_CURRENCIES = ["XOF", "EUR"];

/** The optional values of a scenario: left empty, they stay unknown, never 0. */
const OPTIONAL_NUMBERS = ["lossRate", "pricePerKg", "variableCostPerKg", "costPerAdditionalTree"];
const REQUIRED_NUMBERS = ["treeCount", "yieldPerTreeKg", "annualFixedCost", "initialInvestment"];

/** Each value of the form with the field of its source. */
export const SOURCE_FIELDS = {
    treeCount: "treeCountSource",
    yieldPerTreeKg: "yieldSource",
    lossRate: "lossRateSource",
    pricePerKg: "priceSource",
    annualFixedCost: "annualFixedCostSource",
    variableCostPerKg: "variableCostSource",
    initialInvestment: "investmentSource",
    costPerAdditionalTree: "costPerAdditionalTreeSource",
};

export const EMPTY_SCENARIO_FORM = {
    name: "",
    varietyName: "",
    currency: "",
    ...Object.fromEntries([...REQUIRED_NUMBERS, ...OPTIONAL_NUMBERS].map((field) => [field, ""])),
    ...Object.fromEntries(Object.values(SOURCE_FIELDS).map((field) => [field, ""])),
};

function textOf(value) {
    return value === null || value === undefined ? "" : String(value);
}

/**
 * The values of the form for a recorded scenario, or empty ones for a new
 * scenario. The sources start empty: on a correction, an empty source keeps
 * the recorded one while its value stays the same, as the API does.
 */
export function scenarioFormValues(scenario) {
    if (!scenario) return EMPTY_SCENARIO_FORM;
    return {
        ...EMPTY_SCENARIO_FORM,
        name: textOf(scenario.name),
        varietyName: textOf(scenario.varietyName),
        currency: textOf(scenario.currency),
        ...Object.fromEntries([...REQUIRED_NUMBERS, ...OPTIONAL_NUMBERS].map((field) => [field, textOf(scenario[field])])),
    };
}

/*
 * Same rules as the API. The patterns read the value as typed, so a comma, an
 * exponent or a sign is refused rather than read some other way.
 */
const TREE_COUNT_PATTERN = /^\d{1,7}$/;
const YIELD_PATTERN = /^\d{1,4}(\.\d)?$/;
const RATE_PATTERN = /^\d(\.\d{1,3})?$/;
const PRICE_PATTERN = /^\d{1,10}(\.\d{1,4})?$/;
const AMOUNT_PATTERN = /^\d{1,12}(\.\d{1,2})?$/;
const TEXT_MAX_LENGTH = 255;

export const SCENARIO_MESSAGES = {
    nameRequired: "Name is required.",
    nameTooLong: `Name must be at most ${TEXT_MAX_LENGTH} characters.`,
    varietyTooLong: `Variety must be at most ${TEXT_MAX_LENGTH} characters.`,
    treeCount: "Trees must be a whole number from 1 to 1,000,000.",
    yieldPerTreeKg: "Yield per tree must be greater than 0 and at most 1,000 kg, with at most 1 decimal.",
    lossRate: "Loss rate must be between 0 and 1, with at most 3 decimals.",
    pricePerKg: "Price per kg must be greater than 0, with at most 4 decimals.",
    currency: "Currency is required.",
    annualFixedCostRequired: "Fixed operating cost is required.",
    annualFixedCost: "Fixed operating cost must be 0 or more, with at most 2 decimals.",
    variableCostPerKg: "Variable operating cost must be 0 or more, with at most 4 decimals.",
    initialInvestmentRequired: "Investment is required.",
    initialInvestment: "Investment must be greater than 0, with at most 2 decimals.",
    costPerAdditionalTree: "Cost per added tree must be 0 or more, with at most 2 decimals.",
    source: `Source must be at most ${TEXT_MAX_LENGTH} characters.`,
};

/**
 * The errors of the scenario form, by field, with the rules of the API; empty
 * when the form can be sent.
 */
export function validateScenarioForm(values) {
    const errors = {};
    const text = (field) => values[field].trim();

    if (!text("name")) errors.name = SCENARIO_MESSAGES.nameRequired;
    else if (values.name.length > TEXT_MAX_LENGTH) errors.name = SCENARIO_MESSAGES.nameTooLong;
    if (values.varietyName.length > TEXT_MAX_LENGTH) errors.varietyName = SCENARIO_MESSAGES.varietyTooLong;

    const trees = text("treeCount");
    if (!TREE_COUNT_PATTERN.test(trees) || Number(trees) < 1 || Number(trees) > 1_000_000) {
        errors.treeCount = SCENARIO_MESSAGES.treeCount;
    }
    const yieldPerTree = text("yieldPerTreeKg");
    if (!YIELD_PATTERN.test(yieldPerTree) || Number(yieldPerTree) <= 0 || Number(yieldPerTree) > 1000) {
        errors.yieldPerTreeKg = SCENARIO_MESSAGES.yieldPerTreeKg;
    }
    const lossRate = text("lossRate");
    if (lossRate && (!RATE_PATTERN.test(lossRate) || Number(lossRate) > 1)) {
        errors.lossRate = SCENARIO_MESSAGES.lossRate;
    }
    const price = text("pricePerKg");
    if (price && (!PRICE_PATTERN.test(price) || Number(price) <= 0)) {
        errors.pricePerKg = SCENARIO_MESSAGES.pricePerKg;
    }
    if (!SCENARIO_CURRENCIES.includes(values.currency)) errors.currency = SCENARIO_MESSAGES.currency;

    const fixedCost = text("annualFixedCost");
    if (!fixedCost) errors.annualFixedCost = SCENARIO_MESSAGES.annualFixedCostRequired;
    else if (!AMOUNT_PATTERN.test(fixedCost)) errors.annualFixedCost = SCENARIO_MESSAGES.annualFixedCost;
    const variableCost = text("variableCostPerKg");
    if (variableCost && !PRICE_PATTERN.test(variableCost)) {
        errors.variableCostPerKg = SCENARIO_MESSAGES.variableCostPerKg;
    }
    const investment = text("initialInvestment");
    if (!investment) errors.initialInvestment = SCENARIO_MESSAGES.initialInvestmentRequired;
    else if (!AMOUNT_PATTERN.test(investment) || Number(investment) <= 0) {
        errors.initialInvestment = SCENARIO_MESSAGES.initialInvestment;
    }
    const costPerTree = text("costPerAdditionalTree");
    if (costPerTree && !AMOUNT_PATTERN.test(costPerTree)) {
        errors.costPerAdditionalTree = SCENARIO_MESSAGES.costPerAdditionalTree;
    }

    for (const sourceField of Object.values(SOURCE_FIELDS)) {
        if (values[sourceField].length > TEXT_MAX_LENGTH) errors[sourceField] = SCENARIO_MESSAGES.source;
    }
    return errors;
}

/**
 * Body of the scenario form, already checked: the numbers as numbers, and an
 * optional value, the variety or a source left empty not sent at all. The API
 * then keeps the value unknown, and records user_entry for a source left out,
 * or keeps the recorded one while its value stays the same.
 */
export function scenarioRequestBody(values) {
    const body = { name: values.name, currency: values.currency };
    if (values.varietyName.trim()) body.varietyName = values.varietyName;
    for (const field of [...REQUIRED_NUMBERS, ...OPTIONAL_NUMBERS]) {
        const value = values[field].trim();
        if (value) body[field] = Number(value);
        const sourceField = SOURCE_FIELDS[field];
        if (value && values[sourceField].trim()) body[sourceField] = values[sourceField];
    }
    return body;
}

/** Whether two variety names are the same ignoring case, accents and spaces, as the API compares them. */
export function sameVarietyName(first, second) {
    const key = (name) => String(name ?? "").normalize("NFD").replace(/\p{M}+/gu, "").trim().toLowerCase();
    return key(first) !== "" && key(first) === key(second);
}

/*
 * The inputs of a variant, read from the text typed, with the bounds of the
 * API. Each gives `{ value, error }`: no value and no error while empty.
 */
function parsed(text, pattern, isInBounds, message) {
    const trimmed = String(text ?? "").trim();
    if (!trimmed) return { value: null, error: null };
    if (!pattern.test(trimmed) || !isInBounds(Number(trimmed))) return { value: null, error: message };
    return { value: Number(trimmed), error: null };
}

export const VARIANT_MESSAGES = {
    treeCountChange: "Trees added must be a whole number from -1,000,000 to 1,000,000.",
    yieldPerTreeChangePct: "Change must be above -100% and at most 1,000%, with at most 1 decimal.",
    lossRate: "Loss rate must be between 0 and 1, with at most 3 decimals.",
    additionalInvestment: "Additional investment must be 0 or more, with at most 2 decimals.",
};

export function parseTreeCountChange(text) {
    return parsed(text, /^[+-]?\d{1,7}$/, (value) => Math.abs(value) <= 1_000_000, VARIANT_MESSAGES.treeCountChange);
}

export function parseYieldChangePct(text) {
    return parsed(text, /^[+-]?\d{1,4}(\.\d)?$/, (value) => value > -100 && value <= 1000,
        VARIANT_MESSAGES.yieldPerTreeChangePct);
}

export function parseLossRate(text) {
    return parsed(text, RATE_PATTERN, (value) => value <= 1, VARIANT_MESSAGES.lossRate);
}

export function parseAdditionalInvestment(text) {
    return parsed(text, AMOUNT_PATTERN, () => true, VARIANT_MESSAGES.additionalInvestment);
}

/**
 * Query parameters of an analysis: only those given, so the API keeps its
 * default for the others, and each variant has its own cached analysis.
 */
export function analysisParams(values = {}) {
    const params = {};
    for (const name of ["treeCountChange", "yieldPerTreeChangePct", "lossRate", "additionalInvestment", "horizonYears"]) {
        if (values[name] !== null && values[name] !== undefined) params[name] = values[name];
    }
    return params;
}

/*
 * The state of the screen kept in its address, so a variant can be shared:
 * the scenario, the slider, the levers and the years shown. Values out of
 * bounds fall back to the defaults below.
 */
export const DEFAULT_YIELD_INCREASE = 10;
export const DEFAULT_HORIZON_YEARS = 10;
export const MAX_YIELD_INCREASE = 50;
export const MAX_HORIZON_YEARS = 30;

function wholeNumberIn(text, min, max) {
    const trimmed = String(text ?? "").trim();
    if (!/^\d{1,3}$/.test(trimmed)) return null;
    const value = Number(trimmed);
    return value >= min && value <= max ? value : null;
}

/** The slider, from 0 to 50 %, +10 % when absent or out of bounds. */
export function readYieldIncrease(text) {
    return wholeNumberIn(text, 0, MAX_YIELD_INCREASE) ?? DEFAULT_YIELD_INCREASE;
}

/** The years shown, from 1 to 30, 10 when absent or out of bounds. */
export function readHorizonYears(text) {
    return wholeNumberIn(text, 1, MAX_HORIZON_YEARS) ?? DEFAULT_HORIZON_YEARS;
}

/** The scenario named in the address, or null when none or not an identifier. */
export function readScenarioId(text) {
    const trimmed = String(text ?? "").trim();
    return /^[1-9]\d{0,17}$/.test(trimmed) ? Number(trimmed) : null;
}

/** The keys of the address, by the part of the view each one keeps. */
const VIEW_KEYS = {
    scenarioId: "scenario",
    horizonYears: "horizon",
    yieldIncrease: "yieldChange",
    investment: "investment",
    trees: "trees",
    yieldPct: "yieldPct",
    loss: "loss",
};

/** The view kept in the address: what is typed stays as typed, and is checked where it is used. */
export function readScenarioView(searchParams) {
    const text = (key) => searchParams.get(VIEW_KEYS[key]) ?? "";
    return {
        scenarioId: readScenarioId(searchParams.get(VIEW_KEYS.scenarioId)),
        horizonYears: readHorizonYears(searchParams.get(VIEW_KEYS.horizonYears)),
        yieldIncrease: readYieldIncrease(searchParams.get(VIEW_KEYS.yieldIncrease)),
        investment: text("investment"),
        trees: text("trees"),
        yieldPct: text("yieldPct"),
        loss: text("loss"),
    };
}

/**
 * The address of a view, as a query string, always in the same order: a
 * default value or an empty field leaves its key out, so the address only
 * says what differs.
 */
export function scenarioViewSearch(view) {
    const params = new URLSearchParams();
    if (view.scenarioId !== null) params.set(VIEW_KEYS.scenarioId, String(view.scenarioId));
    if (view.horizonYears !== DEFAULT_HORIZON_YEARS) params.set(VIEW_KEYS.horizonYears, String(view.horizonYears));
    if (view.yieldIncrease !== DEFAULT_YIELD_INCREASE) params.set(VIEW_KEYS.yieldIncrease, String(view.yieldIncrease));
    for (const key of ["investment", "trees", "yieldPct", "loss"]) {
        if (view[key] !== "") params.set(VIEW_KEYS[key], view[key]);
    }
    return params.toString();
}

/*
 * How the figures of an analysis are written. A negative value takes the
 * minus sign (−), a change a plus sign as well, so neither depends on colour.
 */
const MINUS = "−";

function isMissing(value) {
    return value === null || value === undefined;
}

/** The currency an amount is not entered in: its other amount is converted by the API. */
export function otherCurrency(currency) {
    return currency === "XOF" ? "EUR" : "XOF";
}

/** The unit of an amount column: "€" or "FCFA". */
export function currencyUnit(currency) {
    return currency === "XOF" ? "FCFA" : "€";
}

/** An amount in one currency, as the API rounded it: "€39,916.80", "−€6,707.00", "26,183,704 FCFA". */
export function formatMoney(value, currency) {
    if (isMissing(value)) return NO_VALUE;
    const magnitude = currency === "XOF" ? formatXof(Math.abs(value)) : formatEur(Math.abs(value));
    return value < 0 ? `${MINUS}${magnitude}` : magnitude;
}

/** A change of an amount, signed: "+€3,991.68", "−€1,200.00", "€0.00". */
export function formatSignedMoney(value, currency) {
    const text = formatMoney(value, currency);
    return !isMissing(value) && value > 0 ? `+${text}` : text;
}

function pick(amount, currency) {
    return amount?.[currency === "XOF" ? "xof" : "eur"];
}

/** An amount of the analysis in the currency of the scenario. */
export function formatAmount(amount, currency) {
    return formatMoney(pick(amount, currency), currency);
}

/** The same amount in the other currency, or null while the API has no rate to convert it. */
export function formatOtherAmount(amount, currency) {
    const other = otherCurrency(currency);
    const value = pick(amount, other);
    return isMissing(value) ? null : formatMoney(value, other);
}

/** A change of an amount of the analysis, signed, in the currency of the scenario. */
export function formatSignedAmount(amount, currency) {
    return formatSignedMoney(pick(amount, currency), currency);
}

/** The same change in the other currency, signed, or null without a rate. */
export function formatSignedOtherAmount(amount, currency) {
    const other = otherCurrency(currency);
    const value = pick(amount, other);
    return isMissing(value) ? null : formatSignedMoney(value, other);
}

/** A quantity of the analysis: "44,000 kg", "−6,707 kg". */
export function formatMass(value) {
    if (isMissing(value)) return NO_VALUE;
    return value < 0 ? `${MINUS}${formatKilograms(Math.abs(value))}` : formatKilograms(value);
}

/** A change of quantity, signed: "+4,400 kg", "−200 kg", "0 kg". */
export function formatSignedKilograms(value) {
    const text = formatMass(value);
    return !isMissing(value) && value > 0 ? `+${text}` : text;
}

/** A return as the API rounded it, to 0.1: "19.4%", "−2%". */
export function formatReturn(value) {
    if (isMissing(value)) return NO_VALUE;
    const magnitude = `${Math.abs(value).toLocaleString("en-US", { maximumFractionDigits: 1 })}%`;
    return value < 0 ? `${MINUS}${magnitude}` : magnitude;
}

/** A change of return, in percentage points: "+2.3 pts", "−0.4 pts", "0 pts". */
export function formatSignedPoints(value) {
    if (isMissing(value)) return NO_VALUE;
    const magnitude = `${Math.abs(value).toLocaleString("en-US", { maximumFractionDigits: 1 })} pts`;
    if (value > 0) return `+${magnitude}`;
    return value < 0 ? `${MINUS}${magnitude}` : magnitude;
}

/** A price or a cost per kg, with the up to 4 decimals the API keeps: "€0.9072", "300 FCFA". */
export function formatUnitPrice(value, currency) {
    if (isMissing(value)) return NO_VALUE;
    if (currency === "XOF") {
        return `${Number(value).toLocaleString("en-US", { maximumFractionDigits: 4 })} FCFA`;
    }
    return Number(value).toLocaleString("en-US", {
        style: "currency",
        currency: "EUR",
        minimumFractionDigits: 2,
        maximumFractionDigits: 4,
    });
}

/** A duration as the API rounded it, to 0.1 year: "5.2 years", "1 year". */
export function formatYears(value) {
    if (isMissing(value)) return NO_VALUE;
    return `${Number(value).toLocaleString("en-US", { maximumFractionDigits: 1 })} ${value === 1 ? "year" : "years"}`;
}

/** A number of trees added, signed: "+20 trees", "−10 trees". */
export function formatSignedTrees(value) {
    if (isMissing(value)) return NO_VALUE;
    const magnitude = `${formatNumber(Math.abs(value))} ${Math.abs(value) === 1 ? "tree" : "trees"}`;
    if (value > 0) return `+${magnitude}`;
    return value < 0 ? `${MINUS}${magnitude}` : magnitude;
}

/** A change in percent as typed, signed: "+10%", "−5%". */
export function formatSignedPercent(value) {
    if (isMissing(value)) return NO_VALUE;
    const magnitude = `${Math.abs(value).toLocaleString("en-US", { maximumFractionDigits: 1 })}%`;
    if (value > 0) return `+${magnitude}`;
    return value < 0 ? `${MINUS}${magnitude}` : magnitude;
}

/*
 * Names of the causes of an annual benefit, by the type the API gives and the
 * direction of the quantity it adds: more sold, or less.
 */
const BENEFIT_LABELS = {
    TREE_COUNT: ["Additional trees", "Fewer trees"],
    YIELD_PER_TREE: ["Higher yield per tree", "Lower yield per tree"],
    LOSS_RATE: ["Loss reduction", "Higher losses"],
    VARIABLE_OPERATING_COST: ["Additional operating cost", "Operating cost saved"],
};

/** The name of one line of an annual benefit; an unknown type is shown as received. */
export function benefitLineLabel(line) {
    const labels = BENEFIT_LABELS[line.type];
    if (!labels) return String(line.type);
    return line.quantityKg >= 0 ? labels[0] : labels[1];
}

/** What a line of an annual benefit is computed from, with the factors the API gives. */
export function benefitLineDetails(line, currency) {
    const factors = `${formatSignedKilograms(line.quantityKg)} × ${formatUnitPrice(line.unitAmount, currency)}/kg`;
    return line.type === "VARIABLE_OPERATING_COST" ? `${factors} variable cost` : factors;
}
