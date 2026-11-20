import { useEffect, useId, useState } from "react";
import { RotateCcw } from "lucide-react";
import { Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";
import { useTranslation } from "react-i18next";
import { simulateWhatIf } from "../api/energyApi";
import { AXIS_TICK, BAR_RADIUS, GRID_STROKE, TOOLTIP_STYLE } from "../chartStyles";
import { useEnergyFormat } from "../format";
import { Button, Card, PageHeader } from "../../../shared/ui";
import InlineAlert from "./InlineAlert";

const DEFAULTS = { additionalPanels: 40, additionalBatteryKwh: 80, productionScaleKgPerDay: 500 };
const DEBOUNCE_MS = 300; // so we don't fire a request on every pixel of slider drag

function SliderField({ label, value, onChange, min, max, step, unitKey }) {
  const { t } = useTranslation("energysupply");
  const format = useEnergyFormat();
  const id = useId();
  const withUnit = (amount) => t(unitKey, { value: format.grouped(amount) });
  return (
    <div>
      <div className="flex items-center justify-between">
        <label htmlFor={id} className="text-sm font-medium">{label}</label>
        <span className="text-sm font-bold text-text-accent tabular-nums">{withUnit(value)}</span>
      </div>
      <input
        id={id}
        type="range"
        min={min}
        max={max}
        step={step}
        value={value}
        onChange={(e) => onChange(Number(e.target.value))}
        className="mt-3 h-2 w-full cursor-pointer appearance-none rounded-pill bg-neutral-bg accent-primary"
      />
      <div className="mt-1 flex justify-between text-xs text-text-muted">
        <span>{withUnit(min)}</span>
        <span>{withUnit(max)}</span>
      </div>
    </div>
  );
}

function Row({ label, value, noBorder }) {
  return (
    <div className={`flex items-center justify-between py-3 ${noBorder ? "" : "border-b border-border-subtle"}`}>
      <span className="text-sm text-text-muted">{label}</span>
      <span className="text-sm font-bold tabular-nums text-text-accent">{value}</span>
    </div>
  );
}

export default function WhatIfAnalysis() {
  const { t } = useTranslation("energysupply");
  const format = useEnergyFormat();
  const [inputs, setInputs] = useState(DEFAULTS);
  const [result, setResult] = useState(null);
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let cancelled = false;
    const timeout = setTimeout(() => {
      setLoading(true);
      setError(null);
      simulateWhatIf(inputs)
        .then((data) => {
          if (!cancelled) setResult(data);
        })
        .catch((err) => {
          if (!cancelled) setError(err.message);
        })
        .finally(() => {
          if (!cancelled) setLoading(false);
        });
    }, DEBOUNCE_MS);
    return () => {
      cancelled = true;
      clearTimeout(timeout);
    };
  }, [inputs]);

  const setField = (field) => (value) => {
    setResult(null);
    setError(null);
    setInputs((prev) => ({ ...prev, [field]: value }));
  };
  const resetScenario = () => {
    setResult(null);
    setError(null);
    setInputs({ ...DEFAULTS });
  };

  const chartData = result
    ? [
        { metric: t("whatIf.chartPvGeneration"), value: result.additionalPvGenerationKwhPerYear },
        { metric: t("whatIf.chartProductionDemand"), value: result.additionalProductionDemandKwhPerYear },
        { metric: t("whatIf.chartNetBalance"), value: result.netAnnualEnergyBalanceKwh },
      ]
    : [];

  function renderOutcome() {
    if (loading || (!result && !error)) return <p role="status" className="text-sm text-text-muted">{t("whatIf.calculating")}</p>;
    if (!result) return <p className="text-sm text-error-fg">{t("whatIf.noResult")}</p>;
    return (
      <div>
        <Row
          label={t("whatIf.newCapacity")}
          value={t("units.kwpValue", { value: result.newSystemCapacityKwp == null ? "" : format.fixed(result.newSystemCapacityKwp) })}
        />
        <Row
          label={t("whatIf.additionalCapex")}
          value={t("units.euro", { value: result.additionalCapexEur == null ? "" : format.grouped(result.additionalCapexEur) })}
        />
        <Row
          label={t("whatIf.payback")}
          value={
            result.newPaybackYears == null
              ? t("whatIf.notAvailable")
              : t("units.yearsValue", { value: format.fixed(result.newPaybackYears) })
          }
        />
        <Row
          label={t("whatIf.productionDemand")}
          value={t("units.kwhPerYearValue", { value: format.grouped(result.additionalProductionDemandKwhPerYear) })}
        />
        <Row
          label={t("whatIf.netEnergyBalance")}
          value={t("units.kwhPerYearValue", { value: format.grouped(result.netAnnualEnergyBalanceKwh) })}
        />
        <Row
          label={result.netAnnualCo2AvoidedKg >= 0 ? t("whatIf.co2Avoided") : t("whatIf.co2Increase")}
          value={t("units.kgPerYearValue", {
            value: result.netAnnualCo2AvoidedKg == null ? "" : format.grouped(Math.abs(result.netAnnualCo2AvoidedKg)),
          })}
          noBorder
        />
      </div>
    );
  }

  return (
    <div className="space-y-6 p-6">
      <PageHeader title={t("whatIf.title")} subtitle={t("whatIf.subtitle")} />

      {error && <InlineAlert>{t("whatIf.simulationFailed", { error })}</InlineAlert>}

      <div className="grid gap-6 lg:grid-cols-5">
        <Card
          className="lg:col-span-2"
          title={t("whatIf.inputsTitle")}
          actions={
            <Button variant="ghost" size="sm" icon={RotateCcw} onClick={resetScenario}>
              {t("whatIf.reset")}
            </Button>
          }
        >
          <div className="grid gap-7">
            <SliderField
              label={t("whatIf.additionalPanels")}
              value={inputs.additionalPanels}
              onChange={setField("additionalPanels")}
              min={0}
              max={200}
              step={5}
              unitKey="units.panelsValue"
            />
            <SliderField
              label={t("whatIf.additionalBattery")}
              value={inputs.additionalBatteryKwh}
              onChange={setField("additionalBatteryKwh")}
              min={0}
              max={400}
              step={10}
              unitKey="units.kwhValue"
            />
            <SliderField
              label={t("whatIf.productionScale")}
              value={inputs.productionScaleKgPerDay}
              onChange={setField("productionScaleKgPerDay")}
              min={50}
              max={1000}
              step={50}
              unitKey="units.kgPerDayValue"
            />
          </div>
        </Card>

        <Card className="lg:col-span-3" title={t("whatIf.outcomeTitle")}>
          {renderOutcome()}
        </Card>
      </div>

      {result && (
        <Card title={t("whatIf.impactTitle")}>
          <div className="h-72 w-full">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={chartData}>
                <CartesianGrid stroke={GRID_STROKE} vertical={false} />
                <XAxis dataKey="metric" tick={AXIS_TICK} />
                <YAxis tick={AXIS_TICK} tickFormatter={(value) => format.number(value)} />
                <Tooltip
                  contentStyle={TOOLTIP_STYLE}
                  formatter={(value) => [format.number(value), t("whatIf.valueSeries")]}
                />
                <Bar dataKey="value" fill="var(--color-chart-1)" radius={BAR_RADIUS} isAnimationActive={false} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </Card>
      )}
    </div>
  );
}
