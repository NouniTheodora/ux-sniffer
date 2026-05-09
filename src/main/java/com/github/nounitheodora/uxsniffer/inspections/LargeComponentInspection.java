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

public class LargeComponentInspection extends AbstractVueSmellInspection {

    public static final int DEFAULT_SCRIPT_LOC_THRESHOLD = 128;
    public static final int DEFAULT_FUNCTIONS_THRESHOLD = 4;

    public int scriptLocThreshold = DEFAULT_SCRIPT_LOC_THRESHOLD;
    public int functionsThreshold = DEFAULT_FUNCTIONS_THRESHOLD;

    @Override
    public @NotNull String getDisplayName() {
        return UxSnifferBundle.message("inspection.large.component.name");
    }

    @Override
    public @Nullable JComponent createOptionsPanel() {
        JPanel panel = new JPanel(new GridLayout(0, 2, 8, 6));

        panel.add(new JLabel("Max script block lines:"));
        JSpinner locSpinner = new JSpinner(new SpinnerNumberModel(scriptLocThreshold, 1, 5000, 1));
        locSpinner.addChangeListener(e -> scriptLocThreshold = (int) locSpinner.getValue());
        panel.add(locSpinner);

        panel.add(new JLabel("Max functions:"));
        JSpinner fnSpinner = new JSpinner(new SpinnerNumberModel(functionsThreshold, 1, 100, 1));
        fnSpinner.addChangeListener(e -> functionsThreshold = (int) fnSpinner.getValue());
        panel.add(fnSpinner);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.add(panel, BorderLayout.NORTH);
        return wrapper;
    }

    @Override
    public @Nullable String analyze(@NotNull String fileText) {
        String script = extractScriptContent(fileText);
        if (script.isEmpty()) return null;
        boolean locExceeded = countLines(script) > scriptLocThreshold;
        boolean fnExceeded = countFunctions(script) > functionsThreshold;
        if (!locExceeded && !fnExceeded) return null;
        return buildMessage(script, locExceeded, fnExceeded);
    }

    @Override
    public @NotNull PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return new PsiElementVisitor() {
            @Override
            public void visitFile(@NotNull PsiFile file) {
                if (!isVueFile(file)) return;

                String script = extractScriptContent(file.getText());
                if (script.isEmpty()) return;

                boolean locExceeded = countLines(script) > scriptLocThreshold;
                boolean fnExceeded = countFunctions(script) > functionsThreshold;

                if (!locExceeded && !fnExceeded) return;

                registerProblemOnFile(holder, file,
                        buildMessage(script, locExceeded, fnExceeded));
            }
        };
    }

    @NotNull String buildMessage(@NotNull String script, boolean locExceeded, boolean fnExceeded) {
        int loc = countLines(script);
        int fn = countFunctions(script);

        if (locExceeded && fnExceeded) {
            return UxSnifferBundle.message("inspection.large.component.both", loc, scriptLocThreshold, fn, functionsThreshold);
        }
        if (locExceeded) {
            return UxSnifferBundle.message("inspection.large.component.loc", loc, scriptLocThreshold);
        }
        return UxSnifferBundle.message("inspection.large.component.functions", fn, functionsThreshold);
    }

    int countFunctions(@NotNull String scriptContent) {
        int count = 0;
        for (String line : scriptContent.split("\n", -1)) {
            String trimmed = line.trim();
            if (trimmed.startsWith("function ") ||
                    trimmed.startsWith("async function ") ||
                    isArrowFunctionAssignment(trimmed)) {
                count++;
            }
        }
        return count;
    }

    boolean isArrowFunctionAssignment(@NotNull String trimmedLine) {
        if (!trimmedLine.startsWith("const ") && !trimmedLine.startsWith("let ")) return false;
        if (!trimmedLine.contains("=> {") && !trimmedLine.contains("=> (")) return false;
        // Exclude Vue reactivity primitives — their callbacks are not component functions
        return !trimmedLine.contains("computed(") &&
               !trimmedLine.contains("watch(") &&
               !trimmedLine.contains("watchEffect(");
    }
}
