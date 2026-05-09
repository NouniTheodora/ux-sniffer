package com.github.nounitheodora.uxsniffer.inspections;

import com.github.nounitheodora.uxsniffer.UxSnifferBundle;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InheritanceInspection extends AbstractVueSmellInspection {

    static final Pattern EXTENDS_PATTERN = Pattern.compile(
            "\\bextends\\s*:\\s*(\\w+)");

    @Override
    public @NotNull String getDisplayName() {
        return UxSnifferBundle.message("inspection.inheritance.name");
    }

    @Override
    public @Nullable String analyze(@NotNull String fileText) {
        String script = extractScriptContent(fileText);
        if (script.isEmpty()) return null;
        String component = detectExtends(script);
        if (component == null) return null;
        return buildMessage(component);
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

                String component = detectExtends(script);
                if (component == null) return;

                registerProblemOnFile(holder, file, buildMessage(component));
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
        return UxSnifferBundle.message("inspection.inheritance.message", componentName);
    }
}
