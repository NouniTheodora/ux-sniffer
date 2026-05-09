package com.github.nounitheodora.uxsniffer.inspections;

import com.github.nounitheodora.uxsniffer.UxSnifferBundle;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.regex.Pattern;

public class MultipleBooleansForStateInspection extends AbstractVueSmellInspection {

    public static final int DEFAULT_BOOLEAN_THRESHOLD = 4;

    public int booleanThreshold = DEFAULT_BOOLEAN_THRESHOLD;

    static final Pattern BOOLEAN_REF_PATTERN = Pattern.compile(
            "\\bref\\s*\\(\\s*(true|false)\\s*\\)");

    @Override
    public @NotNull String getDisplayName() {
        return UxSnifferBundle.message("inspection.multiple.booleans.name");
    }

    @Override
    public @Nullable JComponent createOptionsPanel() {
        JPanel panel = new JPanel(new GridLayout(0, 2, 8, 6));
        panel.add(new JLabel("Max boolean refs:"));
        JSpinner spinner = new JSpinner(new SpinnerNumberModel(booleanThreshold, 1, 50, 1));
        spinner.addChangeListener(e -> booleanThreshold = (int) spinner.getValue());
        panel.add(spinner);
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.add(panel, BorderLayout.NORTH);
        return wrapper;
    }

    @Override
    public @Nullable String analyze(@NotNull String fileText) {
        String script = extractScriptContent(fileText);
        if (script.isEmpty()) return null;
        int count = countBooleanRefs(script);
        if (count <= booleanThreshold) return null;
        return buildMessage(count);
    }

    @Override
    public @NotNull PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return new PsiElementVisitor() {
            @Override
            public void visitFile(@NotNull PsiFile file) {
                if (!isVueFile(file)) return;

                String text = file.getText();
                String script = extractScriptContent(text);
                if (script.isEmpty()) return;

                int count = countBooleanRefs(script);
                if (count <= booleanThreshold) return;

                registerProblemOnFile(holder, file, buildMessage(count));
            }
        };
    }

    int countBooleanRefs(@NotNull String scriptContent) {
        int count = 0;
        for (String line : scriptContent.split("\n", -1)) {
            String trimmed = line.trim();
            if (trimmed.startsWith("//") || trimmed.startsWith("*") || trimmed.startsWith("/*")) continue;

            if (BOOLEAN_REF_PATTERN.matcher(trimmed).find()) {
                count++;
            }
        }
        return count;
    }

    @NotNull String buildMessage(int count) {
        return UxSnifferBundle.message("inspection.multiple.booleans.message", count, booleanThreshold);
    }
}
