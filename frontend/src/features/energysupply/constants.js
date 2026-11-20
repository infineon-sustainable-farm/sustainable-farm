// Enum values of the energy supply API (backend entity enums), in the order of the forms.
export const COMPONENT_CATEGORIES = ["PV_ARRAY", "BATTERY", "INVERTER", "GENERATOR", "MONITORING"];
export const COMPONENT_STATUSES = ["OPERATIONAL", "UNDER_MAINTENANCE", "OUT_OF_SERVICE"];
export const LOAD_CATEGORIES = ["IRRIGATION", "STORAGE_COOLING", "LIGHTING", "SECURITY", "MONITORING", "PROCESSING"];
export const ENERGY_SOURCES = ["SOLAR", "BATTERY", "GENERATOR", "GRID"];

/** Next free registry id after the highest ENG-nnn, or "" once ENG-999 is used. */
export function nextComponentId(components) {
  const highestId = components.reduce((highest, component) => {
    const match = /^ENG-(\d{3})$/.exec(component.componentId);
    return match ? Math.max(highest, Number(match[1])) : highest;
  }, 0);
  return highestId < 999 ? `ENG-${String(highestId + 1).padStart(3, "0")}` : "";
}
