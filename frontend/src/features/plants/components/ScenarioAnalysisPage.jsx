import { useEffect, useRef, useState } from "react";
import { Loader2, Plus, TriangleAlert } from "lucide-react";
import { useSearchParams } from "react-router-dom";
import { useSaveScenario } from "../hooks/useSaveScenario";
import { useScenarioAnalysis } from "../hooks/useScenarioAnalysis";
import { useScenarios } from "../hooks/useScenarios";
import { analysisParams, readScenarioView, scenarioViewSearch } from "../utils/scenarioRequests";
import AnnualBenefitTable from "./AnnualBenefitTable";
import PlantsEmptyState from "./PlantsEmptyState";
import ScenarioAssumptionsTable from "./ScenarioAssumptionsTable";
import ScenarioBaselineSection from "./ScenarioBaselineSection";
import ScenarioFormModal from "./ScenarioFormModal";
import ScenarioLeverCards from "./ScenarioLeverCards";
import ScenarioYieldChart from "./ScenarioYieldChart";
import YieldIncreaseSimulator from "./YieldIncreaseSimulator";

function LoadingBox({ label }) {
    return (
        <div className="flex items-center gap-3 rounded-xl border border-gray-200 bg-white px-5 py-4 text-sm text-gray-500">
            <Loader2 size={18} className="animate-spin" />
            {label}
        </div>
    );
}

function ErrorBox({ title, message, action }) {
    return (
        <div className="flex flex-wrap items-center justify-between gap-4 rounded-xl border border-error/30 bg-error/5 px-5 py-4">
            <div className="flex items-start gap-3">
                <TriangleAlert size={20} className="mt-0.5 shrink-0 text-error" />
                <div>
                    <p className="text-sm font-bold text-gray-900">{title}</p>
                    {/* gray-600: #4a5565 on the tinted box (#f6f1f1) -> 6.76:1, as in the other sections. */}
                    <p className="mt-0.5 text-sm text-gray-600">{message}</p>
                </div>
            </div>
            {action}
        </div>
    );
}

function ActionButton({ onClick, disabled, children }) {
    return (
        <button
            type="button"
            onClick={onClick}
            disabled={disabled}
            className="rounded-lg bg-primary px-4 py-2 text-sm font-semibold text-white hover:opacity-90 disabled:opacity-60"
        >
            {children}
        </button>
    );
}

/**
 * Scenario Analysis: a planting plan entered by the user, what it gives in a
 * year of full production and year by year, and what a variant of it would
 * change, to support a go/no-go decision. Every figure is computed by the API
 * from the assumptions; nothing is computed here.
 *
 * The scenario shown and the variants explored are kept in the address, so a
 * view can be shared. Outside the dev profile there is no scenario until the
 * user enters a baseline. The mock-up's Home button is left out, as on the
 * other screens: the sidebar leads to the Overview.
 */
