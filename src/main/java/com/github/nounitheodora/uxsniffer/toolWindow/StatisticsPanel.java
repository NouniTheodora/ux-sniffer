package com.github.nounitheodora.uxsniffer.toolWindow;

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

        JPanel content = new JPanel(new BorderLayout(0, 16));
        content.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        // Summary cards
        JPanel summaryPanel = new JPanel(new GridLayout(1, 3, 12, 0));
        summaryPanel.add(createSummaryCard("Total Smells", String.valueOf(findings.size())));
        summaryPanel.add(createSummaryCard("Files Affected", String.valueOf(uniqueFiles.size())));
        summaryPanel.add(createSummaryCard("Smell Types", String.valueOf(smellCounts.size())));
        content.add(summaryPanel, BorderLayout.NORTH);

        // Split: bar chart on top, files table on bottom
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setResizeWeight(0.55);
        splitPane.setBorder(null);

        // Bar chart
        JPanel chartSection = new JPanel(new BorderLayout(0, 6));
        JBLabel chartTitle = new JBLabel("Smell Distribution");
        chartTitle.setFont(chartTitle.getFont().deriveFont(Font.BOLD, 13f));
        chartSection.add(chartTitle, BorderLayout.NORTH);

        BarChartPanel barChart = new BarChartPanel(sortedSmells);
        chartSection.add(new JBScrollPane(barChart), BorderLayout.CENTER);
        splitPane.setTopComponent(chartSection);

        // Top files table
        JPanel filesSection = new JPanel(new BorderLayout(0, 6));
        JBLabel filesTitle = new JBLabel("Top Affected Files");
        filesTitle.setFont(filesTitle.getFont().deriveFont(Font.BOLD, 13f));
        filesSection.add(filesTitle, BorderLayout.NORTH);

        String[] columns = {"#", "File", "Smells"};
        DefaultTableModel filesModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        int rank = 1;
        for (Map.Entry<String, Integer> entry : sortedFiles) {
            filesModel.addRow(new Object[]{rank++, entry.getKey(), entry.getValue()});
        }
        JBTable filesTable = new JBTable(filesModel);
        filesTable.getColumnModel().getColumn(0).setPreferredWidth(40);
        filesTable.getColumnModel().getColumn(0).setMaxWidth(50);
        filesTable.getColumnModel().getColumn(1).setPreferredWidth(300);
        filesTable.getColumnModel().getColumn(2).setPreferredWidth(80);
        filesTable.getColumnModel().getColumn(2).setMaxWidth(100);
        filesSection.add(new JBScrollPane(filesTable), BorderLayout.CENTER);
        splitPane.setBottomComponent(filesSection);

        content.add(splitPane, BorderLayout.CENTER);
        add(content, BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    void reset() {
        removeAll();
        add(emptyLabel, BorderLayout.CENTER);
        revalidate();
        repaint();
    }

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

    static Color getColorForIndex(int index) {
        return CHART_COLORS[index % CHART_COLORS.length];
    }

    static class BarChartPanel extends JPanel {

        private final List<Map.Entry<String, Integer>> data;
        private static final int BAR_HEIGHT = 26;
        private static final int BAR_GAP = 6;
        private static final int LABEL_WIDTH = 220;

        BarChartPanel(@NotNull List<Map.Entry<String, Integer>> data) {
            this.data = data;
            setPreferredSize(new Dimension(400, data.size() * (BAR_HEIGHT + BAR_GAP) + BAR_GAP));
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
                g2.setColor(getColorForIndex(i));
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
