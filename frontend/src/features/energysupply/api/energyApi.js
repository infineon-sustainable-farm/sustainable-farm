const BASE_URL = "http://localhost:8080/api/energy";

async function getJson(url, options) {
  const res = await fetch(url, options);
  if (!res.ok) {
    throw new Error(`Request failed: ${res.status} ${res.statusText}`);
  }
  return res.json();
}

// Dashboard
export function getDashboardSummary() {
  return getJson(`${BASE_URL}/dashboard/summary`);
}

// Components (used for the "System Status Overview" table)
export function getComponents() {
  return getJson(`${BASE_URL}/components`);
}

// Solar & Generation
export function getSolarLast7Days() {
  return getJson(`${BASE_URL}/solar/last-7-days`);
}
export function getAllSolarLogs() {
  return getJson(`${BASE_URL}/solar`);
}

// Consumption
export function getConsumptionToday() {
  return getJson(`${BASE_URL}/consumption/today`);
}
export function getAllConsumption() {
  return getJson(`${BASE_URL}/consumption`);
}
export function getConsumptionByCategory(category) {
  return getJson(`${BASE_URL}/consumption/breakdown/${category}`);
}

// Generator
export function getGeneratorEvents() {
  return getJson(`${BASE_URL}/generator/events`);
}
export function createGeneratorEvent(event) {
  return getJson(`${BASE_URL}/generator/events`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(event),
  });
}

// Carbon
export function getCarbonMetrics() {
  return getJson(`${BASE_URL}/carbon`);
}

// Alerts
export function getAllAlerts() {
  return getJson(`${BASE_URL}/alerts`);
}
export function getOpenAlerts() {
  return getJson(`${BASE_URL}/alerts/open`);
}

// What-If simulation
export function simulateWhatIf({ additionalPanels, additionalBatteryKwh, productionScaleKgPerDay }) {
  return getJson(`${BASE_URL}/whatif/simulate`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ additionalPanels, additionalBatteryKwh, productionScaleKgPerDay }),
  });
}
