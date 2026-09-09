import { useState } from "react";
import { DoorOpen, HardHat, Truck, UserRound } from "lucide-react";
import { LEVEL_META, type LevelKey } from "../api/sitesecurityTypes";

/* ------------------------------------------------------------------ */
/*  Conceptual perimeter diagram — NOT a geographic map.               */
/* ------------------------------------------------------------------ */

const BOUNDS =
  "M 96,320 C 88,206 150,118 262,96 C 372,74 540,88 620,158 C 678,214 686,344 626,422 C 570,496 452,532 330,514 C 214,497 104,434 96,320 Z";

interface ZoneRect {
  x: number;
  y: number;
  w: number;
  h: number;
  rx: number;
  labelX: number;
  labelY: number;
  labelW: number;
}

const ZONE_RECTS: Record<"controlled" | "restricted" | "critical", ZoneRect> = {
  controlled: { x: 215, y: 170, w: 350, h: 265, rx: 30, labelX: 390, labelY: 192, labelW: 140 },
  restricted: { x: 272, y: 210, w: 236, h: 170, rx: 20, labelX: 390, labelY: 356, labelW: 142 },
  critical: { x: 322, y: 252, w: 136, h: 78, rx: 14, labelX: 390, labelY: 292, labelW: 92 },
};

const GATES = [
  { id: "main", name: "Main Entrance", icon: DoorOpen, x: 465, y: 510, dot: "#10B981", lx: 465, ly: 554, w: 128, anchor: "middle" as const },
  { id: "visitor", name: "Visitor Gate", icon: UserRound, x: 96, y: 320, dot: "#10B981", lx: 162, ly: 320, w: 100, anchor: "middle" as const },
  { id: "staff", name: "Staff / Operational Gate", icon: HardHat, x: 667, y: 282, dot: "#10B981", lx: 587, ly: 236, w: 158, anchor: "middle" as const },
  { id: "service", name: "Service / Emergency Gate", icon: Truck, x: 452, y: 92, dot: "#D97706", lx: 452, ly: 142, w: 168, anchor: "middle" as const },
];

function LabelChip({
  x,
  y,
  w,
  color,
  text,
  size = 9.5,
}: {
  x: number;
  y: number;
  w: number;
  color: string;
  text: string;
  size?: number;
}) {
  return (
    <g className="pointer-events-none select-none">
      <rect x={x - w / 2} y={y - 13} width={w} height={26} rx={13} fill="white" stroke={color} strokeOpacity={0.5} strokeWidth={1} />
      <circle cx={x - w / 2 + 14} cy={y} r={3} fill={color} />
      <text x={x - w / 2 + 22} y={y + 3.5} className="svg-label" fontSize={size} fontWeight={600} fill={color}>
        {text}
      </text>
    </g>
  );
}

