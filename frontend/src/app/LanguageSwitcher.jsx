import { useTranslation } from 'react-i18next'
import { useLanguage } from '../shared/i18n'
import { Field, Select } from '../shared/ui'

/**
 * Interface language (English, German, French): a labelled native select, so it works with the
 * keyboard, screen readers and touch without extra code. The choice is remembered by the
 * language detector and mirrored on <html lang>. Each language is named in its own language.
 */
export default function LanguageSwitcher({ className }) {
  const { t } = useTranslation('common')
  const { language, setLanguage, supportedLanguages } = useLanguage()

  return (
    <Field label={t('language.label')} className={className}>
      <Select value={language} onChange={(event) => setLanguage(event.target.value)}>
        {supportedLanguages.map((code) => (
          <option key={code} value={code} lang={code}>
            {t(`language.${code}`)}
          </option>
        ))}
      </Select>
    </Field>
  )
}
