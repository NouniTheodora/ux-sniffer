package com.github.nounitheodora.uxsniffer.scanner;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

final class IgnoreFileParser {

    private static final Logger LOG = Logger.getInstance(IgnoreFileParser.class);
    private static final String IGNORE_FILE_NAME = ".uxsnifferignore";

    private final List<PathMatcher> matchers = new ArrayList<>();
    private final String basePath;

    IgnoreFileParser(@NotNull Project project) {
        VirtualFile projectDir = ProjectUtil.guessProjectDir(project);
        this.basePath = projectDir != null ? projectDir.getPath() : "";

        if (projectDir == null) return;
        VirtualFile ignoreFile = projectDir.findChild(IGNORE_FILE_NAME);
        if (ignoreFile == null || ignoreFile.isDirectory()) return;

        try {
            String content = new String(ignoreFile.contentsToByteArray(), StandardCharsets.UTF_8);
            for (String line : content.split("\\R")) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
                matchers.add(FileSystems.getDefault().getPathMatcher("glob:" + trimmed));
            }
        } catch (IOException e) {
            LOG.warn("Failed to read " + IGNORE_FILE_NAME, e);
        }
    }

    boolean isIgnored(@NotNull VirtualFile file) {
        if (matchers.isEmpty()) return false;

        String filePath = file.getPath();
        String separator = File.separator;
        String relativePath;
        if (!basePath.isEmpty() && filePath.startsWith(basePath + separator)) {
            relativePath = filePath.substring(basePath.length() + 1);
        } else {
            relativePath = filePath;
        }

        java.nio.file.Path path = Paths.get(relativePath);
        String fileName = file.getName();

        for (PathMatcher matcher : matchers) {
            if (matcher.matches(path) || matcher.matches(Paths.get(fileName))) {
                return true;
            }
        }
        return false;
    }
}
