import { describe, expect, it } from "vitest";
import { buildStatCards } from "./dashboardStats";

const STATS = { activePoints: 3, controlledZones: 3, totalZones: 4, visitors: 7, alerts: 2 };
const byId = (cards, id) => cards.find((card) => card.id === id);

describe("buildStatCards", () => {
  it("shows the values of the API", () => {
    const cards = buildStatCards(STATS, 5);

    expect(byId(cards, "points")).toMatchObject({ value: "3", hintValues: { count: 5 } });
    expect(byId(cards, "zones")).toMatchObject({ value: "3", hintValues: { count: 4 } });
    expect(byId(cards, "visitors").value).toBe("7");
    expect(byId(cards, "denied").value).toBe("2");
  });

  it("shows a real zero", () => {
    const cards = buildStatCards({ ...STATS, visitors: 0, alerts: 0 }, 0);

    expect(byId(cards, "visitors").value).toBe("0");
    expect(byId(cards, "denied")).toMatchObject({ value: "0", alert: false });
  });

  it("shows no number before the API has answered", () => {
    for (const card of buildStatCards(null, 0)) {
      expect(card.value).toBe("—");
    }
    expect(byId(buildStatCards(null, 0), "zones").hintKey).toBeNull();
  });

  it("flags denied events as an alert", () => {
    expect(byId(buildStatCards(STATS, 5), "denied").alert).toBe(true);
  });

  it("does not claim a live presence count", () => {
    expect(byId(buildStatCards(STATS, 5), "visitors").hintKey).toBe("dashboard.stats.visitorsHint");
  });
});
