/*
 * Three targets, one entry point. The farm tour and the event are both registrations in the API;
 * the activity is a booking, so its visitor details feed the booking's own contact fields rather
 * than creating a visitor record (the API has no visitor link on a booking). That difference is
 * spelled out in the form when the activity target is chosen.
 */
export const MODES = [
    { value: "TOUR", labelKey: "registration.modes.TOUR" },
    { value: "EVENT", labelKey: "registration.modes.EVENT" },
    { value: "ACTIVITY", labelKey: "registration.modes.ACTIVITY" },
];

/*
 * The visitor type choices of the edit form. The backend separates the kind of group
 * (VisitorType) from the reason for the visit (VisitPurpose); the education/partner choices
 * mapped here match the directory values.
 */
export const VISITOR_TYPE_CHOICES = [
    { value: "GENERAL", labelKey: "registration.typeChoices.GENERAL" },
    { value: "BUYER", labelKey: "registration.typeChoices.BUYER" },
    { value: "SCHOOL", labelKey: "registration.typeChoices.SCHOOL" },
];

export const PAYMENT_METHODS = ["CASH", "ORANGE_MONEY", "MOOV_MONEY"];

export function resolveTypeAndPurpose(choice, groupSize) {
    if (choice === "SCHOOL") return { type: "SCHOOL", purpose: "EDUCATION" };
    const type = groupSize > 1 ? "GROUP" : "INDIVIDUAL";
    if (choice === "BUYER") return { type: "PARTNER", purpose: "PURCHASE" };
    return { type, purpose: "TOURISM" };
}

/*
 * The visit purpose follows the visitor's type already stored in the directory: a school group
 * registers for education, a partner for purchase, everyone else for tourism.
 */
export function resolvePurposeFromType(type) {
    if (type === "SCHOOL") return "EDUCATION";
    if (type === "PARTNER") return "PURCHASE";
    return "TOURISM";
}

/** The edit form's choice for a directory visitor type. */
export function typeChoiceOf(type) {
    if (type === "SCHOOL") return "SCHOOL";
    if (type === "PARTNER") return "BUYER";
    return "GENERAL";
}
