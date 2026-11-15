package com.infineonbit.sustainablefarm.modules.watersupply.dto;

/**
 * Request to postpone a scheduled irrigation (action triggered after a weather suggestion).
 *
 * @param reason reason for the postponement, recorded in the alert to keep track of the water savings achieved
 */
public record IrrigationPostponeRequest(String reason) {
}