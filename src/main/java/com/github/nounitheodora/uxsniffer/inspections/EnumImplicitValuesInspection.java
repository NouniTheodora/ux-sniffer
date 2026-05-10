package com.github.nounitheodora.uxsniffer.inspections;

import com.github.nounitheodora.uxsniffer.UxSnifferBundle;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EnumImplicitValuesInspection extends AbstractVueSmellInspection {

    static final Pattern ENUM_DECLARATION = Pattern.compile(
            "\\benum\\s+(\\w+)\\s*\\{");

    @Override
    public @NotNull String getDisplayName() {
        return UxSnifferBundle.message("inspection.enum.implicit.name");
    }

    @Override
    public @Nullable String analyze(@NotNull String fileText) {
        if (isNotTypeScriptSetup(fileText)) return null;
        String script = extractScriptContent(fileText);
        if (script.isEmpty()) return null;
        List<String> found = detectImplicitEnums(script);
        if (found.isEmpty()) return null;
        return buildMessage(found);
    }

    @Override
    public @NotNull PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return new PsiElementVisitor() {
            @Override
            public void visitFile(@NotNull PsiFile file) {
                if (isTypeScriptFile(file)) {
                    String message = analyzeTypeScript(file.getText());
                    if (message != null) registerProblemOnFile(holder, file, message);
                    return;
                }
                if (!isVueFile(file)) return;

                String text = file.getText();
                if (isNotTypeScriptSetup(text)) return;

                String script = extractScriptContent(text);
                if (script.isEmpty()) return;

                List<String> found = detectImplicitEnums(script);
                if (found.isEmpty()) return;

                registerProblemOnFile(holder, file, buildMessage(found));
            }
        };
    }

    @Override
    public boolean supportsTypeScript() { return true; }

    @Override
    public @Nullable String analyzeTypeScript(@NotNull String fileText) {
        List<String> found = detectImplicitEnums(fileText);
        return found.isEmpty() ? null : buildMessage(found);
    }

    @NotNull List<String> detectImplicitEnums(@NotNull String scriptContent) {
        List<String> found = new ArrayList<>();
        Matcher matcher = ENUM_DECLARATION.matcher(scriptContent);

        while (matcher.find()) {
            String enumName = matcher.group(1);
            int braceStart = matcher.end() - 1;
            String block = extractBlock(scriptContent, braceStart, '{', '}');
            if (block != null && hasImplicitValues(block)) {
                found.add(enumName);
            }
        }

        return found;
    }

    boolean hasImplicitValues(@NotNull String enumBlock) {
        for (String line : enumBlock.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("//") || trimmed.startsWith("*")) continue;
            String member = trimmed.replace(",", "").trim();
            if (!member.isEmpty() && member.matches("\\w+") && !member.contains("=")) {
                return true;
            }
        }
        return false;
    }

    @NotNull String buildMessage(@NotNull List<String> enumNames) {
        if (enumNames.size() == 1) {
            return UxSnifferBundle.message("inspection.enum.implicit.single", enumNames.getFirst());
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < enumNames.size(); i++) {
            if (i > 0 && i == enumNames.size() - 1) sb.append(" and ");
            else if (i > 0) sb.append(", ");
            sb.append("'").append(enumNames.get(i)).append("'");
        }
        return UxSnifferBundle.message("inspection.enum.implicit.multiple", sb);
    }
}
