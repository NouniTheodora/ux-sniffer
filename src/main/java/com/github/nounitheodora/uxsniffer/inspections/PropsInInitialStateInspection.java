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

/**
 * PropsInInitialStateInspection:
 * This class uses two regex patterns to detect ref(props.x) and reactive({ ... props.x }).
 * It collects all affected prop names, deduplicates them, and
 * reports in a single message suggesting using computed() instead.
 */
public class PropsInInitialStateInspection extends AbstractVueSmellInspection {

    static final Pattern REF_PROPS_PATTERN = Pattern.compile(
            "\\bref\\(\\s*props\\.(\\w+)\\s*\\)");

    static final Pattern REACTIVE_PROPS_PATTERN = Pattern.compile(
            "\\breactive\\(\\s*\\{[^}]*props\\.(\\w+)");

    @Override
    public @NotNull String getDisplayName() {
        return "Props used in initial state";
    }

    @Override
    public @Nullable String analyze(@NotNull String fileText) {
        String script = extractScriptContent(fileText);
        if (script.isEmpty()) return null;
        List<String> found = detectPropsInState(script);
        if (found.isEmpty()) return null;
        return buildMessage(found);
    }

    @Override
    public @NotNull PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return new PsiElementVisitor() {
            @Override
            public void visitFile(@NotNull PsiFile file) {
                if (!isVueFile(file)) return;

                String script = extractScriptContent(file.getText());
                if (script.isEmpty()) return;

                List<String> found = detectPropsInState(script);
                if (found.isEmpty()) return;

                holder.registerProblem(file, buildMessage(found), ProblemHighlightType.WARNING);
            }
        };
    }

    @NotNull List<String> detectPropsInState(@NotNull String scriptContent) {
        Set<String> seen = new LinkedHashSet<>();

        for (String line : scriptContent.split("\n", -1)) {
            String trimmed = line.trim();
            if (trimmed.startsWith("//") || trimmed.startsWith("*") || trimmed.startsWith("/*")) continue;

            Matcher refMatcher = REF_PROPS_PATTERN.matcher(trimmed);
            while (refMatcher.find()) {
                seen.add(refMatcher.group(1));
            }

            Matcher reactiveMatcher = REACTIVE_PROPS_PATTERN.matcher(trimmed);
            while (reactiveMatcher.find()) {
                seen.add(reactiveMatcher.group(1));
            }
        }

        return new ArrayList<>(seen);
    }

    @NotNull String buildMessage(@NotNull List<String> propNames) {
        if (propNames.size() == 1) {
            return String.format(
                    "Prop '%s' used to initialise reactive state. Use computed(() => props.%s) to stay in sync.",
                    propNames.get(0), propNames.get(0));
        }
        return String.format(
                "Props %s used to initialise reactive state. Use computed() to stay in sync with prop changes.",
                formatPropList(propNames));
    }

    private @NotNull String formatPropList(@NotNull List<String> names) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < names.size(); i++) {
            if (i > 0 && i == names.size() - 1) sb.append(" and ");
            else if (i > 0) sb.append(", ");
            sb.append("'").append(names.get(i)).append("'");
        }
        return sb.toString();
    }
}
