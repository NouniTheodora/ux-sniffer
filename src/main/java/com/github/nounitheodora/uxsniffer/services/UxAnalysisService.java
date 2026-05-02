package com.github.nounitheodora.uxsniffer.services;

import com.github.nounitheodora.uxsniffer.costs.CostMapper;
import com.github.nounitheodora.uxsniffer.costs.CostMapping;
import com.github.nounitheodora.uxsniffer.scanner.ProjectScanner;
import com.github.nounitheodora.uxsniffer.scanner.SmellFinding;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

/**
 * Facade (GoF design pattern) for the UXSniffer analysis subsystem.
 *
 * Provides a single, simplified entry point for all analysis operations,
 * hiding the internal subsystem classes (ProjectScanner, individual
 * inspections, and future components like CostMapper or ReportExporter)
 * from the rest of the plugin (tool window, actions, etc.).
 */
@Service(Service.Level.PROJECT)
public final class UxAnalysisService {

    private final Project project;
    private final ProjectScanner scanner;
    private List<SmellFinding> lastFindings = List.of();

    public UxAnalysisService(@NotNull Project project) {
        this.project = project;
        this.scanner = new ProjectScanner(project);
    }

    public static @NotNull UxAnalysisService getInstance(@NotNull Project project) {
        return project.getService(UxAnalysisService.class);
    }

    public @NotNull List<SmellFinding> scanProject() {
        lastFindings = scanner.scan();
        return lastFindings;
    }

    public @NotNull List<SmellFinding> getLastFindings() {
        return lastFindings;
    }

    public @NotNull Project getProject() {
        return project;
    }

    public @NotNull CostMapper getCostMapper() {
        return CostMapper.getInstance();
    }

    public @NotNull List<CostMapping> getCostsForFinding(@NotNull SmellFinding finding) {
        return CostMapper.getInstance().getMappingsForSmellByDisplayName(finding.smellName());
    }

    public @NotNull Set<String> getAffectedCostIdsForFinding(@NotNull SmellFinding finding) {
        String smellId = CostMapper.getInstance().getSmellIdForDisplayName(finding.smellName());
        if (smellId == null) return Set.of();
        return CostMapper.getInstance().getAffectedCostIds(smellId);
    }

    public @Nullable String getSmellIdForFinding(@NotNull SmellFinding finding) {
        return CostMapper.getInstance().getSmellIdForDisplayName(finding.smellName());
    }
}