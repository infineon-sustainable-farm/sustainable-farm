const numberFormat = new Intl.NumberFormat("en-US", { maximumFractionDigits: 2 });

export function formatNumber(value) {
    return value == null || value === "" ? "—" : numberFormat.format(Number(value));
}

export function formatQuantity(value) {
    const formatted = formatNumber(value);
    return formatted === "—" ? formatted : `${formatted} kg`;
}

export function formatMoney(value) {
    const formatted = formatNumber(value);
    return formatted === "—" ? formatted : `${formatted} FCFA`;
}
