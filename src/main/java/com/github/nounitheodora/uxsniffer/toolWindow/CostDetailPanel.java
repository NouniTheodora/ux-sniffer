package com.github.nounitheodora.uxsniffer.toolwindow;

import com.github.nounitheodora.uxsniffer.quality.CostMapper;
import com.github.nounitheodora.uxsniffer.quality.CostMapping;
import com.github.nounitheodora.uxsniffer.quality.PafCost;
import com.github.nounitheodora.uxsniffer.quality.SmellInfo;
import com.github.nounitheodora.uxsniffer.scanner.SmellFinding;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import org.jetbrains.annotations.NotNull;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.util.List;

class CostDetailPanel extends JPanel {

    private final transient Project project;
    private final JBLabel emptyLabel;
    private final JTabbedPane detailTabs;

    CostDetailPanel(@NotNull Project project) {
        this.project = project;
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

        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        headerPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

        JBLabel nameLabel = new JBLabel(String.format(
                "<html><b style='font-size:12px'>%s</b> <font color='gray'>[%s]</font></html>",
                finding.smellName(), smellId));
        headerPanel.add(nameLabel);
        headerPanel.add(createSeverityChip(severity));

        panel.add(headerPanel);

        if (smellInfo != null) {
            addSection(panel, "What is this smell?", smellInfo.definition());
            addSection(panel, "Suggested refactoring", smellInfo.refactoring());
        }

        addDetectedInSection(panel, finding);

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
                addCostSectionHeader(panel, "Direct Costs", "This smell directly causes or triggers these costs");
                for (CostMapping m : primary) {
                    addCostCard(panel, mapper, m);
                }
            }
            if (!secondary.isEmpty()) {
                panel.add(Box.createRigidArea(new Dimension(0, 10)));
                addCostSectionHeader(panel, "Indirect Costs", "Fixing this smell requires additional effort in these areas");
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

    private @NotNull String getRelativePath(@NotNull String absolutePath) {
        VirtualFile projectDir = ProjectUtil.guessProjectDir(project);
        if (projectDir != null) {
            String basePath = projectDir.getPath();
            if (absolutePath.startsWith(basePath + "/")) {
                return absolutePath.substring(basePath.length() + 1);
            }
        }
        return absolutePath;
    }

    private void addDetectedInSection(@NotNull JPanel panel, @NotNull SmellFinding finding) {
        JBLabel titleLabel = new JBLabel("Detected in");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 11f));
        titleLabel.setForeground(new Color(80, 80, 80));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 3, 0));
        panel.add(titleLabel);

        String relativePath = getRelativePath(finding.filePath());

        JPanel pathPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        pathPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JBLabel pathLabel = new JBLabel(String.format(
                "<html><font color='gray'>%s</font></html>", relativePath));
        pathLabel.setFont(pathLabel.getFont().deriveFont(Font.PLAIN, 12f));
        pathPanel.add(pathLabel);

        JBLabel copiedLabel = new JBLabel("Copied!");
        copiedLabel.setForeground(new Color(66, 184, 131));
        copiedLabel.setFont(copiedLabel.getFont().deriveFont(Font.BOLD, 11f));
        copiedLabel.setVisible(false);

        pathPanel.add(createCopyButton(relativePath, copiedLabel));
        pathPanel.add(copiedLabel);

        pathPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 12, 0));
        panel.add(pathPanel);
    }

    private @NotNull JButton createCopyButton(@NotNull String relativePath, @NotNull JBLabel copiedLabel) {
        JButton copyButton = new JButton(AllIcons.Actions.Copy);
        copyButton.setToolTipText("Copy path to clipboard");
        copyButton.setBorderPainted(false);
        copyButton.setContentAreaFilled(false);
        copyButton.setPreferredSize(new Dimension(20, 20));
        copyButton.addActionListener(e -> {
            StringSelection selection = new StringSelection(relativePath);
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);
            copiedLabel.setVisible(true);
            Timer timer = new Timer(2000, evt -> copiedLabel.setVisible(false));
            timer.setRepeats(false);
            timer.start();
        });
        return copyButton;
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

    private void addCostSectionHeader(@NotNull JPanel panel, @NotNull String title,
                                       @NotNull String description) {
        JBLabel titleLabel = new JBLabel(title);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 12f));
        titleLabel.setForeground(UIManager.getColor("Label.foreground"));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(titleLabel);

        JBLabel desc = new JBLabel(String.format("<html><font color='#777777'>%s</font></html>", description));
        desc.setFont(desc.getFont().deriveFont(Font.PLAIN, 11f));
        desc.setAlignmentX(Component.LEFT_ALIGNMENT);
        desc.setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));
        panel.add(desc);

        panel.add(Box.createRigidArea(new Dimension(0, 8)));
    }

    private void addCostCard(@NotNull JPanel panel, @NotNull CostMapper mapper, @NotNull CostMapping mapping) {
        Color categoryColor = getCategoryColor(mapper, mapping);
        PafCost cost = mapper.getCost(mapping.costId());
        String categoryLabel = cost != null ? cost.pafCategory() : "Unknown";

        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 3, 0, 0, categoryColor),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)
        ));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));

        JPanel titleRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        titleRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JBLabel nameLabel = new JBLabel(String.format(
                "<html><b>%s</b> <font color='gray'>[%s]</font></html>",
                mapping.costName(), mapping.costId()));
        titleRow.add(nameLabel);
        titleRow.add(createChip(categoryLabel, categoryColor));
        card.add(titleRow);

        JBLabel logic = new JBLabel(String.format("<html><div style='width:480px'>%s</div></html>",
                mapping.causationLogic()));
        logic.setFont(logic.getFont().deriveFont(Font.PLAIN, logic.getFont().getSize() - 1f));
        logic.setAlignmentX(Component.LEFT_ALIGNMENT);
        logic.setBorder(BorderFactory.createEmptyBorder(3, 0, 0, 0));
        card.add(logic);

        panel.add(card);
        panel.add(Box.createRigidArea(new Dimension(0, 4)));
    }

    private @NotNull JBLabel createChip(@NotNull String text, @NotNull Color chipColor) {
        JBLabel chip = new JBLabel(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(chipColor.getRed(), chipColor.getGreen(), chipColor.getBlue(), 25));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.setColor(chipColor);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        chip.setForeground(chipColor);
        chip.setFont(chip.getFont().deriveFont(Font.BOLD, 11f));
        chip.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));
        chip.setOpaque(false);
        return chip;
    }

    private @NotNull JBLabel createSeverityChip(@NotNull String severity) {
        Color chipColor = switch (severity) {
            case "High" -> new Color(220, 60, 50);
            case "Medium" -> new Color(230, 160, 0);
            default -> new Color(100, 140, 180);
        };
        return createChip(severity, chipColor);
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