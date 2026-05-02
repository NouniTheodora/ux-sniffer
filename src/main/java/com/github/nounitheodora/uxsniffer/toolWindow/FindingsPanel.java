package com.github.nounitheodora.uxsniffer.toolWindow;

import com.github.nounitheodora.uxsniffer.costs.CostMapper;
import com.github.nounitheodora.uxsniffer.costs.CostMapping;
import com.github.nounitheodora.uxsniffer.costs.PafCost;
import com.github.nounitheodora.uxsniffer.scanner.SmellFinding;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;

class FindingsPanel extends JPanel {

    private final JBLabel summaryLabel;
    private final DefaultTableModel tableModel;
    private final List<SmellFinding> currentFindings = new ArrayList<>();
    private final CostDetailPanel costDetailPanel;

    FindingsPanel(@NotNull Project project) {
        setLayout(new BorderLayout());

        summaryLabel = new JBLabel("Click 'Scan Project' to analyze all .vue files.");
        summaryLabel.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        add(summaryLabel, BorderLayout.NORTH);

        String[] columns = {"Smell", "File", "Message"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        JBTable table = new JBTable(tableModel);
        table.setAutoCreateRowSorter(true);
        table.getColumnModel().getColumn(0).setPreferredWidth(180);
        table.getColumnModel().getColumn(1).setPreferredWidth(160);
        table.getColumnModel().getColumn(2).setPreferredWidth(460);

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int viewRow = table.getSelectedRow();
                    if (viewRow < 0) return;
                    int modelRow = table.convertRowIndexToModel(viewRow);
                    if (modelRow < 0 || modelRow >= currentFindings.size()) return;
                    SmellFinding finding = currentFindings.get(modelRow);
                    VirtualFile vf = LocalFileSystem.getInstance().findFileByPath(finding.filePath());
                    if (vf != null) {
                        new OpenFileDescriptor(project, vf).navigate(true);
                    }
                }
            }
        });

        costDetailPanel = new CostDetailPanel();

        table.getSelectionModel().addListSelectionListener((ListSelectionEvent e) -> {
            if (e.getValueIsAdjusting()) return;
            int viewRow = table.getSelectedRow();
            if (viewRow < 0) {
                costDetailPanel.clear();
                return;
            }
            int modelRow = table.convertRowIndexToModel(viewRow);
            if (modelRow < 0 || modelRow >= currentFindings.size()) {
                costDetailPanel.clear();
                return;
            }
            costDetailPanel.showCostsFor(currentFindings.get(modelRow));
        });

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                new JBScrollPane(table), costDetailPanel);
        splitPane.setResizeWeight(0.65);
        splitPane.setDividerSize(5);
        add(splitPane, BorderLayout.CENTER);
    }

    void setScanning() {
        summaryLabel.setText("Scanning...");
        tableModel.setRowCount(0);
        currentFindings.clear();
        costDetailPanel.clear();
    }

    void update(@NotNull List<SmellFinding> findings) {
        currentFindings.addAll(findings);
        for (SmellFinding f : findings) {
            tableModel.addRow(new Object[]{f.smellName(), f.fileName(), f.message()});
        }

        if (findings.isEmpty()) {
            summaryLabel.setText("No UX smells found.");
        } else {
            Map<String, Integer> smellCounts = new LinkedHashMap<>();
            Set<String> filesWithSmells = new HashSet<>();
            for (SmellFinding f : findings) {
                smellCounts.merge(f.smellName(), 1, Integer::sum);
                filesWithSmells.add(f.filePath());
            }
            summaryLabel.setText(String.format(
                    "%d smell(s) found across %d file(s). %d distinct smell type(s) detected.",
                    findings.size(), filesWithSmells.size(), smellCounts.size()));
        }
    }

    private static class CostDetailPanel extends JPanel {

        private final JBLabel titleLabel;
        private final JPanel costsContainer;
        private final JBLabel emptyLabel;

        CostDetailPanel() {
            setLayout(new BorderLayout());
            setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));

            titleLabel = new JBLabel();
            titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));
            titleLabel.setBorder(BorderFactory.createEmptyBorder(4, 0, 6, 0));
            add(titleLabel, BorderLayout.NORTH);

            costsContainer = new JPanel();
            costsContainer.setLayout(new BoxLayout(costsContainer, BoxLayout.Y_AXIS));
            JBScrollPane scrollPane = new JBScrollPane(costsContainer);
            scrollPane.setBorder(BorderFactory.createEmptyBorder());
            add(scrollPane, BorderLayout.CENTER);

            emptyLabel = new JBLabel("Select a finding to view associated PAF quality costs.");
            emptyLabel.setForeground(Color.GRAY);
            emptyLabel.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));
            costsContainer.add(emptyLabel);
        }

        void clear() {
            titleLabel.setText("");
            costsContainer.removeAll();
            costsContainer.add(emptyLabel);
            costsContainer.revalidate();
            costsContainer.repaint();
        }

        void showCostsFor(@NotNull SmellFinding finding) {
            CostMapper mapper = CostMapper.getInstance();
            List<CostMapping> mappings = mapper.getMappingsForSmellByDisplayName(finding.smellName());

            costsContainer.removeAll();

            if (mappings.isEmpty()) {
                titleLabel.setText("PAF Cost Impact: " + finding.smellName());
                JBLabel noData = new JBLabel("No PAF cost mappings available for this smell.");
                noData.setForeground(Color.GRAY);
                noData.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
                costsContainer.add(noData);
            } else {
                String smellId = mapper.getSmellIdForDisplayName(finding.smellName());
                titleLabel.setText(String.format("PAF Cost Impact: %s (%s) — %d cost(s)",
                        finding.smellName(), smellId, mappings.size()));

                List<CostMapping> primary = mappings.stream()
                        .filter(m -> "Primary".equals(m.priority())).toList();
                List<CostMapping> secondary = mappings.stream()
                        .filter(m -> "Secondary".equals(m.priority())).toList();

                if (!primary.isEmpty()) {
                    addSectionHeader("Primary Costs");
                    for (CostMapping m : primary) {
                        addCostCard(mapper, m);
                    }
                }
                if (!secondary.isEmpty()) {
                    addSectionHeader("Secondary Costs");
                    for (CostMapping m : secondary) {
                        addCostCard(mapper, m);
                    }
                }
            }

            costsContainer.add(Box.createVerticalGlue());
            costsContainer.revalidate();
            costsContainer.repaint();
        }

        private void addSectionHeader(@NotNull String text) {
            JBLabel header = new JBLabel(text);
            header.setFont(header.getFont().deriveFont(Font.BOLD, header.getFont().getSize() - 1f));
            header.setForeground(new Color(100, 100, 100));
            header.setBorder(BorderFactory.createEmptyBorder(8, 0, 4, 0));
            header.setAlignmentX(Component.LEFT_ALIGNMENT);
            costsContainer.add(header);
        }

        private void addCostCard(@NotNull CostMapper mapper, @NotNull CostMapping mapping) {
            JPanel card = new JPanel();
            card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
            card.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 3, 0, 0, getCategoryColor(mapping)),
                    BorderFactory.createEmptyBorder(6, 8, 6, 8)
            ));
            card.setAlignmentX(Component.LEFT_ALIGNMENT);
            card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));

            PafCost cost = mapper.getCost(mapping.costId());
            String categoryLabel = cost != null ? cost.pafCategory() : "Unknown";

            JBLabel header = new JBLabel(String.format("<html><b>%s</b> [%s] <font color='gray'>— %s</font></html>",
                    mapping.costName(), mapping.costId(), categoryLabel));
            header.setAlignmentX(Component.LEFT_ALIGNMENT);
            card.add(header);

            JBLabel relationship = new JBLabel(String.format("<html><i>%s</i> — %s</html>",
                    mapping.relationshipType(), mapping.causationLogic()));
            relationship.setFont(relationship.getFont().deriveFont(Font.PLAIN, relationship.getFont().getSize() - 1f));
            relationship.setAlignmentX(Component.LEFT_ALIGNMENT);
            relationship.setBorder(BorderFactory.createEmptyBorder(3, 0, 2, 0));
            card.add(relationship);

            JBLabel trigger = new JBLabel(String.format("<html><font color='#666666'>Trigger: %s</font></html>",
                    mapping.triggerCondition()));
            trigger.setFont(trigger.getFont().deriveFont(Font.PLAIN, trigger.getFont().getSize() - 1f));
            trigger.setAlignmentX(Component.LEFT_ALIGNMENT);
            card.add(trigger);

            costsContainer.add(card);
            costsContainer.add(Box.createRigidArea(new Dimension(0, 4)));
        }

        private @NotNull Color getCategoryColor(@NotNull CostMapping mapping) {
            CostMapper mapper = CostMapper.getInstance();
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
}