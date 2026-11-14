import { Link } from "react-router-dom";
import { INVALID_LINK_MESSAGE, authErrorMessage } from "../utils/authErrors";
import { LINK_BUTTON } from "../utils/formClasses";
import { ErrorAlert } from "./formUi";

/** Loading, invalid and unreachable states of an emailed link. Internal links only. */
export default function LinkStatus({ state, error }) {
    if (state === "loading") {
        return <p role="status" className="mt-4 font-sans text-[13px] text-slate-600">Checking your link…</p>;
    }
    return (<div className="mt-5 space-y-4">
      <ErrorAlert>{state === "invalid" ? INVALID_LINK_MESSAGE : authErrorMessage(error, INVALID_LINK_MESSAGE, "The link could not be checked. Try again later.")}</ErrorAlert>
      <Link to="/sitesecurity" className={`${LINK_BUTTON} block text-center`}>Go to sign in</Link>
    </div>);
}
