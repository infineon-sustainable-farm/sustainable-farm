import { CartesianGrid, Legend, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { useTranslation } from 'react-i18next';
import { Card, EmptyState } from '../../../../shared/ui';

// Colours and sizes from the tokens of src/index.css, as the machinery chart.
const TEXT_XS = 12;
const AXIS_TICK = { fill: 'var(--color-chart-3)', fontSize: TEXT_XS };
const TOOLTIP_STYLE = {
  borderRadius: 'var(--radius-tile)',
  border: 'thin solid var(--color-border-divider)',
  fontSize: 'var(--text-xs)',
};
const LEGEND_STYLE = { fontSize: 'var(--text-xs)' };

/** Clicks and sign-ups of the last 30 metrics of the active campaign. */
function ConversionChart({ metrics, hasCampaign, className }) {
  const { t } = useTranslation('salesmarketing');
  const data = metrics.map((metric) => ({
    date: metric.metricDate,
    clicks: Number(metric.clicks || 0),
    signups: Number(metric.signups || 0),
  }));

  return (
    <Card className={className} title={t('campaigns.trajectory')}>
      {data.length < 2 ? (
        <EmptyState message={hasCampaign ? t('campaigns.trajectoryEmpty') : t('campaigns.trajectoryNoCampaign')} />
      ) : (
        <div className="h-52 w-full">
          <ResponsiveContainer width="100%" height="100%">
            <LineChart data={data} margin={{ top: 8, right: 8, left: 0, bottom: 0 }}>
              <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="var(--color-chart-grid)" />
              <XAxis dataKey="date" hide />
              <YAxis tickLine={false} axisLine={false} width={40} tick={AXIS_TICK} />
              <Tooltip contentStyle={TOOLTIP_STYLE} />
              <Legend wrapperStyle={LEGEND_STYLE} />
              <Line
                type="linear"
                dataKey="clicks"
                name={t('campaigns.clickThrough')}
                stroke="var(--color-chart-1)"
                strokeWidth={3}
                dot={false}
                isAnimationActive={false}
              />
              <Line
                type="linear"
                dataKey="signups"
                name={t('campaigns.signups')}
                stroke="var(--color-chart-2)"
                strokeWidth={2}
                strokeDasharray="4 4"
                dot={false}
                isAnimationActive={false}
              />
            </LineChart>
          </ResponsiveContainer>
        </div>
      )}
    </Card>
  );
}

export default ConversionChart;
