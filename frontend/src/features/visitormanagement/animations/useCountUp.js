import { useEffect, useState } from "react";

const REDUCED_MOTION_QUERY = "(prefers-reduced-motion: reduce)";
const REDUCED_MOTION =
    typeof window !== "undefined" && window.matchMedia(REDUCED_MOTION_QUERY).matches;

/**
 * Animates the value from 0 to `target` with a smooth ease-out curve, at most
 * once per mount so refetches never restart the number.
 *
 * Respects prefers-reduced-motion: users who disabled animation get the final
 * figure immediately, without any intermediate frame.
 */
export function useCountUp(target, durationMs = 900) {
    const [value, setValue] = useState(() => (REDUCED_MOTION ? target : 0));

    useEffect(() => {
        if (REDUCED_MOTION) {
            return undefined;
        }

        let frame;
        let animated = 0;
        const startTime = performance.now();

        function tick(now) {
            const progress = Math.min((now - startTime) / durationMs, 1);
            const eased = 1 - Math.pow(1 - progress, 3);
            animated = target * eased;
            setValue(animated);
            if (progress < 1) {
                frame = requestAnimationFrame(tick);
            }
        }

        frame = requestAnimationFrame(tick);
        return () => cancelAnimationFrame(frame);
    }, [target, durationMs]);

    return value;
}