package com.github.nounitheodora.uxsniffer.inspections;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.util.JDOMExternalizerUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Template Method pattern — defines the skeleton of the smell detection algorithm
 * (file validation, script extraction, line counting) while letting each concrete
 * subclass override {@link #analyze(String)} with its specific detection logic.
 *
 * @see <a href="https://refactoring.guru/design-patterns/template-method">Template Method — Refactoring Guru</a>
 */
public abstract class AbstractVueSmellInspection extends LocalInspectionTool {

    @Override
    public abstract @NotNull String getDisplayName();

    /**
     * Analyzes raw .vue file text and returns a warning message if the smell
     * is detected, or null if the file is clean. Used by the project scanner
     * to collect findings without needing PSI.
     */
    public @Nullable String analyze(@NotNull String fileText) {
        return null;
    }

    /**
     * Analyzes a raw TypeScript file (not wrapped in a .vue component).
     * Override in inspections that apply to plain .ts files.
     */
    public @Nullable String analyzeTypeScript(@NotNull String fileText) {
        return null;
    }

    public boolean supportsTypeScript() {
        return false;
    }

    @Override
    public @NotNull String getGroupDisplayName() {
        return "Vue.js UX Smells";
    }

    @Override
    public boolean isEnabledByDefault() {
        return true;
    }

    protected void registerProblemOnFile(@NotNull ProblemsHolder holder, @NotNull PsiFile file,
                                           @NotNull String message) {
        holder.registerProblem(file, message, ProblemHighlightType.WARNING);
    }

    protected boolean isVueFile(@NotNull PsiFile file) {
        VirtualFile vFile = file.getVirtualFile();
        if (vFile == null) vFile = file.getOriginalFile().getVirtualFile();
        return vFile != null && "vue".equalsIgnoreCase(vFile.getExtension());
    }

    protected boolean isTypeScriptFile(@NotNull PsiFile file) {
        VirtualFile vFile = file.getVirtualFile();
        if (vFile == null) vFile = file.getOriginalFile().getVirtualFile();
        return vFile != null && "ts".equalsIgnoreCase(vFile.getExtension());
    }

    protected int countLines(@NotNull String text) {
        String[] lines = text.split("\n", -1);
        if (lines.length > 0 && lines[lines.length - 1].isEmpty()) {
            return lines.length - 1;
        }
        return lines.length;
    }

    protected @NotNull String extractScriptContent(@NotNull String text) {
        int scriptStart = text.indexOf("<script");
        if (scriptStart < 0) return "";
        int contentStart = text.indexOf('>', scriptStart);
        if (contentStart < 0) return "";
        int scriptEnd = text.indexOf("</script>", contentStart);
        if (scriptEnd < 0) return "";
        return text.substring(contentStart + 1, scriptEnd);
    }

    protected boolean isNotTypeScriptSetup(@NotNull String text) {
        int scriptStart = text.indexOf("<script");
        if (scriptStart < 0) return true;
        int tagEnd = text.indexOf('>', scriptStart);
        if (tagEnd < 0) return true;
        String tag = text.substring(scriptStart, tagEnd);
        return !tag.contains("setup") ||
               (!tag.contains("lang=\"ts\"") && !tag.contains("lang=\"typescript\""));
    }

    /**
     * Extracts the content between a matched pair of open/close characters
     * starting at the given index. Returns null if not found.
     */
    protected @Nullable String extractBlock(@NotNull String text, int start, char open, char close) {
        if (start >= text.length() || text.charAt(start) != open) return null;
        int depth = 0;
        for (int i = start; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == open) depth++;
            else if (c == close) {
                depth--;
                if (depth == 0) return text.substring(start + 1, i);
            }
        }
        return null;
    }

    protected static int readIntField(@NotNull Element element, @NotNull String name, int defaultValue) {
        String value = JDOMExternalizerUtil.readField(element, name);
        if (value == null) return defaultValue;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
