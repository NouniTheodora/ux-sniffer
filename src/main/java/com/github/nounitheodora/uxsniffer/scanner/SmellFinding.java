package com.github.nounitheodora.uxsniffer.scanner;

import org.jetbrains.annotations.NotNull;

public record SmellFinding(String smellName, String filePath, String fileName, String message) {

    public SmellFinding(@NotNull String smellName, @NotNull String filePath,
                        @NotNull String fileName, @NotNull String message) {
        this.smellName = smellName;
        this.filePath = filePath;
        this.fileName = fileName;
        this.message = message;
    }

    @Override
    public @NotNull String smellName() {
        return smellName;
    }

    @Override
    public @NotNull String filePath() {
        return filePath;
    }

    @Override
    public @NotNull String fileName() {
        return fileName;
    }

    @Override
    public @NotNull String message() {
        return message;
    }
}
