package com.github.nounitheodora.uxsniffer.toolWindow;

import com.github.nounitheodora.uxsniffer.scanner.SmellFinding;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

final class HtmlReportExporter {

    private HtmlReportExporter() {}

    static @NotNull String generate(@NotNull List<SmellFinding> findings, @NotNull String projectName) {
        Map<String, Integer> smellCounts = new LinkedHashMap<>();
        Map<String, Integer> fileCounts = new LinkedHashMap<>();
        Set<String> uniqueFiles = new HashSet<>();

        for (SmellFinding f : findings) {
            smellCounts.merge(f.smellName(), 1, Integer::sum);
            fileCounts.merge(f.fileName(), 1, Integer::sum);
            uniqueFiles.add(f.filePath());
        }

        List<Map.Entry<String, Integer>> sortedSmells = smellCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .collect(Collectors.toList());

        List<Map.Entry<String, Integer>> sortedFiles = fileCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .collect(Collectors.toList());

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

        String smellLabels = sortedSmells.stream()
                .map(e -> "'" + escapeJs(e.getKey()) + "'")
                .collect(Collectors.joining(", "));
        String smellData = sortedSmells.stream()
                .map(e -> String.valueOf(e.getValue()))
                .collect(Collectors.joining(", "));

        StringBuilder colorsBuilder = new StringBuilder();
        for (int i = 0; i < sortedSmells.size(); i++) {
            if (i > 0) colorsBuilder.append(", ");
            Color c = StatisticsPanel.getColorForIndex(i);
            colorsBuilder.append(String.format("'rgba(%d, %d, %d, 0.8)'", c.getRed(), c.getGreen(), c.getBlue()));
        }
        String smellColors = colorsBuilder.toString();

        String fileLabels = sortedFiles.stream()
                .map(e -> "'" + escapeJs(e.getKey()) + "'")
                .collect(Collectors.joining(", "));
        String fileData = sortedFiles.stream()
                .map(e -> String.valueOf(e.getValue()))
                .collect(Collectors.joining(", "));

        StringBuilder findingsRows = new StringBuilder();
        int idx = 1;
        for (SmellFinding f : findings) {
            findingsRows.append("<tr>")
                    .append("<td>").append(idx++).append("</td>")
                    .append("<td>").append(escapeHtml(f.smellName())).append("</td>")
                    .append("<td>").append(escapeHtml(f.fileName())).append("</td>")
                    .append("<td>").append(escapeHtml(f.message())).append("</td>")
                    .append("</tr>\n");
        }

        StringBuilder topFilesRows = new StringBuilder();
        int rank = 1;
        for (Map.Entry<String, Integer> entry : sortedFiles) {
            topFilesRows.append("<tr>")
                    .append("<td>").append(rank++).append("</td>")
                    .append("<td>").append(escapeHtml(entry.getKey())).append("</td>")
                    .append("<td>").append(entry.getValue()).append("</td>")
                    .append("</tr>\n");
        }

        return """
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
                    h2 { font-size: 18px; margin: 24px 0 12px; }
                    .meta { color: #666; margin-bottom: 20px; }
                    .cards { display: flex; gap: 16px; margin-bottom: 24px; }
                    .card { flex: 1; background: #fff; border: 1px solid #e0e0e0; border-radius: 8px; padding: 16px; text-align: center; }
                    .card-value { font-size: 32px; font-weight: bold; color: #42b883; }
                    .card-label { font-size: 13px; color: #888; margin-top: 4px; }
                    .charts { display: flex; gap: 24px; margin-bottom: 24px; }
                    .chart-box { flex: 1; background: #fff; border: 1px solid #e0e0e0; border-radius: 8px; padding: 16px; }
                    table { width: 100%%; border-collapse: collapse; background: #fff; border: 1px solid #e0e0e0; border-radius: 8px; overflow: hidden; margin-bottom: 16px; }
                    th { background: #f5f5f5; text-align: left; padding: 10px 12px; font-size: 13px; border-bottom: 2px solid #e0e0e0; }
                    td { padding: 8px 12px; border-bottom: 1px solid #f0f0f0; font-size: 13px; }
                    tr:hover td { background: #f9f9f9; }
                    footer { text-align: center; color: #aaa; font-size: 12px; margin-top: 32px; padding-top: 16px; border-top: 1px solid #eee; }
                  </style>
                </head>
                <body>
                  <h1>UXSniffer Report</h1>
                  <p class="meta">Project: %s &middot; Generated: %s</p>

                  <div class="cards">
                    <div class="card"><div class="card-value">%d</div><div class="card-label">Total Smells</div></div>
                    <div class="card"><div class="card-value">%d</div><div class="card-label">Files Affected</div></div>
                    <div class="card"><div class="card-value">%d</div><div class="card-label">Smell Types</div></div>
                  </div>

                  <div class="charts">
                    <div class="chart-box"><h2>Smell Distribution</h2><canvas id="pieChart"></canvas></div>
                    <div class="chart-box"><h2>Smells per File</h2><canvas id="barChart"></canvas></div>
                  </div>

                  <h2>Top Affected Files</h2>
                  <table>
                    <thead><tr><th>#</th><th>File</th><th>Smells</th></tr></thead>
                    <tbody>
                %s    </tbody>
                  </table>

                  <h2>All Findings (%d)</h2>
                  <table>
                    <thead><tr><th>#</th><th>Smell</th><th>File</th><th>Message</th></tr></thead>
                    <tbody>
                %s    </tbody>
                  </table>

                  <footer>Generated by <strong>UXSniffer</strong> — IntelliJ plugin for Vue.js UX smell detection</footer>

                  <script>
                    new Chart(document.getElementById('pieChart'), {
                      type: 'doughnut',
                      data: { labels: [%s], datasets: [{ data: [%s], backgroundColor: [%s] }] },
                      options: { responsive: true, plugins: { legend: { position: 'right' } } }
                    });
                    new Chart(document.getElementById('barChart'), {
                      type: 'bar',
                      data: { labels: [%s], datasets: [{ label: 'Smells', data: [%s], backgroundColor: 'rgba(66,184,131,0.7)' }] },
                      options: { responsive: true, indexAxis: 'y', plugins: { legend: { display: false } } }
                    });
                  </script>
                </body>
                </html>
                """.formatted(
                escapeHtml(projectName),
                escapeHtml(projectName), timestamp,
                findings.size(), uniqueFiles.size(), smellCounts.size(),
                topFilesRows, findings.size(), findingsRows,
                smellLabels, smellData, smellColors,
                fileLabels, fileData
        );
    }

    private static String escapeHtml(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private static String escapeJs(String s) {
        return s.replace("\\", "\\\\").replace("'", "\\'");
    }
}