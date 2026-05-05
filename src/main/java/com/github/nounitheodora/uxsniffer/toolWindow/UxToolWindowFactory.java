package com.github.nounitheodora.uxsniffer.toolwindow;

import com.github.nounitheodora.uxsniffer.report.HtmlReportExporter;
import com.github.nounitheodora.uxsniffer.scanner.SmellFinding;
import com.github.nounitheodora.uxsniffer.services.UxAnalysisService;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.fileChooser.FileChooserFactory;
import com.intellij.openapi.fileChooser.FileSaverDescriptor;
import com.intellij.openapi.fileChooser.FileSaverDialog;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFileWrapper;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.util.concurrency.AppExecutorUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class UxToolWindowFactory implements ToolWindowFactory {

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        JPanel mainPanel = new JPanel(new BorderLayout());

        FindingsPanel findingsPanel = new FindingsPanel(project);
        StatisticsPanel statisticsPanel = new StatisticsPanel();

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Findings", findingsPanel);
        tabbedPane.addTab("Statistics", statisticsPanel);
        mainPanel.add(tabbedPane, BorderLayout.CENTER);

        // Bottom toolbar
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton scanButton = new JButton("Scan Project");
        JButton exportButton = new JButton("Export Report");
        exportButton.setEnabled(false);
        toolbar.add(scanButton);
        toolbar.add(exportButton);
        mainPanel.add(toolbar, BorderLayout.SOUTH);

        List<SmellFinding> currentFindings = new ArrayList<>();

        scanButton.addActionListener(e -> {
            scanButton.setEnabled(false);
            exportButton.setEnabled(false);
            findingsPanel.setScanning();
            statisticsPanel.reset();
            currentFindings.clear();

            AppExecutorUtil.getAppExecutorService().submit(() -> {
                List<SmellFinding> findings = ReadAction.compute(() ->
                        UxAnalysisService.getInstance(project).scanProject());

                ApplicationManager.getApplication().invokeLater(() -> {
                    currentFindings.addAll(findings);
                    findingsPanel.update(findings);
                    statisticsPanel.update(findings);
                    scanButton.setEnabled(true);
                    exportButton.setEnabled(!findings.isEmpty());
                });
            });
        });

        exportButton.addActionListener(e -> {
            String projectName = project.getName();
            com.intellij.openapi.vfs.VirtualFile projectDir = com.intellij.openapi.project.ProjectUtil.guessProjectDir(project);
            String basePath = projectDir != null ? projectDir.getPath() : "";
            String html = HtmlReportExporter.generate(currentFindings, projectName, basePath);

            FileSaverDescriptor descriptor = new FileSaverDescriptor(
                    "Export UXSniffer Report", "Save HTML report", "html");
            FileSaverDialog dialog = FileChooserFactory.getInstance()
                    .createSaveFileDialog(descriptor, project);
            String defaultFileName = "UXSniffer_Report_" + projectName.replaceAll("[^a-zA-Z0-9._-]", "_") + ".html";
            VirtualFileWrapper wrapper = dialog.save((com.intellij.openapi.vfs.VirtualFile) null, defaultFileName);
            if (wrapper != null) {
                try {
                    Files.writeString(wrapper.getFile().toPath(), html, StandardCharsets.UTF_8);
                    BrowserUtil.browse(wrapper.getFile());
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(mainPanel,
                            "Failed to save report: " + ex.getMessage(),
                            "Export Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        Content content = ContentFactory.getInstance().createContent(mainPanel, "", false);
        toolWindow.getContentManager().addContent(content);
    }

    @Override
    public boolean shouldBeAvailable(@NotNull Project project) {
        return true;
    }
}
