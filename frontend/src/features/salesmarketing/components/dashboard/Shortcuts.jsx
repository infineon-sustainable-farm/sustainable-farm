import { ChevronRight, History, NotebookPen, SlidersHorizontal } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';

const SHORTCUTS = [
  { key: 'historical', icon: History },
  { key: 'model', icon: SlidersHorizontal },
  { key: 'seasonal', icon: NotebookPen },
];

/** Three shortcuts to the forecasting screen. */
function Shortcuts() {
  const { t } = useTranslation('salesmarketing');
  const navigate = useNavigate();
  return (
    <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
      {SHORTCUTS.map(({ key, icon: Icon }) => (
        <button
          key={key}
          type="button"
          onClick={() => navigate('/forecasting')}
          className="group flex items-center gap-4 rounded-card border border-border-subtle bg-surface-raised p-4 text-left shadow-raised transition duration-(--duration-fast) ease-standard hover:shadow-overlay"
        >
          <span className="flex h-12 w-12 items-center justify-center rounded-tile bg-neutral-bg text-neutral-fg group-hover:bg-primary-tint group-hover:text-primary">
            <Icon size={20} aria-hidden="true" />
          </span>
          <span>
            <span className="block font-semibold">{t(`dashboard.shortcuts.${key}.title`)}</span>
            <span className="block text-sm text-text-muted">{t(`dashboard.shortcuts.${key}.text`)}</span>
          </span>
          <ChevronRight size={18} aria-hidden="true" className="ml-auto text-text-muted group-hover:text-primary" />
        </button>
      ))}
    </div>
  );
}

export default Shortcuts;
