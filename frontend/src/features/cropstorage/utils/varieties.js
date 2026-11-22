// Mango varieties come from the Plants catalogue (key + name). Cultivar names (Kent, Keitt,
// Amelie, Tommy Atkins...) are proper names and stay as Plants names them; the catalogue
// entries whose name is an English word are shown translated (cropstorage:varieties.<KEY>).
const TRANSLATED_VARIETY_KEYS = new Set(["OTHER"]);

/** Label of a catalogue variety: translated for the word entries, the Plants name otherwise. */
export function varietyLabel(t, varietyKey, varietyName) {
    const key = varietyKey == null ? "" : String(varietyKey).toUpperCase();
    if (t && TRANSLATED_VARIETY_KEYS.has(key)) {
        return t(`varieties.${key}`, { defaultValue: varietyName ?? key });
    }
    return varietyName ?? varietyKey ?? null;
}
