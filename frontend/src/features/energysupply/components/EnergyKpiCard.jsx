import { KpiCard } from "../../../shared/ui";

/** Key figure of an energy screen: the kit tile, with the unit after the value in smaller text. */
function EnergyKpiCard({ label, value, unit, icon }) {
  return (
    <KpiCard
      icon={icon}
      label={label}
      value={
        <>
          {value}
          {unit && <span className="ml-1 text-base font-semibold text-text-muted">{unit}</span>}
        </>
      }
    />
  );
}
export default EnergyKpiCard;
