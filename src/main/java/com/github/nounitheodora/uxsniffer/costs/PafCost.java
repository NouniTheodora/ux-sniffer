package com.github.nounitheodora.uxsniffer.costs;

import org.jetbrains.annotations.NotNull;

public record PafCost(
        @NotNull String costId,
        @NotNull String costName,
        @NotNull String pafCategory,
        @NotNull String definition
) {}