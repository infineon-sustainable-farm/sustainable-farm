package com.infineonbit.sustainablefarm.modules.watersupply.dto;

/**
 * Demande de report d'une irrigation planifiee (action declenchee apres une suggestion meteo).
 *
 * @param reason motif du report, trace dans l'alerte afin d'historiser l'economie d'eau realisee
 */
public record IrrigationPostponeRequest(String reason) {
}