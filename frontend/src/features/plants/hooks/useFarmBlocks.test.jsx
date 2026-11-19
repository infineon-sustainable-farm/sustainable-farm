// @vitest-environment jsdom
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { renderHook, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { useFarmBlocks } from "./useFarmBlocks";

const fetchFarmFields = vi.hoisted(() => vi.fn());
vi.mock("../api/plantsApi", () => ({ fetchFarmFields }));

const FIELDS = [
    { id: 1, farmId: 1, code: "B", name: "Bloc B" },
    { id: 2, farmId: 1, code: null, name: "Field without a plants code" },
    { id: 3, farmId: 2, code: "A", name: "Bloc A" },
];

function wrapper({ children }) {
    const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    return <QueryClientProvider client={client}>{children}</QueryClientProvider>;
}

describe("useFarmBlocks", () => {
    it("keeps the fields that carry a block code, sorted by code", async () => {
        fetchFarmFields.mockResolvedValue(FIELDS);
        const { result } = renderHook(() => useFarmBlocks(), { wrapper });
        await waitFor(() => expect(result.current.isSuccess).toBe(true));
        expect(result.current.data.map((block) => block.code)).toEqual(["A", "B"]);
    });

    it("keeps the blocks of one farm when a farm is given", async () => {
        fetchFarmFields.mockResolvedValue(FIELDS);
        const { result } = renderHook(() => useFarmBlocks({ farmId: 1 }), { wrapper });
        await waitFor(() => expect(result.current.isSuccess).toBe(true));
        expect(result.current.data.map((block) => block.code)).toEqual(["B"]);
    });
});
