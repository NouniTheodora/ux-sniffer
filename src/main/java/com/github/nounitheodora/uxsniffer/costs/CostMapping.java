package com.github.nounitheodora.uxsniffer.costs;

import org.jetbrains.annotations.NotNull;

public record CostMapping(
        @NotNull String mappingId,
        @NotNull String smellId,
        @NotNull String smellName,
        @NotNull String costId,
        @NotNull String costName,
        @NotNull String relationshipType,
        @NotNull String causationLogic,
        @NotNull String triggerCondition,
        @NotNull String priority
) {}