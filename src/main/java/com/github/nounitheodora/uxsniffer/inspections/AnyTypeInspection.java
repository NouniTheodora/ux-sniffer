package com.github.nounitheodora.uxsniffer.inspections;

import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AnyTypeInspection extends AbstractVueSmellInspection {

    static final Pattern ANY_TYPE_PATTERN = Pattern.compile(
            "[:<,]\\s*any\\b");

    @Override
    public @NotNull String getDisplayName() {
        return "Any type usage";
    }

    @Override
    public @Nullable String analyze(@NotNull String fileText) {
        if (!isTypeScriptSetup(fileText)) return null;
        String script = extractScriptContent(fileText);
        if (script.isEmpty()) return null;
        int count = countAnyUsages(script);
        if (count == 0) return null;
        return buildMessage(count);
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

                int count = countAnyUsages(script);
                if (count == 0) return;

                holder.registerProblem(file, buildMessage(count), ProblemHighlightType.WARNING);
            }
        };
    }

    int countAnyUsages(@NotNull String scriptContent) {
        int count = 0;
        for (String line : scriptContent.split("\n", -1)) {
            String trimmed = line.trim();
            if (trimmed.startsWith("//") || trimmed.startsWith("*") || trimmed.startsWith("/*")) continue;

            Matcher matcher = ANY_TYPE_PATTERN.matcher(trimmed);
            while (matcher.find()) {
                count++;
            }
        }
        return count;
    }

    @NotNull String buildMessage(int count) {
        if (count == 1) {
            return "TypeScript 'any' type used. This disables type checking and may hide errors. Define explicit types instead.";
        }
        return String.format(
                "TypeScript 'any' type used %d times. This disables type checking and may hide errors. Define explicit types instead.",
                count);
    }
}
