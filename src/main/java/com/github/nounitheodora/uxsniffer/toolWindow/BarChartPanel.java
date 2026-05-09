package com.github.nounitheodora.uxsniffer.toolwindow;

import org.jetbrains.annotations.NotNull;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.List;
import java.util.Map;

class BarChartPanel extends JPanel {

    private final transient List<Map.Entry<String, Integer>> data;
    private final transient Color[] colors;
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
            Color barColor = i < colors.length ? colors[i] : StatisticsPanel.getColorForIndex(i);
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