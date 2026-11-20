import { useQueries } from "@tanstack/react-query";
import { fetchBlockRainfall } from "../api/plantsApi";
import { rainfallState, rainfallWindow } from "../utils/rainfall";

/**
 * The rain measured over the last 30 days for each of the given blocks, as a
 * map from the block code to what its cell shows (see `rainfallState`).
 *
 * One read per distinct block, kept ten minutes. A refused read is not tried
 * again: a block linked to no rain gauge answers 404 every time.
 */
export function useBlockRainfall(blockCodes) {
    const blocks = [...new Set(blockCodes.filter(Boolean))].sort();
    const { from, to } = rainfallWindow(new Date());

    const queries = useQueries({
        queries: blocks.map((blockCode) => ({
            queryKey: ["plants", "block-rainfall", { blockCode, from, to }],
            queryFn: () => fetchBlockRainfall({ blockCode, from, to }),
            retry: false,
            staleTime: 10 * 60 * 1000,
        })),
    });

    return Object.fromEntries(blocks.map((blockCode, index) => [blockCode, rainfallState(queries[index])]));
}