export default function ScenarioAnalysisPage() {
    const [searchParams, setSearchParams] = useSearchParams();
    const scenarios = useScenarios();
    const saveScenario = useSaveScenario();
    // undefined: no form; null: a new scenario; otherwise the scenario to correct.
    const [formScenario, setFormScenario] = useState(undefined);
    const enterButtonRef = useRef(null);
    const editButtonRef = useRef(null);
    const addButtonRef = useRef(null);
    // Which button opened the form, so focus goes back to it when the form closes.
    const openedFrom = useRef(null);

    useEffect(() => {
        document.title = "Scenario Analysis — Plants";
    }, []);

    // The view lives in the page, read once from the address and copied back to it whole: an input
    // driven by the address alone would lag behind the router, and two changes in a row would race.
    const [view, setView] = useState(() => readScenarioView(searchParams));

    useEffect(() => {
        const search = scenarioViewSearch(view);
        if (search !== searchParams.toString()) setSearchParams(search, { replace: true });
    }, [view, searchParams, setSearchParams]);

    const list = scenarios.data ?? [];
    const scenario = view.scenarioId === null
        ? list[0] ?? null
        : list.find((candidate) => candidate.id === view.scenarioId) ?? null;
    const isUnknown = scenarios.isSuccess && view.scenarioId !== null && scenario === null;
    const levers = { trees: view.trees, yieldPct: view.yieldPct, loss: view.loss };

    const baseline = useScenarioAnalysis(scenario?.id ?? null, analysisParams({ horizonYears: view.horizonYears }), {
        keepPrevious: true,
    });
    // Asked of the API only for an identifier the list does not hold, so its 404 is what the screen says.
    const unknown = useScenarioAnalysis(view.scenarioId, {}, { enabled: isUnknown });

    function changeView(changes) {
        setView((current) => ({ ...current, ...changes }));
    }

    function openForm(target, opener) {
        saveScenario.reset();
        openedFrom.current = opener;
        setFormScenario(target);
    }

    function closeForm() {
        saveScenario.reset();
        setFormScenario(undefined);
        const opener = { enter: enterButtonRef, edit: editButtonRef, add: addButtonRef }[openedFrom.current]?.current;
        (opener?.isConnected ? opener : editButtonRef.current)?.focus();
    }

    function submitForm(body) {
        const id = formScenario === null ? null : formScenario.id;
        saveScenario.mutate({ id, scenario: body }, {
            onSuccess: (saved) => {
                if (id === null) changeView({ scenarioId: saved.id });
                closeForm();
            },
        });
    }

    return (
        <div className="min-h-dvh bg-gray-50">
            {/* pt-20 under md: the fixed menu button would otherwise cover the title on a phone. */}
            <div className="mx-auto flex max-w-6xl flex-col gap-8 px-4 pt-20 pb-8 sm:px-6 md:pt-8">
                <header>
                    {/* primary-dark, not primary: #065e55 on the gray-50 page -> 7.33:1. */}
                    <p className="font-heading text-xs font-bold tracking-widest text-primary-dark uppercase">
                        Strategic Planning
                    </p>
                    <h2 className="font-heading mt-1 text-3xl font-bold text-gray-900">Scenario Analysis</h2>
                    <p className="mt-2 max-w-xl text-sm text-gray-500">
                        {"Cost-benefit analysis if Plants' mango output increases, to support a go/no-go decision."}
                    </p>
                </header>

                {scenarios.isPending && <LoadingBox label="Loading the scenarios…" />}

                {scenarios.isError && (
                    <ErrorBox
                        title="The scenarios could not be loaded"
                        message="The request to the server did not succeed."
                        action={(
                            <ActionButton onClick={() => scenarios.refetch()} disabled={scenarios.isFetching}>
                                {scenarios.isFetching ? "Retrying…" : "Retry"}
                            </ActionButton>
                        )}
                    />
                )}

                {scenarios.isSuccess && list.length === 0 && (
                    <PlantsEmptyState
                        title="No baseline scenario entered yet"
                        hint="Enter the assumptions of a planting plan: trees, yield per tree, costs and investment. The server computes every result from them."
                        action={(
                            <button
                                ref={enterButtonRef}
                                type="button"
                                onClick={() => openForm(null, "enter")}
                                className="inline-flex items-center gap-2 rounded-lg bg-primary px-4 py-2 text-sm font-semibold text-white hover:opacity-90"
                            >
                                <Plus size={16} />
                                Enter baseline
                            </button>
                        )}
                    />
                )}

                {isUnknown && (
                    <ErrorBox
                        title="This scenario cannot be shown"
                        message={unknown.isError ? unknown.error.message : "Looking for the scenario…"}
                        action={list.length > 0 && (
                            <ActionButton onClick={() => changeView({ scenarioId: null })}>
                                {`Show ${list[0].name}`}
                            </ActionButton>
                        )}
                    />
                )}

                {scenario && (
                    <>
                        <ScenarioBaselineSection
                            scenarios={list}
                            scenario={scenario}
                            query={baseline}
                            onSelect={(id) => changeView({ scenarioId: id })}
                            onEdit={() => openForm(scenario, "edit")}
                            onAdd={() => openForm(null, "add")}
                            editButtonRef={editButtonRef}
                            addButtonRef={addButtonRef}
                        />

                        <ScenarioAssumptionsTable
                            scenario={scenario}
                            phaseShares={baseline.data?.analysis.phaseShares ?? null}
                        />

                        <YieldIncreaseSimulator
                            scenario={scenario}
                            yieldIncrease={view.yieldIncrease}
                            onYieldIncreaseChange={(value) => changeView({ yieldIncrease: value })}
                            investmentText={view.investment}
                            onInvestmentChange={(text) => changeView({ investment: text })}
                        />

                        <ScenarioYieldChart scenario={scenario} />

                        <ScenarioLeverCards
                            scenario={scenario}
                            levers={levers}
                            onChange={(key, text) => changeView({ [key]: text })}
                        />

                        <AnnualBenefitTable scenario={scenario} levers={levers} />
                    </>
                )}
            </div>

            {formScenario !== undefined && (
                <ScenarioFormModal
                    scenario={formScenario}
                    isSubmitting={saveScenario.isPending}
                    error={saveScenario.error}
                    onSubmit={submitForm}
                    onClose={closeForm}
                />
            )}
        </div>
    );
}
