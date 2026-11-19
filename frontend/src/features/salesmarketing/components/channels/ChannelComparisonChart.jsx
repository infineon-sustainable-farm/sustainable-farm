import { Bar, BarChart, CartesianGrid, Legend, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { useTranslation } from 'react-i18next';
import { Card } from '../../../../shared/ui';
import { useRevenueFormat } from './channelModel';

// Colours and sizes from the tokens of src/index.css, as the machinery chart.
const TEXT_XS = 12;
const AXIS_TICK = { fill: 'var(--color-chart-3)', fontSize: TEXT_XS };
const TOOLTIP_STYLE = {
  borderRadius: 'var(--radius-tile)',
  border: 'thin solid var(--color-border-divider)',
  fontSize: 'var(--text-xs)',
};
const LEGEND_STYLE = { fontSize: 'var(--text-xs)' };

/** Revenue target of the latest period against the actual revenue of the current year, per channel. */
function ChannelComparisonChart({ channels }) {
  const { t } = useTranslation('salesmarketing');
  const formatRevenue = useRevenueFormat();
  const data = channels.map((channel) => ({
    key: channel.channelId || channel.key,
    label: channel.shortLabel,
    target: channel.revenueTargetEur ?? 0,
    actual: channel.actualRevenueEur,
  }));

  return (
    <Card title={t('channels.comparison')}>
      <div className="h-64 w-full">
        <ResponsiveContainer width="100%" height="100%">
          <BarChart data={data} margin={{ top: 8, right: 8, left: 0, bottom: 0 }}>
            <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="var(--color-chart-grid)" />
            <XAxis dataKey="label" tickLine={false} axisLine={false} tick={AXIS_TICK} />
            <YAxis
              tickLine={false}
              axisLine={false}
              width={56}
              tick={AXIS_TICK}
              tickFormatter={(value) => (value === 0 ? '0' : formatRevenue(value))}
            />
            <Tooltip contentStyle={TOOLTIP_STYLE} formatter={(value, name) => [formatRevenue(value), name]} />
            <Legend wrapperStyle={LEGEND_STYLE} />
            <Bar
              dataKey="target"
              name={t('channels.revenueTarget')}
              fill="var(--color-chart-1)"
              radius={[4, 4, 0, 0]}
              maxBarSize={32}
              isAnimationActive={false}
            />
            <Bar
              dataKey="actual"
              name={t('channels.actualYtd')}
              fill="var(--color-chart-2)"
              radius={[4, 4, 0, 0]}
              maxBarSize={32}
              isAnimationActive={false}
            />
          </BarChart>
        </ResponsiveContainer>
      </div>
    </Card>
  );
}

export default ChannelComparisonChart;
