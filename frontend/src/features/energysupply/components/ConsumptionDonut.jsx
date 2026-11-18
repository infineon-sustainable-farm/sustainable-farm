import { useTranslation } from "react-i18next";
import { SERIES_COLORS, SERIES_SWATCHES } from "../chartStyles";
import { useEnergyFormat, useEnumLabels } from "../format";

// Geometry of the ring, in SVG user units of its 180 x 180 view box.
const SIZE = 180;
const CENTER = SIZE / 2;
const RADIUS = 70;
const STROKE = 26;
const CIRCUMFERENCE = 2 * Math.PI * RADIUS;

/** Share of today's consumption per load category, as a ring with its legend. */
export default function ConsumptionDonut({ data, total }) {
  const { t } = useTranslation("energysupply");
  const format = useEnergyFormat();
  const labels = useEnumLabels();
  const colorAt = (index) => SERIES_COLORS[index % SERIES_COLORS.length];
  const share = (value) => (total ? Math.round((value / total) * 100) : 0);

  return (
    <div className="flex flex-col items-center gap-6 sm:flex-row sm:items-center sm:justify-center">
      <svg
        className="size-44 shrink-0"
        viewBox={`0 0 ${SIZE} ${SIZE}`}
        role="img"
        aria-label={t("consumption.donutLabel")}
      >
        <g transform={`translate(${CENTER},${CENTER}) rotate(-90)`}>
          {data.map((d, i) => {
            const frac = total ? d.kwhConsumed / total : 0;
            const offset = data.slice(0, i).reduce((sum, item) => sum + (total ? item.kwhConsumed / total : 0), 0);
            return (
              <circle
                key={d.loadCategory}
                r={RADIUS}
                fill="none"
                stroke={colorAt(i)}
                strokeWidth={STROKE}
                strokeDasharray={`${frac * CIRCUMFERENCE} ${CIRCUMFERENCE - frac * CIRCUMFERENCE}`}
                strokeDashoffset={-offset * CIRCUMFERENCE}
              />
            );
          })}
        </g>
        <text x={CENTER} y={CENTER - 6} textAnchor="middle" className="fill-text text-2xl font-bold">
          {format.number(total)}
        </text>
        <text x={CENTER} y={CENTER + 12} textAnchor="middle" className="fill-text-muted text-xs">
          {t("consumption.kwhToday")}
        </text>
      </svg>
      <ul className="grid gap-2">
        {data.map((d, i) => (
          <li key={d.loadCategory} className="flex items-center gap-2 text-sm">
            <span aria-hidden="true" className={`size-3 shrink-0 rounded-sm ${SERIES_SWATCHES[i % SERIES_SWATCHES.length]}`} />
            <span>{labels.loadCategory(d.loadCategory)}</span>
            <span className="ml-auto pl-4 font-semibold text-text-muted tabular-nums">
              {t("units.percent", { value: format.number(share(d.kwhConsumed)) })}
            </span>
          </li>
        ))}
      </ul>
    </div>
  );
}
