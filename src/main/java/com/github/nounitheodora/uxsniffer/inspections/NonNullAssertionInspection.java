package com.github.nounitheodora.uxsniffer.inspections;

import com.github.nounitheodora.uxsniffer.UxSnifferBundle;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NonNullAssertionInspection extends AbstractVueSmellInspection {

    static final Pattern NON_NULL_PATTERN = Pattern.compile(
            "\\w!\\.");

    @Override
    public @NotNull String getDisplayName() {
        return UxSnifferBundle.message("inspection.non.null.assertion.name");
    }

    @Override
    public @Nullable String analyze(@NotNull String fileText) {
        if (isNotTypeScriptSetup(fileText)) return null;
        String script = extractScriptContent(fileText);
        if (script.isEmpty()) return null;
        int count = countNonNullAssertions(script);
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
                if (isNotTypeScriptSetup(text)) return;

                String script = extractScriptContent(text);
                if (script.isEmpty()) return;

                int count = countNonNullAssertions(script);
                if (count == 0) return;

                registerProblemOnFile(holder, file, buildMessage(count));
            }
        };
    }

    @Override
    public boolean supportsTypeScript() { return true; }

    @Override
    public @Nullable String analyzeTypeScript(@NotNull String fileText) {
        int count = countNonNullAssertions(fileText);
        return count == 0 ? null : buildMessage(count);
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
            return UxSnifferBundle.message("inspection.non.null.single");
        }
        return UxSnifferBundle.message("inspection.non.null.multiple", count);
    }
}
