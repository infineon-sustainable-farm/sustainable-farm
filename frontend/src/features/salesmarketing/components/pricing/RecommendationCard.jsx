import { Lightbulb } from 'lucide-react';
import { useTranslation } from 'react-i18next';

/** Message of the latest pricing recommendation for the current product. */
function RecommendationCard({ recommendation }) {
  const { t } = useTranslation('salesmarketing');
  return (
    <section className="flex items-start gap-4 rounded-card bg-primary p-5 text-on-primary shadow-raised">
      <Lightbulb size={20} aria-hidden="true" className="shrink-0" />
      <div>
        <h2 className="mb-1 text-sm font-semibold">{t('pricing.recommendation')}</h2>
        <p className="text-sm">{recommendation?.message || t('pricing.noRecommendation')}</p>
      </div>
    </section>
  );
}

export default RecommendationCard;
