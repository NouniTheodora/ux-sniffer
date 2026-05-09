package com.github.nounitheodora.uxsniffer.inspections;

import com.github.nounitheodora.uxsniffer.UxSnifferBundle;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public class TooManyPropsInspection extends AbstractVueSmellInspection {

    public static final int DEFAULT_PROPS_THRESHOLD = 13;

    public int propsThreshold = DEFAULT_PROPS_THRESHOLD;

    @Override
    public @NotNull String getDisplayName() {
        return UxSnifferBundle.message("inspection.too.many.props.name");
    }

    @Override
    public @Nullable JComponent createOptionsPanel() {
        JPanel panel = new JPanel(new GridLayout(0, 2, 8, 6));
        panel.add(new JLabel("Max props:"));
        JSpinner spinner = new JSpinner(new SpinnerNumberModel(propsThreshold, 1, 200, 1));
        spinner.addChangeListener(e -> propsThreshold = (int) spinner.getValue());
        panel.add(spinner);
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.add(panel, BorderLayout.NORTH);
        return wrapper;
    }

    @Override
    public @Nullable String analyze(@NotNull String fileText) {
        String script = extractScriptContent(fileText);
        if (script.isEmpty()) return null;
        int props = countProps(script);
        if (props <= propsThreshold) return null;
        return buildMessage(props);
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

                int props = countProps(script);
                if (props <= propsThreshold) return;

                registerProblemOnFile(holder, file, buildMessage(props));
            }
        };
    }

    @NotNull String buildMessage(int props) {
        return UxSnifferBundle.message("inspection.too.many.props.message", props, propsThreshold);
    }

    int countProps(@NotNull String scriptContent) {
        // TypeScript generic: defineProps<{ ... }>()
        int tsIdx = scriptContent.indexOf("defineProps<{");
        if (tsIdx >= 0) {
            String block = extractBlock(scriptContent, tsIdx + "defineProps<".length(), '{', '}');
            if (block != null) return countObjectProps(block);
        }

        // Object syntax: defineProps({ ... })
        int objIdx = scriptContent.indexOf("defineProps({");
        if (objIdx >= 0) {
            String block = extractBlock(scriptContent, objIdx + "defineProps(".length(), '{', '}');
            if (block != null) return countObjectProps(block);
        }

        // Array syntax: defineProps([ ... ])
        int arrIdx = scriptContent.indexOf("defineProps([");
        if (arrIdx >= 0) {
            String block = extractBlock(scriptContent, arrIdx + "defineProps(".length(), '[', ']');
            if (block != null) return countArrayItems(block);
        }

        return 0;
    }

    int countObjectProps(@NotNull String propsBlock) {
        int count = 0;
        int depth = 0;
        for (String line : propsBlock.split("\n")) {
            String trimmed = line.trim();
            // Count top-level prop keys before processing braces on this line
            if (depth == 0 && trimmed.matches("[a-zA-Z_$][a-zA-Z0-9_$]*\\??:.*")) {
                count++;
            }
            for (char c : trimmed.toCharArray()) {
                if (c == '{' || c == '[' || c == '(') depth++;
                else if (c == '}' || c == ']' || c == ')') depth--;
            }
        }
        return count;
    }

    int countArrayItems(@NotNull String arrayContent) {
        if (arrayContent.trim().isEmpty()) return 0;
        int count = 0;
        for (String item : arrayContent.split(",")) {
            if (!item.trim().isEmpty()) count++;
        }
        return count;
    }
}
