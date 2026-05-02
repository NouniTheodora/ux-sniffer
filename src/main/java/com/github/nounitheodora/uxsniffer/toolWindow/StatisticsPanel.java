package com.github.nounitheodora.uxsniffer.toolWindow;

import com.github.nounitheodora.uxsniffer.costs.CostMapper;
import com.github.nounitheodora.uxsniffer.costs.CostMapping;
import com.github.nounitheodora.uxsniffer.costs.PafCost;
import com.github.nounitheodora.uxsniffer.scanner.SmellFinding;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

class StatisticsPanel extends JPanel {

    static final Color[] CHART_COLORS = {
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

    private static final Color INTERNAL_FAILURE_COLOR = new Color(220, 80, 60);
    private static final Color APPRAISAL_COLOR = new Color(60, 130, 200);

    private final JBLabel emptyLabel;

    StatisticsPanel() {
        setLayout(new BorderLayout());
        emptyLabel = new JBLabel("Run a scan to see statistics.");
        emptyLabel.setHorizontalAlignment(SwingConstants.CENTER);
        emptyLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
        add(emptyLabel, BorderLayout.CENTER);
    }

    void update(@NotNull List<SmellFinding> findings) {
        removeAll();

        if (findings.isEmpty()) {
            add(emptyLabel, BorderLayout.CENTER);
            revalidate();
            repaint();
            return;
        }

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

        // Summary cards at the top (always visible)
        JPanel summaryPanel = new JPanel(new GridLayout(1, 3, 12, 0));
        summaryPanel.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        summaryPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        summaryPanel.add(createSummaryCard("Total Smells", String.valueOf(findings.size())));
        summaryPanel.add(createSummaryCard("Files Affected", String.valueOf(uniqueFiles.size())));
        summaryPanel.add(createSummaryCard("Smell Types", String.valueOf(smellCounts.size())));

        // Tabbed content
        JTabbedPane tabs = new JTabbedPane(JTabbedPane.TOP);
        tabs.addTab("Smell Distribution", buildSmellDistributionTab(sortedSmells));
        tabs.addTab("Quality Costs", buildCostAnalysisTab(findings, smellCounts));
        tabs.addTab("Files (" + sortedFiles.size() + ")", buildFilesTab(findings, sortedFiles));

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(summaryPanel, BorderLayout.NORTH);
        mainPanel.add(tabs, BorderLayout.CENTER);

        add(mainPanel, BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    void reset() {
        removeAll();
        add(emptyLabel, BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    // ─── Tab 1: Smell Distribution ───────────────────────────────────────────────

    private @NotNull JPanel buildSmellDistributionTab(@NotNull List<Map.Entry<String, Integer>> sortedSmells) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JBLabel description = new JBLabel(
                "<html><font color='#555555'>This chart shows how many times each smell type was " +
                "detected across the project. Smells that appear more often may indicate " +
                "systemic issues in the codebase.</font></html>");
        description.setFont(description.getFont().deriveFont(Font.PLAIN, 11f));
        description.setAlignmentX(Component.LEFT_ALIGNMENT);
        description.setBorder(BorderFactory.createEmptyBorder(0, 0, 12, 0));
        panel.add(description);

        BarChartPanel barChart = new BarChartPanel(sortedSmells, CHART_COLORS);
        JBScrollPane chartScroll = new JBScrollPane(barChart);
        chartScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        chartScroll.setBorder(BorderFactory.createEmptyBorder());
        panel.add(chartScroll);

        panel.add(Box.createVerticalGlue());

        JPanel wrapper = new JPanel(new BorderLayout());
        JBScrollPane scroll = new JBScrollPane(panel);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        wrapper.add(scroll, BorderLayout.CENTER);
        return wrapper;
    }

    // ─── Tab 2: Quality Costs ────────────────────────────────────────────────────

    private @NotNull JPanel buildCostAnalysisTab(@NotNull List<SmellFinding> findings,
                                                  @NotNull Map<String, Integer> smellCounts) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        // Explanation
        JBLabel description = new JBLabel(
                "<html><font color='#555555'>Each detected smell triggers quality costs based on the " +
                "PAF (Prevention-Appraisal-Failure) model. <b>Internal Failure</b> costs represent " +
                "rework needed before release. <b>Appraisal</b> costs represent the effort for " +
                "reviews, testing, and verification that the smell demands.</font></html>");
        description.setFont(description.getFont().deriveFont(Font.PLAIN, 11f));
        description.setAlignmentX(Component.LEFT_ALIGNMENT);
        description.setBorder(BorderFactory.createEmptyBorder(0, 0, 14, 0));
        panel.add(description);

        CostMapper mapper = CostMapper.getInstance();

        // Calculate cost hits
        Map<String, Integer> costHits = new LinkedHashMap<>();
        int internalFailureHits = 0;
        int appraisalHits = 0;

        for (Map.Entry<String, Integer> smellEntry : smellCounts.entrySet()) {
            String displayName = smellEntry.getKey();
            int occurrences = smellEntry.getValue();
            List<CostMapping> mappings = mapper.getMappingsForSmellByDisplayName(displayName);

            for (CostMapping m : mappings) {
                costHits.merge(m.costId(), occurrences, Integer::sum);
                PafCost cost = mapper.getCost(m.costId());
                if (cost != null) {
                    if ("Internal Failure".equals(cost.pafCategory())) {
                        internalFailureHits += occurrences;
                    } else {
                        appraisalHits += occurrences;
                    }
                }
            }
        }

        // PAF category cards
        JPanel pafSummary = new JPanel(new GridLayout(1, 2, 12, 0));
        pafSummary.setMaximumSize(new Dimension(Integer.MAX_VALUE, 75));
        pafSummary.setAlignmentX(Component.LEFT_ALIGNMENT);
        pafSummary.add(createColoredCard("Internal Failure",
                internalFailureHits + " occurrence(s) require rework", INTERNAL_FAILURE_COLOR));
        pafSummary.add(createColoredCard("Appraisal",
                appraisalHits + " occurrence(s) require reviews/testing", APPRAISAL_COLOR));
        panel.add(pafSummary);
        panel.add(Box.createRigidArea(new Dimension(0, 16)));

        // Cost breakdown chart
        JBLabel chartLabel = new JBLabel("Cost breakdown by category");
        chartLabel.setFont(chartLabel.getFont().deriveFont(Font.BOLD, 12f));
        chartLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        chartLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 6, 0));
        panel.add(chartLabel);

        JBLabel chartExplain = new JBLabel(
                "<html><font color='#888888'>Each bar shows how many smell occurrences contribute " +
                "to that specific cost. A higher bar means more smells feed into that cost area.</font></html>");
        chartExplain.setFont(chartExplain.getFont().deriveFont(Font.PLAIN, 11f));
        chartExplain.setAlignmentX(Component.LEFT_ALIGNMENT);
        chartExplain.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        panel.add(chartExplain);

        List<Map.Entry<String, Integer>> sortedCosts = costHits.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .collect(Collectors.toList());

        List<Map.Entry<String, Integer>> costChartData = new ArrayList<>();
        Color[] costColors = new Color[sortedCosts.size()];
        for (int i = 0; i < sortedCosts.size(); i++) {
            Map.Entry<String, Integer> entry = sortedCosts.get(i);
            PafCost cost = mapper.getCost(entry.getKey());
            String label = cost != null
                    ? cost.costName() + " [" + cost.costId() + "]"
                    : entry.getKey();
            costChartData.add(Map.entry(label, entry.getValue()));
            costColors[i] = cost != null && "Internal Failure".equals(cost.pafCategory())
                    ? INTERNAL_FAILURE_COLOR : APPRAISAL_COLOR;
        }

        if (!costChartData.isEmpty()) {
            BarChartPanel costChart = new BarChartPanel(costChartData, costColors);
            JBScrollPane costScroll = new JBScrollPane(costChart);
            costScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
            costScroll.setBorder(BorderFactory.createEmptyBorder());
            panel.add(costScroll);
        }

        panel.add(Box.createVerticalGlue());

        JPanel wrapper = new JPanel(new BorderLayout());
        JBScrollPane scroll = new JBScrollPane(panel);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        wrapper.add(scroll, BorderLayout.CENTER);
        return wrapper;
    }

    // ─── Tab 3: Files ────────────────────────────────────────────────────────────

    private @NotNull JPanel buildFilesTab(@NotNull List<SmellFinding> findings,
                                           @NotNull List<Map.Entry<String, Integer>> sortedFiles) {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JBLabel description = new JBLabel(
                "<html><font color='#555555'>Files ranked by total quality cost exposure. " +
                "A file with few smells can still have high cost exposure if those smells " +
                "trigger many cost categories. Sort by any column to prioritize your refactoring effort.</font></html>");
        description.setFont(description.getFont().deriveFont(Font.PLAIN, 11f));
        description.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        panel.add(description, BorderLayout.NORTH);

        String[] columns = {"#", "File", "Smells", "Failure Costs", "Appraisal Costs", "Total Costs"};
        DefaultTableModel filesModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }

            @Override
            public Class<?> getColumnClass(int column) {
                if (column == 1) return String.class;
                return Integer.class;
            }
        };

