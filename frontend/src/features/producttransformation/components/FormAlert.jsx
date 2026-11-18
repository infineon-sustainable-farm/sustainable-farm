import { CircleAlert, TriangleAlert } from 'lucide-react';

// Banners of the Product Transformation screens and forms, as machinery: request error (red) and
// warning (amber).
const TONES = {
  error: { className: 'bg-error-tint text-error-fg', icon: CircleAlert, role: 'alert' },
  warning: { className: 'bg-warning-tint text-warning-fg', icon: TriangleAlert, role: undefined },
};

export default function FormAlert({ tone = 'error', children, className = '' }) {
  const { className: toneClass, icon: Icon, role } = TONES[tone] ?? TONES.error;
  return (
    <div role={role} className={`flex items-start gap-2 rounded-control px-3 py-2 text-sm ${toneClass} ${className}`}>
      <Icon size={16} aria-hidden="true" className="mt-0.5 shrink-0" />
      <div className="min-w-0">{children}</div>
    </div>
  );
}
