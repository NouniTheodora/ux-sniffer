package com.github.nounitheodora.uxsniffer.report;

import com.github.nounitheodora.uxsniffer.quality.CostMapper;
import com.github.nounitheodora.uxsniffer.quality.CostMapping;
import com.github.nounitheodora.uxsniffer.quality.PafCost;
import com.github.nounitheodora.uxsniffer.quality.SmellInfo;
import com.github.nounitheodora.uxsniffer.scanner.SmellFinding;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class JsonReportExporter {

    private static final String SMELL_COUNT = "smellCount";

    private JsonReportExporter() {}

    public static @NotNull String generate(@NotNull List<SmellFinding> findings,
                                           @NotNull String projectName,
                                           @NotNull String projectBasePath,
                                           @NotNull List<String> excludedFiles) {
        CostMapper mapper = CostMapper.getInstance();
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("project", projectName);
        report.put("basePath", projectBasePath);
        report.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        report.put("totalFindings", findings.size());
        report.put("summary", buildSummary(findings));
        report.put("findings", buildFindings(findings, projectBasePath, mapper));
        report.put("fileAnalysis", buildFileAnalysis(findings, projectBasePath, mapper));
        report.put("excludedFiles", buildExcludedFiles(excludedFiles, projectBasePath));

        return gson.toJson(report);
    }

    private static @NotNull Map<String, Object> buildExcludedFiles(@NotNull List<String> excludedFiles,
                                                                    @NotNull String basePath) {
        Map<String, Object> section = new LinkedHashMap<>();
        section.put("totalExcluded", excludedFiles.size());
        section.put("files", excludedFiles.stream()
                .map(f -> toRelativePath(f, basePath))
                .sorted()
                .toList());
        return section;
    }

    private static @NotNull Map<String, Object> buildSummary(@NotNull List<SmellFinding> findings) {
        Map<String, Integer> smellCounts = new LinkedHashMap<>();
        long uniqueFiles = findings.stream().map(SmellFinding::filePath).distinct().count();

        for (SmellFinding f : findings) {
            smellCounts.merge(f.smellName(), 1, Integer::sum);
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("uniqueFilesAffected", uniqueFiles);
        summary.put("smellCounts", smellCounts);
        return summary;
    }

    private static @NotNull List<Map<String, Object>> buildFindings(
            @NotNull List<SmellFinding> findings,
            @NotNull String basePath,
            @NotNull CostMapper mapper) {
        List<Map<String, Object>> result = new ArrayList<>();

        for (SmellFinding f : findings) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("smellName", f.smellName());
            entry.put("fileName", f.fileName());
            entry.put("filePath", toRelativePath(f.filePath(), basePath));
            entry.put("message", f.message());

            String smellId = mapper.getSmellIdForDisplayName(f.smellName());
            if (smellId != null) {
                entry.put("smellId", smellId);
                SmellInfo info = mapper.getSmellInfo(smellId);
                if (info != null) {
                    entry.put("severity", info.severity());
                    entry.put("refactoring", info.refactoring());
                }
                entry.put("costImpact", buildCostImpact(smellId, mapper));
            }

            result.add(entry);
        }
        return result;
    }

    private static @NotNull List<Map<String, String>> buildCostImpact(
            @NotNull String smellId, @NotNull CostMapper mapper) {
        List<Map<String, String>> costs = new ArrayList<>();
        for (CostMapping cm : mapper.getMappingsForSmell(smellId)) {
            Map<String, String> cost = new LinkedHashMap<>();
            cost.put("costId", cm.costId());
            cost.put("costName", cm.costName());
            PafCost pafCost = mapper.getCost(cm.costId());
            if (pafCost != null) {
                cost.put("pafCategory", pafCost.pafCategory());
            }
            cost.put("relationshipType", cm.relationshipType());
            cost.put("priority", cm.priority());
            costs.add(cost);
        }
        return costs;
    }

    private static @NotNull List<Map<String, Object>> buildFileAnalysis(
            @NotNull List<SmellFinding> findings,
            @NotNull String basePath,
            @NotNull CostMapper mapper) {
        Map<String, List<String>> smellsByFile = new LinkedHashMap<>();
        for (SmellFinding f : findings) {
            String relPath = toRelativePath(f.filePath(), basePath);
            smellsByFile.computeIfAbsent(relPath, k -> new ArrayList<>()).add(f.smellName());
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : smellsByFile.entrySet()) {
            Map<String, Object> fileEntry = new LinkedHashMap<>();
            fileEntry.put("file", entry.getKey());
            fileEntry.put(SMELL_COUNT, entry.getValue().size());
            fileEntry.put("smells", entry.getValue());

            long costCount = entry.getValue().stream()
                    .map(mapper::getSmellIdForDisplayName)
                    .filter(Objects::nonNull)
                    .mapToLong(id -> mapper.getMappingsForSmell(id).size())
                    .sum();
            fileEntry.put("totalCostMappings", costCount);

            result.add(fileEntry);
        }

        result.sort((a, b) -> Integer.compare(
                (int) b.get(SMELL_COUNT), (int) a.get(SMELL_COUNT)));
        return result;
    }

    private static @NotNull String toRelativePath(@NotNull String filePath, @NotNull String basePath) {
        if (!basePath.isEmpty() && filePath.startsWith(basePath)) {
            String rel = filePath.substring(basePath.length());
            if (rel.startsWith("/")) rel = rel.substring(1);
            return rel;
        }
        return filePath;
    }
}