import { useEffect, useState } from "react";
import { Loader2, TriangleAlert } from "lucide-react";
import {
    useCreateStaff,
    useStaffList,
    useStaffStatusAction,
    useUpdateStaff,
} from "../../hooks/useStaff";
import { formatEnumLabel } from "../../../../shared/utils/formatEnumLabel";
import StaffForm from "../Forms/StaffForm";
import StaffTable from "../StaffTable";

const ROLES = ["GUIDE", "RECEPTION", "FARM", "MANAGER"];

export default function StaffPage() {
    const [formState, setFormState] = useState({ open: false, staff: null });
    const [formKey, setFormKey] = useState(0);
    const [roleFilter, setRoleFilter] = useState("");

    useEffect(() => {
        document.title = "Staff — Visitor Management";
    }, []);

    const {
        data: staff,
        isPending,
        isError,
        refetch,
        isFetching,
    } = useStaffList();

    const createMutation = useCreateStaff();
    const updateMutation = useUpdateStaff();
    const statusMutation = useStaffStatusAction();

    const editingStaff = formState.staff;
    const formMutation = editingStaff ? updateMutation : createMutation;

    const visibleStaff = (staff ?? []).filter(
        (member) => roleFilter === "" || member.role === roleFilter,
    );

    function resetForm() {
        setFormState({ open: false, staff: null });
        setFormKey((current) => current + 1);
        createMutation.reset();
        updateMutation.reset();
    }

    function openCreate() {
        createMutation.reset();
        updateMutation.reset();
        setFormState({ open: true, staff: null });
        setFormKey((current) => current + 1);
    }

    function openEdit(member) {
        createMutation.reset();
        updateMutation.reset();
        setFormState({ open: true, staff: member });
        setFormKey((current) => current + 1);
    }

    function handleSubmit(values) {
        if (editingStaff) {
            updateMutation.mutate(
                { id: editingStaff.id, data: values },
                { onSuccess: resetForm },
            );
        } else {
            createMutation.mutate(values, { onSuccess: resetForm });
        }
    }

    const pendingAction = statusMutation.isPending ? statusMutation.variables : null;
    const actionError = statusMutation.isError
        ? { id: statusMutation.variables?.staff?.id, message: statusMutation.error.message }
        : null;

    return (
        <div className="min-h-full bg-[#F5F7FA] px-8 py-7">
            <section className="rounded-lg border border-line bg-white p-7">
                <h2 className="font-heading mb-5 inline-block border-b-[3px] border-accent pb-2 text-2xl font-bold text-primary-dark">
                    Staff &amp; guides
                </h2>

                <div className="mb-4 flex flex-wrap items-end gap-3.5">
                    <div>
                        <label
                            htmlFor="staff-role-filter"
                            className="mb-1 block text-xs tracking-wide text-primary uppercase"
                        >
                            Role
                        </label>
                        <select
                            id="staff-role-filter"
                            value={roleFilter}
                            onChange={(event) => setRoleFilter(event.target.value)}
                            className="min-w-[150px] rounded-md border border-line bg-[#F7FDFB] px-2.5 py-2 text-sm"
                        >
                            <option value="">All roles</option>
                            {ROLES.map((role) => (
                                <option key={role} value={role}>
                                    {formatEnumLabel(role)}
                                </option>
                            ))}
                        </select>
                    </div>
                    <button
                        type="button"
                        onClick={openCreate}
                        className="ml-auto rounded-md bg-primary px-5 py-2.5 text-xs font-semibold tracking-wider text-white uppercase hover:bg-primary-dark"
                    >
                        + New staff member
                    </button>
                </div>

                {formState.open && (
                    <StaffForm
                        key={formKey}
                        staff={editingStaff}
                        isSubmitting={formMutation.isPending}
                        submitError={formMutation.error?.message}
                        serverFieldErrors={formMutation.error?.data?.fieldErrors}
                        onSubmit={handleSubmit}
                        onCancel={resetForm}
                    />
                )}

                {isPending && (
                    <div className="flex items-center justify-center gap-3 rounded-lg border border-line bg-white px-6 py-16 text-sm text-muted">
                        <Loader2 size={18} className="animate-spin" />
                        Loading staff…
                    </div>
                )}

                {isError && (
                    <div className="flex flex-col items-center gap-4 rounded-lg border border-error/30 bg-error/5 px-6 py-16 text-center">
                        <TriangleAlert size={28} className="text-error" />
                        <div>
                            <p className="font-heading text-base font-bold text-ink">
                                Staff could not be loaded
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
                        <StaffTable
                            staff={visibleStaff}
                            pendingAction={pendingAction}
                            actionError={actionError}
                            onAction={(member, action) =>
                                statusMutation.mutate({ staff: member, action })
                            }
                            onEdit={openEdit}
                        />
                        <p className="mt-6 text-[11px] text-muted">
                            Guides assigned to a time slot and staff recorded on a briefing come from
                            this list. Deactivating a member keeps the record but removes them from
                            every selector.
                        </p>
                    </>
                )}
            </section>
        </div>
    );
}