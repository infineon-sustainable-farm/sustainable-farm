/**
 * Columns of an alert row from lg up, on a 12 column grid shared with the header row of the list:
 * priority, where, issue, when, action; the message runs under where to action.
 */
export const ALERT_COLUMNS = {
    priority: "lg:col-span-2",
    place: "lg:col-span-2",
    issue: "lg:col-span-4",
    when: "lg:col-span-3",
    action: "lg:col-span-1",
    message: "lg:col-span-10 lg:col-start-3",
};