        CostMapper costMapper = CostMapper.getInstance();
        int rank = 1;
        for (Map.Entry<String, Integer> entry : sortedFiles) {
            String fileName = entry.getKey();
            int failureCosts = 0;
            int appraisalCosts = 0;

            for (SmellFinding f : findings) {
                if (!f.fileName().equals(fileName)) continue;
                List<CostMapping> mappings = costMapper.getMappingsForSmellByDisplayName(f.smellName());
                for (CostMapping m : mappings) {
                    PafCost cost = costMapper.getCost(m.costId());
                    if (cost != null && "Internal Failure".equals(cost.pafCategory())) {
                        failureCosts++;
                    } else {
                        appraisalCosts++;
                    }
                }
            }

            filesModel.addRow(new Object[]{
                    rank++, fileName, entry.getValue(),
                    failureCosts, appraisalCosts, failureCosts + appraisalCosts
            });
        }

        JBTable filesTable = new JBTable(filesModel);
        filesTable.getColumnModel().getColumn(0).setPreferredWidth(30);
        filesTable.getColumnModel().getColumn(0).setMaxWidth(40);
        filesTable.getColumnModel().getColumn(1).setPreferredWidth(250);
        filesTable.getColumnModel().getColumn(2).setPreferredWidth(60);
        filesTable.getColumnModel().getColumn(2).setMaxWidth(70);
        filesTable.getColumnModel().getColumn(3).setPreferredWidth(90);
        filesTable.getColumnModel().getColumn(3).setMaxWidth(110);
        filesTable.getColumnModel().getColumn(4).setPreferredWidth(100);
        filesTable.getColumnModel().getColumn(4).setMaxWidth(120);
        filesTable.getColumnModel().getColumn(5).setPreferredWidth(80);
        filesTable.getColumnModel().getColumn(5).setMaxWidth(100);
        filesTable.setAutoCreateRowSorter(true);

