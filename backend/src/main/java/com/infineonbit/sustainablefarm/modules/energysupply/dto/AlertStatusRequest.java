package com.infineonbit.sustainablefarm.modules.energysupply.dto;

import com.infineonbit.sustainablefarm.modules.energysupply.entity.AlertStatus;
import jakarta.validation.constraints.NotNull;

public record AlertStatusRequest(@NotNull AlertStatus status) {
}
