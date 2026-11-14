import { useEffect, useState } from "react";
import { MailCheck } from "lucide-react";
import { fetchUserEmails, fetchUserHistory } from "../api/sitesecurityApi";
import { actorLabel, canConfirmDelivery, formatInstant, historyActionLabel } from "../utils/userStatus";
import { Eyebrow } from "./ui";
import EmailStateLine from "./EmailStateLine";

function useLoaded(load, id) {
    const [state, setState] = useState({ data: null, error: null });
    useEffect(() => {
        let active = true;
        load(id)
            .then((data) => active && setState({ data: data ?? [], error: null }))
            .catch((err) => active && setState({ data: null, error: err.message }));
        return () => {
            active = false;
        };
    }, [load, id]);
    return state;
}

function Section({ title, state, empty, children }) {
    return (<section className="space-y-3">
      <Eyebrow>{title}</Eyebrow>
      {state.error && <div role="alert" className="rounded-xl border border-sec-red/30 bg-sec-red/5 px-4 py-3 font-sans text-[13px] text-sec-red">{state.error}</div>}
      {!state.error && !state.data && <p role="status" className="font-sans text-[13px] text-slate-600">Loading…</p>}
      {state.data && state.data.length === 0 && <p className="font-sans text-[13px] text-slate-600">{empty}</p>}
      {state.data && state.data.length > 0 && children(state.data)}
    </section>);
}

/* Mounted with a key per user (and per refresh), so each load starts fresh. */
export default function UserHistoryContent({ userId, onConfirmDelivery }) {
    const emails = useLoaded(fetchUserEmails, userId);
    const history = useLoaded(fetchUserHistory, userId);
    return (<div className="space-y-6">
      <Section title="Emails" state={emails} empty="No email was sent about this account.">
        {(list) => (<ul className="space-y-3">
            {list.map((delivery) => (<li key={delivery.deliveryId} className="rounded-xl border border-slate-100 px-4 py-3">
                <EmailStateLine delivery={delivery}/>
                {canConfirmDelivery(delivery) && (<button type="button" onClick={() => onConfirmDelivery(delivery)} className="mt-2 inline-flex items-center gap-1.5 font-sans text-[12px] font-semibold text-primary-700 hover:underline">
                    <MailCheck className="size-3.5"/>
                    Confirm real delivery
                  </button>)}
              </li>))}
          </ul>)}
      </Section>
      <Section title="Events" state={history} empty="No events recorded.">
        {(list) => (<ol className="space-y-3">
            {list.map((entry, i) => (<li key={`${entry.at}-${i}`} className="rounded-xl border border-slate-100 px-4 py-3">
                <p className="font-sans text-[13px] font-semibold text-navy-900">{historyActionLabel(entry.action)}</p>
                <p className="mt-0.5 font-mono text-[11px] text-slate-600">{formatInstant(entry.at)} · {actorLabel(entry.actor)}</p>
                {entry.detail && <p className="mt-1.5 font-sans text-[12.5px] text-slate-600">{entry.detail}</p>}
              </li>))}
          </ol>)}
      </Section>
    </div>);
}
