import { useState } from "react";
import {
  LayoutDashboard,
  Sun,
  Gauge,
  Fuel,
  Leaf,
  SlidersHorizontal,
  AlertTriangle,
  Menu,
  Settings,
} from "lucide-react";
import Dashboard from "./Dashboard";
import EnergyComponents from "./EnergyComponents";
import SolarGeneration from "./SolarGeneration";
import ConsumptionTracking from "./ConsumptionTracking";
import GeneratorBackup from "./GeneratorBackup";
import CarbonFootprint from "./CarbonFootprint";
import WhatIfAnalysis from "./WhatIfAnalysis";
import AlertsNotifications from "./AlertsNotifications";
import "../theme/theme-additions.css";

const NAV_ITEMS = [
  { key: "dashboard", label: "Dashboard", icon: LayoutDashboard },
  { key: "components", label: "Components", icon: Settings },
  { key: "solar", label: "Solar & Generation", icon: Sun },
  { key: "consumption", label: "Consumption Tracking", icon: Gauge },
  { key: "generator", label: "Generator Backup", icon: Fuel },
  { key: "carbon", label: "Carbon Footprint", icon: Leaf },
  { key: "scenario", label: "What-If Analysis", icon: SlidersHorizontal },
  { key: "alerts", label: "Alerts & Notifications", icon: AlertTriangle },
];

const TITLES = {
  dashboard: "System Overview",
  components: "Energy Components",
  solar: "Solar & Generation Monitoring",
  consumption: "Energy Consumption Tracking",
  generator: "Generator Backup Management",
  carbon: "Carbon Footprint",
  scenario: "What-If Scenario Analysis",
  alerts: "Alerts & Notifications",
};

export default function EnergyApp() {
  const [activeTab, setActiveTab] = useState("dashboard");
  const [sidebarOpen, setSidebarOpen] = useState(false);

  function renderTab() {
    switch (activeTab) {
      case "dashboard":
        return <Dashboard />;
      case "components":
        return <EnergyComponents />;
      case "solar":
        return <SolarGeneration />;
      case "consumption":
        return <ConsumptionTracking />;
      case "generator":
        return <GeneratorBackup />;
      case "carbon":
        return <CarbonFootprint />;
      case "scenario":
        return <WhatIfAnalysis />;
      case "alerts":
        return <AlertsNotifications />;
      default:
        return <Dashboard />;
    }
  }

  return (
    <div className="energy-supply-theme flex min-h-screen bg-background text-foreground">
      {/* Mobile overlay */}
      {sidebarOpen && (
        <button
          aria-label="Close navigation"
          className="fixed inset-0 z-30 bg-black/50 lg:hidden"
          onClick={() => setSidebarOpen(false)}
        />
      )}

      {/* Sidebar */}
      <aside
        className={`fixed inset-y-0 left-0 z-40 flex w-[240px] flex-col bg-sidebar text-sidebar-foreground transition-transform lg:static lg:translate-x-0 ${
          sidebarOpen ? "translate-x-0" : "-translate-x-full"
        }`}
      >
        <div className="flex flex-col items-center gap-2 border-b border-sidebar-border px-5 py-5 text-center">
          <div className="flex size-[100px] shrink-0 items-center justify-center overflow-hidden rounded-full bg-white">
            <img src="/logo.webp" alt="Farm logo" className="h-auto max-h-[80px] w-auto max-w-[80px] object-contain" />
          </div>
          <div className="min-w-0">
            <p className="font-heading text-sm font-bold uppercase tracking-[0.05em] text-white">CropCore</p>
            <p className="truncate text-xs text-sidebar-foreground/70">Module 05 · Energy</p>
          </div>
        </div>

        <nav className="flex-1 overflow-y-auto px-3 py-4">
          <p className="px-3 pb-2 text-[12px] font-semibold uppercase tracking-[0.1em] text-sidebar-foreground/50">
            Navigation
          </p>
          <ul className="grid gap-1">
            {NAV_ITEMS.map(({ key, label, icon: Icon }) => {
              const active = activeTab === key;
              return (
                <li key={key}>
                  <button
                    onClick={() => {
                      setActiveTab(key);
                      setSidebarOpen(false);
                    }}
                    className={`group flex w-full items-center gap-3 rounded-md px-3 py-2.5 text-left text-sm font-medium transition-colors ${
                      active
                        ? "bg-white/15 text-white shadow-sm"
                        : "text-sidebar-foreground/85 hover:bg-white/10 hover:text-white"
                    }`}
                  >
                    <Icon size={18} className="shrink-0" />
                    <span className="flex-1 truncate">{label}</span>
                  </button>
                </li>
              );
            })}
          </ul>
        </nav>

        <div className="border-t border-sidebar-border px-5 py-4">
          <div className="flex items-center gap-2 text-xs text-sidebar-foreground/70">
            Site location · Banfora, Burkina Faso
          </div>
        </div>
      </aside>

      {/* Main */}
      <div className="flex min-w-0 flex-1 flex-col">
        <header className="sticky top-0 z-20 flex items-center gap-4 border-b border-border bg-card px-4 py-3 md:px-6">
          <button
            aria-label="Open navigation"
            className="rounded-md p-2 text-muted-foreground hover:bg-muted lg:hidden"
            onClick={() => setSidebarOpen(true)}
          >
            <Menu size={20} />
          </button>
          <div className="min-w-0 flex-1">
            <p className="text-[12px] font-semibold uppercase tracking-[0.1em] text-primary">
              Energy Supply Systems
            </p>
            <h1 className="truncate font-heading text-[18px] font-bold uppercase tracking-[0.04em] text-foreground">
              {TITLES[activeTab]}
            </h1>
          </div>
          <div className="flex items-center gap-3">
            <div className="hidden text-right sm:block">
              <p className="text-sm font-semibold text-foreground">MADIEGA S Aida Justine</p>
              <p className="text-xs text-muted-foreground">Energy Supply Systems</p>
            </div>
            <div className="flex size-10 items-center justify-center rounded-full bg-primary font-heading text-sm font-bold text-primary-foreground">
              AM
            </div>
          </div>
        </header>

        <main className="flex-1 overflow-y-auto p-4 md:p-6">{renderTab()}</main>
      </div>
    </div>
  );
}
