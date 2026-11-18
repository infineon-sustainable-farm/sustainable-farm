import { useMemo } from 'react';
import { useTranslation } from 'react-i18next';
import { Globe, Network, PlaneTakeoff, Store } from 'lucide-react';
import { localIsoDate, useSalesFormat } from '../../format';

// Look of a channel, picked from its name (or its position when the name matches nothing).
export const CHANNEL_PRESETS = [
  {
    key: 'online',
    match: ['online', 'e-commerce', 'ecommerce', 'web', 'store'],
    icon: Globe,
    iconBg: 'bg-primary-tint text-primary',
  },
  {
    key: 'distributors',
    match: ['distributor', 'distribution', 'wholesale'],
    icon: Network,
    iconBg: 'bg-warning-tint text-warning-fg',
  },
  {
    key: 'retail',
    match: ['retail', 'partner', 'storefront'],
    icon: Store,
    iconBg: 'bg-info-tint text-info-fg',
  },
  {
    key: 'export',
    match: ['export', 'direct', 'flight'],
    icon: PlaneTakeoff,
    iconBg: 'bg-primary text-on-primary',
  },
];

export function getErrorMessage(err, fallback) {
  const data = err?.data;
  if (!data) return err?.message || (err?.status ? `${fallback} (${err.status})` : fallback);
  if (typeof data === 'string') return data;
  if (data.message) {
    if (data.fieldErrors && typeof data.fieldErrors === 'object') {
      const details = Object.values(data.fieldErrors).join(', ');
      return details ? `${data.message}: ${details}` : data.message;
    }
    return data.message;
  }
  return fallback;
}

function matchPreset(name = '', index = 0) {
  const lower = name.toLowerCase();
  const found = CHANNEL_PRESETS.find((p) => p.match.some((m) => lower.includes(m)));
  return found || CHANNEL_PRESETS[index % CHANNEL_PRESETS.length];
}

function getLatestTarget(targets, channelId) {
  const forChannel = targets.filter((t) => t.channelId === channelId);
  if (forChannel.length === 0) return null;
  return [...forChannel].sort((a, b) => {
    const da = new Date(a.periodEnd || a.periodStart || 0).getTime();
    const db = new Date(b.periodEnd || b.periodStart || 0).getTime();
    return db - da;
  })[0];
}

/**
 * Channel with its look, its translated preset name and short label, and the revenue target of its
 * latest period.
 */
export function enrichChannel(channel, index, targets, t) {
  const preset = matchPreset(channel.name, index);
  const target = getLatestTarget(targets, channel.channelId);
  const revenueTargetEur = target?.revenueTargetEur != null ? Number(target.revenueTargetEur) : null;
  return {
    ...preset,
    ...channel,
    name: channel.name || t(`channels.presets.${preset.key}.name`),
    revenueTargetEur,
    shortLabel: t(`channels.presets.${preset.key}.short`),
  };
}

export function emptyChannelForm() {
  return { name: '' };
}

export function emptyTargetForm(channelId = null) {
  const today = new Date();
  const end = new Date(today);
  end.setMonth(end.getMonth() + 3);
  return {
    channelId,
    periodStart: localIsoDate(today),
    periodEnd: localIsoDate(end),
    revenueTargetEur: '',
  };
}

/** Revenue in euros, compact: "€4.5M", "€450K", "€800". */
export function useRevenueFormat() {
  const { t } = useTranslation('salesmarketing');
  const format = useSalesFormat();
  return useMemo(
    () => (eur) => {
      if (eur == null) return t('channels.none');
      const n = Number(eur);
      const fixed = (value, digits) =>
        format.number(value, { minimumFractionDigits: digits, maximumFractionDigits: digits, useGrouping: false });
      if (n >= 1_000_000) return t('channels.revenueMillions', { value: fixed(n / 1_000_000, 1) });
      if (n >= 1_000) return t('channels.revenueThousands', { value: fixed(n / 1_000, 0) });
      return t('channels.revenue', { value: format.number(n) });
    },
    [t, format]
  );
}
