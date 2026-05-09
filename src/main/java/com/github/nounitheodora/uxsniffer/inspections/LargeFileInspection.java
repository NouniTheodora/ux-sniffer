package com.github.nounitheodora.uxsniffer.inspections;

import com.github.nounitheodora.uxsniffer.UxSnifferBundle;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public class LargeFileInspection extends AbstractVueSmellInspection {

    public static final int DEFAULT_LOC_THRESHOLD = 218;
    public static final int DEFAULT_IMPORTS_THRESHOLD = 20;

    public int locThreshold = DEFAULT_LOC_THRESHOLD;
    public int importsThreshold = DEFAULT_IMPORTS_THRESHOLD;

    @Override
    public @NotNull String getDisplayName() {
        return UxSnifferBundle.message("inspection.large.file.name");
    }

    @Override
    public @Nullable JComponent createOptionsPanel() {
        JPanel panel = new JPanel(new GridLayout(0, 2, 8, 6));

        panel.add(new JLabel("Max lines of code:"));
        JSpinner locSpinner = new JSpinner(new SpinnerNumberModel(locThreshold, 1, 10000, 1));
        locSpinner.addChangeListener(e -> locThreshold = (int) locSpinner.getValue());
        panel.add(locSpinner);

        panel.add(new JLabel("Max import statements:"));
        JSpinner importsSpinner = new JSpinner(new SpinnerNumberModel(importsThreshold, 1, 1000, 1));
        importsSpinner.addChangeListener(e -> importsThreshold = (int) importsSpinner.getValue());
        panel.add(importsSpinner);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.add(panel, BorderLayout.NORTH);
        return wrapper;
    }

    @Override
    public @Nullable String analyze(@NotNull String fileText) {
        boolean locExceeded = countLines(fileText) > locThreshold;
        boolean importsExceeded = countImports(fileText) > importsThreshold;
        if (!locExceeded && !importsExceeded) return null;
        return buildMessage(fileText, locExceeded, importsExceeded);
    }

    @Override
    public boolean supportsTypeScript() { return true; }

    @Override
    public @Nullable String analyzeTypeScript(@NotNull String fileText) {
        return analyze(fileText);
    }

    @Override
    public @NotNull PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return new PsiElementVisitor() {
            @Override
            public void visitFile(@NotNull PsiFile file) {
                if (!isVueFile(file)) return;

                String text = file.getText();
                boolean locExceeded = countLines(text) > locThreshold;
                boolean importsExceeded = countImports(text) > importsThreshold;

                if (!locExceeded && !importsExceeded) return;

                registerProblemOnFile(holder, file, buildMessage(text, locExceeded, importsExceeded));
            }
        };
    }

    @NotNull String buildMessage(@NotNull String text, boolean locExceeded, boolean importsExceeded) {
        int loc = countLines(text);
        int imports = countImports(text);

        if (locExceeded && importsExceeded) {
            return UxSnifferBundle.message("inspection.large.file.both", loc, locThreshold, imports, importsThreshold);
        }
        if (locExceeded) {
            return UxSnifferBundle.message("inspection.large.file.loc", loc, locThreshold);
        }
        return UxSnifferBundle.message("inspection.large.file.imports", imports, importsThreshold);
    }

    int countImports(@NotNull String text) {
        int count = 0;
        for (String line : text.split("\n", -1)) {
            if (line.trim().startsWith("import ")) count++;
        }
        return count;
    }

    int findFirstImportOffset(@NotNull String text) {
        if (text.startsWith("import ")) return 0;
        int idx = text.indexOf("\nimport ");
        return idx >= 0 ? idx + 1 : -1;
    }
}
