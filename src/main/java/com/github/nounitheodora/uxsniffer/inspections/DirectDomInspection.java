package com.github.nounitheodora.uxsniffer.inspections;

import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class DirectDomInspection extends AbstractVueSmellInspection {

    static final String[] DOCUMENT_METHODS = {
            "getElementById",
            "getElementsByTagName",
            "getElementsByClassName",
            "querySelector",
            "querySelectorAll",
            "createElement"
    };

    static final String[] DOM_MUTATION_METHODS = {
            "appendChild",
            "removeChild",
            "replaceChild",
            "setAttribute"
    };

    static final String[] DOM_PROPERTIES = {
            "innerHTML",
            "innerText",
            "textContent"
    };

    @Override
    public @NotNull String getDisplayName() {
        return "Direct DOM manipulation";
    }

    @Override
    public @NotNull PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return new PsiElementVisitor() {
            @Override
            public void visitFile(@NotNull PsiFile file) {
                if (!isVueFile(file)) return;

                String script = extractScriptContent(file.getText());
                if (script.isEmpty()) return;

                List<String> found = detectDomApis(script);
                if (found.isEmpty()) return;

                holder.registerProblem(file, buildMessage(found), ProblemHighlightType.WARNING);
            }
        };
    }

    @NotNull List<String> detectDomApis(@NotNull String scriptContent) {
        List<String> found = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();

        for (String line : scriptContent.split("\n", -1)) {
            String trimmed = line.trim();
            if (trimmed.startsWith("//") || trimmed.startsWith("*") || trimmed.startsWith("/*")) continue;

            for (String method : DOCUMENT_METHODS) {
                if (trimmed.contains("document." + method + "(") && seen.add(method)) {
                    found.add(method);
                }
            }

            for (String method : DOM_MUTATION_METHODS) {
                if (trimmed.contains("." + method + "(") && seen.add(method)) {
                    found.add(method);
                }
            }

            for (String prop : DOM_PROPERTIES) {
                if (trimmed.contains("." + prop) && seen.add(prop)) {
                    found.add(prop);
                }
            }
        }

        return found;
    }

    @NotNull String buildMessage(@NotNull List<String> apis) {
        if (apis.size() == 1) {
            return String.format(
                    "Direct DOM manipulation via '%s'. Use Vue template refs instead.",
                    apis.get(0));
        }
        return String.format(
                "Direct DOM manipulation via %s. Use Vue template refs instead.",
                formatApiList(apis));
    }

    private @NotNull String formatApiList(@NotNull List<String> apis) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < apis.size(); i++) {
            if (i > 0 && i == apis.size() - 1) sb.append(" and ");
            else if (i > 0) sb.append(", ");
            sb.append("'").append(apis.get(i)).append("'");
        }
        return sb.toString();
    }
}