export default function PerimeterMap({
  selected,
  onSelect,
  interactive = true,
  compact = false,
}: {
  selected: LevelKey | null;
  onSelect?: (zone: LevelKey) => void;
  interactive?: boolean;
  compact?: boolean;
}) {
  const [hover, setHover] = useState<LevelKey | null>(null);

  const zoneProps = (id: LevelKey) => ({
    onClick: (e: React.MouseEvent) => {
      e.stopPropagation();
      if (interactive) onSelect?.(id);
    },
    onMouseEnter: () => interactive && setHover(id),
    onMouseLeave: () => setHover(null),
    style: { cursor: interactive ? "pointer" : "default" },
  });

  const strokeFor = (id: LevelKey) => {
    if (selected === id) return 3;
    if (hover === id) return 2.2;
    return 1.4;
  };

  return (
    <div className="relative">
      <svg viewBox="0 0 760 600" className="h-auto w-full" role="img" aria-label="Conceptual perimeter diagram">
        <defs>
          <pattern id="hatch-amber" width="9" height="9" patternTransform="rotate(45)" patternUnits="userSpaceOnUse">
            <line x1="0" y1="0" x2="0" y2="9" stroke="#D97706" strokeOpacity="0.14" strokeWidth="3.5" />
          </pattern>
          <pattern id="hatch-rose" width="8" height="8" patternTransform="rotate(45)" patternUnits="userSpaceOnUse">
            <line x1="0" y1="0" x2="0" y2="8" stroke="#E11D48" strokeOpacity="0.16" strokeWidth="3" />
          </pattern>
        </defs>

        {/* centre guides */}
        <line x1="380" y1="56" x2="380" y2="544" stroke="#E2E8F0" strokeWidth={1} strokeDasharray="2 7" />
        <line x1="60" y1="300" x2="700" y2="300" stroke="#E2E8F0" strokeWidth={1} strokeDasharray="2 7" />

        {/* FARM BOUNDARY / OPEN ZONE */}
        <g {...zoneProps("open")}>
          <path d={BOUNDS} fill={LEVEL_META.open.fill} stroke="none" />
          <path d={BOUNDS} fill="none" stroke="#CBD5E1" strokeWidth={7} />
          <path
            d={BOUNDS}
            fill="none"
            stroke="#101B2E"
            strokeWidth={selected === "open" || hover === "open" ? 6.5 : 5}
            strokeLinecap="round"
            strokeDasharray="0.1 21"
          />
          {/* patrol sweep highlight */}
          <path d={BOUNDS} fill="none" stroke="#B23A68" strokeOpacity={0.5} strokeWidth={2.2} strokeLinecap="round" className="fence-sweep" />
        </g>

        <text x={366} y={72} className="svg-label pointer-events-none" fontSize={9.5} fontWeight={600} fill="#94A3B8" textAnchor="end">
          FARM BOUNDARY
        </text>

        {/* inner zones (drawn outermost → innermost so clicks hit correctly) */}
        {(["controlled", "restricted", "critical"] as const).map((id) => {
          const z = ZONE_RECTS[id];
          const meta = LEVEL_META[id];
          return (
            <g key={id} {...zoneProps(id)}>
              {selected === id && (
                <rect
                  x={z.x - 5}
                  y={z.y - 5}
                  width={z.w + 10}
                  height={z.h + 10}
                  rx={z.rx + 5}
                  fill="none"
                  stroke={meta.solid}
                  strokeOpacity={0.25}
                  strokeWidth={7}
                />
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
              {id !== "controlled" && (
                <rect
                  x={z.x}
                  y={z.y}
                  width={z.w}
                  height={z.h}
                  rx={z.rx}
                  fill={`url(#hatch-${id === "restricted" ? "amber" : "rose"})`}
                  className="pointer-events-none"
                />
              )}
            </g>
          );
        })}

        {/* zone labels */}
        <LabelChip x={150} y={172} w={96} color={LEVEL_META.open.solid} text="OPEN ZONE" />
        <LabelChip
          x={ZONE_RECTS.controlled.labelX}
          y={ZONE_RECTS.controlled.labelY}
          w={ZONE_RECTS.controlled.labelW}
          color={LEVEL_META.controlled.solid}
          text="CONTROLLED ZONE"
        />
        <LabelChip
          x={ZONE_RECTS.restricted.labelX}
          y={ZONE_RECTS.restricted.labelY}
          w={ZONE_RECTS.restricted.labelW}
          color={LEVEL_META.restricted.solid}
          text="RESTRICTED ZONE"
        />
        <LabelChip
          x={ZONE_RECTS.critical.labelX}
          y={ZONE_RECTS.critical.labelY}
          w={ZONE_RECTS.critical.labelW}
          color={LEVEL_META.critical.solid}
          text="CRITICAL"
        />

        {/* gates */}
        {GATES.map((g) => {
          const Icon = g.icon;
          return (
            <g key={g.id} className="cursor-default">
              <title>{g.name}</title>
              {g.id === "service" && (
                <circle cx={g.x + 13} cy={g.y - 13} r="8" fill="#D97706" opacity={0.5} className="gate-ping" />
              )}
              <rect
                x={g.x - 16}
                y={g.y - 16}
                width={32}
                height={32}
                rx={9}
                fill="white"
                stroke="#101B2E"
                strokeWidth={1.4}
              />
              <svg x={g.x - 9} y={g.y - 9} width={18} height={18} viewBox="0 0 24 24">
                <Icon size={24} color="#101B2E" strokeWidth={1.9} />
              </svg>
              <circle cx={g.x + 13} cy={g.y - 13} r={4.5} fill={g.dot} stroke="white" strokeWidth={1.6} />
              <title>{g.name}</title>
              {!compact && (
                <LabelChip x={g.lx} y={g.ly} w={g.w} color="#475569" text={g.name.toUpperCase()} size={8} />
              )}
            </g>
          );
        })}

        {/* north arrow */}
        <g className="pointer-events-none select-none" transform="translate(706,58)">
          <circle r="16" fill="white" stroke="#CBD5E1" />
          <path d="M 0,-9 L 4.5,4 L 0,1.2 L -4.5,4 Z" fill="#101B2E" />
          <text y="30" textAnchor="middle" className="svg-label" fontSize={9} fontWeight={600} fill="#64748B">
            N
          </text>
        </g>

        {/* scale / disclaimer */}
        <g className="pointer-events-none select-none" transform="translate(28,566)">
          <line x1="0" y1="0" x2="64" y2="0" stroke="#94A3B8" strokeWidth={1.4} />
          <line x1="0" y1="-4" x2="0" y2="4" stroke="#94A3B8" strokeWidth={1.4} />
          <line x1="64" y1="-4" x2="64" y2="4" stroke="#94A3B8" strokeWidth={1.4} />
          <text x="0" y="16" className="svg-label" fontSize={8.5} fill="#94A3B8">
            CONCEPT — NOT TO SCALE
          </text>
        </g>
        <text x={734} y={584} textAnchor="end" className="svg-label pointer-events-none" fontSize={8.5} fill="#B6C0D0">
          PRELIMINARY CONCEPT — SITE VALIDATION REQUIRED
        </text>
      </svg>
    </div>
  );
}
