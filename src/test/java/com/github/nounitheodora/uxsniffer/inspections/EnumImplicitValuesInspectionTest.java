package com.github.nounitheodora.uxsniffer.inspections;

import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.*;

@RunWith(Enclosed.class)
public class EnumImplicitValuesInspectionTest {

    @RunWith(Parameterized.class)
    public static class AnalyzeTypeScriptTest {

        private final EnumImplicitValuesInspection inspection = new EnumImplicitValuesInspection();

        @Parameterized.Parameter
        public String description;

        @Parameterized.Parameter(1)
        public String input;

        @Parameterized.Parameter(2)
        public boolean expectFinding;

        @Parameterized.Parameter(3)
        public String expectedSubstring;

        @Parameterized.Parameters(name = "{0}")
        public static Collection<Object[]> data() {
            return Arrays.asList(new Object[][]{
                    {"implicit enum", "enum Color {\n  Red,\n  Green,\n  Blue,\n}\n", true, "Color"},
                    {"explicit enum", "enum Color {\n  Red = 'Red',\n  Green = 'Green',\n}\n", false, null},
                    {"no enum", "const x: number = 1\n", false, null},
            });
        }

        @Test
        public void testAnalyzeTypeScript() {
            String result = inspection.analyzeTypeScript(input);
            if (expectFinding) {
                assertNotNull(result);
                assertTrue(result.contains(expectedSubstring));
            } else {
                assertNull(result);
            }
        }
    }

    @RunWith(Parameterized.class)
    public static class DetectNoSmellTest {

        private final EnumImplicitValuesInspection inspection = new EnumImplicitValuesInspection();

        @Parameterized.Parameter
        public String description;

        @Parameterized.Parameter(1)
        public String input;

        @Parameterized.Parameters(name = "{0}")
        public static Collection<Object[]> data() {
            return Arrays.asList(new Object[][]{
                    {"explicit string enum", "\nenum Color {\n  Red = 'Red',\n  Green = 'Green',\n  Blue = 'Blue',\n}\n"},
                    {"explicit number enum", "\nenum Status {\n  Active = 1,\n  Inactive = 0,\n}\n"},
                    {"no enum at all", "\nconst color = ref('red')\n"},
            });
        }

        @Test
        public void testDetect_noSmellFound() {
            List<String> found = inspection.detectImplicitEnums(input);
            assertTrue(found.isEmpty());
        }
    }

    public static class NonParameterizedTest {

        private final EnumImplicitValuesInspection inspection = new EnumImplicitValuesInspection();

        @Test
        public void testDetect_implicitEnum() {
            String script = "\nenum Color {\n  Red,\n  Green,\n  Blue,\n}\n";
            List<String> found = inspection.detectImplicitEnums(script);
            assertEquals(1, found.size());
            assertEquals("Color", found.getFirst());
        }

        @Test
        public void testDetect_multipleEnums() {
            String script = "\nenum Color {\n  Red,\n  Blue,\n}\n\nenum Size {\n  Small,\n  Large,\n}\n";
            List<String> found = inspection.detectImplicitEnums(script);
            assertEquals(2, found.size());
            assertEquals("Color", found.get(0));
            assertEquals("Size", found.get(1));
        }

        @Test
        public void testDetect_mixedExplicitImplicit() {
            String script = "\nenum Color {\n  Red = 'Red',\n  Green = 'Green',\n}\n\nenum Size {\n  Small,\n  Large,\n}\n";
            List<String> found = inspection.detectImplicitEnums(script);
            assertEquals(1, found.size());
            assertEquals("Size", found.getFirst());
        }

        @Test
        public void testHasImplicit_allImplicit() {
            assertTrue(inspection.hasImplicitValues("  Red,\n  Green,\n  Blue,\n"));
        }

        @Test
        public void testHasImplicit_allExplicit() {
            assertFalse(inspection.hasImplicitValues("  Red = 'Red',\n  Green = 'Green',\n"));
        }

        @Test
        public void testSupportsTypeScript() {
            assertTrue(inspection.supportsTypeScript());
        }

        @Test
        public void testBuildMessage_single() {
            String msg = inspection.buildMessage(List.of("Color"));
            assertEquals("Enum 'Color' has implicit values. Assign explicit values to prevent reordering issues.", msg);
        }

        @Test
        public void testBuildMessage_multiple() {
            String msg = inspection.buildMessage(List.of("Color", "Size"));
            assertEquals("Enums 'Color' and 'Size' have implicit values. Assign explicit values to prevent reordering issues.", msg);
        }
    }
}
