import { CheckCircle, Droplets, Factory, Package, TrendingUp, Zap } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { KpiCard } from '../../../shared/ui';
import { formatNumber, formatPercentage, formatWeight } from '../utils/format';

/** The eight key figures of the Product Transformation dashboard. */
export default function DashboardKpis({ kpis }) {
  const { t } = useTranslation('producttransformation');

  const cards = [
    {
      id: 'harvestQuantity',
      value: formatWeight(kpis?.harvestQuantity),
      icon: Package,
      tone: 'warning',
      hint: t('dashboard.kpis.harvestQuantityHint'),
    },
    { id: 'activeBatches', value: kpis?.activeBatches ?? 0, icon: TrendingUp, tone: 'primary' },
    { id: 'productionOutput', value: formatNumber(kpis?.productionOutput), icon: Factory, tone: 'info' },
    {
      id: 'qualityPassRate',
      value: formatPercentage(kpis?.qualityPassRate),
      icon: CheckCircle,
      tone: 'primary',
      hint: t('dashboard.kpis.target', { status: kpis?.qualityTargetStatus ?? t('dashboard.naShort') }),
    },
    { id: 'equipmentUtilization', value: formatPercentage(kpis?.equipmentUtilization), icon: Factory, tone: 'info' },
    {
      id: 'energyConsumption',
      value: t('dashboard.kpis.perKgEnergy', { value: formatNumber(kpis?.energyConsumption) }),
      icon: Zap,
      tone: 'warning',
    },
    { id: 'gradeAShare', value: formatPercentage(kpis?.gradeAPercentage), icon: CheckCircle, tone: 'primary' },
    {
      id: 'waterConsumption',
      value: t('dashboard.kpis.perKgWater', { value: formatNumber(kpis?.waterConsumption) }),
      icon: Droplets,
      tone: 'info',
    },
  ];

  return (
    <div className="mb-8 grid grid-cols-1 gap-6 md:grid-cols-2 lg:grid-cols-4">
      {cards.map(({ id, ...card }) => (
        <KpiCard key={id} label={t(`dashboard.kpis.${id}`)} {...card} />
      ))}
    </div>
  );
}
