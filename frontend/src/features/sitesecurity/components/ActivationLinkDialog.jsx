import { useState } from "react";
import { AlertTriangle, Copy, Link2 } from "lucide-react";
import { createActivationLink } from "../api/sitesecurityApi";
import { formatInstant } from "../utils/userStatus";
import { INPUT_CLASS, PRIMARY_BUTTON, SECONDARY_BUTTON } from "../utils/formClasses";
import { ErrorAlert } from "./formUi";
import Modal from "./Modal";

/*
 * Fallback when email is down. The link is fetched only after the admin
 * confirms (the previous link stops working), shown once, and dropped on close.
 */
export default function ActivationLinkDialog({ user, onClose, onCreated }) {
    const [result, setResult] = useState(null);
    const [copied, setCopied] = useState(false);
    const [error, setError] = useState(null);
    const [isSubmitting, setIsSubmitting] = useState(false);

    const close = () => {
        setResult(null);
        setCopied(false);
        setError(null);
        onClose();
    };

    const create = async () => {
        setIsSubmitting(true);
        setError(null);
        try {
            setResult(await createActivationLink(user.id));
            onCreated?.();
        }
        catch (err) {
            setError(err.message || "The link could not be created.");
        }
        finally {
            setIsSubmitting(false);
        }
    };

    const copy = async () => {
        try {
            await navigator.clipboard.writeText(result.link);
            setCopied(true);
        }
        catch {
            setError("Copying failed: select the link and copy it by hand.");
        }
    };

    return (<Modal open={Boolean(user)} onClose={close} eyebrow="Fallback" title="Activation link">
      <div className="space-y-4">
        <div className="flex gap-2.5 rounded-xl border border-amber-200 bg-amber-50 px-4 py-3 font-sans text-[12.5px] text-amber-800">
          <AlertTriangle className="mt-0.5 size-4 shrink-0"/>
          <p>
            Anyone with this link can activate the account of {user?.fullName || user?.email}. Give it to that person only, by a channel you trust.
            Creating it makes the previous link stop working. It is shown only once.
          </p>
        </div>
        <ErrorAlert>{error}</ErrorAlert>
        {result ? (<>
            <label className="block">
              <span className="mb-1.5 block text-[12px] font-semibold text-slate-600">Link (valid until {formatInstant(result.expiresAt)})</span>
              <input readOnly value={result.link} onFocus={(e) => e.target.select()} className={`${INPUT_CLASS} font-mono text-[12px]`}/>
            </label>
            <div className="flex justify-end gap-2.5">
              <button type="button" onClick={copy} className={SECONDARY_BUTTON}>
                <Copy className="size-4"/>
                {copied ? "Copied" : "Copy link"}
              </button>
              <button type="button" data-autofocus onClick={close} className={PRIMARY_BUTTON}>Done</button>
            </div>
          </>) : (<div className="flex justify-end gap-2.5">
            <button type="button" onClick={close} className={SECONDARY_BUTTON}>Cancel</button>
            <button type="button" data-autofocus onClick={create} disabled={isSubmitting} className={PRIMARY_BUTTON}>
              <Link2 className="size-4"/>
              Create link
            </button>
          </div>)}
      </div>
    </Modal>);
}
