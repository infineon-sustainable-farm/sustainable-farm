import { useTranslation } from 'react-i18next'
import { useLanguage } from '../shared/i18n'
import { focusOnPrimary, focusOnSurface, shellTransition } from './styles'

// Two tones: on the primary sidebar, and on the page background (launcher).
const TONES = {
  onPrimary: {
    group: 'bg-primary-hover',
    active: 'bg-on-primary text-primary-pressed',
    idle: 'text-on-primary hover:bg-primary-active',
    focus: focusOnPrimary,
  },
  onSurface: {
    group: 'border border-border-subtle bg-surface-raised',
    active: 'bg-primary text-on-primary',
    idle: 'text-text hover:bg-neutral-bg',
    focus: focusOnSurface,
  },
}

/**
 * Interface language (English, German, French) as a compact segmented control: one button per
 * language showing its code, the current one filled. Each button is named by the language in its
 * own language (aria-label, lang) and marked with aria-pressed; the group is labelled. The choice
 * is remembered by the language detector and mirrored on <html lang>.
 *
 * @param {{ tone?: 'onPrimary' | 'onSurface', className?: string }} props
 */
export default function LanguageSwitcher({ tone = 'onPrimary', className = '' }) {
  const { t } = useTranslation('common')
  const { language, setLanguage, supportedLanguages } = useLanguage()
  const styles = TONES[tone]

  return (
    <div
      role="group"
      aria-label={t('language.label')}
      className={`flex gap-1 rounded-control p-1 ${styles.group} ${className}`}
    >
      {supportedLanguages.map((code) => {
        const current = code === language
        return (
          <button
            key={code}
            type="button"
            lang={code}
            aria-label={t(`language.${code}`)}
            aria-pressed={current}
            onClick={() => setLanguage(code)}
            className={`min-h-8 flex-1 rounded-control px-3 text-xs font-semibold tracking-wider uppercase
              pointer-coarse:min-h-11 ${current ? styles.active : styles.idle} ${styles.focus} ${shellTransition}`}
          >
            {code}
          </button>
        )
      })}
    </div>
  )
}
