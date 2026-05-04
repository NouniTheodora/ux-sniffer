package com.github.nounitheodora.uxsniffer.toolwindow;

import com.github.nounitheodora.uxsniffer.quality.CostMapper;
import com.github.nounitheodora.uxsniffer.quality.CostMapping;
import com.github.nounitheodora.uxsniffer.quality.PafCost;
import com.github.nounitheodora.uxsniffer.quality.SmellInfo;
import com.github.nounitheodora.uxsniffer.scanner.SmellFinding;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import org.jetbrains.annotations.NotNull;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.util.List;

class CostDetailPanel extends JPanel {

    private final JBLabel emptyLabel;
    private final JTabbedPane detailTabs;

    CostDetailPanel() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));

        emptyLabel = new JBLabel("Select a finding above to see details, refactoring advice, and cost impact.");
        emptyLabel.setForeground(Color.GRAY);
        emptyLabel.setHorizontalAlignment(SwingConstants.CENTER);
        emptyLabel.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));
        add(emptyLabel, BorderLayout.CENTER);

        detailTabs = new JTabbedPane(SwingConstants.TOP);
    }

    void clear() {
        removeAll();
        add(emptyLabel, BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    void showCostsFor(@NotNull SmellFinding finding) {
        removeAll();

        CostMapper mapper = CostMapper.getInstance();
        String smellId = mapper.getSmellIdForDisplayName(finding.smellName());
        SmellInfo smellInfo = smellId != null ? mapper.getSmellInfo(smellId) : null;
        List<CostMapping> mappings = mapper.getMappingsForSmellByDisplayName(finding.smellName());

        detailTabs.removeAll();
        detailTabs.addTab("Overview & Fix", buildOverviewTab(finding, smellInfo));
        detailTabs.addTab("Cost Impact (" + mappings.size() + ")", buildCostTab(mapper, mappings));

        add(detailTabs, BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    private @NotNull JPanel buildOverviewTab(@NotNull SmellFinding finding, SmellInfo smellInfo) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        String smellId = smellInfo != null ? smellInfo.smellId() : "";
        String severity = smellInfo != null ? smellInfo.severity() : "Unknown";
        JBLabel header = new JBLabel(String.format(
                "<html><b style='font-size:12px'>%s</b> <font color='gray'>[%s]</font>" +
                " &nbsp; Severity: <b>%s</b></html>",
                finding.smellName(), smellId, severity));
        header.setAlignmentX(Component.LEFT_ALIGNMENT);
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        panel.add(header);

        if (smellInfo != null) {
            addSection(panel, "What is this smell?", smellInfo.definition());
            addSection(panel, "Suggested refactoring", smellInfo.refactoring());
        }

        addSection(panel, "Detected in",
                String.format("<b>%s</b><br><font color='gray'>%s</font>",
                        finding.fileName(), finding.filePath()));

        panel.add(Box.createVerticalGlue());

        JBScrollPane scroll = new JBScrollPane(panel);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(12);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.add(scroll, BorderLayout.CENTER);
        return wrapper;
    }

    private @NotNull JPanel buildCostTab(@NotNull CostMapper mapper,
                                          @NotNull List<CostMapping> mappings) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JBLabel intro = new JBLabel(
                "<html><font color='#555555'>The following quality costs are triggered when this smell " +
                "is present in the codebase. These are based on the PAF (Prevention-Appraisal-Failure) " +
                "quality cost model.</font></html>");
        intro.setFont(intro.getFont().deriveFont(Font.PLAIN, 11f));
        intro.setAlignmentX(Component.LEFT_ALIGNMENT);
        intro.setBorder(BorderFactory.createEmptyBorder(0, 0, 12, 0));
        panel.add(intro);

        if (mappings.isEmpty()) {
            JBLabel noData = new JBLabel("No PAF cost mappings available for this smell yet.");
            noData.setForeground(Color.GRAY);
            noData.setAlignmentX(Component.LEFT_ALIGNMENT);
            panel.add(noData);
        } else {
            List<CostMapping> primary = mappings.stream()
                    .filter(m -> "Primary".equals(m.priority())).toList();
            List<CostMapping> secondary = mappings.stream()
                    .filter(m -> "Secondary".equals(m.priority())).toList();

            if (!primary.isEmpty()) {
                addCostSectionHeader(panel,
                        "Direct costs — this smell directly causes or triggers these:",
                        new Color(220, 80, 60));
                for (CostMapping m : primary) {
                    addCostCard(panel, mapper, m);
                }
            }
            if (!secondary.isEmpty()) {
                panel.add(Box.createRigidArea(new Dimension(0, 10)));
                addCostSectionHeader(panel,
                        "Indirect costs — fixing this smell requires additional effort in these areas:",
                        new Color(60, 130, 200));
                for (CostMapping m : secondary) {
                    addCostCard(panel, mapper, m);
                }
            }
        }

        panel.add(Box.createVerticalGlue());

        JBScrollPane scroll = new JBScrollPane(panel);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(12);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.add(scroll, BorderLayout.CENTER);
        return wrapper;
    }

    private void addSection(@NotNull JPanel panel, @NotNull String title, @NotNull String htmlContent) {
        JBLabel titleLabel = new JBLabel(title);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 11f));
        titleLabel.setForeground(new Color(80, 80, 80));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 3, 0));
        panel.add(titleLabel);

        JBLabel contentLabel = new JBLabel(String.format("<html><div style='width:500px'>%s</div></html>", htmlContent));
        contentLabel.setFont(contentLabel.getFont().deriveFont(Font.PLAIN, 12f));
        contentLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        contentLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 12, 0));
        panel.add(contentLabel);
    }

    private void addCostSectionHeader(@NotNull JPanel panel, @NotNull String text, @NotNull Color color) {
        JBLabel header = new JBLabel(String.format("<html><font color='#555555'>%s</font></html>", text));
        header.setFont(header.getFont().deriveFont(Font.ITALIC, 11f));
        header.setAlignmentX(Component.LEFT_ALIGNMENT);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, color),
                BorderFactory.createEmptyBorder(0, 0, 6, 0)));
        panel.add(header);
        panel.add(Box.createRigidArea(new Dimension(0, 6)));
    }

    private void addCostCard(@NotNull JPanel panel, @NotNull CostMapper mapper, @NotNull CostMapping mapping) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 3, 0, 0, getCategoryColor(mapper, mapping)),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)
        ));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));

        PafCost cost = mapper.getCost(mapping.costId());
        String categoryLabel = cost != null ? cost.pafCategory() : "Unknown";

        JBLabel header = new JBLabel(String.format(
                "<html><b>%s</b> [%s] <font color='gray'>— %s</font></html>",
                mapping.costName(), mapping.costId(), categoryLabel));
        header.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(header);

        JBLabel logic = new JBLabel(String.format("<html><div style='width:480px'>%s</div></html>",
                mapping.causationLogic()));
        logic.setFont(logic.getFont().deriveFont(Font.PLAIN, logic.getFont().getSize() - 1f));
        logic.setAlignmentX(Component.LEFT_ALIGNMENT);
        logic.setBorder(BorderFactory.createEmptyBorder(3, 0, 0, 0));
        card.add(logic);

        panel.add(card);
        panel.add(Box.createRigidArea(new Dimension(0, 4)));
    }

    private @NotNull Color getCategoryColor(@NotNull CostMapper mapper, @NotNull CostMapping mapping) {
        PafCost cost = mapper.getCost(mapping.costId());
        if (cost == null) return Color.GRAY;
        return switch (cost.pafCategory()) {
            case "Internal Failure" -> new Color(220, 80, 60);
            case "External Failure" -> new Color(180, 40, 40);
            case "Appraisal" -> new Color(60, 130, 200);
            default -> Color.GRAY;
        };
    }
}