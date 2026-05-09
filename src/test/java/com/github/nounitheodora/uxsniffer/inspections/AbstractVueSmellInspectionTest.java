package com.github.nounitheodora.uxsniffer.inspections;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.*;

@RunWith(Enclosed.class)
public class AbstractVueSmellInspectionTest {

    @RunWith(Parameterized.class)
    public static class IsNotTypeScriptSetup {

        private final TestInspection inspection = new TestInspection();

        @Parameterized.Parameter(0)
        public String description;

        @Parameterized.Parameter(1)
        public String vue;

        @Parameterized.Parameter(2)
        public boolean expected;

        @Parameterized.Parameters(name = "{0}")
        public static Collection<Object[]> data() {
            return Arrays.asList(new Object[][]{
                    {"ts setup", "<script setup lang=\"ts\">\nconst x = 1\n</script>", false},
                    {"typescript setup", "<script setup lang=\"typescript\">\nconst x = 1\n</script>", false},
                    {"js setup (no lang)", "<script setup>\nconst x = 1\n</script>", true},
                    {"ts without setup", "<script lang=\"ts\">\nexport default {}\n</script>", true},
                    {"no script tag", "<template><div></div></template>", true},
            });
        }

        @Test
        public void testIsNotTypeScriptSetup() {
            assertEquals(expected, inspection.isNotTypeScriptSetup(vue));
        }
    }

    public static class NonParameterized {

        private final TestInspection inspection = new TestInspection();

        // --- extractScriptContent ---

        @Test
        public void testExtractScript_setupBlock() {
            String vue = "<template><div></div></template>\n<script setup>\nconst x = 1\n</script>";
            assertEquals("\nconst x = 1\n", inspection.extractScriptContent(vue));
        }

        @Test
        public void testExtractScript_regularScript() {
            String vue = "<script>\nexport default {}\n</script>";
            assertEquals("\nexport default {}\n", inspection.extractScriptContent(vue));
        }

        @Test
        public void testExtractScript_noScript() {
            String vue = "<template><div>Hello</div></template>";
            assertEquals("", inspection.extractScriptContent(vue));
        }

        @Test
        public void testExtractScript_unclosedScript() {
            String vue = "<script setup>\nconst x = 1";
            assertEquals("", inspection.extractScriptContent(vue));
        }

        @Test
        public void testExtractScript_emptyScript() {
            String vue = "<script setup></script>";
            assertEquals("", inspection.extractScriptContent(vue));
        }

        @Test
        public void testExtractScript_withLangTs() {
            String vue = "<script setup lang=\"ts\">\nconst x: number = 1\n</script>";
            assertEquals("\nconst x: number = 1\n", inspection.extractScriptContent(vue));
        }

        // --- countLines ---

        @Test
        public void testCountLines_singleLine() {
            assertEquals(1, inspection.countLines("hello"));
        }

        @Test
        public void testCountLines_multipleLines() {
            assertEquals(3, inspection.countLines("a\nb\nc"));
        }

        @Test
        public void testCountLines_trailingNewline() {
            assertEquals(3, inspection.countLines("a\nb\nc\n"));
        }

        @Test
        public void testCountLines_emptyString() {
            assertEquals(0, inspection.countLines(""));
        }

        @Test
        public void testCountLines_onlyNewlines() {
            assertEquals(3, inspection.countLines("\n\n\n"));
        }

        // --- extractBlock ---

        @Test
        public void testExtractBlock_simpleBraces() {
            String text = "{ a, b, c }";
            assertEquals(" a, b, c ", inspection.extractBlock(text, 0, '{', '}'));
        }

        @Test
        public void testExtractBlock_nestedBraces() {
            String text = "{ a: { nested: true }, b }";
            assertEquals(" a: { nested: true }, b ", inspection.extractBlock(text, 0, '{', '}'));
        }

        @Test
        public void testExtractBlock_brackets() {
            String text = "['a', 'b', 'c']";
            assertEquals("'a', 'b', 'c'", inspection.extractBlock(text, 0, '[', ']'));
        }

        @Test
        public void testExtractBlock_startNotAtOpen() {
            String text = "hello { world }";
            assertNull(inspection.extractBlock(text, 0, '{', '}'));
        }

        @Test
        public void testExtractBlock_unclosed() {
            String text = "{ a, b";
            assertNull(inspection.extractBlock(text, 0, '{', '}'));
        }

        @Test
        public void testExtractBlock_outOfBounds() {
            String text = "abc";
            assertNull(inspection.extractBlock(text, 10, '{', '}'));
        }

        @Test
        public void testExtractBlock_empty() {
            String text = "{}";
            assertEquals("", inspection.extractBlock(text, 0, '{', '}'));
        }

        // --- getGroupDisplayName / isEnabledByDefault ---

        @Test
        public void testGroupDisplayName() {
            assertEquals("Vue.js UX Smells", inspection.getGroupDisplayName());
        }

        @Test
        public void testEnabledByDefault() {
            assertTrue(inspection.isEnabledByDefault());
        }

        // --- analyze defaults ---

        @Test
        public void testAnalyze_defaultReturnsNull() {
            assertNull(inspection.analyze("any content"));
        }

        @Test
        public void testAnalyzeTypeScript_defaultReturnsNull() {
            assertNull(inspection.analyzeTypeScript("any content"));
        }

        @Test
        public void testSupportsTypeScript_defaultFalse() {
            assertFalse(inspection.supportsTypeScript());
        }
    }

    private static class TestInspection extends AbstractVueSmellInspection {
        @Override
        public @NotNull String getDisplayName() {
            return "Test inspection";
        }
    }
}