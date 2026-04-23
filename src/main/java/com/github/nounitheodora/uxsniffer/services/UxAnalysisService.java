package com.github.nounitheodora.uxsniffer.services;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

@Service(Service.Level.PROJECT)
public final class UxAnalysisService {

    private static final Logger LOG = Logger.getInstance(UxAnalysisService.class);

    private final Project project;

    public UxAnalysisService(@NotNull Project project) {
        this.project = project;
        LOG.info("UxAnalysisService initialized for project: " + project.getName());
    }

    public @NotNull Project getProject() {
        return project;
    }
}
