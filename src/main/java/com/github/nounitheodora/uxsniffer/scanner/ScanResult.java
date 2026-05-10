package com.github.nounitheodora.uxsniffer.scanner;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public record ScanResult(
        @NotNull List<SmellFinding> findings,
        @NotNull List<String> excludedFiles
) {}
