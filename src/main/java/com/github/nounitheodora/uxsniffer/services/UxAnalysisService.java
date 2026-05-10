package com.github.nounitheodora.uxsniffer.services;

import com.github.nounitheodora.uxsniffer.scanner.ProjectScanner;
import com.github.nounitheodora.uxsniffer.scanner.ScanResult;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

/**
 * Facade pattern — provides a single, simplified entry point for all analysis
 * operations, hiding the internal subsystem classes (ProjectScanner, individual
 * inspections, CostMapper, ReportExporter) from the rest of the plugin.
 *
 * @see <a href="https://refactoring.guru/design-patterns/facade">Facade — Refactoring Guru</a>
 */
@Service(Service.Level.PROJECT)
public final class UxAnalysisService {

    private final ProjectScanner scanner;

    public UxAnalysisService(@NotNull Project project) {
        this.scanner = new ProjectScanner(project);
    }

    public static @NotNull UxAnalysisService getInstance(@NotNull Project project) {
        return project.getService(UxAnalysisService.class);
    }

    public @NotNull ScanResult scanProject() {
        return scanner.scan();
    }
}