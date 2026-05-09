package com.github.nounitheodora.uxsniffer.inspections;

import org.junit.Test;

import static org.junit.Assert.*;

public class LargeFileInspectionTest {

    private final LargeFileInspection inspection = new LargeFileInspection();

    // --- countLines ---

    @Test
    public void testCountLines_smallFile() {
        String content = "<template>\n  <div>Hello</div>\n</template>";
        assertEquals(3, inspection.countLines(content));
    }

    @Test
    public void testCountLines_belowThreshold() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < LargeFileInspection.DEFAULT_LOC_THRESHOLD; i++) sb.append("// line\n");
        assertTrue(inspection.countLines(sb.toString()) <= LargeFileInspection.DEFAULT_LOC_THRESHOLD);
    }

    @Test
    public void testCountLines_exceedsThreshold() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < LargeFileInspection.DEFAULT_LOC_THRESHOLD + 5; i++) sb.append("// line\n");
        assertTrue(inspection.countLines(sb.toString()) > LargeFileInspection.DEFAULT_LOC_THRESHOLD);
    }

    // --- countImports ---

    @Test
    public void testCountImports_none() {
        String content = "<template><div>Hello</div></template>\n<script setup>\nconst x = 1\n</script>";
        assertEquals(0, inspection.countImports(content));
    }

    @Test
    public void testCountImports_belowThreshold() {
        String content = "<script setup>\nimport { ref } from 'vue'\nimport MyComp from './MyComp.vue'\n</script>";
        assertEquals(2, inspection.countImports(content));
    }

    @Test
    public void testCountImports_exceedsThreshold() {
        StringBuilder sb = new StringBuilder("<script setup>\n");
        for (int i = 0; i < LargeFileInspection.DEFAULT_IMPORTS_THRESHOLD + 5; i++) {
            sb.append("import Comp").append(i).append(" from './Comp").append(i).append(".vue'\n");
        }
        sb.append("</script>");
        assertTrue(inspection.countImports(sb.toString()) > LargeFileInspection.DEFAULT_IMPORTS_THRESHOLD);
    }

    @Test
    public void testCountImports_ignoresCommentedImports() {
        String content = "// import NotReal from './NotReal.vue'\nimport Real from './Real.vue'\n";
        assertEquals(1, inspection.countImports(content));
    }

    @Test
    public void testCountImports_ignoresImportVariables() {
        String content = "const importedValue = 5;\nlet importHelper = () => {}\nimport Real from './Real.vue'\n";
        assertEquals(1, inspection.countImports(content));
    }

    // --- findFirstImportOffset ---

    @Test
    public void testFindFirstImportOffset_atStart() {
        String content = "import { ref } from 'vue'\nconst x = 1\n";
        assertEquals(0, inspection.findFirstImportOffset(content));
    }

    @Test
    public void testFindFirstImportOffset_afterOtherLines() {
        String content = "<script setup>\nimport { ref } from 'vue'\n";
        int offset = inspection.findFirstImportOffset(content);
        assertTrue("offset should point inside the string", offset > 0 && offset < content.length());
        assertEquals("import", content.substring(offset, offset + "import".length()));
    }

    @Test
    public void testFindFirstImportOffset_noImport() {
        String content = "<template><div>Hello</div></template>\n";
        assertEquals(-1, inspection.findFirstImportOffset(content));
    }

    @Test
    public void testFindFirstImportOffset_ignoresInlineImport() {
        String content = "const x = 1 // not an import\nconst importedThing = 5\n";
        assertEquals(-1, inspection.findFirstImportOffset(content));
    }

    // --- combined message (both violations) ---

    @Test
    public void testBothViolations_bothCountsExceedThresholds() {
        StringBuilder sb = new StringBuilder("<script setup>\n");
        for (int i = 0; i < LargeFileInspection.DEFAULT_IMPORTS_THRESHOLD + 5; i++) {
            sb.append("import Comp").append(i).append(" from './Comp").append(i).append(".vue'\n");
        }
        for (int i = 0; i < LargeFileInspection.DEFAULT_LOC_THRESHOLD; i++) {
            sb.append("const x").append(i).append(" = ").append(i).append("\n");
        }
        sb.append("</script>");

        String content = sb.toString();
        assertTrue("LOC should exceed threshold",
                inspection.countLines(content) > LargeFileInspection.DEFAULT_LOC_THRESHOLD);
        assertTrue("Imports should exceed threshold",
                inspection.countImports(content) > LargeFileInspection.DEFAULT_IMPORTS_THRESHOLD);
    }

    @Test
    public void testBothViolations_combinedMessageContainsBothMetrics() {
        StringBuilder sb = new StringBuilder("<script setup>\n");
        for (int i = 0; i < LargeFileInspection.DEFAULT_IMPORTS_THRESHOLD + 5; i++) {
            sb.append("import Comp").append(i).append(" from './Comp").append(i).append(".vue'\n");
        }
        for (int i = 0; i < LargeFileInspection.DEFAULT_LOC_THRESHOLD; i++) {
            sb.append("const x").append(i).append(" = ").append(i).append("\n");
        }
        sb.append("</script>");

        String content = sb.toString();
        String message = inspection.buildMessage(content,
                inspection.countLines(content) > LargeFileInspection.DEFAULT_LOC_THRESHOLD,
                inspection.countImports(content) > LargeFileInspection.DEFAULT_IMPORTS_THRESHOLD);

        assertTrue("Combined message should mention lines", message.contains("lines"));
        assertTrue("Combined message should mention imports", message.contains("imports"));
    }
}
