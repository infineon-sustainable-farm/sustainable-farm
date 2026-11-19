package com.infineonbit.sustainablefarm.modules.energysupply.dto;

import jakarta.validation.constraints.NotNull;

public record AlertStatusRequest(@NotNull AlertStatus status) {
}
