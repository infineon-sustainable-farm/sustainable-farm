package com.infineonbit.sustainablefarm.modules.machinery.api;

/**
 * A processing machine to register: it joins the register with the category
 * {@link Category#PROCESSING_MACHINERY}.
 *
 * @param code   the unique business code chosen by the caller
 * @param name   the unique name, kept as given (trimmed)
 * @param stage  {@link Stage#PROCESSING} or {@link Stage#DRYING}
 * @param status the initial status
 */
public record ProcessingEquipmentRegistration(String code, String name, Stage stage, Status status) {
}
