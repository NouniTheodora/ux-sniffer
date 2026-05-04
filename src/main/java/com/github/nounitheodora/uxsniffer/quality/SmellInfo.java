package com.github.nounitheodora.uxsniffer.quality;

import org.jetbrains.annotations.NotNull;

public record SmellInfo(
        @NotNull String smellId,
        @NotNull String smellName,
        @NotNull String definition,
        @NotNull String severity,
        @NotNull String refactoring
) {}
