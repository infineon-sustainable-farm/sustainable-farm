/**
 * What-if Scenario Analysis: local simulation only.
 *
 * Every value below is a provisional demonstration assumption taken from the
 * CropCore dashboard prototype. None of them is validated farm data, and the
 * results are not investment advice. The simulation never reads or writes the
 * backend: it does not change any real capacity.
 */
export const SCENARIO_ASSUMPTIONS = Object.freeze({
    costPerM2: 150000,
    costPerSensor: 25000,
    coolingCost: 2000000,
    solarCost: 3500000,
    tonnesPerM2: 1.5,
    baseLossPct: 8,
    sensorLossReductionPts: 0.5,
    maxEffectiveSensors: 10,
    coolingRelativeReduction: 0.3,
    pricePerTonne: 250000,
    annualElectricity: 1500000,
    solarSavingsRate: 0.8,
    baseStorageM2: 132,
    unmetDemandTonnes: 20,
    marginPerTonne: 50000,
    storageMaintenancePerM2: 3600,
    sensorMaintenanceRate: 0.1,
    coolingOperatingCost: 600000,
    solarMaintenanceRate: 0.02,
    projectionYears: 5,
});

export const SCENARIO_LIMITS = Object.freeze({
    storage: { min: 0, max: 200, step: 10 },
    sensors: { min: 0, max: 50, step: 1 },
});

export const SCENARIO_PRESETS = Object.freeze({
    small: Object.freeze({ storageM2: 50, sensors: 10, cooling: false, solar: false }),
    full: Object.freeze({ storageM2: 150, sensors: 30, cooling: true, solar: true }),
    solar: Object.freeze({ storageM2: 0, sensors: 0, cooling: false, solar: true }),
});

export const INITIAL_PRESET = "small";

function clampToStep(value, { min, max, step }) {
    const number = Number(value);
    if (!Number.isFinite(number)) return min;
    const stepped = Math.round((number - min) / step) * step + min;
    return Math.min(max, Math.max(min, stepped));
}

/** Keeps manual inputs inside the slider ranges and steps of the prototype. */
export function normalizeScenarioInputs(inputs) {
    return {
        storageM2: clampToStep(inputs.storageM2, SCENARIO_LIMITS.storage),
        sensors: clampToStep(inputs.sensors, SCENARIO_LIMITS.sensors),
        cooling: Boolean(inputs.cooling),
        solar: Boolean(inputs.solar),
    };
}

export const OUTLOOK = Object.freeze({
    PROFITABLE: "PROFITABLE_WITHIN_HORIZON",
    BEYOND: "BREAK_EVEN_AFTER_HORIZON",
    NOT_VIABLE: "NOT_FINANCIALLY_VIABLE",
    NO_BREAK_EVEN: "NO_BREAK_EVEN",
});

/**
 * Same formulas as the prototype's computeScenario. Pure function: no I/O.
 *
 * @param {{storageM2:number, sensors:number, cooling:boolean, solar:boolean}} rawInputs
 * @param {typeof SCENARIO_ASSUMPTIONS} a assumptions (defaults to the prototype values)
 */
