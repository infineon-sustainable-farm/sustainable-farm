import React, { useEffect, useState } from 'react';
import { api } from '../api/api';

const CAMPAIGN_IMAGES = [
  'https://lh3.googleusercontent.com/aida-public/AB6AXuAuTjKMkgXfIM9dTXmEbLshFRrmCfwY--hiECSrdrazHY1jrRIy801H7qEpnE2tH3lDB9ymKSP8Xz6Hv6UrqSy1HMK1MAPo7OYxFsf4_Wmj_lcXfKGoJEc9gjCJb36UZNSc-cBLGtAWJCCeHdq9DOk91_8iB1Odlj6n47kY6nCEpzkuG-9gYzKrN9GLwOKnZ9P2fpI1B5rzLjZ44fRoJE3HKtRFPCPjEx9NyQb8oDUR-x6A7igE34Of',
  'https://lh3.googleusercontent.com/aida-public/AB6AXuCpqVe-6kzL3j7pX5ptnwwBgzGgOyXxb0E3gBOK5PoXRjZ5q4C2XGKA_4FBt0Rp3H2WlpEAGX2osUWXm_hVqKZ20YYWTyI1UHgja6Uv6_1aWP6iCrlmAeJcrp110WHrTHW2XXzUIHDGZ5FgPqWkN6mWc_niK-9MKT_OIv0IxOy2LsaxIVSW9ms1DFLTucej7BfKC6v5S2GYUkInE9EACdqqWSjkbdxMWanF8ZhJjuBl-PYDbB9-MtG8',
];

function formatMoney(amount, currency = 'EUR') {
  if (amount == null || amount === '') return '--';
  const n = Number(amount);
  const symbol = currency === 'USD' || currency === '$' ? '$' : 'EUR';
  if (n >= 1000) return `${symbol}${(n / 1000).toFixed(n % 1000 === 0 ? 0 : 1)}k`;
  return `${symbol}${n.toLocaleString()}`;
}

function formatMoneyFull(amount, currency = 'EUR') {
  if (amount == null) return '--';
  const symbol = currency === 'USD' || currency === '$' ? '$' : 'EUR';
  return `${symbol}${Number(amount).toLocaleString()}`;
}

function formatDateRange(start, end) {
  if (!start && !end) return 'TBD';
  const fmt = (d) => {
    const date = new Date(d);
    if (isNaN(date.getTime())) return d;
    return date.toLocaleDateString('en-US', { month: 'short', day: 'numeric' });
  };
  return `${fmt(start)} - ${fmt(end)}`;
}

function normalizeStatus(status) {
  return (status || 'draft').toLowerCase();
}

function statusMeta(status) {
  const s = normalizeStatus(status);
  if (s === 'active') return { label: 'Active', dot: 'bg-primary', text: 'text-primary', opacity: '', imageStyle: '' };
  if (s === 'scheduled') return { label: 'Scheduled', dot: 'bg-secondary', text: 'text-secondary', opacity: 'opacity-90', imageStyle: 'grayscale-[30%]' };
  if (s === 'ended') return { label: 'Ended', dot: 'bg-outline-variant', text: 'text-on-surface-variant', opacity: 'opacity-60', imageStyle: '' };
  return { label: 'Draft', dot: 'border border-outline-variant', text: 'text-on-surface-variant', opacity: '', imageStyle: '', draft: true };
}

function getErrorMessage(err, fallback = 'Request failed') {
  const data = err?.response?.data;
  if (!data) return err?.message || fallback;
  if (typeof data === 'string') return data;
  return data.message || fallback;
}

function emptyCampaignForm() {
  return { name: '', status: 'draft', budgetEur: '', budgetSpentEur: '', estimatedReach: '', currency: 'EUR', startDate: '', endDate: '' };
}

function buildMetricPaths(metrics) {
  if (metrics.length < 2) return null;
  const max = Math.max(...metrics.flatMap((m) => [Number(m.clicks || 0), Number(m.signups || 0)]), 1);
  const line = (key) => metrics.map((m, i) => {
    const x = (i / (metrics.length - 1)) * 800;
    const y = 180 - (Number(m[key] || 0) / max) * 160;
    return `${i === 0 ? 'M' : 'L'}${x},${y}`;
  }).join(' ');
  const clicks = line('clicks');
  return { clicks, signups: line('signups'), area: `${clicks} L800,200 L0,200 Z` };
}

