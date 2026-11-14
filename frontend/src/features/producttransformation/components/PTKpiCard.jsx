/**
 * Product Transformation - KPI card.
 *
 * The same tile as the Machinery dashboard's `DashboardKpiCard`, branded for
 * PT and shared by every PT dashboard. It can be a link (when the figure has
 * a screen behind it) or a plain tile (when it does not), so a KPI never looks
 * clickable unless it is.
 *
 * Typography follows the team scale: value 30 px Montserrat bold, label 14 px
 * Inter medium, hint 12 px gray-500. The icon chip repeats the label, so it is
 * hidden from screen readers.
 */

import { Link } from 'react-router-dom';
import { ArrowRight } from 'lucide-react';
import { KPI_TONES, CARD_FRAME } from '../styles/ptStyles';

function CardBody({ icon: Icon, label, value, hint, tone, linked }) {
  return (
    <>
      <div className="flex items-center justify-between">
        <span
          className={`flex h-10 w-10 items-center justify-center rounded-xl ${KPI_TONES[tone] ?? KPI_TONES.primary}`}
        >
          <Icon size={20} aria-hidden="true" />
        </span>
        {linked ? (
          <ArrowRight
            size={16}
            aria-hidden="true"
            className="text-gray-300 transition-all group-hover:translate-x-0.5 group-hover:text-primary"
          />
        ) : null}
      </div>

      <div>
        <p className="font-heading text-3xl font-bold tracking-tight text-on-background">{value}</p>
        <p className="mt-0.5 text-sm font-medium text-gray-700">{label}</p>
        {hint ? <p className="mt-1 text-xs text-gray-500">{hint}</p> : null}
      </div>
    </>
  );
}

export default function PTKpiCard({
  to,
  icon,
  label,
  value,
  hint,
  tone = 'primary',
  delay = 0,
  className = '',
}) {
  const frame = `${CARD_FRAME} group flex flex-col gap-4 p-5 transition-all`;
  const animation = { animationDelay: `${delay}ms` };

  if (to) {
    return (
      <Link
        to={to}
        style={animation}
        className={`${frame} animate-fade-up hover:-translate-y-0.5 hover:shadow-md ${className}`}
      >
        <CardBody icon={icon} label={label} value={value} hint={hint} tone={tone} linked />
      </Link>
    );
  }

  return (
    <div style={animation} className={`${frame} animate-fade-up ${className}`}>
      <CardBody icon={icon} label={label} value={value} hint={hint} tone={tone} />
    </div>
  );
}
