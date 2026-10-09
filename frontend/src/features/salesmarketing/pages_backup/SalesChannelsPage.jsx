import React, { useEffect, useState } from 'react';
import { api } from '../api/api';

const CHANNEL_PRESETS = [
  {
    key: 'online',
    match: ['online', 'e-commerce', 'ecommerce', 'web', 'store'],
    name: 'Online Store',
    icon: 'language',
    iconBg: 'bg-primary-container text-on-primary-container',
    blob: 'bg-primary/5 group-hover:bg-primary/10',
    shortLabel: 'Online',
  },
  {
    key: 'distributors',
    match: ['distributor', 'distribution', 'wholesale'],
    name: 'Distributors',
    icon: 'hub',
    iconBg: 'bg-secondary-container text-on-secondary-container',
    blob: 'bg-secondary/5 group-hover:bg-secondary/10',
    shortLabel: 'Distributors',
  },
  {
    key: 'retail',
    match: ['retail', 'partner', 'storefront'],
    name: 'Retail Partners',
    icon: 'storefront',
    iconBg: 'bg-tertiary-container text-on-tertiary-container',
    blob: 'bg-error/5 group-hover:bg-error/10',
    shortLabel: 'Retail',
  },
  {
    key: 'export',
    match: ['export', 'direct', 'flight'],
    name: 'Export Direct',
    icon: 'flight_takeoff',
    iconBg: 'bg-primary text-on-primary',
    blob: 'bg-primary-fixed/10 group-hover:bg-primary-fixed/20',
    shortLabel: 'Export',
  },
];

function getErrorMessage(err, fallback = 'Request failed') {
  const data = err?.response?.data;
  if (!data) return err?.message || fallback;
  if (typeof data === 'string') return data;
  if (data.message) {
    if (data.validationErrors && typeof data.validationErrors === 'object') {
      const details = Object.values(data.validationErrors).join(', ');
      return details ? `${data.message}: ${details}` : data.message;
    }
    return data.message;
  }
  return fallback;
}

