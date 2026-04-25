package com.github.nounitheodora.uxsniffer.inspections;

import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InheritanceInspection extends AbstractVueSmellInspection {

    static final Pattern EXTENDS_PATTERN = Pattern.compile(
            "\\bextends\\s*:\\s*(\\w+)");

    @Override
    public @NotNull String getDisplayName() {
        return "Inheritance instead of composition";
    }

    @Override
    public @NotNull PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return new PsiElementVisitor() {
            @Override
            public void visitFile(@NotNull PsiFile file) {
                if (!isVueFile(file)) return;

                String script = extractScriptContent(file.getText());
                if (script.isEmpty()) return;

                String component = detectExtends(script);
                if (component == null) return;

                holder.registerProblem(file, buildMessage(component), ProblemHighlightType.WARNING);
            }
        };
    }

    String detectExtends(@NotNull String scriptContent) {
        for (String line : scriptContent.split("\n", -1)) {
            String trimmed = line.trim();
            if (trimmed.startsWith("//") || trimmed.startsWith("*") || trimmed.startsWith("/*")) continue;

            Matcher matcher = EXTENDS_PATTERN.matcher(trimmed);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        return null;
    }

    @NotNull String buildMessage(@NotNull String componentName) {
        return String.format(
                "Inheritance via 'extends: %s' detected. Prefer composables or component composition over class-style inheritance.",
                componentName);
    }
}
