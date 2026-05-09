package com.github.nounitheodora.uxsniffer.scanner;

import org.jetbrains.annotations.NotNull;

public record SmellFinding(@NotNull String smellName, @NotNull String filePath,
                           @NotNull String fileName, @NotNull String message) {
}