import { useCallback, useEffect, useState } from 'react';
import { Plus } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { api } from '../api/api';
import { Button, ConfirmDialog, PageHeader, Skeleton } from '../../../shared/ui';
import Notice from '../components/common/Notice';
import ActiveCampaignCard from '../components/campaigns/ActiveCampaignCard';
import CampaignFormModal from '../components/campaigns/CampaignFormModal';
import CampaignList from '../components/campaigns/CampaignList';
import ConversionChart from '../components/campaigns/ConversionChart';
import { emptyCampaignForm, normalizeStatus, useCampaignFormat } from '../components/campaigns/campaignFormat';

function getErrorMessage(err, fallback) {
  const data = err?.data;
  if (!data) return err?.message || (err?.status ? `${fallback} (${err.status})` : fallback);
  if (typeof data === 'string') return data;
  return data.message || fallback;
}

export default function CampaignsPage() {
  const { t } = useTranslation('salesmarketing');
  const format = useCampaignFormat();
  const [loading, setLoading] = useState(true);
  const [apiError, setApiError] = useState(null);
  const [actionError, setActionError] = useState(null);
  const [actionSuccess, setActionSuccess] = useState(null);
  const [campaigns, setCampaigns] = useState([]);
  const [metrics, setMetrics] = useState([]);
  const [showModal, setShowModal] = useState(false);
  const [editingCampaign, setEditingCampaign] = useState(null);
  const [campaignForm, setCampaignForm] = useState(emptyCampaignForm());
  const [saving, setSaving] = useState(false);
  const [pendingDelete, setPendingDelete] = useState(null);
  const [deleting, setDeleting] = useState(false);

  const fetchCampaigns = useCallback(async () => {
    const requestFailed = t('campaigns.requestFailed');
    try {
      const [campaignRes, metricRes] = await Promise.allSettled([api.getCampaigns(), api.getCampaignMetrics()]);
      setApiError(null);

      // An empty array is a valid response: it means no campaigns have been created yet.
      if (campaignRes.status === 'fulfilled') {
        setCampaigns(Array.isArray(campaignRes.value) ? campaignRes.value : []);
      } else {
        setCampaigns([]);
        setApiError(t('campaigns.loadFailed', { message: getErrorMessage(campaignRes.reason, requestFailed) }));
      }

      // Metrics are optional for the page. If unavailable, keep the chart in its
      // normal empty state instead of presenting an empty campaign list as an API error.
      if (metricRes.status === 'fulfilled') {
        setMetrics(Array.isArray(metricRes.value) ? metricRes.value : []);
      } else {
        setMetrics([]);
      }
    } catch (err) {
      setApiError(getErrorMessage(err, t('campaigns.loadDataFailed')));
      setCampaigns([]);
      setMetrics([]);
    } finally {
      setLoading(false);
    }
  }, [t]);

  useEffect(() => {
    void Promise.resolve().then(fetchCampaigns);
  }, [fetchCampaigns]);

  const activeCampaign = campaigns.find((c) => normalizeStatus(c.status) === 'active') || campaigns[0];
  const activeMetrics = activeCampaign
    ? metrics
        .filter((metric) => metric.campaignId === activeCampaign.campaignId)
        .sort((a, b) => new Date(a.metricDate) - new Date(b.metricDate))
        .slice(-30)
    : [];

  const openCreate = () => {
    setEditingCampaign(null);
    setCampaignForm(emptyCampaignForm());
    setShowModal(true);
  };
  const openEdit = async (campaign) => {
    try {
      const res = await api.getCampaignById(campaign.campaignId);
      const c = res.data;
      setCampaignForm({
        name: c.name || '',
        status: c.status || 'draft',
        budgetEur: c.budgetEur ?? '',
        budgetSpentEur: c.budgetSpentEur ?? '',
        estimatedReach: c.estimatedReach ?? '',
        currency: c.currency || 'EUR',
        startDate: c.startDate || '',
        endDate: c.endDate || '',
      });
      setEditingCampaign(c);
      setShowModal(true);
    } catch (err) {
      setActionError(getErrorMessage(err, t('campaigns.loadForEditFailed', { id: campaign.campaignId })));
    }
  };
  const saveCampaign = async (event) => {
    event.preventDefault();
    setSaving(true);
    setActionError(null);
    const payload = {
      ...campaignForm,
      budgetEur: campaignForm.budgetEur === '' ? null : Number(campaignForm.budgetEur),
      budgetSpentEur: campaignForm.budgetSpentEur === '' ? null : Number(campaignForm.budgetSpentEur),
      estimatedReach: campaignForm.estimatedReach === '' ? null : Number(campaignForm.estimatedReach),
    };
    try {
      if (editingCampaign) await api.updateCampaign(editingCampaign.campaignId, payload);
      else await api.createCampaign(payload);
      setShowModal(false);
      setActionSuccess(editingCampaign ? t('campaigns.updated') : t('campaigns.created'));
      await fetchCampaigns();
    } catch (err) {
      setActionError(getErrorMessage(err, t('campaigns.saveFailed')));
    } finally {
      setSaving(false);
    }
  };
  const deleteCampaign = async () => {
    if (!pendingDelete) return;
    setDeleting(true);
    try {
      await api.deleteCampaign(pendingDelete.campaignId);
      setPendingDelete(null);
      setActionSuccess(t('campaigns.deleted'));
      await fetchCampaigns();
    } catch (err) {
      setActionError(getErrorMessage(err, t('campaigns.deleteFailed')));
    } finally {
      setDeleting(false);
    }
  };

  return (
    <div className="space-y-6 p-6">
      <PageHeader
        title={t('campaigns.title')}
        subtitle={t('campaigns.subtitle')}
        actions={
          <Button icon={Plus} onClick={openCreate}>
            {t('campaigns.new')}
          </Button>
        }
      />

      {apiError && (
        <Notice tone="offline" title={t('campaigns.apiNotice')} onRetry={fetchCampaigns}>
          {apiError}
        </Notice>
      )}
      {actionError && (
        <Notice tone="error" onDismiss={() => setActionError(null)}>
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
            {t('campaigns.loading')}
          </p>
          <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
            <Skeleton radius="card" className="h-64" />
            <Skeleton radius="card" className="h-64 md:col-span-2" />
          </div>
        </div>
      ) : (
        <>
          <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
            <ActiveCampaignCard campaign={activeCampaign} onEdit={openEdit} />
            <ConversionChart className="md:col-span-2" metrics={activeMetrics} hasCampaign={Boolean(activeCampaign)} />
          </div>
          <CampaignList
            campaigns={campaigns}
            onCreate={openCreate}
            onEdit={openEdit}
            onDelete={setPendingDelete}
          />
        </>
      )}

      <CampaignFormModal
        open={showModal}
        editing={editingCampaign}
        form={campaignForm}
        setForm={setCampaignForm}
        saving={saving}
        onClose={() => setShowModal(false)}
        onSubmit={saveCampaign}
      />

      <ConfirmDialog
        open={Boolean(pendingDelete)}
        onClose={() => setPendingDelete(null)}
        onConfirm={deleteCampaign}
        loading={deleting}
        title={t('campaigns.deleteTitle')}
        confirmLabel={deleting ? t('campaigns.deleting') : t('common:actions.delete')}
        message={
          pendingDelete && (
            <>
              <p>{t('campaigns.deleteMessage', { name: pendingDelete.name })}</p>
              <p className="mt-2">
                {t('campaigns.deleteMeta', {
                  range: format.dateRange(pendingDelete.startDate, pendingDelete.endDate),
                  status: pendingDelete.status || 'draft',
                })}
              </p>
            </>
          )
        }
      />
    </div>
  );
}
