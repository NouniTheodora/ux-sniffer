package com.github.nounitheodora.uxsniffer.inspections;

import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class ForceUpdateInspection extends AbstractVueSmellInspection {

    static final String FORCE_UPDATE_PATTERN = "$forceUpdate()";
    static final String LOCATION_RELOAD_PATTERN = "location.reload()";

    @Override
    public @NotNull String getDisplayName() {
        return "Force update / page reload";
    }

    @Override
    public @NotNull PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return new PsiElementVisitor() {
            @Override
            public void visitFile(@NotNull PsiFile file) {
                if (!isVueFile(file)) return;

                String script = extractScriptContent(file.getText());
                if (script.isEmpty()) return;

                List<String> found = detectForceUpdates(script);
                if (found.isEmpty()) return;

                holder.registerProblem(file, buildMessage(found), ProblemHighlightType.WARNING);
            }
        };
    }

    @NotNull List<String> detectForceUpdates(@NotNull String scriptContent) {
        List<String> found = new ArrayList<>();
        boolean hasForceUpdate = false;
        boolean hasReload = false;

        for (String line : scriptContent.split("\n", -1)) {
            String trimmed = line.trim();
            if (trimmed.startsWith("//") || trimmed.startsWith("*") || trimmed.startsWith("/*")) continue;

            if (!hasForceUpdate && trimmed.contains(FORCE_UPDATE_PATTERN)) {
                found.add("$forceUpdate()");
                hasForceUpdate = true;
            }
            if (!hasReload && trimmed.contains(LOCATION_RELOAD_PATTERN)) {
                found.add("location.reload()");
                hasReload = true;
            }
        }

        return found;
    }

    @NotNull String buildMessage(@NotNull List<String> apis) {
        if (apis.size() == 1) {
            String api = apis.get(0);
            if (api.equals("$forceUpdate()")) {
                return "Avoid '$forceUpdate()'. Redesign state so Vue's reactivity handles updates automatically.";
            }
            return "Avoid 'location.reload()'. Use Vue's reactivity or router navigation instead of full page reloads.";
        }
        return "Avoid '$forceUpdate()' and 'location.reload()'. Redesign state so Vue's reactivity handles updates automatically.";
    }
}