export function computeScenario(rawInputs, a = SCENARIO_ASSUMPTIONS) {
    const { storageM2, sensors, cooling, solar } = normalizeScenarioInputs(rawInputs);

    const investment =
        storageM2 * a.costPerM2 +
        sensors * a.costPerSensor +
        (cooling ? a.coolingCost : 0) +
        (solar ? a.solarCost : 0);

    const addedCapacityTonnes = storageM2 * a.tonnesPerM2;
    const usedAddedCapacityTonnes = Math.min(addedCapacityTonnes, a.unmetDemandTonnes);

    // Prototype simplification, not a measured throughput.
    const baselineVolumeTonnes = a.baseStorageM2 * a.tonnesPerM2;
    const simulatedVolumeTonnes = baselineVolumeTonnes + usedAddedCapacityTonnes;

    let lossRatePct = Math.max(
        0,
        a.baseLossPct - Math.min(sensors, a.maxEffectiveSensors) * a.sensorLossReductionPts,
    );
    if (cooling) lossRatePct *= 1 - a.coolingRelativeReduction;
    // Percentage points, not to be confused with the 30 % relative cooling effect.
    const lossReductionPts = a.baseLossPct - lossRatePct;

    const avoidedLossValue = (simulatedVolumeTonnes * a.pricePerTonne * lossReductionPts) / 100;
    const capacityBenefit = usedAddedCapacityTonnes * a.marginPerTonne;
    const energySavings = solar ? a.annualElectricity * a.solarSavingsRate : 0;
    const annualGrossBenefit = avoidedLossValue + capacityBenefit + energySavings;

    const annualCost =
        storageM2 * a.storageMaintenancePerM2 +
        sensors * a.costPerSensor * a.sensorMaintenanceRate +
        (cooling ? a.coolingOperatingCost : 0) +
        (solar ? a.solarCost * a.solarMaintenanceRate : 0);
    const annualNetBenefit = annualGrossBenefit - annualCost;

    let breakEvenMonths = null;
    if (investment > 0 && annualNetBenefit > 0) {
        breakEvenMonths = Math.ceil(investment / (annualNetBenefit / 12));
    } else if (investment === 0 && annualNetBenefit > 0) {
        breakEvenMonths = 0;
    }

    const projection = [];
    for (let year = 1; year <= a.projectionYears; year += 1) {
        const cumulativeCost = investment + annualCost * year;
        const cumulativeBenefit = annualGrossBenefit * year;
        projection.push({
            year,
            cumulativeCost,
            cumulativeBenefit,
            cumulativeNet: cumulativeBenefit - cumulativeCost,
        });
    }
    const fiveYearNetGain = projection[projection.length - 1].cumulativeNet;

    let outlook;
    if (annualNetBenefit <= 0) {
        outlook = OUTLOOK.NOT_VIABLE;
    } else if (
        breakEvenMonths != null &&
        breakEvenMonths <= a.projectionYears * 12 &&
        fiveYearNetGain >= 0
    ) {
        outlook = OUTLOOK.PROFITABLE;
    } else if (breakEvenMonths != null) {
        outlook = OUTLOOK.BEYOND;
    } else {
        outlook = OUTLOOK.NO_BREAK_EVEN;
    }

    return {
        inputs: { storageM2, sensors, cooling, solar },
        investment,
        addedCapacityTonnes,
        usedAddedCapacityTonnes,
        baselineVolumeTonnes,
        simulatedVolumeTonnes,
        lossRatePct,
        lossReductionPts,
        avoidedLossValue,
        capacityBenefit,
        energySavings,
        annualGrossBenefit,
        annualCost,
        annualNetBenefit,
        breakEvenMonths,
        projection,
        fiveYearNetGain,
        outlook,
    };
}

/** Returns the preset key whose four values equal the inputs, or null. */
export function matchingPreset(inputs) {
    const normalized = normalizeScenarioInputs(inputs);
    return (
        Object.entries(SCENARIO_PRESETS).find(([, preset]) =>
            Object.keys(preset).every((key) => preset[key] === normalized[key]),
        )?.[0] ?? null
    );
}

const fcfaFormat = new Intl.NumberFormat("en-US", { maximumFractionDigits: 0 });
const oneDecimal = new Intl.NumberFormat("en-US", {
    minimumFractionDigits: 1,
    maximumFractionDigits: 1,
});

/** FCFA rounded to the unit; a leading sign is kept for gains. */
export function formatFcfa(value, { signed = false } = {}) {
    if (!Number.isFinite(value)) return "—";
    const rounded = Math.round(value);
    const text = `${fcfaFormat.format(Math.abs(rounded))} FCFA`;
    if (rounded < 0) return `−${text}`;
    return signed && rounded > 0 ? `+${text}` : text;
}

export function formatTonnes(value) {
    return Number.isFinite(value) ? `${oneDecimal.format(value)} t` : "—";
}

export function formatPoints(value) {
    return Number.isFinite(value) ? `${oneDecimal.format(value)} pts` : "—";
}

export function formatBreakEven(months) {
    if (months == null) return "No break-even";
    if (months === 0) return "Immediate";
    return `${months} months (${oneDecimal.format(months / 12)} years)`;
}

export const OUTLOOK_LABELS = Object.freeze({
    [OUTLOOK.PROFITABLE]: "Profitable within 5 years",
    [OUTLOOK.BEYOND]: "Break-even after 5 years",
    [OUTLOOK.NOT_VIABLE]: "Not financially viable",
    [OUTLOOK.NO_BREAK_EVEN]: "No break-even",
});

/** One-line summary shown on screen by the "Show summary" action (no file). */
export function buildScenarioSummary(result) {
    return [
        `Investment: ${formatFcfa(result.investment)}`,
        `Operating cost: ${formatFcfa(result.annualCost)}/year`,
        `Net benefit: ${formatFcfa(result.annualNetBenefit)}/year`,
        `Break-even: ${formatBreakEven(result.breakEvenMonths)}`,
        `5-year net: ${formatFcfa(result.fiveYearNetGain, { signed: true })}`,
        `Outlook: ${OUTLOOK_LABELS[result.outlook]}`,
    ].join(" · ");
}
