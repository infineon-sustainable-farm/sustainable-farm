// @vitest-environment jsdom
import { fireEvent, render, screen, within } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import LowStockNotifications from "./LowStockNotifications";

vi.mock("../hooks/useLowStockSpareParts", () => ({
  useLowStockSpareParts: () => ({
    data: [
      { id: 1, name: "Oil filter", quantity: 2, reorderThreshold: 5 },
      { id: 2, name: "Belt", quantity: 0, reorderThreshold: 1 },
    ],
  }),
}));

describe("LowStockNotifications", () => {
  it("opens with the count and the parts, and dismisses them one by one or all", () => {
    render(<LowStockNotifications />);
    const toggle = screen.getByRole("button", { name: /Notifications/ });
    expect(toggle).toHaveTextContent("2");
    expect(toggle).toHaveAttribute("aria-expanded", "true");
    expect(screen.getByText("2 in stock — reorder threshold 5")).toBeInTheDocument();

    const [firstDelete] = screen.getAllByRole("button", { name: "Delete notification" });
    fireEvent.click(firstDelete);
    expect(screen.queryByText("Oil filter")).not.toBeInTheDocument();
    expect(toggle).toHaveTextContent("1");
    expect(JSON.parse(localStorage.getItem("machinery.dismissed-low-stock"))).toEqual({ 1: 2 });

    // Once every notification is dismissed, the panel that opened on its own closes.
    fireEvent.click(screen.getByRole("button", { name: "Delete all" }));
    expect(within(toggle).queryByText("1")).not.toBeInTheDocument();
    expect(toggle).toHaveAttribute("aria-expanded", "false");
    fireEvent.click(toggle);
    expect(screen.getByText("No low stock notifications.")).toBeInTheDocument();
  });

  it("closes on a click outside and stays closed", () => {
    render(<LowStockNotifications />);
    fireEvent.mouseDown(document.body);
    expect(screen.queryByText("Low stock notifications", { selector: "h2" })).not.toBeInTheDocument();
    expect(screen.getByRole("button", { name: /Notifications/ })).toHaveAttribute("aria-expanded", "false");
  });
});
