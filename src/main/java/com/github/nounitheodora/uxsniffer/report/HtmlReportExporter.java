package com.github.nounitheodora.uxsniffer.report;

import com.github.nounitheodora.uxsniffer.quality.CostMapper;
import com.github.nounitheodora.uxsniffer.quality.CostMapping;
import com.github.nounitheodora.uxsniffer.quality.PafCost;
import com.github.nounitheodora.uxsniffer.quality.SmellInfo;
import com.github.nounitheodora.uxsniffer.scanner.SmellFinding;
import org.jetbrains.annotations.NotNull;

import java.awt.Color;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class HtmlReportExporter {

    private static final String INTERNAL_FAILURE = "Internal Failure";
    private static final String TD_CLOSE = "</td>";

    private static final Color[] CHART_COLORS = {
            new Color(66, 184, 131),
            new Color(100, 149, 237),
            new Color(239, 83, 80),
            new Color(255, 167, 38),
            new Color(171, 71, 188),
            new Color(38, 198, 218),
            new Color(255, 112, 67),
            new Color(102, 187, 106),
            new Color(141, 110, 99),
            new Color(236, 64, 122),
            new Color(255, 202, 40),
            new Color(120, 144, 156),
    };

    private HtmlReportExporter() {}

    public static @NotNull String generate(@NotNull List<SmellFinding> findings,
                                               @NotNull String projectName,
                                               @NotNull String projectBasePath,
                                               @NotNull List<String> excludedFiles) {
        CostMapper mapper = CostMapper.getInstance();

        Map<String, Integer> smellCounts = new LinkedHashMap<>();
        Map<String, Integer> fileCountsByRelPath = new LinkedHashMap<>();
        Set<String> uniqueFiles = new HashSet<>();

        for (SmellFinding f : findings) {
            smellCounts.merge(f.smellName(), 1, Integer::sum);
            fileCountsByRelPath.merge(toRelativePath(f.filePath(), projectBasePath), 1, Integer::sum);
            uniqueFiles.add(f.filePath());
        }

        List<Map.Entry<String, Integer>> sortedSmells = smellCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .toList();

        List<Map.Entry<String, Integer>> sortedFiles = fileCountsByRelPath.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .toList();

        int[] costTotals = computeCostTotals(mapper, smellCounts);
        List<Map.Entry<String, Integer>> sortedCosts = computeCostHits(mapper, smellCounts);

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

        String smellLabels = sortedSmells.stream()
                .map(e -> "'" + escapeJs(e.getKey()) + "'")
                .collect(Collectors.joining(", "));
        String smellData = sortedSmells.stream()
                .map(e -> String.valueOf(e.getValue()))
                .collect(Collectors.joining(", "));
        String smellColors = buildSmellColors(sortedSmells.size());
        String costLabels = buildCostLabels(mapper, sortedCosts);
        String costData = sortedCosts.stream()
                .map(e -> String.valueOf(e.getValue()))
                .collect(Collectors.joining(", "));
        String costColors2 = buildCostColors(mapper, sortedCosts);

        String smellDetailsHtml = buildSmellDetailsHtml(mapper, sortedSmells);
        String topFilesRows = buildTopFilesRows(mapper, sortedFiles, findings, projectBasePath);
        String findingsRows = buildFindingsRows(mapper, findings, projectBasePath);
        String excludedFilesHtml = buildExcludedFilesHtml(excludedFiles, projectBasePath);

        return HTML_TEMPLATE.formatted(
                escapeHtml(projectName),
                escapeHtml(projectName), timestamp,
                findings.size(), uniqueFiles.size(), smellCounts.size(),
                costTotals[0], costTotals[1],
                smellDetailsHtml,
                topFilesRows, findings.size(), findingsRows,
                excludedFilesHtml,
                smellLabels, smellData, smellColors,
                costLabels, costData, costColors2
        );
    }

    private static int[] computeCostTotals(@NotNull CostMapper mapper,
                                            @NotNull Map<String, Integer> smellCounts) {
        int internalFailureHits = 0;
        int appraisalHits = 0;

        for (Map.Entry<String, Integer> smellEntry : smellCounts.entrySet()) {
            int occurrences = smellEntry.getValue();
            List<CostMapping> mappings = mapper.getMappingsForSmellByDisplayName(smellEntry.getKey());
            for (CostMapping m : mappings) {
                PafCost cost = mapper.getCost(m.costId());
                if (cost != null) {
                    if (INTERNAL_FAILURE.equals(cost.pafCategory())) {
                        internalFailureHits += occurrences;
                    } else {
                        appraisalHits += occurrences;
                    }
                }
            }
        }
        return new int[]{internalFailureHits, appraisalHits};
    }

    private static @NotNull List<Map.Entry<String, Integer>> computeCostHits(
            @NotNull CostMapper mapper, @NotNull Map<String, Integer> smellCounts) {
        Map<String, Integer> costHits = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> smellEntry : smellCounts.entrySet()) {
            int occurrences = smellEntry.getValue();
            List<CostMapping> mappings = mapper.getMappingsForSmellByDisplayName(smellEntry.getKey());
            for (CostMapping m : mappings) {
                costHits.merge(m.costId(), occurrences, Integer::sum);
            }
        }
        return costHits.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .toList();
    }

    private static @NotNull String buildSmellColors(int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            if (i > 0) sb.append(", ");
            Color c = CHART_COLORS[i % CHART_COLORS.length];
            sb.append(String.format("'rgba(%d, %d, %d, 0.8)'", c.getRed(), c.getGreen(), c.getBlue()));
        }
        return sb.toString();
    }

    private static @NotNull String buildCostLabels(@NotNull CostMapper mapper,
                                                     @NotNull List<Map.Entry<String, Integer>> sortedCosts) {
        return sortedCosts.stream()
                .map(e -> {
                    PafCost cost = mapper.getCost(e.getKey());
                    return "'" + escapeJs(cost != null ? cost.costName() : e.getKey()) + "'";
                })
                .collect(Collectors.joining(", "));
    }

    private static @NotNull String buildCostColors(@NotNull CostMapper mapper,
                                                     @NotNull List<Map.Entry<String, Integer>> sortedCosts) {
        return sortedCosts.stream()
                .map(e -> {
                    PafCost cost = mapper.getCost(e.getKey());
                    boolean isFailure = cost != null && INTERNAL_FAILURE.equals(cost.pafCategory());
                    return isFailure ? "'rgba(220, 80, 60, 0.8)'" : "'rgba(60, 130, 200, 0.8)'";
                })
                .collect(Collectors.joining(", "));
    }

    private static @NotNull String buildSmellDetailsHtml(@NotNull CostMapper mapper,
                                                           @NotNull List<Map.Entry<String, Integer>> sortedSmells) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Integer> entry : sortedSmells) {
            String displayName = entry.getKey();
            String smellId = mapper.getSmellIdForDisplayName(displayName);
            SmellInfo info = smellId != null ? mapper.getSmellInfo(smellId) : null;
            List<CostMapping> mappings = mapper.getMappingsForSmellByDisplayName(displayName);

            sb.append("<div class=\"smell-card\">%n".formatted());
            sb.append("  <div class=\"smell-header\"><span class=\"smell-name\">%s</span> <span class=\"smell-id\">[%s]</span> <span class=\"smell-count\">%d occurrence(s)</span>"
                    .formatted(escapeHtml(displayName), smellId != null ? smellId : "?", entry.getValue()));
            if (info != null) {
                sb.append(" <span class=\"severity %s\">%s</span>".formatted(
                        info.severity().toLowerCase().replace(" ", "-"), escapeHtml(info.severity())));
            }
            sb.append("</div>%n".formatted());

            if (info != null) {
                sb.append("  <div class=\"smell-definition\"><strong>What is this?</strong> %s</div>%n"
                        .formatted(escapeHtml(info.definition())));
                sb.append("  <div class=\"smell-refactoring\"><strong>Suggested fix:</strong> %s</div>%n"
                        .formatted(escapeHtml(info.refactoring())));
            }

            appendCostMappings(sb, mapper, mappings);
            sb.append("</div>%n".formatted());
        }
        return sb.toString();
    }

    private static void appendCostMappings(@NotNull StringBuilder sb, @NotNull CostMapper mapper,
                                            @NotNull List<CostMapping> mappings) {
        if (mappings.isEmpty()) return;

        sb.append("  <div class=\"smell-costs\"><strong>Quality costs triggered:</strong>%n".formatted());
        sb.append("    <ul>%n".formatted());
        for (CostMapping m : mappings) {
            PafCost cost = mapper.getCost(m.costId());
            String category = cost != null ? cost.pafCategory() : "Unknown";
            String cssClass = INTERNAL_FAILURE.equals(category) ? "failure" : "appraisal";
            sb.append("      <li><span class=\"cost-badge %s\">%s</span> %s [%s] — %s</li>%n"
                    .formatted(cssClass, escapeHtml(category),
                            escapeHtml(m.costName()), m.costId(),
                            escapeHtml(m.causationLogic())));
        }
        sb.append("    </ul>%n".formatted());
        sb.append("  </div>%n".formatted());
    }

    private static @NotNull String buildTopFilesRows(@NotNull CostMapper mapper,
                                                       @NotNull List<Map.Entry<String, Integer>> sortedFiles,
                                                       @NotNull List<SmellFinding> findings,
                                                       @NotNull String projectBasePath) {
        StringBuilder sb = new StringBuilder();
        int rank = 1;
        for (Map.Entry<String, Integer> entry : sortedFiles) {
            String relPath = entry.getKey();
            int failureCosts = 0;
            int appraisalCosts = 0;

            for (SmellFinding f : findings) {
                if (!toRelativePath(f.filePath(), projectBasePath).equals(relPath)) continue;
                List<CostMapping> mappings = mapper.getMappingsForSmellByDisplayName(f.smellName());
                for (CostMapping m : mappings) {
                    PafCost cost = mapper.getCost(m.costId());
                    if (cost != null && INTERNAL_FAILURE.equals(cost.pafCategory())) {
                        failureCosts++;
                    } else {
                        appraisalCosts++;
                    }
                }
            }

            sb.append("<tr>")
                    .append("<td>").append(rank++).append(TD_CLOSE)
                    .append("<td>").append(escapeHtml(relPath)).append(TD_CLOSE)
                    .append("<td>").append(entry.getValue()).append(TD_CLOSE)
                    .append("<td>").append(failureCosts).append(TD_CLOSE)
                    .append("<td>").append(appraisalCosts).append(TD_CLOSE)
                    .append("<td><strong>").append(failureCosts + appraisalCosts).append("</strong>").append(TD_CLOSE)
                    .append("</tr>%n".formatted());
        }
        return sb.toString();
    }

    private static @NotNull String buildFindingsRows(@NotNull CostMapper mapper,
                                                       @NotNull List<SmellFinding> findings,
                                                       @NotNull String projectBasePath) {
        StringBuilder sb = new StringBuilder();
        int idx = 1;
        for (SmellFinding f : findings) {
            String smellId = mapper.getSmellIdForDisplayName(f.smellName());
            sb.append("<tr>")
                    .append("<td>").append(idx++).append(TD_CLOSE)
                    .append("<td>").append(escapeHtml(f.smellName()))
                    .append(smellId != null ? " <span class=\"smell-id\">[" + smellId + "]</span>" : "")
                    .append(TD_CLOSE)
                    .append("<td>").append(escapeHtml(toRelativePath(f.filePath(), projectBasePath))).append(TD_CLOSE)
                    .append("<td>").append(escapeHtml(f.message())).append(TD_CLOSE)
                    .append("</tr>%n".formatted());
        }
        return sb.toString();
    }

    private static @NotNull String buildExcludedFilesHtml(@NotNull List<String> excludedFiles,
                                                              @NotNull String basePath) {
        if (excludedFiles.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        sb.append("<h2>Excluded Files (%d)</h2>%n".formatted(excludedFiles.size()));
        sb.append("<p class=\"section-desc\">These files were excluded from analysis by built-in rules (test files, type declarations, build output) or by <code>.uxsnifferignore</code> patterns.</p>%n".formatted());
        sb.append("<details><summary>Show excluded files</summary>%n".formatted());
        sb.append("<table>%n<thead><tr><th>#</th><th>File</th></tr></thead>%n<tbody>%n".formatted());

        List<String> sorted = excludedFiles.stream()
                .map(f -> toRelativePath(f, basePath))
                .sorted()
                .toList();

        int idx = 1;
        for (String file : sorted) {
            sb.append("<tr><td>%d</td><td>%s</td></tr>%n".formatted(idx++, escapeHtml(file)));
        }

        sb.append("</tbody>%n</table>%n</details>%n".formatted());
        return sb.toString();
    }

    private static @NotNull String toRelativePath(@NotNull String absolutePath, @NotNull String basePath) {
        if (!basePath.isEmpty() && absolutePath.startsWith(basePath + "/")) {
            return absolutePath.substring(basePath.length() + 1);
        }
        return absolutePath;
    }

    private static String escapeHtml(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private static String escapeJs(String s) {
        return s.replace("\\", "\\\\").replace("'", "\\'")
                .replace("\n", "\\n").replace("\r", "\\r");
    }

    private static final String HTML_TEMPLATE = """
            <!DOCTYPE html>
            <html lang="en">
            <head>
              <meta charset="UTF-8">
              <title>UXSniffer Report — %s</title>
              <script src="https://cdn.jsdelivr.net/npm/chart.js@4"></script>
              <style>
                * { margin: 0; padding: 0; box-sizing: border-box; }
                body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; max-width: 1200px; margin: 0 auto; padding: 24px; color: #1a1a2e; background: #fafafa; }
                h1 { font-size: 28px; margin-bottom: 4px; }
                h2 { font-size: 20px; margin: 32px 0 12px; padding-bottom: 8px; border-bottom: 2px solid #e0e0e0; }
                h3 { font-size: 16px; margin: 16px 0 8px; }
                .meta { color: #666; margin-bottom: 20px; }
                .cards { display: flex; gap: 16px; margin-bottom: 24px; }
                .card { flex: 1; background: #fff; border: 1px solid #e0e0e0; border-radius: 8px; padding: 16px; text-align: center; }
                .card-value { font-size: 32px; font-weight: bold; color: #42b883; }
                .card-label { font-size: 13px; color: #888; margin-top: 4px; }
                .card-failure { border-left: 4px solid #dc5040; }
                .card-failure .card-value { color: #dc5040; }
                .card-appraisal { border-left: 4px solid #3c82c8; }
                .card-appraisal .card-value { color: #3c82c8; }
                .charts { display: flex; gap: 24px; margin-bottom: 24px; }
                .chart-box { flex: 1; background: #fff; border: 1px solid #e0e0e0; border-radius: 8px; padding: 16px; }
                .chart-box h3 { margin-top: 0; }
                table { width: 100%%; border-collapse: collapse; background: #fff; border: 1px solid #e0e0e0; border-radius: 8px; overflow: hidden; margin-bottom: 16px; }
                th { background: #f5f5f5; text-align: left; padding: 10px 12px; font-size: 13px; border-bottom: 2px solid #e0e0e0; }
                td { padding: 8px 12px; border-bottom: 1px solid #f0f0f0; font-size: 13px; }
                tr:hover td { background: #f9f9f9; }
                .section-desc { color: #555; font-size: 14px; margin-bottom: 16px; line-height: 1.5; }
                .smell-card { background: #fff; border: 1px solid #e0e0e0; border-radius: 8px; padding: 16px; margin-bottom: 12px; }
                .smell-header { margin-bottom: 8px; }
                .smell-name { font-size: 15px; font-weight: bold; }
                .smell-id { color: #888; font-size: 13px; }
                .smell-count { background: #42b883; color: #fff; font-size: 11px; padding: 2px 8px; border-radius: 10px; margin-left: 8px; }
                .severity { font-size: 11px; padding: 2px 8px; border-radius: 10px; margin-left: 4px; }
                .severity.high { background: #fde8e8; color: #c0392b; }
                .severity.medium { background: #fef3e0; color: #e67e22; }
                .severity.context-dependent { background: #e8f4fd; color: #2980b9; }
                .smell-definition { font-size: 13px; color: #444; margin-bottom: 8px; line-height: 1.5; }
                .smell-refactoring { font-size: 13px; color: #2d6b48; background: #f0faf4; padding: 8px 12px; border-radius: 6px; margin-bottom: 8px; line-height: 1.5; }
                .smell-costs { font-size: 13px; }
                .smell-costs ul { margin-top: 6px; padding-left: 16px; }
                .smell-costs li { margin-bottom: 6px; line-height: 1.4; }
                .cost-badge { font-size: 10px; padding: 2px 6px; border-radius: 4px; font-weight: bold; text-transform: uppercase; }
                .cost-badge.failure { background: #fde8e8; color: #c0392b; }
                .cost-badge.appraisal { background: #e8f0fd; color: #2471a3; }
                details { margin-bottom: 16px; }
                details summary { cursor: pointer; color: #42b883; font-weight: bold; font-size: 14px; margin-bottom: 8px; }
                details summary:hover { text-decoration: underline; }
                footer { text-align: center; color: #aaa; font-size: 12px; margin-top: 32px; padding-top: 16px; border-top: 1px solid #eee; }
              </style>
            </head>
            <body>
              <h1>UXSniffer Report — %s</h1>
              <p class="meta">Generated: %s</p>

              <!-- Summary Cards -->
              <div class="cards">
                <div class="card"><div class="card-value">%d</div><div class="card-label">Total Smells</div></div>
                <div class="card"><div class="card-value">%d</div><div class="card-label">Files Affected</div></div>
                <div class="card"><div class="card-value">%d</div><div class="card-label">Smell Types</div></div>
                <div class="card card-failure"><div class="card-value">%d</div><div class="card-label">Internal Failure Costs</div></div>
                <div class="card card-appraisal"><div class="card-value">%d</div><div class="card-label">Appraisal Costs</div></div>
              </div>

              <!-- Charts -->
              <div class="charts">
                <div class="chart-box"><h3>Smell Distribution</h3><canvas id="pieChart"></canvas></div>
                <div class="chart-box"><h3>Quality Costs Breakdown</h3><canvas id="costChart"></canvas></div>
              </div>

              <!-- Smell Details -->
              <h2>Smell Details &amp; Refactoring Suggestions</h2>
              <p class="section-desc">Each detected smell is explained below with its definition, a suggested refactoring approach, and the quality costs it triggers according to the PAF model.</p>
            %s
              <!-- Files Table -->
              <h2>Files Ranked by Smell Count</h2>
              <p class="section-desc">Files are ranked by the number of detected UX smells. Cost columns show the associated quality cost exposure for each file.</p>
              <table>
                <thead><tr><th>#</th><th>File</th><th>Smells</th><th>Failure Costs</th><th>Appraisal Costs</th><th>Total Costs</th></tr></thead>
                <tbody>
            %s    </tbody>
              </table>

              <!-- All Findings -->
              <h2>All Findings (%d)</h2>
              <table>
                <thead><tr><th>#</th><th>Smell</th><th>File</th><th>Message</th></tr></thead>
                <tbody>
            %s    </tbody>
              </table>

              <!-- Excluded Files -->
            %s
              <footer>Generated by <strong>UXSniffer</strong> — IntelliJ plugin for Vue.js UX smell detection &amp; PAF quality cost analysis</footer>

              <script>
                new Chart(document.getElementById('pieChart'), {
                  type: 'doughnut',
                  data: { labels: [%s], datasets: [{ data: [%s], backgroundColor: [%s] }] },
                  options: { responsive: true, plugins: { legend: { position: 'right' } } }
                });
                new Chart(document.getElementById('costChart'), {
                  type: 'bar',
                  data: { labels: [%s], datasets: [{ label: 'Smell occurrences triggering this cost', data: [%s], backgroundColor: [%s] }] },
                  options: { responsive: true, indexAxis: 'y', plugins: { legend: { display: false } }, scales: { x: { title: { display: true, text: 'Number of smell occurrences' } } } }
                });
              </script>
            </body>
            </html>
            """;
}
