const numberFormat = new Intl.NumberFormat("en-US", { maximumFractionDigits: 2 });
const preciseNumberFormat = new Intl.NumberFormat("en-US", { maximumFractionDigits: 16 });

export function formatNumber(value) {
    return value == null || value === "" ? "—" : numberFormat.format(Number(value));
}

export function formatPercentage(value) {
    const formatted = formatNumber(value);
    if (formatted === "—") return formatted;

    const numericDisplay = Number(formatted.replaceAll(",", ""));
    const rawCategory = value < 70 ? 0 : value <= 90 ? 1 : 2;
    const displayCategory =
        numericDisplay < 70 ? 0 : numericDisplay <= 90 ? 1 : 2;
    return rawCategory === displayCategory
        ? formatted
        : preciseNumberFormat.format(value);
}

export function formatQuantity(value) {
    const formatted = formatNumber(value);
    return formatted === "—" ? formatted : `${formatted} kg`;
}

export function formatMoney(value) {
    const formatted = formatNumber(value);
    return formatted === "—" ? formatted : `${formatted} FCFA`;
}
