package com.github.nounitheodora.uxsniffer.toolWindow;

import com.github.nounitheodora.uxsniffer.scanner.SmellFinding;
import com.github.nounitheodora.uxsniffer.services.UxAnalysisService;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.table.JBTable;
import com.intellij.util.concurrency.AppExecutorUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;

public class UxToolWindowFactory implements ToolWindowFactory {

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        JPanel mainPanel = new JPanel(new BorderLayout());

        // Summary label at the top
        JBLabel summaryLabel = new JBLabel("Click 'Scan Project' to analyze all .vue files.");
        summaryLabel.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        mainPanel.add(summaryLabel, BorderLayout.NORTH);

        // Findings table
        String[] columns = {"Smell", "File", "Message"};
        DefaultTableModel tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        JBTable table = new JBTable(tableModel);
        table.setAutoCreateRowSorter(true);
        table.getColumnModel().getColumn(0).setPreferredWidth(180);
        table.getColumnModel().getColumn(1).setPreferredWidth(160);
        table.getColumnModel().getColumn(2).setPreferredWidth(460);

        // Double-click to open file
        List<SmellFinding> currentFindings = new ArrayList<>();
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

        mainPanel.add(new JBScrollPane(table), BorderLayout.CENTER);

        // Bottom toolbar with Scan button
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton scanButton = new JButton("Scan Project");
        toolbar.add(scanButton);
        mainPanel.add(toolbar, BorderLayout.SOUTH);

        scanButton.addActionListener(e -> {
            scanButton.setEnabled(false);
            summaryLabel.setText("Scanning...");
            tableModel.setRowCount(0);
            currentFindings.clear();

            AppExecutorUtil.getAppExecutorService().submit(() -> {
                List<SmellFinding> findings = ReadAction.compute(() ->
                        UxAnalysisService.getInstance(project).scanProject());

                ApplicationManager.getApplication().invokeLater(() -> {
                    currentFindings.addAll(findings);
                    for (SmellFinding f : findings) {
                        tableModel.addRow(new Object[]{f.smellName(), f.fileName(), f.message()});
                    }

                    // Build summary
                    Map<String, Integer> smellCounts = new LinkedHashMap<>();
                    Set<String> filesWithSmells = new HashSet<>();
                    for (SmellFinding f : findings) {
                        smellCounts.merge(f.smellName(), 1, Integer::sum);
                        filesWithSmells.add(f.filePath());
                    }

                    if (findings.isEmpty()) {
                        summaryLabel.setText("No UX smells found.");
                    } else {
                        summaryLabel.setText(String.format(
                                "%d smell(s) found across %d file(s). %d distinct smell type(s) detected.",
                                findings.size(), filesWithSmells.size(), smellCounts.size()));
                    }
                    scanButton.setEnabled(true);
                });
            });
        });

        Content content = ContentFactory.getInstance().createContent(mainPanel, "", false);
        toolWindow.getContentManager().addContent(content);
    }

    @Override
    public boolean shouldBeAvailable(@NotNull Project project) {
        return true;
    }
}
