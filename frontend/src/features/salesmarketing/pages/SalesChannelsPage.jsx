import { useCallback, useEffect, useState } from 'react';
import { Goal, Plus } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { api } from '../api/api';
import { Badge, Button, ConfirmDialog, PageHeader, Skeleton } from '../../../shared/ui';
import Notice from '../components/common/Notice';
import ChannelCard from '../components/channels/ChannelCard';
import ChannelComparisonChart from '../components/channels/ChannelComparisonChart';
import ChannelFormModal from '../components/channels/ChannelFormModal';
import TargetFormModal from '../components/channels/TargetFormModal';
import TargetsTable from '../components/channels/TargetsTable';
import {
  emptyChannelForm,
  emptyTargetForm,
  enrichChannel,
  getErrorMessage,
  useRevenueFormat,
} from '../components/channels/channelModel';

export default function SalesChannelsPage() {
  const { t } = useTranslation('salesmarketing');
  const formatRevenue = useRevenueFormat();
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

  const fetchChannelsData = useCallback(async () => {
    const requestFailed = t('channels.requestFailed');
    try {
      const [channelRes, targetRes, orderRes] = await Promise.allSettled([
        api.getSalesChannels(),
        api.getSalesChannelTargets(),
        api.getOrders(),
      ]);
      setApiError(null);

      const failures = [];

      if (channelRes.status === 'fulfilled') {
        setChannels(Array.isArray(channelRes.value) ? channelRes.value : []);
      } else {
        setChannels([]);
        failures.push(t('channels.failure.channels', { message: getErrorMessage(channelRes.reason, requestFailed) }));
      }

      if (targetRes.status === 'fulfilled') {
        setTargets(Array.isArray(targetRes.value) ? targetRes.value : []);
      } else {
        setTargets([]);
        failures.push(t('channels.failure.targets', { message: getErrorMessage(targetRes.reason, requestFailed) }));
      }
      if (orderRes.status === 'fulfilled') {
        setOrders(Array.isArray(orderRes.value) ? orderRes.value : []);
      } else {
        setOrders([]);
        failures.push(t('channels.failure.orders', { message: getErrorMessage(orderRes.reason, requestFailed) }));
      }

      const joined = failures.join(t('channels.failureSeparator'));
      if (failures.length > 0 && channelRes.status === 'rejected') {
        setApiError(t('channels.loadFailed', { failures: joined }));
      } else if (failures.length > 0) {
        setApiError(t('channels.partialLoad', { failures: joined }));
      }
    } catch (err) {
      setApiError(getErrorMessage(err, t('channels.loadDataFailed')));
      setChannels([]);
      setTargets([]);
    } finally {
      setLoading(false);
    }
  }, [t]);

  useEffect(() => {
    void Promise.resolve().then(fetchChannelsData);
  }, [fetchChannelsData]);

  const displayChannels = channels.map((ch, i) => enrichChannel(ch, i, targets, t));
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

  const filteredTargets = selectedTargetChannelId
    ? targets.filter((target) => target.channelId === selectedTargetChannelId)
    : targets;
  const comparisonChannels = displayChannels.map((channel) => ({
    ...channel,
    actualRevenueEur: actualRevenueByChannel[channel.channelId] ?? 0,
  }));

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
      setChannelForm({ name: c.name || '' });
      setEditingChannelId(channel.channelId);
      setShowChannelModal(true);
    } catch (err) {
      flashError(getErrorMessage(err, t('channels.loadChannelForEditFailed', { id: channel.channelId })));
    } finally {
      setSavingChannel(false);
    }
  };

  const handleSaveChannel = async (e) => {
    e.preventDefault();
    setSavingChannel(true);
    setActionError(null);
    try {
      const payload = { name: channelForm.name };
      if (editingChannelId != null) {
        await api.updateSalesChannel(editingChannelId, payload);
        flashSuccess(t('channels.channelUpdated', { name: payload.name }));
      } else {
        await api.createSalesChannel(payload);
        flashSuccess(t('channels.channelCreated', { name: payload.name }));
      }
      setShowChannelModal(false);
      await fetchChannelsData();
    } catch (err) {
      flashError(
        getErrorMessage(err, editingChannelId != null ? t('channels.updateChannelFailed') : t('channels.createChannelFailed'))
      );
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
      flashSuccess(t('channels.channelDeleted', { name }));
      await fetchChannelsData();
    } catch (err) {
      flashError(getErrorMessage(err, t('channels.deleteChannelFailed')));
    } finally {
      setDeletingChannel(false);
    }
  };

  const openCreateTarget = (channelId = null) => {
    const useId = channelId || selectedTargetChannelId || displayChannels[0]?.channelId || null;
    if (useId == null) {
      flashError(t('channels.selectChannelFirst'));
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
      const found = res.data || target;
      setTargetForm({
        channelId: found.channelId,
        periodStart: found.periodStart ? new Date(found.periodStart).toISOString().slice(0, 10) : '',
        periodEnd: found.periodEnd ? new Date(found.periodEnd).toISOString().slice(0, 10) : '',
        revenueTargetEur: found.revenueTargetEur != null ? String(found.revenueTargetEur) : '',
      });
      setEditingTargetId(target.targetId);
      setShowTargetModal(true);
    } catch (err) {
      flashError(getErrorMessage(err, t('channels.loadTargetForEditFailed', { id: target.targetId })));
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
        flashSuccess(t('channels.targetUpdated'));
      } else {
        await api.createSalesChannelTarget(payload);
        flashSuccess(t('channels.targetCreated'));
      }
      setShowTargetModal(false);
      await fetchChannelsData();
    } catch (err) {
      flashError(
        getErrorMessage(err, editingTargetId != null ? t('channels.updateTargetFailed') : t('channels.createTargetFailed'))
      );
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
      flashSuccess(t('channels.targetDeleted'));
      await fetchChannelsData();
    } catch (err) {
      flashError(getErrorMessage(err, t('channels.deleteTargetFailed')));
    } finally {
      setDeletingTarget(false);
    }
  };

  return (
    <div className="space-y-6 p-6">
      <PageHeader
        title={t('channels.title')}
        subtitle={t('channels.subtitle')}
        actions={
          <>
            <Badge tone={apiError ? 'warning' : 'success'} size="md">
              {apiError ? t('channels.offline') : t('channels.liveSync')}
            </Badge>
            <Button variant={showTargets ? 'primary' : 'secondary'} onClick={() => setShowTargets((s) => !s)} aria-pressed={showTargets}>
              {showTargets ? t('channels.hideTargets') : t('channels.manageTargets')}
            </Button>
            <Button variant="secondary" icon={Goal} onClick={() => openCreateTarget()}>
              {t('channels.addTarget')}
            </Button>
            <Button icon={Plus} onClick={openCreateChannel}>
              {t('channels.newChannel')}
            </Button>
          </>
        }
      />

      {apiError && (
        <Notice tone="offline" title={t('channels.apiNotice')} onRetry={fetchChannelsData}>
          {apiError}
        </Notice>
      )}
      {actionError && (
        <Notice tone="error" title={t('channels.actionFailed')} onDismiss={() => setActionError(null)}>
          {actionError}
        </Notice>
      )}
      {actionSuccess && (
        <Notice tone="success" onDismiss={() => setActionSuccess(null)}>
          {actionSuccess}
        </Notice>
      )}

      {loading ? (
        <div aria-busy="true" className="space-y-4">
          <p role="status" className="text-sm text-text-muted">
            {t('channels.loading')}
          </p>
          <div className="grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-4">
            {[0, 1, 2, 3].map((index) => (
              <Skeleton key={index} radius="card" className="h-48" />
            ))}
          </div>
        </div>
      ) : (
        <>
          <div className="grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-4">
            {displayChannels.map((channel) => (
              <ChannelCard
                key={channel.channelId || channel.key}
                channel={channel}
                menuOpen={openMenuChannelId === channel.channelId}
                onToggleMenu={() =>
                  setOpenMenuChannelId(openMenuChannelId === channel.channelId ? null : channel.channelId)
                }
                onEdit={() => {
                  setOpenMenuChannelId(null);
                  openEditChannel(channel);
                }}
                onAddTarget={() => {
                  setOpenMenuChannelId(null);
                  setSelectedTargetChannelId(channel.channelId);
                  setShowTargets(true);
                  openCreateTarget(channel.channelId);
                }}
                onDelete={() => {
                  setOpenMenuChannelId(null);
                  setPendingDeleteChannel(channel);
                }}
              />
            ))}
          </div>

          <ChannelComparisonChart channels={comparisonChannels} />

          {showTargets && (
            <TargetsTable
              targets={filteredTargets}
              channels={displayChannels}
              selectedChannelId={selectedTargetChannelId}
              onSelectChannel={setSelectedTargetChannelId}
              onCreate={openCreateTarget}
              onEdit={openEditTarget}
              onDelete={setPendingDeleteTarget}
            />
          )}
        </>
      )}

      <ChannelFormModal
        open={showChannelModal}
        editingId={editingChannelId}
        form={channelForm}
        setForm={setChannelForm}
        saving={savingChannel}
        onClose={() => setShowChannelModal(false)}
        onSubmit={handleSaveChannel}
      />

      <TargetFormModal
        open={showTargetModal}
        editingId={editingTargetId}
        form={targetForm}
        setForm={setTargetForm}
        channels={displayChannels}
        saving={savingTarget}
        onClose={() => setShowTargetModal(false)}
        onSubmit={handleSaveTarget}
      />

      <ConfirmDialog
        open={Boolean(pendingDeleteChannel)}
        onClose={() => setPendingDeleteChannel(null)}
        onConfirm={confirmDeleteChannel}
        loading={deletingChannel}
        title={t('channels.deleteChannelTitle')}
        confirmLabel={deletingChannel ? t('channels.deleting') : t('common:actions.delete')}
        message={pendingDeleteChannel && t('channels.deleteChannelMessage', { name: pendingDeleteChannel.name })}
      />

      <ConfirmDialog
        open={Boolean(pendingDeleteTarget)}
        onClose={() => setPendingDeleteTarget(null)}
        onConfirm={confirmDeleteTarget}
        loading={deletingTarget}
        title={t('channels.deleteTargetTitle')}
        confirmLabel={deletingTarget ? t('channels.deleting') : t('common:actions.delete')}
        message={
          pendingDeleteTarget &&
          t('channels.deleteTargetMessage', {
            start: pendingDeleteTarget.periodStart,
            end: pendingDeleteTarget.periodEnd,
            revenue: formatRevenue(pendingDeleteTarget.revenueTargetEur),
          })
        }
      />
    </div>
  );
}