        panel.add(new JBScrollPane(filesTable), BorderLayout.CENTER);
        return panel;
    }

    // ─── Shared helpers ──────────────────────────────────────────────────────────

    private JPanel createSummaryCard(String title, String value) {
        JPanel card = new JPanel(new BorderLayout(0, 2));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIManager.getColor("Component.borderColor"), 1),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)));

        JBLabel titleLabel = new JBLabel(title);
        titleLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.PLAIN, 11f));
        card.add(titleLabel, BorderLayout.NORTH);

        JBLabel valueLabel = new JBLabel(value);
        valueLabel.setFont(valueLabel.getFont().deriveFont(Font.BOLD, 22f));
        card.add(valueLabel, BorderLayout.CENTER);

        return card;
    }

    private JPanel createColoredCard(String title, String value, Color accentColor) {
        JPanel card = new JPanel(new BorderLayout(0, 2));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 4, 0, 0, accentColor),
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(UIManager.getColor("Component.borderColor"), 1),
                        BorderFactory.createEmptyBorder(8, 12, 8, 12))));

        JBLabel titleLabel = new JBLabel(title);
        titleLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.PLAIN, 11f));
        card.add(titleLabel, BorderLayout.NORTH);

        JBLabel valueLabel = new JBLabel(value);
        valueLabel.setFont(valueLabel.getFont().deriveFont(Font.BOLD, 14f));
        card.add(valueLabel, BorderLayout.CENTER);

        return card;
    }

    static Color getColorForIndex(int index) {
        return CHART_COLORS[index % CHART_COLORS.length];
    }

    static class BarChartPanel extends JPanel {

        private final List<Map.Entry<String, Integer>> data;
        private final Color[] colors;
        private static final int BAR_HEIGHT = 26;
        private static final int BAR_GAP = 6;
        private static final int LABEL_WIDTH = 380;

        BarChartPanel(@NotNull List<Map.Entry<String, Integer>> data, @NotNull Color[] colors) {
            this.data = data;
            this.colors = colors;
            setPreferredSize(new Dimension(700, data.size() * (BAR_HEIGHT + BAR_GAP) + BAR_GAP));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (data.isEmpty()) return;

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int maxValue = data.stream().mapToInt(Map.Entry::getValue).max().orElse(1);
            int barAreaWidth = getWidth() - LABEL_WIDTH - 60;

            int y = BAR_GAP;
            for (int i = 0; i < data.size(); i++) {
                Map.Entry<String, Integer> entry = data.get(i);
                String label = entry.getKey();
                int value = entry.getValue();

                g2.setFont(getFont().deriveFont(Font.PLAIN, 12f));
                FontMetrics fm = g2.getFontMetrics();
                int textY = y + (BAR_HEIGHT + fm.getAscent() - fm.getDescent()) / 2;

                g2.setColor(getForeground());
                String display = label;
                while (fm.stringWidth(display) > LABEL_WIDTH - 10 && display.length() > 4) {
                    display = display.substring(0, display.length() - 4) + "...";
                }
                g2.drawString(display, 5, textY);

                int barWidth = Math.max((int) ((double) value / maxValue * barAreaWidth), 4);
                Color barColor = i < colors.length ? colors[i] : getColorForIndex(i);
                g2.setColor(barColor);
                g2.fillRoundRect(LABEL_WIDTH, y, barWidth, BAR_HEIGHT, 6, 6);

                g2.setColor(getForeground());
                g2.setFont(getFont().deriveFont(Font.BOLD, 12f));
                g2.drawString(String.valueOf(value), LABEL_WIDTH + barWidth + 8, textY);

                y += BAR_HEIGHT + BAR_GAP;
            }
            g2.dispose();
        }
    }
}
