import { useEffect, useState } from "react";
import { RotateCcw } from "lucide-react";
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from "recharts";
import { simulateWhatIf } from "../api/energyApi";

// WhatIfResponseDto (backend) currently exposes only "proposed" values —
// there is no baseline/current scenario in the response:
//   additionalCapexEur, newPaybackYears, newCo2ReductionPct, newSystemCapacityKwp
// A "Current vs Proposed" comparison isn't possible until the backend
// adds baseline fields. This version shows only the proposed outcome.

const DEFAULTS = { additionalPanels: 40, additionalBatteryKwh: 80, productionScaleKgPerDay: 500 };

function SliderField({ label, value, onChange, min, max, step, unit }) {
  return (
    <div>
      <div className="flex items-center justify-between">
        <label className="text-sm font-medium text-foreground">{label}</label>
        <span className="font-heading text-sm font-bold text-primary tabular-nums">{value.toLocaleString()} {unit}</span>
      </div>
      <input
        type="range" min={min} max={max} step={step} value={value}
        onChange={(e) => onChange(Number(e.target.value))}
        className="mt-3 h-2 w-full cursor-pointer appearance-none rounded-full bg-muted accent-primary"
      />
      <div className="mt-1 flex justify-between text-[12px] text-muted-foreground">
        <span>{min} {unit}</span><span>{max} {unit}</span>
      </div>
    </div>
  );
}

function Row({ label, value, noBorder }) {
  return (
    <div className={`flex items-center justify-between py-3 ${noBorder ? "" : "border-b border-border"}`}>
      <span className="text-sm text-muted-foreground">{label}</span>
      <span className="font-heading text-sm font-bold tabular-nums text-primary">{value}</span>
    </div>
  );
}

export default function WhatIfAnalysis() {
  const [inputs, setInputs] = useState(DEFAULTS);
  const [result, setResult] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    const timeout = setTimeout(() => {
      simulateWhatIf(inputs)
        .then(setResult)
        .catch((err) => setError(err.message));
    }, 300); // debounce so we don't fire a request on every pixel of slider drag
    return () => clearTimeout(timeout);
  }, [inputs]);

  const setField = (field) => (value) => setInputs((prev) => ({ ...prev, [field]: value }));

  const chartData = result
    ? [
        { metric: "Capacity (kWp)", value: result.newSystemCapacityKwp },
        { metric: "CO₂ Reduction (%)", value: result.newCo2ReductionPct },
        { metric: "Payback (yrs)", value: result.newPaybackYears },
      ]
    : [];

  return (
    <section>
      <div className="mb-5">
        <h1 className="font-heading text-[24px] font-bold uppercase tracking-[0.05em] text-foreground">What-If Scenario Analysis</h1>
        <p className="mt-1 text-sm leading-relaxed text-muted-foreground">
          Model an expansion of the energy system and see the resulting capacity, cost, payback, and emissions.
        </p>
      </div>

      {error && <p className="mb-4 text-sm text-destructive">Simulation failed: {error}</p>}

      <div className="grid gap-6 lg:grid-cols-5">
        <div className="rounded-lg border border-border bg-card shadow-sm border-t-[3px] border-t-primary lg:col-span-2">
          <div className="flex items-center justify-between gap-3 border-b border-border px-5 py-3.5">
            <h2 className="font-heading text-[13px] font-bold uppercase tracking-[0.05em] text-foreground">Scenario Inputs</h2>
            <button onClick={() => setInputs(DEFAULTS)} className="inline-flex items-center gap-1.5 rounded-md px-2.5 py-1 text-xs font-semibold text-primary hover:bg-primary/10">
              <RotateCcw className="size-3.5" />Reset
            </button>
          </div>
          <div className="grid gap-7 p-5">
            <SliderField label="Additional solar panels" value={inputs.additionalPanels} onChange={setField("additionalPanels")} min={0} max={200} step={5} unit="panels" />
            <SliderField label="Additional battery capacity" value={inputs.additionalBatteryKwh} onChange={setField("additionalBatteryKwh")} min={0} max={400} step={10} unit="kWh" />
            <SliderField label="Production scale-up" value={inputs.productionScaleKgPerDay} onChange={setField("productionScaleKgPerDay")} min={50} max={1000} step={50} unit="kg/day" />
          </div>
        </div>

        <div className="rounded-lg border border-border bg-card shadow-sm border-t-[3px] border-t-primary lg:col-span-3">
          <div className="border-b border-border px-5 py-3.5">
            <h2 className="font-heading text-[13px] font-bold uppercase tracking-[0.05em] text-foreground">Projected Outcome</h2>
          </div>
          {!result ? (
            <p className="p-5 text-sm text-muted-foreground">Calculating...</p>
          ) : (
            <div className="p-5">
              <Row label="New System Capacity" value={`${result.newSystemCapacityKwp?.toFixed(1)} kWp`} />
              <Row label="Additional CAPEX" value={`€${result.additionalCapexEur?.toLocaleString()}`} />
              <Row label="Payback Period" value={`${result.newPaybackYears?.toFixed(1)} yrs`} />
              <Row label="CO₂ Reduction" value={`${result.newCo2ReductionPct}%`} noBorder />
            </div>
          )}
        </div>
      </div>

      {result && (
        <div className="mt-6 rounded-lg border border-border bg-card shadow-sm border-t-[3px] border-t-primary">
          <div className="border-b border-border px-5 py-3.5">
            <h2 className="font-heading text-[13px] font-bold uppercase tracking-[0.05em] text-foreground">Impact Overview</h2>
          </div>
          <div className="p-5" style={{ height: 280 }}>
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={chartData}>
                <CartesianGrid stroke="#dbe3e1" vertical={false} />
                <XAxis dataKey="metric" tick={{ fontSize: 11 }} />
                <YAxis tick={{ fontSize: 11 }} />
                <Tooltip />
                <Bar dataKey="value" fill="#4caf50" radius={[4, 4, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>
      )}
    </section>
  );
}