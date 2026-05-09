package com.github.nounitheodora.uxsniffer.scanner;

import com.github.nounitheodora.uxsniffer.inspections.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class ProjectScanner {
    private static final List<String> EXCLUDED_FILE_SUFFIXES = List.of(
            ".spec.ts", ".test.ts", ".spec.js", ".test.js",
            ".spec.vue", ".test.vue", ".d.ts"
    );

    private static final List<String> EXCLUDED_DIRECTORY_SEGMENTS = List.of(
            "__tests__", "__test__", "__mocks__",
            "node_modules", "dist", "build", ".output", ".nuxt", ".vite",
            "styles", "css", "scss", "assets"
    );

    private final Project project;

    private static final List<AbstractVueSmellInspection> INSPECTIONS = List.of(
            new LargeFileInspection(),
            new LargeComponentInspection(),
            new TooManyPropsInspection(),
            new DirectDomInspection(),
            new ForceUpdateInspection(),
            new PropsInInitialStateInspection(),
            new UncontrolledComponentInspection(),
            new InheritanceInspection(),
            new AnyTypeInspection(),
            new NonNullAssertionInspection(),
            new MultipleBooleansForStateInspection(),
            new EnumImplicitValuesInspection()
    );

    public ProjectScanner(@NotNull Project project) {
        this.project = project;
    }

    public @NotNull List<SmellFinding> scan() {
        List<SmellFinding> findings = new ArrayList<>();
        IgnoreFileParser ignoreParser = new IgnoreFileParser(project);
        GlobalSearchScope scope = GlobalSearchScope.projectScope(project);

        Collection<VirtualFile> vueFiles = FilenameIndex.getAllFilesByExt(project, "vue", scope);
        for (VirtualFile vf : vueFiles) {
            scanVueFile(vf, ignoreParser, findings);
        }

        Collection<VirtualFile> tsFiles = FilenameIndex.getAllFilesByExt(project, "ts", scope);
        for (VirtualFile vf : tsFiles) {
            scanTypeScriptFile(vf, ignoreParser, findings);
        }

        return findings;
    }

    private static void scanVueFile(@NotNull VirtualFile vf, @NotNull IgnoreFileParser ignoreParser,
                                    @NotNull List<SmellFinding> findings) {
        if (isExcludedFile(vf) || ignoreParser.isIgnored(vf)) return;
        String text = readFile(vf);
        if (text == null) return;

        for (AbstractVueSmellInspection inspection : INSPECTIONS) {
            String message = inspection.analyze(text);
            if (message != null) {
                findings.add(new SmellFinding(
                        inspection.getDisplayName(), vf.getPath(), vf.getName(), message));
            }
        }
    }

    private static void scanTypeScriptFile(@NotNull VirtualFile vf, @NotNull IgnoreFileParser ignoreParser,
                                           @NotNull List<SmellFinding> findings) {
        if (isExcludedFile(vf) || ignoreParser.isIgnored(vf)) return;
        String text = readFile(vf);
        if (text == null) return;

        for (AbstractVueSmellInspection inspection : INSPECTIONS) {
            if (inspection.supportsTypeScript()) {
                String message = inspection.analyzeTypeScript(text);
                if (message != null) {
                    findings.add(new SmellFinding(
                            inspection.getDisplayName(), vf.getPath(), vf.getName(), message));
                }
            }
        }
    }

    private static boolean isExcludedFile(@NotNull VirtualFile vf) {
        String name = vf.getName().toLowerCase();
        for (String suffix : EXCLUDED_FILE_SUFFIXES) {
            if (name.endsWith(suffix)) return true;
        }
        String path = vf.getPath().toLowerCase();
        for (String segment : EXCLUDED_DIRECTORY_SEGMENTS) {
            if (path.contains("/" + segment + "/")) return true;
        }
        return false;
    }

    private static String readFile(@NotNull VirtualFile vf) {
        try {
            return new String(vf.contentsToByteArray(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return null;
        }
    }
}