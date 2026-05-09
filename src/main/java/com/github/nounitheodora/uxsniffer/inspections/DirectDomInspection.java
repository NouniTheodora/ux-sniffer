package com.github.nounitheodora.uxsniffer.inspections;

import com.github.nounitheodora.uxsniffer.UxSnifferBundle;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * DirectDomInspection:
 * This class scans the <script setup> block for 14 DOM APIs across 3 categories:
 * document.* methods (getElementById, querySelector, etc.), DOM mutation methods (appendChild, removeChild, etc.),
 * and DOM properties (innerHTML, innerText, textContent).
 * It skips commented-out lines. Reports all detected APIs in a single combined message.
 */
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
        return UxSnifferBundle.message("inspection.direct.dom.name");
    }

    @Override
    public @Nullable String analyze(@NotNull String fileText) {
        String script = extractScriptContent(fileText);
        if (script.isEmpty()) return null;
        List<String> found = detectDomApis(script);
        if (found.isEmpty()) return null;
        return buildMessage(found);
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

                List<String> found = detectDomApis(script);
                if (found.isEmpty()) return;

                registerProblemOnFile(holder, file, buildMessage(found));
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
            return UxSnifferBundle.message("inspection.direct.dom.single", apis.get(0));
        }
        return UxSnifferBundle.message("inspection.direct.dom.multiple", formatApiList(apis));
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
