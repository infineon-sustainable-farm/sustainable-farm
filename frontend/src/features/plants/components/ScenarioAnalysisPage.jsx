import { useEffect, useRef, useState } from "react";
import { Plus } from "lucide-react";
import { useTranslation } from "react-i18next";
import { useSearchParams } from "react-router-dom";
import { Button, Card, EmptyState, PageHeader } from "../../../shared/ui";
import { useSaveScenario } from "../hooks/useSaveScenario";
import { useScenarioAnalysis } from "../hooks/useScenarioAnalysis";
import { useScenarios } from "../hooks/useScenarios";
import { analysisParams, readScenarioView, scenarioViewSearch } from "../utils/scenarioRequests";
import AnnualBenefitTable from "./AnnualBenefitTable";
import CashFlowByYearTable from "./CashFlowByYearTable";
import ScenarioAssumptionsTable from "./ScenarioAssumptionsTable";
import ScenarioBaselineSection from "./ScenarioBaselineSection";
import ScenarioFormModal from "./ScenarioFormModal";
import ScenarioLeverCards from "./ScenarioLeverCards";
import { ErrorBox, LoadingBox, RetryButton } from "./ScenarioStatus";
import ScenarioYieldChart from "./ScenarioYieldChart";
import YieldIncreaseSimulator from "./YieldIncreaseSimulator";

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
    const { t } = useTranslation("plants");
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
        document.title = t("scenarios.documentTitle");
    }, [t]);

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
        <div className="p-6 space-y-6">
            <PageHeader title={t("scenarios.title")} subtitle={t("scenarios.subtitle")} />

            {scenarios.isPending && <LoadingBox label={t("scenarios.loading")} />}

            {scenarios.isError && (
                <ErrorBox
                    title={t("scenarios.loadError")}
                    message={t("scenarios.requestFailed")}
                    action={<RetryButton onClick={() => scenarios.refetch()} isFetching={scenarios.isFetching} />}
                />
            )}

            {scenarios.isSuccess && list.length === 0 && (
                <Card>
                    <EmptyState
                        title={t("scenarios.emptyTitle")}
                        message={t("scenarios.emptyHint")}
                        action={(
                            <Button ref={enterButtonRef} icon={Plus} onClick={() => openForm(null, "enter")}>
                                {t("scenarios.enterBaseline")}
                            </Button>
                        )}
                    />
                </Card>
            )}

            {isUnknown && (
                <ErrorBox
                    title={t("scenarios.unknownTitle")}
                    message={unknown.isError ? unknown.error.message : t("scenarios.unknownLooking")}
                    action={list.length > 0 && (
                        <Button onClick={() => changeView({ scenarioId: null })}>
                            {t("scenarios.showScenario", { name: list[0].name })}
                        </Button>
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

                    <CashFlowByYearTable
                        query={baseline}
                        horizonYears={view.horizonYears}
                        onHorizonChange={(years) => changeView({ horizonYears: years })}
                    />
                </>
            )}

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
