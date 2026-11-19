import { useEffect, useState } from "react";
import { Pencil, Plus, Trash2 } from "lucide-react";
import { useTranslation } from "react-i18next";
import {
    useCreateVisitor,
    useDeleteVisitor,
    useUpdateVisitor,
    useVisitors,
} from "../../hooks/useVisitors";
import {
    Button,
    ConfirmDialog,
    Field,
    IconButton,
    Input,
    Modal,
    PageHeader,
    Table,
} from "../../../../shared/ui";
import { useVisitorFormat } from "../../utils/format";
import { useLabels } from "../../utils/labels";
import { LoadError, LoadingBlock } from "../LoadState";
import VisitorForm from "../Forms/VisitorForm";

/*
 * The API has no search parameter and the visitor base of a farm is small, so the search filters
 * the loaded list client-side rather than querying the server per keystroke.
 */
function matchesSearch(visitor, query) {
    if (!query) return true;
    const needle = query.trim().toLowerCase();
    return [visitor.fullName, visitor.email, visitor.phone, visitor.language]
        .filter(Boolean)
        .some((value) => value.toLowerCase().includes(needle));
}

export default function VisitorsPage() {
    const { t } = useTranslation("visitormanagement");
    const format = useVisitorFormat();
    const labels = useLabels();
    const [search, setSearch] = useState("");
    const [formState, setFormState] = useState({ open: false, visitor: null });
    const [formKey, setFormKey] = useState(0);
    const [deleteTarget, setDeleteTarget] = useState(null);

    useEffect(() => {
        document.title = t("visitors.documentTitle");
    }, [t]);

    const { data: visitors, isPending, isError, refetch, isFetching } = useVisitors();

    const createMutation = useCreateVisitor();
    const updateMutation = useUpdateVisitor();
    const deleteMutation = useDeleteVisitor();

    const editingVisitor = formState.visitor;
    const formMutation = editingVisitor ? updateMutation : createMutation;
    const pendingDeleteId = deleteMutation.isPending ? deleteMutation.variables : null;

    const visibleVisitors = (visitors ?? []).filter((visitor) => matchesSearch(visitor, search));

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
            updateMutation.mutate({ id: editingVisitor.id, visitor: values }, { onSuccess: resetForm });
        } else {
            createMutation.mutate(values, { onSuccess: resetForm });
        }
    }

    function openDelete(visitor) {
        deleteMutation.reset();
        setDeleteTarget(visitor);
    }

    function closeDelete() {
        deleteMutation.reset();
        setDeleteTarget(null);
    }

    /*
     * Deletion asks for a confirmation because it is a hard delete: the backend accepts it only
     * while the visitor has no registrations, feedback or surveys attached. A refusal stays in
     * the dialog.
     */
    function handleDelete() {
        deleteMutation.mutate(deleteTarget.id, { onSuccess: () => setDeleteTarget(null) });
    }

    const columns = [
        { key: "fullName", header: t("visitors.columns.name") },
        { key: "type", header: t("visitors.columns.type"), cell: (visitor) => labels.enum(visitor.type) },
        { key: "groupSize", header: t("visitors.columns.group") },
        { key: "email", header: t("visitors.columns.email"), cell: (visitor) => visitor.email ?? format.none },
        {
            key: "phone",
            header: t("visitors.columns.phone"),
            className: "whitespace-nowrap",
            cell: (visitor) => visitor.phone ?? format.none,
        },
        {
            key: "language",
            header: t("visitors.columns.language"),
            cell: (visitor) => (visitor.language ? labels.language(visitor.language) : format.none),
        },
        {
            key: "specialNeeds",
            header: t("visitors.columns.specialNeeds"),
            className: "max-w-60",
            cell: (visitor) => visitor.specialNeeds ?? format.none,
        },
    ];

    function renderContent() {
        if (isPending) return <LoadingBlock label={t("visitors.loading")} />;
        if (isError) {
            return <LoadError title={t("visitors.loadError")} onRetry={() => refetch()} retrying={isFetching} />;
        }
        return (
            <>
                <Table
                    caption={t("visitors.title")}
                    columns={columns}
                    rows={visibleVisitors}
                    emptyMessage={search ? t("visitors.emptySearch") : t("visitors.empty")}
                    rowActions={(visitor) => (
                        <>
                            <IconButton icon={Pencil} label={t("visitors.actions.edit")} onClick={() => openEdit(visitor)} />
                            <IconButton
                                icon={Trash2}
                                tone="danger"
                                label={
                                    pendingDeleteId === visitor.id
                                        ? t("visitors.actions.deleting")
                                        : t("visitors.actions.delete")
                                }
                                disabled={pendingDeleteId === visitor.id}
                                onClick={() => openDelete(visitor)}
                            />
                        </>
                    )}
                />
                <p className="mt-6 text-xs text-text-muted">{t("visitors.footnote")}</p>
            </>
        );
    }

    return (
        <div className="p-6">
            <PageHeader
                className="mb-4"
                title={t("visitors.title")}
                actions={
                    <Button icon={Plus} onClick={openCreate}>
                        {t("visitors.new")}
                    </Button>
                }
            />

            <div className="mb-4 flex flex-wrap items-end gap-4">
                <Field id="visitor-search" className="min-w-56 flex-1" label={t("visitors.search")}>
                    <Input
                        type="search"
                        value={search}
                        onChange={(event) => setSearch(event.target.value)}
                        placeholder={t("visitors.searchPlaceholder")}
                    />
                </Field>
            </div>

            {renderContent()}

            <Modal
                open={formState.open}
                onClose={resetForm}
                size="xl"
                dismissible={!formMutation.isPending}
                title={
                    editingVisitor
                        ? t("visitors.editTitle", { name: editingVisitor.fullName })
                        : t("visitors.newTitle")
                }
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

            <ConfirmDialog
                open={Boolean(deleteTarget)}
                title={t("visitors.deleteTitle")}
                message={deleteTarget ? t("visitors.deleteMessage", { name: deleteTarget.fullName }) : ""}
                confirmLabel={t("visitors.confirmYes")}
                cancelLabel={t("visitors.confirmNo")}
                onClose={closeDelete}
                onConfirm={handleDelete}
                loading={deleteMutation.isPending}
                error={deleteMutation.isError ? deleteMutation.error.message : null}
            />
        </div>
    );
}
