import { useState } from "react";
import { useTranslation } from "react-i18next";
import { DoorOpen, HardHat, Truck, UserRound } from "lucide-react";
import { LEVEL_META } from "../api/sitesecurityTypes";

/*
 * Conceptual perimeter diagram, NOT a geographic map. Colours are design tokens (CSS variables
 * of src/index.css); every label is translated.
 */
const BOUNDS =
  "M 96,320 C 88,206 150,118 262,96 C 372,74 540,88 620,158 C 678,214 686,344 626,422 C 570,496 452,532 330,514 C 214,497 104,434 96,320 Z";
const ZONE_RECTS = {
  controlled: { x: 215, y: 170, w: 350, h: 265, rx: 30, labelX: 390, labelY: 192, labelW: 150 },
  restricted: { x: 272, y: 210, w: 236, h: 170, rx: 20, labelX: 390, labelY: 356, labelW: 150 },
  critical: { x: 322, y: 252, w: 136, h: 78, rx: 14, labelX: 390, labelY: 292, labelW: 110 },
};
const GATES = [
  { id: "main", icon: DoorOpen, x: 465, y: 510, lx: 465, ly: 554, w: 150, attention: false },
  { id: "visitor", icon: UserRound, x: 96, y: 320, lx: 170, ly: 320, w: 120, attention: false },
  { id: "staff", icon: HardHat, x: 667, y: 282, lx: 587, ly: 236, w: 170, attention: false },
  { id: "service", icon: Truck, x: 452, y: 92, lx: 452, ly: 142, w: 180, attention: true },
];
const OPEN_ZONE = "open";
const INNER_ZONES = ["controlled", "restricted", "critical"];
const INK = "var(--color-text)";
const MUTED = "var(--color-text-muted)";
const LINE = "var(--color-border-divider)";
const PAPER = "var(--color-surface-raised)";

function LabelChip({ x, y, w, color, text, size = 10 }) {
  return (
    <g className="pointer-events-none select-none">
      <rect x={x - w / 2} y={y - 13} width={w} height={26} rx={13} fill={PAPER} stroke={color} strokeOpacity={0.5} />
      <circle cx={x - w / 2 + 14} cy={y} r={3} fill={color} />
      <text x={x - w / 2 + 22} y={y + 3.5} fontSize={size} fontWeight={600} fill={color} letterSpacing="0.08em">
        {text}
      </text>
    </g>
  );
}

export default function PerimeterMap({ selected, onSelect, interactive = true, compact = false }) {
  const { t } = useTranslation("sitesecurity");
  const [hover, setHover] = useState(null);
  const zoneProps = (id) => ({
    onClick: (e) => {
      e.stopPropagation();
      if (interactive) onSelect?.(id);
    },
    onMouseEnter: () => interactive && setHover(id),
    onMouseLeave: () => setHover(null),
    style: { cursor: interactive ? "pointer" : "default" },
    ...(interactive && {
      role: "button",
      tabIndex: 0,
      "aria-label": t("map.zoneLabel", { zone: t(`levels.${id}`) }),
      "aria-pressed": selected === id,
      className: "focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-focus-ring",
      onKeyDown: (e) => {
        if (e.key === "Enter" || e.key === " ") {
          e.preventDefault();
          onSelect?.(id);
        }
      },
    }),
  });
  const strokeFor = (id) => (selected === id ? 3 : hover === id ? 2.2 : 1.4);

  return (
    <svg viewBox="0 0 760 600" className="h-auto w-full" role="group" aria-label={t("map.label")}>
      <line x1="380" y1="56" x2="380" y2="544" stroke={LINE} strokeDasharray="2 7" />
      <line x1="60" y1="300" x2="700" y2="300" stroke={LINE} strokeDasharray="2 7" />

      <g {...zoneProps(OPEN_ZONE)}>
        <path d={BOUNDS} fill={LEVEL_META.open.fill} stroke="none" />
        <path d={BOUNDS} fill="none" stroke={LINE} strokeWidth={7} />
        <path
          d={BOUNDS}
          fill="none"
          stroke={INK}
          strokeWidth={selected === "open" || hover === "open" ? 6.5 : 5}
          strokeLinecap="round"
          strokeDasharray="0.1 21"
        />
      </g>
      <text x={366} y={72} fontSize={10} fontWeight={600} fill={MUTED} textAnchor="end" className="pointer-events-none">
        {t("map.boundary")}
      </text>

      {INNER_ZONES.map((id) => {
        const z = ZONE_RECTS[id];
        const meta = LEVEL_META[id];
        return (
          <g key={id} {...zoneProps(id)}>
            {selected === id && (
              <rect x={z.x - 5} y={z.y - 5} width={z.w + 10} height={z.h + 10} rx={z.rx + 5} fill="none" stroke={meta.solid} strokeOpacity={0.25} strokeWidth={7} />
            )}
            <rect
              x={z.x}
              y={z.y}
              width={z.w}
              height={z.h}
              rx={z.rx}
              fill={meta.fill}
              stroke={meta.solid}
              strokeOpacity={hover === id || selected === id ? 0.85 : 0.45}
              strokeWidth={strokeFor(id)}
            />
          </g>
        );
      })}

      <LabelChip x={150} y={172} w={110} color={LEVEL_META.open.solid} text={t("map.zone.open")} />
      {INNER_ZONES.map((id) => (
        <LabelChip
          key={id}
          x={ZONE_RECTS[id].labelX}
          y={ZONE_RECTS[id].labelY}
          w={ZONE_RECTS[id].labelW}
          color={LEVEL_META[id].solid}
          text={t(`map.zone.${id}`)}
        />
      ))}

      {GATES.map((gate) => {
        const Icon = gate.icon;
        return (
          <g key={gate.id}>
            <title>{t(`map.gates.${gate.id}`)}</title>
            <rect x={gate.x - 16} y={gate.y - 16} width={32} height={32} rx={9} fill={PAPER} stroke={INK} strokeWidth={1.4} />
            <svg x={gate.x - 9} y={gate.y - 9} width={18} height={18} viewBox="0 0 24 24">
              <Icon size={24} color="currentColor" style={{ color: INK }} strokeWidth={1.9} />
            </svg>
            <circle
              cx={gate.x + 13}
              cy={gate.y - 13}
              r={4.5}
              fill={gate.attention ? "var(--color-warning-fg)" : "var(--color-success-fg)"}
              stroke={PAPER}
              strokeWidth={1.6}
            />
            {!compact && <LabelChip x={gate.lx} y={gate.ly} w={gate.w} color={MUTED} text={t(`map.gates.${gate.id}`)} size={8.5} />}
          </g>
        );
      })}

      <g className="pointer-events-none select-none" transform="translate(706,58)">
        <circle r="16" fill={PAPER} stroke={LINE} />
        <path d="M 0,-9 L 4.5,4 L 0,1.2 L -4.5,4 Z" fill={INK} />
        <text y="30" textAnchor="middle" fontSize={9} fontWeight={600} fill={MUTED}>
          {t("map.north")}
        </text>
      </g>
      <text x={28} y={582} fontSize={8.5} fill={MUTED} className="pointer-events-none">
        {t("map.notToScale")}
      </text>
      <text x={734} y={584} textAnchor="end" fontSize={8.5} fill={MUTED} className="pointer-events-none">
        {t("map.validationRequired")}
      </text>
    </svg>
  );
}
