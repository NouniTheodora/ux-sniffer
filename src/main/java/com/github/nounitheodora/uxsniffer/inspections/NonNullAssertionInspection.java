package com.github.nounitheodora.uxsniffer.inspections;

import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NonNullAssertionInspection extends AbstractVueSmellInspection {

    static final Pattern NON_NULL_PATTERN = Pattern.compile(
            "\\w!\\.");

    @Override
    public @NotNull String getDisplayName() {
        return "Non-null assertion";
    }

    @Override
    public @NotNull PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return new PsiElementVisitor() {
            @Override
            public void visitFile(@NotNull PsiFile file) {
                if (!isVueFile(file)) return;

                String text = file.getText();
                if (!isTypeScriptSetup(text)) return;

                String script = extractScriptContent(text);
                if (script.isEmpty()) return;

                int count = countNonNullAssertions(script);
                if (count == 0) return;

                holder.registerProblem(file, buildMessage(count), ProblemHighlightType.WARNING);
            }
        };
    }

    int countNonNullAssertions(@NotNull String scriptContent) {
        int count = 0;
        for (String line : scriptContent.split("\n", -1)) {
            String trimmed = line.trim();
            if (trimmed.startsWith("//") || trimmed.startsWith("*") || trimmed.startsWith("/*")) continue;
            if (trimmed.contains("!==") || trimmed.contains("!=")) {
                trimmed = trimmed.replace("!==", "   ").replace("!=", "  ");
            }

            Matcher matcher = NON_NULL_PATTERN.matcher(trimmed);
            while (matcher.find()) {
                count++;
            }
        }
        return count;
    }

    @NotNull String buildMessage(int count) {
        if (count == 1) {
            return "Non-null assertion operator ('!') used. This may hide null/undefined errors at runtime. Use proper null checks instead.";
        }
        return String.format(
                "Non-null assertion operator ('!') used %d times. This may hide null/undefined errors at runtime. Use proper null checks instead.",
                count);
    }
}
