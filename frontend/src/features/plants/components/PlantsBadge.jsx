/*
 * One hue per tone: the label is the team's state token (--color-success,
 * --color-warning, --color-error) darkened, on the same token lightened, so
 * the label reaches WCAG AA (4.5:1) on its own background. The tokens
 * themselves are too light for text: --color-warning is 2.25:1 on white.
 *   success  #227a52 on #e5f7ef -> 4.75:1
 *   warning  #8a631b on #f8efde -> 4.74:1
 *   error    #9a3f32 on #f7e9e7 -> 5.67:1
 *   neutral  gray-700 #364153 on gray-100 #f3f4f6 -> 9.36:1
 */
const TONES = {
    success: "bg-[#e5f7ef] text-[#227a52]",
    warning: "bg-[#f8efde] text-[#8a631b]",
    error: "bg-[#f7e9e7] text-[#9a3f32]",
    neutral: "bg-gray-100 text-gray-700",
};

/**
 * The badge of the module: a rounded pill without border, 12 px semibold.
 * `tone` is success, warning, error or neutral; an unknown tone reads neutral.
 * The label always says what the colour says, so colour is never the only cue.
 */
export default function PlantsBadge({ tone = "neutral", children }) {
    return (
        <span className={`inline-block rounded-full px-2 py-0.5 text-xs font-semibold ${TONES[tone] ?? TONES.neutral}`}>
            {children}
        </span>
    );
}
