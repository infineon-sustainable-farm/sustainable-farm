import { useEffect, useState } from "react";
import { Loader2, TriangleAlert } from "lucide-react";
import {
    useCreateVisitor,
    useUpdateVisitor,
    useVisitors,
} from "../../hooks/useVisitors";
import { formatEnumLabel } from "../../../../shared/utils/formatEnumLabel";
import Modal from "../Modal";
import VisitorForm from "../Forms/VisitorForm";

const TINY_BUTTON =
    "rounded border border-[#C3D8D4] bg-white px-1.5 py-0.5 text-[11px] font-semibold text-primary hover:bg-[#ECFAF7] disabled:opacity-60";

const COLUMNS = [
    "Name",
    "Type",
    "Group",
    "Email",
    "Phone",
    "Language",
    "Special needs",
    "Actions",
];

/*
 * The API has no search parameter and the visitor base of a farm is small, so
 * the search filters the loaded list client-side rather than querying the
 * server per keystroke.
 */
function matchesSearch(visitor, query) {
    if (!query) return true;
    const needle = query.trim().toLowerCase();
    return [visitor.fullName, visitor.email, visitor.phone, visitor.language]
        .filter(Boolean)
        .some((value) => value.toLowerCase().includes(needle));
}

export default function VisitorsPage() {
    const [search, setSearch] = useState("");
    const [formState, setFormState] = useState({ open: false, visitor: null });
    const [formKey, setFormKey] = useState(0);

    useEffect(() => {
        document.title = "Visitors — Visitor Management";
    }, []);

    const {
        data: visitors,
        isPending,
        isError,
        refetch,
        isFetching,
    } = useVisitors();

    const createMutation = useCreateVisitor();
    const updateMutation = useUpdateVisitor();

    const editingVisitor = formState.visitor;
    const formMutation = editingVisitor ? updateMutation : createMutation;

    const visibleVisitors = (visitors ?? []).filter((visitor) =>
        matchesSearch(visitor, search),
    );

    function resetForm() {
        setFormState({ open: false, visitor: null });
        setFormKey((current) => current + 1);
        createMutation.reset();
        updateMutation.reset();
    }

    function openCreate() {
        createMutation.reset();
        updateMutation.reset();
        setFormState({ open: true, visitor: null });
        setFormKey((current) => current + 1);
    }

    function openEdit(visitor) {
        createMutation.reset();
        updateMutation.reset();
        setFormState({ open: true, visitor });
        setFormKey((current) => current + 1);
    }

    function handleSubmit(values) {
        if (editingVisitor) {
            updateMutation.mutate(
                { id: editingVisitor.id, visitor: values },
                { onSuccess: resetForm },
            );
        } else {
            createMutation.mutate(values, { onSuccess: resetForm });
        }
    }

    return (
        <div className="min-h-full bg-[#F5F7FA] px-8 py-7">
            <section className="rounded-lg border border-line bg-white p-7">
                <h2 className="font-heading mb-5 inline-block border-b-[3px] border-accent pb-2 text-2xl font-bold text-primary-dark">
                    Visitors
                </h2>

                <div className="mb-4 flex flex-wrap items-end gap-3.5">
                    <div className="min-w-[220px] flex-1">
                        <label
                            htmlFor="visitor-search"
                            className="mb-1 block text-xs tracking-wide text-primary uppercase"
                        >
                            Search
                        </label>
                        <input
                            id="visitor-search"
                            type="search"
                            value={search}
                            onChange={(event) => setSearch(event.target.value)}
                            placeholder="Name, email, phone or language"
                            className="w-full rounded-md border border-line bg-[#F7FDFB] px-2.5 py-2 text-sm"
                        />
                    </div>
                    <button
                        type="button"
                        onClick={openCreate}
                        className="rounded-md bg-primary px-5 py-2.5 text-xs font-semibold tracking-wider text-white uppercase hover:bg-primary-dark"
                    >
                        + New visitor
                    </button>
                </div>

                {formState.open && (
                    <Modal
                        title={editingVisitor ? `Edit visitor — ${editingVisitor.fullName}` : "New visitor"}
                        onClose={resetForm}
                    >
                        <VisitorForm
                            key={formKey}
                            visitor={editingVisitor}
                            isSubmitting={formMutation.isPending}
                            submitError={formMutation.error?.message}
                            serverFieldErrors={formMutation.error?.data?.fieldErrors}
                            onSubmit={handleSubmit}
                            onCancel={resetForm}
                        />
                    </Modal>
                )}

                {isPending && (
                    <div className="flex items-center justify-center gap-3 rounded-lg border border-line bg-white px-6 py-16 text-sm text-muted">
                        <Loader2 size={18} className="animate-spin" />
                        Loading visitors…
                    </div>
                )}

                {isError && (
                    <div className="flex flex-col items-center gap-4 rounded-lg border border-error/30 bg-error/5 px-6 py-16 text-center">
                        <TriangleAlert size={28} className="text-error" />
                        <div>
                            <p className="font-heading text-base font-bold text-ink">
                                Visitors could not be loaded
                            </p>
                            <p className="mt-1 text-sm text-muted">
                                The request to the server did not succeed.
                            </p>
                        </div>
                        <button
                            type="button"
                            onClick={() => refetch()}
                            disabled={isFetching}
                            className="rounded-md bg-primary px-4 py-2 text-xs font-semibold tracking-wider text-white uppercase hover:bg-primary-dark disabled:opacity-60"
                        >
                            {isFetching ? "Retrying…" : "Retry"}
                        </button>
                    </div>
                )}

                {!isPending && !isError && (
                    <>
                        <div className="overflow-x-auto">
                            <table className="w-full border-collapse text-sm">
                                <thead>
                                    <tr>
                                        {COLUMNS.map((column) => (
                                            <th
                                                key={column}
                                                scope="col"
                                                className="border-b-2 border-line bg-[#F4F8F8] px-3 py-2.5 text-left text-xs font-semibold tracking-wide whitespace-nowrap text-ink uppercase"
                                            >
                                                {column}
                                            </th>
                                        ))}
                                    </tr>
                                </thead>
                                <tbody>
                                    {visibleVisitors.length === 0 && (
                                        <tr>
                                            <td
                                                colSpan={COLUMNS.length}
                                                className="px-3 py-8 text-center text-muted"
                                            >
                                                {search
                                                    ? "No visitor matches this search."
                                                    : "No visitor yet."}
                                            </td>
                                        </tr>
                                    )}
                                    {visibleVisitors.map((visitor) => (
                                        <tr
                                            key={visitor.id}
                                            className="border-b border-line align-top"
                                        >
                                            <td className="px-3 py-2.5 font-medium text-ink">
                                                {visitor.fullName}
                                            </td>
                                            <td className="px-3 py-2.5 text-ink">
                                                {formatEnumLabel(visitor.type)}
                                            </td>
                                            <td className="px-3 py-2.5 text-ink">
                                                {visitor.groupSize}
                                            </td>
                                            <td className="px-3 py-2.5 text-ink">
                                                {visitor.email ?? "—"}
                                            </td>
                                            <td className="px-3 py-2.5 whitespace-nowrap text-ink">
                                                {visitor.phone ?? "—"}
                                            </td>
                                            <td className="px-3 py-2.5 text-ink">
                                                {visitor.language ?? "—"}
                                            </td>
                                            <td className="max-w-[240px] px-3 py-2.5 text-ink">
                                                {visitor.specialNeeds ?? "—"}
                                            </td>
                                            <td className="px-3 py-2.5">
                                                <button
                                                    type="button"
                                                    onClick={() => openEdit(visitor)}
                                                    className={TINY_BUTTON}
                                                >
                                                    ✎ edit
                                                </button>
                                            </td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        </div>
                        <p className="mt-6 text-[11px] text-muted">
                            Visitors are created here or from the registration form; the API offers
                            no deletion, and a visitor is kept even after their registrations end.
                        </p>
                    </>
                )}
            </section>
        </div>
    );
}