function formatRevenue(eur) {
  if (eur == null) return '—';
  const n = Number(eur);
  if (n >= 1_000_000) return `ââ€šÂ¬${(n / 1_000_000).toFixed(1)}M`;
  if (n >= 1_000) return `ââ€šÂ¬${(n / 1_000).toFixed(0)}K`;
  return `ââ€šÂ¬${n.toLocaleString('en-US')}`;
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

function enrichChannel(channel, index, targets) {
  const preset = matchPreset(channel.name, index);
  const target = getLatestTarget(targets, channel.channelId);
  const revenueTargetEur = target?.revenueTargetEur != null ? Number(target.revenueTargetEur) : null;

  return {
    ...preset,
    ...channel,
    name: channel.name || preset.name,
    revenueLabel: formatRevenue(revenueTargetEur),
    revenueTargetEur,
    shortLabel: preset.shortLabel || (channel.name || '').split(' ')[0],
  };
}

function emptyChannelForm() {
  return { name: '' };
}

function emptyTargetForm(channelId = null) {
  const today = new Date();
  const end = new Date(today);
  end.setMonth(end.getMonth() + 3);
  return {
    channelId,
    periodStart: today.toISOString().slice(0, 10),
    periodEnd: end.toISOString().slice(0, 10),
    revenueTargetEur: '',
  };
}

export default function SalesChannelsPage() {
  const [loading, setLoading] = useState(true);
  const [apiError, setApiError] = useState(null);
  const [actionError, setActionError] = useState(null);
  const [actionSuccess, setActionSuccess] = useState(null);
  const [channels, setChannels] = useState([]);
const [targets, setTargets] = useState([]);
const [orders, setOrders] = useState([]);
  const [showTargets, setShowTargets] = useState(false);
  const [selectedTargetChannelId, setSelectedTargetChannelId] = useState(null);
  const [openMenuChannelId, setOpenMenuChannelId] = useState(null);

  const [showChannelModal, setShowChannelModal] = useState(false);
  const [editingChannelId, setEditingChannelId] = useState(null);
  const [channelForm, setChannelForm] = useState(emptyChannelForm());
  const [savingChannel, setSavingChannel] = useState(false);

  const [showTargetModal, setShowTargetModal] = useState(false);
  const [editingTargetId, setEditingTargetId] = useState(null);
  const [targetForm, setTargetForm] = useState(emptyTargetForm());
  const [savingTarget, setSavingTarget] = useState(false);

  const [pendingDeleteChannel, setPendingDeleteChannel] = useState(null);
  const [deletingChannel, setDeletingChannel] = useState(false);
  const [pendingDeleteTarget, setPendingDeleteTarget] = useState(null);
  const [deletingTarget, setDeletingTarget] = useState(false);

  useEffect(() => {
    fetchChannelsData();
  }, []);

  useEffect(() => {
    const handleClickOutside = () => setOpenMenuChannelId(null);
    if (openMenuChannelId != null) {
      window.addEventListener('click', handleClickOutside);
      return () => window.removeEventListener('click', handleClickOutside);
    }
  }, [openMenuChannelId]);

  const flashSuccess = (msg) => {
    setActionSuccess(msg);
    setActionError(null);
    window.setTimeout(() => setActionSuccess(null), 4000);
  };

  const flashError = (msg) => {
    setActionError(msg);
    setActionSuccess(null);
    window.setTimeout(() => setActionError(null), 6000);
  };

  const fetchChannelsData = async () => {
    setLoading(true);
    setApiError(null);
    try {
      const [channelRes, targetRes, orderRes] = await Promise.allSettled([
  api.getSalesChannels(),
  api.getSalesChannelTargets(),
  api.getOrders(),
]);

      const failures = [];

      if (channelRes.status === 'fulfilled') {
        setChannels(Array.isArray(channelRes.value.data) ? channelRes.value.data : []);
      } else {
        setChannels([]);
        failures.push(`Channels: ${getErrorMessage(channelRes.reason)}`);
      }

      if (targetRes.status === 'fulfilled') {
        setTargets(Array.isArray(targetRes.value.data) ? targetRes.value.data : []);
      } else {
        setTargets([]);
        failures.push(`Targets: ${getErrorMessage(targetRes.reason)}`);
      }
      if (orderRes.status === 'fulfilled') {
  setOrders(Array.isArray(orderRes.value.data) ? orderRes.value.data : []);
} else {
  setOrders([]);
  failures.push(`Orders: ${getErrorMessage(orderRes.reason)}`);
}

      if (failures.length > 0 && channelRes.status === 'rejected') {
        setApiError(
          'Backend REST API at http://localhost:8080/api is offline or unreachable. Channel data is unavailable. ' +
            failures.join(' · ')
        );
      } else if (failures.length > 0) {
        setApiError(`Partial load — ${failures.join(' · ')}`);
      }
    } catch (err) {
      setApiError(getErrorMessage(err, 'Error connecting to Spring Boot backend API at http://localhost:8080/api'));
      setChannels([]);
      setTargets([]);
    } finally {
      setLoading(false);
    }
  };

  const displayChannels = channels.map((ch, i) => enrichChannel(ch, i, targets));
  const currentYear = new Date().getFullYear();

const actualRevenueByChannel = orders.reduce((acc, order) => {
  const channelId = Number(order.salesChannelId);
  const orderDate = order.orderDate;

  if (!channelId || !orderDate) return acc;

  const date = new Date(orderDate);

  if (date.getFullYear() !== currentYear) return acc;

  const revenueEur = Number(order.totalValueEur ?? 0);

  if (!Number.isFinite(revenueEur)) return acc;

  acc[channelId] = (acc[channelId] || 0) + revenueEur;

  return acc;
}, {});

  const allTargets = targets;

  const filteredTargets = selectedTargetChannelId
    ? allTargets.filter((t) => t.channelId === selectedTargetChannelId)
    : allTargets;
  const comparisonMax = Math.max(
  ...displayChannels.flatMap((channel) => [
    channel.revenueTargetEur ?? 0,
    actualRevenueByChannel[channel.channelId] ?? 0,
  ]),
  1
);
  const comparisonChannels = displayChannels.map((channel) => {
  const actualRevenueEur =
    actualRevenueByChannel[channel.channelId] ?? 0;

  return {
    ...channel,
    actualRevenueEur,
    targetBar: Math.round(
      ((channel.revenueTargetEur ?? 0) / comparisonMax) * 100
    ),
    actualBar: Math.round(
      (actualRevenueEur / comparisonMax) * 100
    ),
  };
});

  const openCreateChannel = () => {
    setChannelForm(emptyChannelForm());
    setEditingChannelId(null);
    setShowChannelModal(true);
    setActionError(null);
  };

  const openEditChannel = async (channel) => {
    setActionError(null);
    setSavingChannel(true);
    try {
      const res = await api.getSalesChannelById(channel.channelId);
      const c = res.data || channel;
      setChannelForm({
        name: c.name || '',
      });
      setEditingChannelId(channel.channelId);
      setShowChannelModal(true);
    } catch (err) {
      flashError(getErrorMessage(err, `Unable to load sales channel #${channel.channelId} for editing.`));
    } finally {
      setSavingChannel(false);
    }
  };

  const handleSaveChannel = async (e) => {
    e.preventDefault();
    setSavingChannel(true);
    setActionError(null);
    try {
      const payload = {
        name: channelForm.name,
      };

      if (editingChannelId != null) {
        await api.updateSalesChannel(editingChannelId, payload);
        flashSuccess(`Sales channel "${payload.name}" updated successfully.`);
      } else {
        await api.createSalesChannel(payload);
        flashSuccess(`Sales channel "${payload.name}" created successfully.`);
      }
      setShowChannelModal(false);
      await fetchChannelsData();
    } catch (err) {
      flashError(getErrorMessage(err, editingChannelId != null ? 'Failed to update channel' : 'Failed to create channel'));
    } finally {
      setSavingChannel(false);
    }
  };

  const confirmDeleteChannel = async () => {
    if (!pendingDeleteChannel) return;
    setDeletingChannel(true);
    try {
      await api.deleteSalesChannel(pendingDeleteChannel.channelId);
      const name = pendingDeleteChannel.name;
      setPendingDeleteChannel(null);
      flashSuccess(`Sales channel "${name}" deleted successfully.`);
      await fetchChannelsData();
    } catch (err) {
      flashError(getErrorMessage(err, 'Failed to delete channel'));
    } finally {
      setDeletingChannel(false);
    }
  };

  const openCreateTarget = (channelId = null) => {
    const useId = channelId || selectedTargetChannelId || displayChannels[0]?.channelId || null;
    if (useId == null) {
      flashError('Please select a channel first.');
      return;
    }
    setTargetForm(emptyTargetForm(useId));
    setEditingTargetId(null);
    setShowTargetModal(true);
    setActionError(null);
  };

  const openEditTarget = async (target) => {
    setActionError(null);
    setSavingTarget(true);
    try {
      const res = await api.getSalesChannelTargetById(target.targetId);
      const t = res.data || target;
      setTargetForm({
        channelId: t.channelId,
        periodStart: t.periodStart ? new Date(t.periodStart).toISOString().slice(0, 10) : '',
        periodEnd: t.periodEnd ? new Date(t.periodEnd).toISOString().slice(0, 10) : '',
        revenueTargetEur: t.revenueTargetEur != null ? String(t.revenueTargetEur) : '',
      });
      setEditingTargetId(target.targetId);
      setShowTargetModal(true);
    } catch (err) {
      flashError(getErrorMessage(err, `Unable to load target #${target.targetId} for editing.`));
    } finally {
      setSavingTarget(false);
    }
  };

  const handleSaveTarget = async (e) => {
    e.preventDefault();
    setSavingTarget(true);
    setActionError(null);
    try {
      const payload = {
        channelId: Number(targetForm.channelId),
        periodStart: targetForm.periodStart,
        periodEnd: targetForm.periodEnd,
        revenueTargetEur: Number(targetForm.revenueTargetEur),
      };

      if (editingTargetId != null) {
        await api.updateSalesChannelTarget(editingTargetId, payload);
        flashSuccess('Target updated successfully.');
      } else {
        await api.createSalesChannelTarget(payload);
        flashSuccess('Target created successfully.');
      }
      setShowTargetModal(false);
      await fetchChannelsData();
    } catch (err) {
      flashError(getErrorMessage(err, editingTargetId != null ? 'Failed to update target' : 'Failed to create target'));
    } finally {
      setSavingTarget(false);
    }
  };

  const confirmDeleteTarget = async () => {
    if (!pendingDeleteTarget) return;
    setDeletingTarget(true);
    try {
      await api.deleteSalesChannelTarget(pendingDeleteTarget.targetId);
      setPendingDeleteTarget(null);
      flashSuccess('Target deleted successfully.');
      await fetchChannelsData();
    } catch (err) {
      flashError(getErrorMessage(err, 'Failed to delete target'));
    } finally {
      setDeletingTarget(false);
    }
  };

  return (
    <div className="flex flex-col w-full h-full min-h-[calc(100vh-80px)]">
      <div className="px-margin-desktop pt-xl pb-lg flex flex-col md:flex-row justify-between items-start md:items-end gap-md">
        <div>
          <h2 className="font-headline-lg text-headline-lg text-primary">Sales Channels</h2>
          <p className="font-body-lg text-body-lg text-on-surface-variant mt-sm max-w-2xl">
            Real-time performance metrics across all distribution networks.
          </p>
        </div>
        <div className="flex items-center gap-md flex-wrap">
          <div className="flex items-center gap-sm bg-surface-container-high px-md py-sm rounded-full">
            <span className={`w-3 h-3 rounded-full ${apiError ? 'bg-secondary' : 'bg-primary'} animate-pulse`} />
            <span className="font-mono-label text-mono-label text-on-surface uppercase">
              {apiError ? 'Offline Mode' : 'Live Sync Active'}
            </span>
          </div>
          <button
            onClick={() => setShowTargets((s) => !s)}
            className={`px-lg py-sm rounded-full font-label-md text-label-md uppercase tracking-wider transition-colors shadow-sm ${
              showTargets
                ? 'bg-secondary text-on-secondary'
                : 'bg-surface-container text-on-surface hover:bg-surface-container-high'
            }`}
          >
            {showTargets ? 'Hide Targets' : 'Manage Targets'}
          </button>
          <button
            onClick={() => openCreateTarget()}
            className="px-lg py-sm rounded-full font-label-md text-label-md uppercase tracking-wider bg-surface-container text-primary hover:bg-primary-container hover:text-on-primary-container transition-colors shadow-sm"
          >
            <span className="material-symbols-outlined text-[16px] align-text-bottom mr-xs">track_changes</span>
            Add Target
          </button>
          <button
            onClick={openCreateChannel}
            className="bg-primary text-on-primary px-lg py-sm rounded-full font-label-md text-label-md uppercase tracking-wider hover:bg-on-primary-fixed-variant transition-colors shadow-sm flex items-center gap-xs"
          >
            <span className="material-symbols-outlined text-[18px]">add</span>
            New Channel
          </button>
        </div>
      </div>

      <div className="px-margin-desktop pb-xxl flex flex-col gap-xl">
        {apiError && (
          <div className="bg-secondary-container/20 border-l-4 border-secondary p-md rounded-xl flex items-center justify-between shadow-sm">
            <div className="flex items-center gap-md">
              <span className="material-symbols-outlined text-secondary text-[24px]">wifi_off</span>
              <div>
                <p className="font-label-md text-label-md text-on-surface font-bold">API Connection Notice</p>
                <p className="font-body-sm text-body-sm text-on-surface-variant">{apiError}</p>
              </div>
            </div>
            <button
              onClick={fetchChannelsData}
              className="px-md py-xs bg-secondary text-on-secondary font-label-md text-label-md rounded-lg hover:opacity-90 transition-opacity flex items-center gap-xs"
            >
              <span className="material-symbols-outlined text-[16px]">refresh</span> Retry
            </button>
          </div>
        )}

        {actionError && (
          <div className="bg-error-container border-l-4 border-error p-md rounded-xl flex items-center justify-between shadow-sm">
            <div className="flex items-center gap-md">
              <span className="material-symbols-outlined text-error text-[24px]">error</span>
              <div>
                <p className="font-label-md text-label-md text-on-error-container font-bold">Action failed</p>
                <p className="font-body-sm text-body-sm text-on-error-container">{actionError}</p>
              </div>
            </div>
            <button onClick={() => setActionError(null)} className="text-on-error-container px-sm">
              <span className="material-symbols-outlined">close</span>
            </button>
          </div>
        )}

        {actionSuccess && (
          <div className="bg-[#e8f5e9] border-l-4 border-[#2e7d32] p-md rounded-xl flex items-center justify-between shadow-sm">
            <div className="flex items-center gap-md">
              <span className="material-symbols-outlined text-[#2e7d32] text-[24px]">check_circle</span>
              <p className="font-body-sm text-body-sm text-[#1b5e20]">{actionSuccess}</p>
            </div>
            <button onClick={() => setActionSuccess(null)} className="text-[#1b5e20] px-sm">
              <span className="material-symbols-outlined">close</span>
            </button>
          </div>
        )}

        {loading ? (
          <div className="p-xl bg-surface-container-lowest rounded-xl text-center shadow-sm">
            <span className="material-symbols-outlined text-[32px] text-primary animate-spin">sync</span>
            <p className="font-body-sm text-body-sm text-on-surface-variant mt-sm">Loading sales channelsââ‚¬Â¦</p>
          </div>
        ) : (
          <>
            {/* Channel Cards */}
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-gutter">
              {displayChannels.map((channel) => {
                return (
                  <div
                    key={channel.channelId || channel.key}
                    className="bg-surface-container-lowest rounded-xl p-lg shadow-sm hover:shadow-md transition-shadow relative overflow-hidden group"
                  >
                    <div className={`absolute -right-12 -top-12 w-32 h-32 rounded-full blur-2xl transition-colors ${channel.blob}`} />
                    <div className="flex justify-between items-start mb-xl relative z-10">
                      <div className="flex items-center gap-sm min-w-0">
                        <div className={`w-10 h-10 rounded-full flex items-center justify-center shrink-0 ${channel.iconBg}`}>
                          <span className="material-symbols-outlined">{channel.icon}</span>
                        </div>
                        <div className="min-w-0">
                          <h3 className="font-headline-sm text-headline-sm text-on-surface truncate">
                            {channel.name}
                          </h3>
                        </div>
                      </div>
                      <div className="flex items-center gap-xs">
                        <span className="material-symbols-outlined text-outline">remove</span>
                        <div className="relative">
                          <button
                            onClick={(e) => {
                              e.stopPropagation();
                              setOpenMenuChannelId(openMenuChannelId === channel.channelId ? null : channel.channelId);
                            }}
                            className="w-8 h-8 rounded-full flex items-center justify-center text-on-surface-variant hover:bg-surface-container hover:text-primary transition-colors"
                          >
                            <span className="material-symbols-outlined text-[18px]">more_vert</span>
                          </button>
                          {openMenuChannelId === channel.channelId && (
                            <div
                              onClick={(e) => e.stopPropagation()}
                              className="absolute right-0 top-full mt-xs bg-surface-container-lowest rounded-lg shadow-lg border border-outline-variant/30 py-xs z-30 min-w-[180px]"
                            >
                              <button
                                onClick={() => {
                                  setOpenMenuChannelId(null);
                                  openEditChannel(channel);
                                }}
                                className="w-full px-md py-sm text-left font-body-sm hover:bg-surface-container text-on-surface flex items-center gap-xs"
                              >
                                <span className="material-symbols-outlined text-[18px] text-on-surface-variant">edit</span>
                                Edit Channel
                              </button>
                              <button
                                onClick={() => {
                                  setOpenMenuChannelId(null);
                                  setSelectedTargetChannelId(channel.channelId);
                                  setShowTargets(true);
                                  openCreateTarget(channel.channelId);
                                }}
                                className="w-full px-md py-sm text-left font-body-sm hover:bg-surface-container text-on-surface flex items-center gap-xs"
                              >
                                <span className="material-symbols-outlined text-[18px] text-on-surface-variant">track_changes</span>
                                Add Target
                              </button>
                              <button
                                onClick={() => {
                                  setOpenMenuChannelId(null);
                                  setPendingDeleteChannel(channel);
                                }}
                                className="w-full px-md py-sm text-left font-body-sm hover:bg-error-container/30 text-error flex items-center gap-xs"
                              >
                                <span className="material-symbols-outlined text-[18px]">delete</span>
                                Delete Channel
                              </button>
                            </div>
                          )}
                        </div>
                      </div>
                    </div>
                    <div className="flex flex-col gap-md relative z-10">
                      <div>
                        <p className="font-label-md text-label-md text-on-surface-variant uppercase tracking-widest opacity-80 mb-xs">
                          Revenue Target
                        </p>
                        <p className="font-headline-lg text-headline-lg text-on-surface">
                          {formatRevenue(channel.revenueTargetEur)}
                        </p>
                      </div>
                      <div className="w-full h-px bg-outline-variant/30" />
                      <div>
                        <p className="font-label-md text-label-md text-on-surface-variant uppercase tracking-widest opacity-80 mb-xs">
                          Channel ID
                        </p>
                        <div className="flex items-baseline gap-sm flex-wrap">
                          <p className="font-headline-md text-headline-md text-on-surface">
                            {channel.channelId ?? '—'}
                          </p>
                        </div>
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>

            {/* Performance Comparison */}
            <div className="bg-surface-container-lowest rounded-xl p-xl shadow-sm mt-md">
              <div className="flex justify-between items-center mb-xl border-b border-outline-variant/30 pb-lg">
                <h3 className="font-headline-md text-headline-md text-on-surface">Channel Performance Comparison</h3>
                <div className="flex gap-md">
                  <div className="flex items-center gap-xs">
                    <div className="w-3 h-3 rounded-sm bg-primary" />
                    <span className="font-label-md text-label-md text-on-surface-variant">Revenue Target</span>
                  </div>
                  <div className="flex items-center gap-xs">
                    <div className="w-3 h-3 rounded-sm bg-secondary" />
              <span className="font-label-md text-label-md text-on-surface-variant">
  Actual Revenue YTD
</span>
                  </div>
                </div>
              </div>

              <div className="h-64 w-full relative flex items-end justify-between px-lg gap-gutter">
                <div className="absolute inset-0 flex flex-col justify-between z-0 pointer-events-none pb-8">
                  <div className="w-full h-px bg-outline-variant/20" />
                  <div className="w-full h-px bg-outline-variant/20" />
                  <div className="w-full h-px bg-outline-variant/20" />
                  <div className="w-full h-px bg-outline-variant/20" />
                  <div className="w-full h-px bg-outline-variant/40" />
                </div>
                <div className="absolute left-0 top-0 bottom-8 flex flex-col justify-between text-on-surface-variant font-mono-label text-[10px] -ml-6 pb-2 z-10 pointer-events-none opacity-50">
                  <span>{formatRevenue(comparisonMax)}</span>
                  <span>{formatRevenue(comparisonMax * 0.66)}</span>
                  <span>{formatRevenue(comparisonMax * 0.33)}</span>
                  <span>0</span>
                </div>

                {comparisonChannels.map((channel) => (
                  <div
                    key={`bar-${channel.channelId || channel.key}`}
                    className="relative z-10 flex-1 flex flex-col justify-end items-center h-full pb-8 group"
                  >
                    <div className="w-full max-w-[64px] flex items-end gap-1 h-[calc(100%-2rem)]">
                      <div
                        className="flex-1 bg-primary/20 rounded-t-sm group-hover:bg-primary/40 transition-colors"
                        style={{ height: `${channel.targetBar}%` }}
                        title={`Target: ${formatRevenue(channel.revenueTargetEur)}`}
                      />
                      <div
                        className="flex-1 bg-secondary rounded-t-sm shadow-[0_0_12px_rgba(156,68,0,0.3)]"
                        style={{ height: `${channel.actualBar}%` }}
                        title={`Actual Revenue YTD: ${formatRevenue(channel.actualRevenueEur)}`}
                      />
                    </div>
                    <span className="absolute bottom-0 font-label-md text-label-md text-on-surface-variant w-full text-center truncate">
                      {channel.shortLabel}
                    </span>
                  </div>
                ))}
              </div>
            </div>

            {/* Targets Management Table */}
            {showTargets && (
              <div className="bg-surface-container-lowest rounded-xl shadow-sm overflow-hidden flex flex-col">
                <div className="px-lg py-md flex flex-wrap items-center justify-between border-b border-surface-container/50 gap-md">
                  <div>
                    <h3 className="font-headline-md text-headline-md text-on-surface">Sales Targets</h3>
                    <p className="font-body-sm text-body-sm text-on-surface-variant">
                      Period-based revenue objectives for each channel.
                    </p>
                  </div>
                  <div className="flex items-center gap-md">
                    <select
                      value={selectedTargetChannelId || ''}
                      onChange={(e) => setSelectedTargetChannelId(e.target.value ? Number(e.target.value) : null)}
                      className="h-10 px-md rounded-lg bg-surface-container border border-outline-variant/40 font-body-sm focus:outline-none focus:ring-2 focus:ring-primary/20"
                    >
                      <option value="">All Channels</option>
                      {displayChannels.map((ch) => (
                        <option key={ch.channelId || ch.key} value={ch.channelId}>
                          {ch.name}
                        </option>
                      ))}
                    </select>
                    <button
                      onClick={() => openCreateTarget()}
                      className="h-10 px-md bg-primary text-on-primary rounded-lg font-label-md text-label-md hover:bg-on-primary-fixed-variant transition-colors flex items-center gap-xs"
                    >
                      <span className="material-symbols-outlined text-[18px]">add</span>
                      New Target
                    </button>
                  </div>
                </div>
                <div className="overflow-x-auto w-full">
                  <table className="w-full text-left min-w-[800px]">
                    <thead>
                      <tr className="bg-surface font-label-md text-label-md text-on-surface-variant uppercase tracking-wider">
                        <th className="py-md px-lg">Channel</th>
                        <th className="py-md px-lg">Period</th>
                        <th className="py-md px-lg">Target Revenue</th>
                        <th className="py-md px-lg text-right pr-lg">Actions</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-surface-container font-body-md text-body-md text-on-surface">
                      {filteredTargets.length === 0 ? (
                        <tr>
                          <td colSpan={4} className="py-xl px-lg text-center text-on-surface-variant">
                            No targets found. Click &quot;New Target&quot; to add one.
                          </td>
                        </tr>
                      ) : (
                        filteredTargets.map((target) => {
                          const ch = displayChannels.find((c) => c.channelId === target.channelId);
                          return (
                            <tr key={target.targetId} className="hover:bg-surface-container-low/50 transition-colors group">
                              <td className="py-md px-lg">
                                <div className="flex items-center gap-sm">
                                  <div className={`w-9 h-9 rounded-full flex items-center justify-center ${ch?.iconBg || 'bg-surface-container'}`}>
                                    <span className="material-symbols-outlined text-[18px]">{ch?.icon || 'category'}</span>
                                  </div>
                                  <div>
                                    <p className="font-medium">{ch?.name || `Channel #${target.channelId}`}</p>
                                    <p className="font-body-sm text-body-sm text-on-surface-variant">
                                      {ch?.shortLabel || target.channelId}
                                    </p>
                                  </div>
                                </div>
                              </td>
                              <td className="py-md px-lg">
                                <div className="font-body-sm text-body-sm">
                                  <span className="font-mono-label text-mono-label">{target.periodStart}</span>
                                  <span className="mx-sm text-on-surface-variant">ââ€ â€™</span>
                                  <span className="font-mono-label text-mono-label">{target.periodEnd}</span>
                                </div>
                              </td>
                              <td className="py-md px-lg font-headline-sm text-headline-sm text-on-surface">
                                {formatRevenue(target.revenueTargetEur)}
                              </td>
                              <td className="py-md px-lg text-right pr-lg">
                                <div className="flex items-center justify-end gap-xs">
                                  <button
                                    onClick={() => openEditTarget(target)}
                                    title="Edit target"
                                    className="p-sm text-on-surface-variant hover:text-primary hover:bg-surface-container rounded-lg transition-colors"
                                  >
                                    <span className="material-symbols-outlined text-[18px]">edit</span>
                                  </button>
                                  <button
                                    onClick={() => setPendingDeleteTarget(target)}
                                    title="Delete target"
                                    className="p-sm text-on-surface-variant hover:text-error hover:bg-error-container/30 rounded-lg transition-colors"
                                  >
                                    <span className="material-symbols-outlined text-[18px]">delete</span>
                                  </button>
                                </div>
                              </td>
                            </tr>
                          );
                        })
                      )}
                    </tbody>
                  </table>
                </div>
              </div>
            )}
          </>
        )}
      </div>

      {/* Create / Edit Channel Modal */}
      {showChannelModal && (
        <div className="fixed inset-0 z-[100] flex items-center justify-center p-md bg-on-surface/40 backdrop-blur-sm">
          <div className="bg-surface-container-lowest rounded-xl shadow-xl w-full max-w-2xl p-xl relative max-h-[90vh] overflow-y-auto">
            <button
              type="button"
              onClick={() => setShowChannelModal(false)}
              className="absolute top-md right-md text-on-surface-variant hover:text-primary"
            >
              <span className="material-symbols-outlined">close</span>
            </button>
            <h3 className="font-headline-lg text-headline-lg text-on-surface mb-xs">
              {editingChannelId != null ? `Edit Sales Channel #${editingChannelId}` : 'New Sales Channel'}
            </h3>
            <p className="font-body-sm text-body-sm text-on-surface-variant mb-lg">
              {editingChannelId != null ? (
                <>Updates via <code className="text-primary">PUT /api/sales-channels/{editingChannelId}</code></>
              ) : (
                <>Creates via <code className="text-primary">POST /api/sales-channels</code></>
              )}
            </p>
            <form onSubmit={handleSaveChannel} className="flex flex-col gap-md">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-md">
                <div className="md:col-span-2">
                  <label className="font-label-md text-label-md text-on-surface-variant uppercase mb-xs block">
                    Channel Name *
                  </label>
                  <input
                    required
                    type="text"
                    value={channelForm.name}
                    onChange={(e) => setChannelForm((f) => ({ ...f, name: e.target.value }))}
                    placeholder="e.g. Online Store, Distributors, Export Directââ‚¬Â¦"
                    className="w-full h-11 px-md rounded-lg bg-surface-container border border-outline-variant/40 font-body-sm"
                  />
                </div>
              </div>

              <div className="flex gap-md mt-md">
                <button
                  type="button"
                  onClick={() => setShowChannelModal(false)}
                  className="flex-1 h-11 rounded-xl bg-surface-container font-label-md text-label-md text-on-surface hover:bg-surface-container-high transition-colors"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={savingChannel}
                  className="flex-1 h-11 rounded-xl bg-primary text-on-primary font-label-md text-label-md disabled:opacity-60 flex items-center justify-center gap-xs hover:bg-on-primary-fixed-variant transition-colors"
                >
                  {savingChannel ? (
                    <>
                      <span className="material-symbols-outlined text-[18px] animate-spin">sync</span> Savingââ‚¬Â¦
                    </>
                  ) : editingChannelId != null ? (
                    <>
                      <span className="material-symbols-outlined text-[18px]">save</span> Save Changes
                    </>
                  ) : (
                    <>
                      <span className="material-symbols-outlined text-[18px]">add</span> Create Channel
                    </>
                  )}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Create / Edit Target Modal */}
      {showTargetModal && (
        <div className="fixed inset-0 z-[100] flex items-center justify-center p-md bg-on-surface/40 backdrop-blur-sm">
          <div className="bg-surface-container-lowest rounded-xl shadow-xl w-full max-w-lg p-xl relative max-h-[90vh] overflow-y-auto">
            <button
              type="button"
              onClick={() => setShowTargetModal(false)}
              className="absolute top-md right-md text-on-surface-variant hover:text-primary"
            >
              <span className="material-symbols-outlined">close</span>
            </button>
            <h3 className="font-headline-lg text-headline-lg text-on-surface mb-xs">
              {editingTargetId != null ? `Edit Target #${editingTargetId}` : 'New Sales Target'}
            </h3>
            <p className="font-body-sm text-body-sm text-on-surface-variant mb-lg">
              {editingTargetId != null ? (
                <>Updates via <code className="text-primary">PUT /api/sales-channel-targets/{editingTargetId}</code></>
              ) : (
                <>Creates via <code className="text-primary">POST /api/sales-channel-targets</code></>
              )}
            </p>
            <form onSubmit={handleSaveTarget} className="flex flex-col gap-md">
              <div>
                <label className="font-label-md text-label-md text-on-surface-variant uppercase mb-xs block">
                  Channel *
                </label>
                <select
                  required
                  value={targetForm.channelId || ''}
                  onChange={(e) => setTargetForm((f) => ({ ...f, channelId: Number(e.target.value) }))}
                  className="w-full h-11 px-md rounded-lg bg-surface-container border border-outline-variant/40 font-body-sm"
                >
                  <option value="" disabled>Select a channelââ‚¬Â¦</option>
                  {displayChannels.map((ch) => (
                    <option key={ch.channelId || ch.key} value={ch.channelId}>
                      {ch.name}
                    </option>
                  ))}
                </select>
              </div>
              <div className="grid grid-cols-1 md:grid-cols-2 gap-md">
                <div>
                  <label className="font-label-md text-label-md text-on-surface-variant uppercase mb-xs block">
                    Period Start *
                  </label>
                  <input
                    required
                    type="date"
                    value={targetForm.periodStart}
                    onChange={(e) => setTargetForm((f) => ({ ...f, periodStart: e.target.value }))}
                    className="w-full h-11 px-md rounded-lg bg-surface-container border border-outline-variant/40 font-body-sm"
                  />
                </div>
                <div>
                  <label className="font-label-md text-label-md text-on-surface-variant uppercase mb-xs block">
                    Period End *
                  </label>
                  <input
                    required
                    type="date"
                    value={targetForm.periodEnd}
                    onChange={(e) => setTargetForm((f) => ({ ...f, periodEnd: e.target.value }))}
                    className="w-full h-11 px-md rounded-lg bg-surface-container border border-outline-variant/40 font-body-sm"
                  />
                </div>
              </div>
              <div>
                <label className="font-label-md text-label-md text-on-surface-variant uppercase mb-xs block">
                  Revenue Target (ââ€šÂ¬) *
                </label>
                <input
                  required
                  type="number"
                  min="0"
                  step="0.01"
                  value={targetForm.revenueTargetEur}
                  onChange={(e) => setTargetForm((f) => ({ ...f, revenueTargetEur: e.target.value }))}
                  placeholder="e.g. 4500000"
                  className="w-full h-11 px-md rounded-lg bg-surface-container border border-outline-variant/40 font-body-sm"
                />
              </div>

              <div className="flex gap-md mt-md">
                <button
                  type="button"
                  onClick={() => setShowTargetModal(false)}
                  className="flex-1 h-11 rounded-xl bg-surface-container font-label-md text-label-md text-on-surface hover:bg-surface-container-high transition-colors"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={savingTarget}
                  className="flex-1 h-11 rounded-xl bg-primary text-on-primary font-label-md text-label-md disabled:opacity-60 flex items-center justify-center gap-xs hover:bg-on-primary-fixed-variant transition-colors"
                >
                  {savingTarget ? (
                    <>
                      <span className="material-symbols-outlined text-[18px] animate-spin">sync</span> Savingââ‚¬Â¦
                    </>
                  ) : editingTargetId != null ? (
                    <>
                      <span className="material-symbols-outlined text-[18px]">save</span> Save Changes
                    </>
                  ) : (
                    <>
                      <span className="material-symbols-outlined text-[18px]">add</span> Create Target
                    </>
                  )}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Delete channel confirmation */}
      {pendingDeleteChannel && (
        <div className="fixed inset-0 z-[100] flex items-center justify-center p-md bg-on-surface/40 backdrop-blur-sm">
          <div className="bg-surface-container-lowest rounded-xl shadow-xl w-full max-w-md p-xl">
            <h3 className="font-headline-lg text-headline-lg text-on-surface mb-sm">Delete channel?</h3>
            <p className="font-body-sm text-body-sm text-on-surface-variant mb-lg">
              This calls <code className="text-primary">DELETE /api/sales-channels/{pendingDeleteChannel.channelId}</code> for{' '}
              <strong>{pendingDeleteChannel.name}</strong>. This cannot be undone.
            </p>
            <div className="flex gap-md">
              <button
                type="button"
                onClick={() => setPendingDeleteChannel(null)}
                className="flex-1 h-11 rounded-xl bg-surface-container font-label-md text-label-md hover:bg-surface-container-high transition-colors"
              >
                Cancel
              </button>
              <button
                type="button"
                disabled={deletingChannel}
                onClick={confirmDeleteChannel}
                className="flex-1 h-11 rounded-xl bg-error text-on-error font-label-md text-label-md disabled:opacity-60 hover:opacity-90 transition-opacity"
              >
                {deletingChannel ? 'Deletingââ‚¬Â¦' : 'Delete'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Delete target confirmation */}
      {pendingDeleteTarget && (
        <div className="fixed inset-0 z-[100] flex items-center justify-center p-md bg-on-surface/40 backdrop-blur-sm">
          <div className="bg-surface-container-lowest rounded-xl shadow-xl w-full max-w-md p-xl">
            <h3 className="font-headline-lg text-headline-lg text-on-surface mb-sm">Delete target?</h3>
            <p className="font-body-sm text-body-sm text-on-surface-variant mb-lg">
              This calls <code className="text-primary">DELETE /api/sales-channel-targets/{pendingDeleteTarget.targetId}</code> —{' '}
              {pendingDeleteTarget.periodStart} ââ€ â€™ {pendingDeleteTarget.periodEnd} ({formatRevenue(pendingDeleteTarget.revenueTargetEur)}).
              This cannot be undone.
            </p>
            <div className="flex gap-md">
              <button
                type="button"
                onClick={() => setPendingDeleteTarget(null)}
                className="flex-1 h-11 rounded-xl bg-surface-container font-label-md text-label-md hover:bg-surface-container-high transition-colors"
              >
                Cancel
              </button>
              <button
                type="button"
                disabled={deletingTarget}
                onClick={confirmDeleteTarget}
                className="flex-1 h-11 rounded-xl bg-error text-on-error font-label-md text-label-md disabled:opacity-60 hover:opacity-90 transition-opacity"
              >
                {deletingTarget ? 'Deletingââ‚¬Â¦' : 'Delete'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
