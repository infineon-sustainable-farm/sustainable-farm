package com.infineonbit.sustainablefarm.modules.watersupply.dto;

import java.util.List;

public record SafetyBriefingRequest(String version, List<String> allowedZones, List<String> forbiddenZones,
                                    List<String> risks, String text, Integer maxCapacity,
                                    Boolean guideRequired) {
}