export default function CampaignsPage() {
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

  useEffect(() => {
    fetchCampaigns();
  }, []);

  const fetchCampaigns = async () => {
    setLoading(true);
    setApiError(null);
    try {
      const [campaignRes, metricRes] = await Promise.allSettled([
        api.getCampaigns(),
        api.getCampaignMetrics(),
      ]);

      // An empty array is a valid response: it means no campaigns have been created yet.
      if (campaignRes.status === 'fulfilled') {
        setCampaigns(Array.isArray(campaignRes.value.data) ? campaignRes.value.data : []);
      } else {
        setCampaigns([]);
        setApiError(
          `Unable to load campaigns: ${getErrorMessage(campaignRes.reason)}`
        );
      }

      // Metrics are optional for the page. If unavailable, keep the chart in its
      // normal empty state instead of presenting an empty campaign list as an API error.
      if (metricRes.status === 'fulfilled') {
        setMetrics(Array.isArray(metricRes.value.data) ? metricRes.value.data : []);
      } else {
        setMetrics([]);
      }
    } catch (err) {
      setApiError(getErrorMessage(err, 'Error connecting to Spring Boot backend API at http://localhost:8080/api'));
      setCampaigns([]);
      setMetrics([]);
    } finally {
      setLoading(false);
    }
  };

  const activeCampaign = campaigns.find((c) => normalizeStatus(c.status) === 'active') || campaigns[0];
  const activeMetrics = activeCampaign
    ? metrics.filter((metric) => metric.campaignId === activeCampaign.campaignId).sort((a, b) => new Date(a.metricDate) - new Date(b.metricDate)).slice(-30)
    : [];
  const metricPaths = buildMetricPaths(activeMetrics);

  const spent = Number(activeCampaign?.budgetSpentEur ?? 0);
  const budget = Number(activeCampaign?.budgetEur ?? 0);
  const progress = budget > 0 ? Math.min(100, Math.round((spent / budget) * 100)) : 0;

  const openCreate = () => { setEditingCampaign(null); setCampaignForm(emptyCampaignForm()); setShowModal(true); };
  const openEdit = async (campaign) => {
    try {
      const res = await api.getCampaignById(campaign.campaignId);
      const c = res.data;
      setCampaignForm({ name: c.name || '', status: c.status || 'draft', budgetEur: c.budgetEur ?? '', budgetSpentEur: c.budgetSpentEur ?? '', estimatedReach: c.estimatedReach ?? '', currency: c.currency || 'EUR', startDate: c.startDate || '', endDate: c.endDate || '' });
      setEditingCampaign(c);
      setShowModal(true);
    } catch (err) { setActionError(getErrorMessage(err, `Unable to load campaign #${campaign.campaignId} for editing.`)); }
  };
  const saveCampaign = async (event) => {
    event.preventDefault(); setSaving(true); setActionError(null);
    const payload = { ...campaignForm, budgetEur: campaignForm.budgetEur === '' ? null : Number(campaignForm.budgetEur), budgetSpentEur: campaignForm.budgetSpentEur === '' ? null : Number(campaignForm.budgetSpentEur), estimatedReach: campaignForm.estimatedReach === '' ? null : Number(campaignForm.estimatedReach) };
    try {
      if (editingCampaign) await api.updateCampaign(editingCampaign.campaignId, payload); else await api.createCampaign(payload);
      setShowModal(false); setActionSuccess(editingCampaign ? 'Campaign updated successfully.' : 'Campaign created successfully.'); await fetchCampaigns();
    } catch (err) { setActionError(getErrorMessage(err, 'Failed to save campaign')); } finally { setSaving(false); }
  };
  const deleteCampaign = async () => {
    if (!pendingDelete) return; setDeleting(true);
    try { await api.deleteCampaign(pendingDelete.campaignId); setPendingDelete(null); setActionSuccess('Campaign deleted successfully.'); await fetchCampaigns(); }
    catch (err) { setActionError(getErrorMessage(err, 'Failed to delete campaign')); } finally { setDeleting(false); }
  };

  return (
    <div className="flex flex-col w-full h-full p-xxl gap-xxl">
      <div className="flex justify-between items-end relative z-10 w-full mb-md">
        <div>
          <h2 className="font-headline-lg text-headline-lg text-on-surface mb-unit">Campaign Hub</h2>
          <p className="font-body-md text-body-md text-on-surface-variant max-w-2xl">
            Monitor active initiatives and plan upcoming promotions across all sales channels.
          </p>
        </div>
        <button type="button" onClick={openCreate} className="bg-primary text-on-primary font-label-md text-label-md px-lg py-sm rounded-lg flex items-center gap-xs hover:bg-primary-fixed hover:text-on-primary-fixed transition-colors shadow-sm hover:shadow-md">
          <span className="material-symbols-outlined text-[18px]">add</span>
          NEW CAMPAIGN
        </button>
      </div>

      {apiError && (
        <div className="bg-secondary-container/20 border-l-4 border-secondary p-md rounded-xl flex items-center justify-between shadow-sm relative z-10">
          <div className="flex items-center gap-md">
            <span className="material-symbols-outlined text-secondary text-[24px]">wifi_off</span>
            <div>
              <p className="font-label-md text-label-md text-on-surface font-bold">API Connection Notice</p>
              <p className="font-body-sm text-body-sm text-on-surface-variant">{apiError}</p>
            </div>
          </div>
          <button
            onClick={fetchCampaigns}
            className="px-md py-xs bg-secondary text-on-secondary font-label-md text-label-md rounded-lg hover:opacity-90 transition-opacity flex items-center gap-xs"
          >
            <span className="material-symbols-outlined text-[16px]">refresh</span> Retry
          </button>
        </div>
      )}

      {actionError && <div className="bg-error-container border-l-4 border-error p-md rounded-xl flex justify-between"><p className="font-body-sm text-on-error-container">{actionError}</p><button type="button" onClick={() => setActionError(null)}><span className="material-symbols-outlined">close</span></button></div>}
      {actionSuccess && <div className="bg-[#e8f5e9] border-l-4 border-[#2e7d32] p-md rounded-xl flex justify-between"><p className="font-body-sm text-[#1b5e20]">{actionSuccess}</p><button type="button" onClick={() => setActionSuccess(null)}><span className="material-symbols-outlined">close</span></button></div>}

      {loading ? (
        <div className="p-xl bg-surface-container rounded-xl text-center relative z-10">
          <span className="material-symbols-outlined text-[32px] text-primary animate-spin">sync</span>
          <p className="font-body-sm text-body-sm text-on-surface-variant mt-sm">Loading campaigns...</p>
        </div>
      ) : (
        <>
          <div className="grid grid-cols-12 gap-gutter w-full relative z-10">
            <div className="col-span-12 md:col-span-4 bg-surface-container rounded-xl shadow-sm p-lg flex flex-col relative overflow-hidden group">
              <div className="absolute -right-12 -top-12 w-48 h-48 bg-primary/10 rounded-full blur-3xl pointer-events-none transition-transform group-hover:scale-110 duration-700" />
              <div className="flex items-start justify-between mb-lg relative z-10">
                <span className="bg-primary-container/10 text-primary-container px-sm py-xs rounded font-label-md text-[10px] tracking-widest">
                  ACTIVE NOW
                </span>
                <button type="button" onClick={() => activeCampaign && openEdit(activeCampaign)} className="text-on-surface-variant hover:text-primary transition-colors">
                  <span className="material-symbols-outlined">more_horiz</span>
                </button>
              </div>
              <h3 className="font-headline-md text-headline-md text-on-surface mb-xs relative z-10 leading-tight">
                {activeCampaign?.name || 'No active campaign'}
              </h3>
              <p className="font-body-sm text-body-sm text-on-surface-variant mb-xl relative z-10">
                {activeCampaign ? 'Live campaign budget and performance overview.' : 'Create a campaign to start tracking performance.'}
              </p>
              <div className="grid grid-cols-2 gap-md mt-auto relative z-10">
                <div>
                  <p className="font-label-md text-[10px] text-on-surface-variant uppercase tracking-wider mb-unit">Budget Spent</p>
                  <p className="font-headline-sm text-headline-sm text-on-surface">
                    {activeCampaign ? formatMoneyFull(spent, activeCampaign.currency) : '--'}{' '}
                    <span className="font-body-sm text-body-sm text-on-surface-variant">
                      / {activeCampaign && budget ? (budget / 1000).toFixed(0) + 'k' : '--'}
                    </span>
                  </p>
                </div>
                <div>
                  <p className="font-label-md text-[10px] text-on-surface-variant uppercase tracking-wider mb-unit">Est. Reach</p>
                  <p className="font-headline-sm text-headline-sm text-on-surface">
                    {activeCampaign?.estimatedReach != null ? Number(activeCampaign.estimatedReach).toLocaleString() : '--'}
                  </p>
                </div>
              </div>
              <div className="w-full bg-outline-variant/30 h-1 mt-md rounded-full relative z-10">
                <div className="bg-primary h-full rounded-full" style={{ width: `${progress}%` }} />
              </div>
            </div>

            <div className="col-span-12 md:col-span-8 bg-surface-container rounded-xl shadow-sm p-lg flex flex-col relative">
              <div className="flex justify-between items-center mb-xl">
                <h3 className="font-headline-sm text-headline-sm text-on-surface">Conversion Trajectory</h3>
                <div className="flex items-center gap-sm">
                  <span className="flex items-center gap-xs font-label-md text-label-md text-on-surface-variant">
                    <span className="w-2 h-2 rounded-full bg-primary" /> Click-through
                  </span>
                  <span className="flex items-center gap-xs font-label-md text-label-md text-on-surface-variant">
                    <span className="w-2 h-2 rounded-full bg-secondary" /> Sign-ups
                  </span>
                </div>
              </div>
              <div className="flex-1 w-full h-full min-h-[200px] relative">
                {metricPaths ? <svg className="w-full h-full absolute inset-0" preserveAspectRatio="none" viewBox="0 0 800 200">
                  <defs>
                    <linearGradient id="campaignGrad1" x1="0%" x2="0%" y1="0%" y2="100%">
                      <stop className="text-primary" offset="0%" stopColor="currentColor" stopOpacity="0.2" />
                      <stop className="text-primary" offset="100%" stopColor="currentColor" stopOpacity="0" />
                    </linearGradient>
                  </defs>
                  <line className="text-on-surface" stroke="currentColor" strokeOpacity="0.1" strokeWidth="1" x1="0" x2="800" y1="50" y2="50" />
                  <line className="text-on-surface" stroke="currentColor" strokeOpacity="0.1" strokeWidth="1" x1="0" x2="800" y1="100" y2="100" />
                  <line className="text-on-surface" stroke="currentColor" strokeOpacity="0.1" strokeWidth="1" x1="0" x2="800" y1="150" y2="150" />
                  <path d={metricPaths.area} fill="url(#campaignGrad1)" />
                  <path className="text-secondary opacity-70" d={metricPaths.signups} fill="none" stroke="currentColor" strokeDasharray="4 4" strokeWidth="2" />
                  <path className="text-primary" d={metricPaths.clicks} fill="none" stroke="currentColor" strokeWidth="3" />
                </svg> : <div className="absolute inset-0 flex items-center justify-center text-center px-md font-body-sm text-on-surface-variant">{activeCampaign ? 'Not enough campaign metrics to draw a trajectory yet.' : 'Create a campaign to start tracking conversion metrics.'}</div>}
              </div>
            </div>
          </div>

          <div className="w-full relative z-10 flex flex-col gap-md">
            <h3 className="font-headline-sm text-headline-sm text-on-surface mb-sm flex items-center gap-sm">
              <span className="material-symbols-outlined text-primary text-[20px]">view_list</span>
              All Campaigns
            </h3>
            <div className="grid grid-cols-12 gap-gutter w-full">
              {campaigns.map((campaign, idx) => {
                const meta = statusMeta(campaign.status);
                const isDraft = meta.draft || normalizeStatus(campaign.status) === 'draft';
                const isEnded = normalizeStatus(campaign.status) === 'ended';
                const moneyLabel = isEnded ? 'Spent' : 'Budget';
                const moneyValue = isEnded
                  ? formatMoney(campaign.budgetSpentEur ?? campaign.budgetEur, campaign.currency)
                  : formatMoney(campaign.budgetEur, campaign.currency);

                return (
                  <div
                    key={campaign.campaignId || idx}
                    onClick={() => openEdit(campaign)}
                    className={`col-span-12 lg:col-span-6 xl:col-span-4 rounded-xl p-md flex items-center gap-md hover:shadow-md transition-shadow cursor-pointer ${
                      isDraft
                        ? 'border border-dashed border-outline-variant bg-transparent'
                        : `bg-surface-container ${meta.opacity}`
                    }`}
                  >
                    {isDraft ? (
                      <div className="w-16 h-16 rounded-lg bg-surface flex-shrink-0 flex items-center justify-center border border-dashed border-outline-variant">
                        <span className="material-symbols-outlined text-outline-variant">edit</span>
                      </div>
                    ) : isEnded ? (
                      <div className="w-16 h-16 rounded-lg bg-outline-variant/20 flex-shrink-0 flex items-center justify-center">
                        <span className="material-symbols-outlined text-outline-variant">inventory_2</span>
                      </div>
                    ) : (
                      <div className="w-16 h-16 rounded-lg bg-surface flex-shrink-0 overflow-hidden relative">
                        <img
                          className={`w-full h-full object-cover mix-blend-multiply opacity-80 ${meta.imageStyle}`}
                          alt=""
                          src={CAMPAIGN_IMAGES[idx % CAMPAIGN_IMAGES.length]}
                        />
                      </div>
                    )}
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center gap-sm mb-xs">
                        <span className={`w-2 h-2 rounded-full ${meta.dot}`} />
                        <p className={`font-label-md text-[10px] tracking-wider uppercase ${meta.text}`}>{meta.label}</p>
                      </div>
                      <h4 className={`font-body-md text-body-md font-semibold text-on-surface truncate ${isDraft ? 'italic' : ''}`}>
                        {campaign.name}
                      </h4>
                      <p className="font-body-sm text-[12px] text-on-surface-variant truncate">
                        {formatDateRange(campaign.startDate, campaign.endDate)}
                      </p>
                    </div>
                    <div className={`text-right flex-shrink-0 ${isDraft ? 'opacity-50' : ''}`}>
                      <p className="font-body-md text-body-md font-medium text-on-surface">{moneyValue}</p>
                      <p className="font-body-sm text-[11px] text-on-surface-variant">{moneyLabel}</p>
                    </div>
                    <button type="button" onClick={(event) => { event.stopPropagation(); setPendingDelete(campaign); }} className="text-on-surface-variant hover:text-error"><span className="material-symbols-outlined text-[18px]">delete</span></button>
                  </div>
                );
              })}
              {campaigns.length === 0 && (
                <div className="col-span-12 rounded-xl border border-dashed border-outline-variant bg-surface-container/40 px-lg py-2xl text-center">
                  <span className="material-symbols-outlined text-[36px] text-on-surface-variant mb-sm">campaign</span>
                  <h4 className="font-headline-sm text-headline-sm text-on-surface mb-xs">
                    No campaigns yet
                  </h4>
                  <p className="font-body-sm text-body-sm text-on-surface-variant max-w-md mx-auto mb-lg">
                    Your campaign list is empty. Create your first campaign to start tracking budgets, reach, and performance.
                  </p>
                  <button
                    type="button"
                    onClick={openCreate}
                    className="bg-primary text-on-primary font-label-md text-label-md px-lg py-sm rounded-lg inline-flex items-center gap-xs hover:opacity-90 transition-opacity"
                  >
                    <span className="material-symbols-outlined text-[18px]">add</span>
                    Create your first campaign
                  </button>
                </div>
              )}
            </div>
          </div>
        </>
      )}

      {showModal && (
        <div className="fixed inset-0 z-[100] flex items-center justify-center p-md bg-on-surface/40 backdrop-blur-sm">
          <div className="bg-surface-container-lowest rounded-xl shadow-xl w-full max-w-lg p-xl relative max-h-[90vh] overflow-y-auto">
            <button
              type="button"
              onClick={() => setShowModal(false)}
              className="absolute top-md right-md text-on-surface-variant hover:text-primary"
            >
              <span className="material-symbols-outlined">close</span>
            </button>
            <h3 className="font-headline-lg text-headline-lg text-on-surface mb-xs">
              {editingCampaign ? `Edit Campaign #${editingCampaign.campaignId}` : 'New Campaign'}
            </h3>
            <form onSubmit={saveCampaign} className="flex flex-col gap-md">
              <div>
                <label className="font-label-md text-label-md text-on-surface-variant uppercase mb-xs block">Campaign Name *</label>
                <input
                  required
                  value={campaignForm.name}
                  onChange={(e) => setCampaignForm((f) => ({ ...f, name: e.target.value }))}
                  className="w-full h-11 px-md rounded-lg bg-surface-container font-body-sm"
                />
              </div>

              <div className="grid grid-cols-2 gap-md">
                <div>
                  <label className="font-label-md text-label-md text-on-surface-variant uppercase mb-xs block">Status</label>
                  <select
                    value={campaignForm.status}
                    onChange={(e) => setCampaignForm((f) => ({ ...f, status: e.target.value }))}
                    className="w-full h-11 px-md rounded-lg bg-surface-container font-body-sm"
                  >
                    <option value="draft">Draft</option>
                    <option value="scheduled">Scheduled</option>
                    <option value="active">Active</option>
                    <option value="ended">Ended</option>
                  </select>
                </div>
                <div>
                  <label className="font-label-md text-label-md text-on-surface-variant uppercase mb-xs block">Currency</label>
                  <select
                    value={campaignForm.currency}
                    onChange={(e) => setCampaignForm((f) => ({ ...f, currency: e.target.value }))}
                    className="w-full h-11 px-md rounded-lg bg-surface-container font-body-sm"
                  >
                    <option value="EUR">EUR</option>
                  </select>
                </div>
              </div>

              <div className="grid grid-cols-2 gap-md">
                <div>
                  <label className="font-label-md text-label-md text-on-surface-variant uppercase mb-xs block">Budget (EUR)</label>
                  <input
                    type="number"
                    min="0"
                    step="0.01"
                    value={campaignForm.budgetEur}
                    onChange={(e) => setCampaignForm((f) => ({ ...f, budgetEur: e.target.value }))}
                    className="w-full h-11 px-md rounded-lg bg-surface-container font-body-sm"
                  />
                </div>
                <div>
                  <label className="font-label-md text-label-md text-on-surface-variant uppercase mb-xs block">Spent (EUR)</label>
                  <input
                    type="number"
                    min="0"
                    step="0.01"
                    value={campaignForm.budgetSpentEur}
                    onChange={(e) => setCampaignForm((f) => ({ ...f, budgetSpentEur: e.target.value }))}
                    className="w-full h-11 px-md rounded-lg bg-surface-container font-body-sm"
                  />
                </div>
              </div>

              <div>
                <label className="font-label-md text-label-md text-on-surface-variant uppercase mb-xs block">Estimated Reach</label>
                <input
                  type="number"
                  min="0"
                  value={campaignForm.estimatedReach}
                  onChange={(e) => setCampaignForm((f) => ({ ...f, estimatedReach: e.target.value }))}
                  className="w-full h-11 px-md rounded-lg bg-surface-container font-body-sm"
                />
              </div>

              <div className="grid grid-cols-2 gap-md">
                <div>
                  <label className="font-label-md text-label-md text-on-surface-variant uppercase mb-xs block">Start Date</label>
                  <input
                    type="date"
                    value={campaignForm.startDate}
                    onChange={(e) => setCampaignForm((f) => ({ ...f, startDate: e.target.value }))}
                    className="w-full h-11 px-md rounded-lg bg-surface-container font-body-sm"
                  />
                </div>
                <div>
                  <label className="font-label-md text-label-md text-on-surface-variant uppercase mb-xs block">End Date</label>
                  <input
                    type="date"
                    value={campaignForm.endDate}
                    onChange={(e) => setCampaignForm((f) => ({ ...f, endDate: e.target.value }))}
                    className="w-full h-11 px-md rounded-lg bg-surface-container font-body-sm"
                  />
                </div>
              </div>

              <div className="flex gap-md mt-md">
                <button
                  type="button"
                  onClick={() => setShowModal(false)}
                  className="flex-1 h-11 rounded-xl bg-surface-container font-label-md text-label-md"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={saving}
                  className="flex-1 h-11 rounded-xl bg-primary text-on-primary font-label-md text-label-md disabled:opacity-60"
                >
                  {saving ? 'Saving...' : editingCampaign ? 'Update' : 'Create'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {pendingDelete && (
        <div className="fixed inset-0 z-[100] flex items-center justify-center p-md bg-on-surface/40 backdrop-blur-sm">
          <div className="bg-surface-container-lowest rounded-xl shadow-xl w-full max-w-md p-xl">
            <h3 className="font-headline-lg text-headline-lg text-on-surface mb-sm">Delete campaign?</h3>
            <p className="font-body-sm text-body-sm text-on-surface-variant mb-lg">
              This calls <code className="text-primary">DELETE /api/campaigns/{pendingDelete.campaignId}</code> for{' '}
              <strong>{pendingDelete.name}</strong>. This cannot be undone.
            </p>
            <p className="font-body-sm text-body-sm text-on-surface-variant mb-lg">
                {formatDateRange(pendingDelete.startDate, pendingDelete.endDate)} - {pendingDelete.status || 'draft'}
            </p>
            <div className="flex gap-md">
              <button
                type="button"
                onClick={() => setPendingDelete(null)}
                className="flex-1 h-11 rounded-xl bg-surface-container font-label-md text-label-md"
              >
                Cancel
              </button>
              <button
                type="button"
                disabled={deleting}
                onClick={deleteCampaign}
                className="flex-1 h-11 rounded-xl bg-error text-on-error font-label-md text-label-md disabled:opacity-60"
              >
                {deleting ? 'Deleting...' : 'Delete'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
