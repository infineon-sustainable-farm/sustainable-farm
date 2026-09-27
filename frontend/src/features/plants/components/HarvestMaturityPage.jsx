import { useEffect, useState } from "react";
import { useVarietyBlocks } from "../hooks/useVarieties";
import BlockFilter from "./BlockFilter";
import YieldForecastSection from "./YieldForecastSection";

/**
 * Harvest & Maturity: the expected yield by month first, as it is what the
 * screen is for. One block filter drives the whole page.
 *
 * The mock-up's maturity status, estimated harvest date, variance, lot number
 * and upcoming deadlines are left out: the API records none of them.
 */
export default function HarvestMaturityPage() {
    // Raw stored block value ("A"); an empty string means "no filter".
    const [selectedBlock, setSelectedBlock] = useState("");

    useEffect(() => {
        document.title = "Harvest & Maturity — Plants";
    }, []);

    const { data: blocks } = useVarietyBlocks();

    return (
        <div className="min-h-dvh bg-gray-50">
            <div className="mx-auto flex max-w-6xl flex-col gap-6 px-4 py-8 sm:px-6">
                <header className="flex flex-wrap items-start justify-between gap-4">
                    <div>
                        <p className="font-heading text-xs font-bold tracking-widest text-primary uppercase">
                            Task 5
                        </p>
                        <h2 className="font-heading mt-1 text-3xl font-bold text-gray-900">
                            Harvest &amp; Maturity
                        </h2>
                        <p className="mt-2 max-w-xl text-sm text-gray-500">
                            Recorded harvests and expected yield by month, by block.
                        </p>
                    </div>
                </header>

                <BlockFilter blocks={blocks ?? []} value={selectedBlock} onChange={setSelectedBlock} />

                <YieldForecastSection blockCode={selectedBlock || null} />
            </div>
        </div>
    );
}
