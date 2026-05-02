package com.github.nounitheodora.uxsniffer.toolWindow;

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

        add(new JBScrollPane(table), BorderLayout.CENTER);
    }

    void setScanning() {
        summaryLabel.setText("Scanning...");
        tableModel.setRowCount(0);
        currentFindings.clear();
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
}
