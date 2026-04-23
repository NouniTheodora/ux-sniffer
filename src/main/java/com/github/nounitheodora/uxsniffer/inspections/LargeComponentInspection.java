package com.github.nounitheodora.uxsniffer.inspections;

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
        return "Large Vue.js component";
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

                holder.registerProblem(file,
                        buildMessage(script, locExceeded, fnExceeded),
                        ProblemHighlightType.WARNING);
            }
        };
    }

    @NotNull String buildMessage(@NotNull String script, boolean locExceeded, boolean fnExceeded) {
        int loc = countLines(script);
        int fn = countFunctions(script);

        if (locExceeded && fnExceeded) {
            return String.format(
                    "Large component: script block has %d lines (threshold: %d) and %d functions (threshold: %d). Consider splitting logic into composables.",
                    loc, scriptLocThreshold, fn, functionsThreshold);
        }
        if (locExceeded) {
            return String.format(
                    "Large component: script block has %d lines (threshold: %d). Consider splitting logic into composables.",
                    loc, scriptLocThreshold);
        }
        return String.format(
                "Large component: %d functions defined (threshold: %d). Consider extracting logic into composables.",
                fn, functionsThreshold);
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
