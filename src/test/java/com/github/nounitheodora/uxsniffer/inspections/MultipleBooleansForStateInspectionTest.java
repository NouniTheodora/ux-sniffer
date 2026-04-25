package com.github.nounitheodora.uxsniffer.inspections;

import org.junit.Test;

import static org.junit.Assert.*;

public class MultipleBooleansForStateInspectionTest {

    private final MultipleBooleansForStateInspection inspection = new MultipleBooleansForStateInspection();

    @Test
    public void testCount_fiveBooleans() {
        String script = "\nconst isLoading = ref(true)\n" +
                "const isRunning = ref(false)\n" +
                "const isPaused = ref(false)\n" +
                "const isFinished = ref(false)\n" +
                "const hasError = ref(false)\n";
        assertEquals(5, inspection.countBooleanRefs(script));
    }

    @Test
    public void testCount_belowThreshold() {
        String script = "\nconst isLoading = ref(true)\nconst isVisible = ref(false)\n";
        assertEquals(2, inspection.countBooleanRefs(script));
    }

    @Test
    public void testCount_nonBooleanRefs() {
        String script = "\nconst count = ref(0)\nconst name = ref('hello')\nconst items = ref([])\n";
        assertEquals(0, inspection.countBooleanRefs(script));
    }

    @Test
    public void testCount_mixedRefs() {
        String script = "\nconst isLoading = ref(true)\nconst count = ref(0)\nconst isOpen = ref(false)\n";
        assertEquals(2, inspection.countBooleanRefs(script));
    }

    @Test
    public void testCount_commentedOut() {
        String script = "\n// const isLoading = ref(true)\n";
        assertEquals(0, inspection.countBooleanRefs(script));
    }

    @Test
    public void testCount_refWithSpaces() {
        String script = "\nconst isOpen = ref( true )\n";
        assertEquals(1, inspection.countBooleanRefs(script));
    }

    @Test
    public void testExceedsThreshold() {
        String script = "\nconst a = ref(true)\nconst b = ref(false)\n" +
                "const c = ref(false)\nconst d = ref(false)\nconst e = ref(false)\n";
        assertTrue(inspection.countBooleanRefs(script) > MultipleBooleansForStateInspection.DEFAULT_BOOLEAN_THRESHOLD);
    }

    @Test
    public void testAtThreshold() {
        String script = "\nconst a = ref(true)\nconst b = ref(false)\n" +
                "const c = ref(false)\nconst d = ref(false)\n";
        assertEquals(MultipleBooleansForStateInspection.DEFAULT_BOOLEAN_THRESHOLD,
                inspection.countBooleanRefs(script));
    }

    @Test
    public void testBuildMessage() {
        String msg = inspection.buildMessage(5);
        assertEquals("Multiple booleans for state: 5 boolean refs defined (threshold: 4). Consider using an enum or union type to manage state transitions instead.", msg);
    }
}
