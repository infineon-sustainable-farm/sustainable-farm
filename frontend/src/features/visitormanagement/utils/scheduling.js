/*
 * The two canonical slots of the farm day, from the backend's SchedulingRules
 * (morning 9–11, afternoon 2–4). They are the choices offered for a new slot;
 * when editing, a slot's own range is added if it is not one of them.
 */
export const SLOT_PRESETS = [
    { key: "09:00:00-11:00:00", startTime: "09:00:00", endTime: "11:00:00" },
    { key: "14:00:00-16:00:00", startTime: "14:00:00", endTime: "16:00:00" },
];
