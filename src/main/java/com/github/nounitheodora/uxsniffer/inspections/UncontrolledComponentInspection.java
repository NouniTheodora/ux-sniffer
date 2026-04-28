package com.github.nounitheodora.uxsniffer.inspections;

import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UncontrolledComponentInspection extends AbstractVueSmellInspection {

    static final Pattern INPUT_TAG_PATTERN = Pattern.compile(
            "<(input|textarea|select)\\b([^>]*)/?>");

    @Override
    public @NotNull String getDisplayName() {
        return "Uncontrolled form component";
    }

    @Override
    public @Nullable String analyze(@NotNull String fileText) {
        String template = extractTemplateContent(fileText);
        if (template.isEmpty()) return null;
        List<String> found = detectUncontrolled(template);
        if (found.isEmpty()) return null;
        return buildMessage(found);
    }

    @Override
    public @NotNull PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return new PsiElementVisitor() {
            @Override
            public void visitFile(@NotNull PsiFile file) {
                if (!isVueFile(file)) return;

                String template = extractTemplateContent(file.getText());
                if (template.isEmpty()) return;

                List<String> found = detectUncontrolled(template);
                if (found.isEmpty()) return;

                holder.registerProblem(file, buildMessage(found), ProblemHighlightType.WARNING);
            }
        };
    }

    @NotNull String extractTemplateContent(@NotNull String text) {
        int start = text.indexOf("<template");
        if (start < 0) return "";
        int contentStart = text.indexOf('>', start);
        if (contentStart < 0) return "";
        int end = text.indexOf("</template>", contentStart);
        if (end < 0) return "";
        return text.substring(contentStart + 1, end);
    }

    @NotNull List<String> detectUncontrolled(@NotNull String templateContent) {
        List<String> found = new ArrayList<>();
        Matcher matcher = INPUT_TAG_PATTERN.matcher(templateContent);

        while (matcher.find()) {
            String tag = matcher.group(1);
            String attrs = matcher.group(2);

            if (hasRef(attrs) && !hasValueBinding(attrs)) {
                found.add(tag);
            }
        }

        return found;
    }

    boolean hasRef(@NotNull String attrs) {
        return attrs.contains(" ref=") || attrs.matches("(?s)^\\s*ref=.*");
    }

    boolean hasValueBinding(@NotNull String attrs) {
        return attrs.contains("v-model") ||
               attrs.contains(":value") ||
               attrs.contains("v-bind:value");
    }

    @NotNull String buildMessage(@NotNull List<String> tags) {
        if (tags.size() == 1) {
            return String.format(
                    "Uncontrolled <%s>: uses a ref but has no v-model or :value binding. Bind the value reactively instead.",
                    tags.get(0));
        }
        return String.format(
                "%d uncontrolled form elements use a ref but have no v-model or :value binding. Bind values reactively instead.",
                tags.size());
    }
}
