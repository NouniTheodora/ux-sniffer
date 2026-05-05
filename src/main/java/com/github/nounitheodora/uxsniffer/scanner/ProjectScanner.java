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
            if (ignoreParser.isIgnored(vf)) continue;
            String text = readFile(vf);
            if (text == null) continue;

            for (AbstractVueSmellInspection inspection : INSPECTIONS) {
                String message = inspection.analyze(text);
                if (message != null) {
                    findings.add(new SmellFinding(
                            inspection.getDisplayName(), vf.getPath(), vf.getName(), message));
                }
            }
        }

        Collection<VirtualFile> tsFiles = FilenameIndex.getAllFilesByExt(project, "ts", scope);
        for (VirtualFile vf : tsFiles) {
            if (ignoreParser.isIgnored(vf)) continue;
            String text = readFile(vf);
            if (text == null) continue;

            for (AbstractVueSmellInspection inspection : INSPECTIONS) {
                if (!inspection.supportsTypeScript()) continue;
                String message = inspection.analyzeTypeScript(text);
                if (message != null) {
                    findings.add(new SmellFinding(
                            inspection.getDisplayName(), vf.getPath(), vf.getName(), message));
                }
            }
        }

        return findings;
    }

    private static String readFile(@NotNull VirtualFile vf) {
        try {
            return new String(vf.contentsToByteArray(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return null;
        }
    }
}