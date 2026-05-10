package com.github.nounitheodora.uxsniffer.inspections;

import org.junit.Test;

import static org.junit.Assert.*;

public class NonNullAssertionInspectionTest {

    private final NonNullAssertionInspection inspection = new NonNullAssertionInspection();

    @Test
    public void testCount_singleAssertion() {
        String script = "\nconst value = found!.value\n";
        assertEquals(1, inspection.countNonNullAssertions(script));
    }

    @Test
    public void testCount_multipleAssertions() {
        String script = "\nconst a = obj!.name\nconst b = item!.value\n";
        assertEquals(2, inspection.countNonNullAssertions(script));
    }

    @Test
    public void testCount_noAssertions() {
        String script = "\nconst value = item?.value ?? 'default'\n";
        assertEquals(0, inspection.countNonNullAssertions(script));
    }

    @Test
    public void testCount_notEqualOperator() {
        String script = "\nif (a !== b && c != d) { }\n";
        assertEquals(0, inspection.countNonNullAssertions(script));
    }

    @Test
    public void testCount_logicalNot() {
        String script = "\nconst visible = !isHidden\n";
        assertEquals(0, inspection.countNonNullAssertions(script));
    }

    @Test
    public void testCount_commentedOut() {
        String script = "\n// const value = found!.value\n";
        assertEquals(0, inspection.countNonNullAssertions(script));
    }

    @Test
    public void testSupportsTypeScript() {
        assertTrue(inspection.supportsTypeScript());
    }

    @Test
    public void testAnalyzeTypeScript_withAssertions() {
        String ts = "const value = found!.name\nconst other = item!.value\n";
        String result = inspection.analyzeTypeScript(ts);
        assertNotNull(result);
        assertTrue(result.contains("2"));
    }

    @Test
    public void testAnalyzeTypeScript_noAssertions() {
        String ts = "const value = item?.name ?? 'default'\n";
        assertNull(inspection.analyzeTypeScript(ts));
    }

    @Test
    public void testBuildMessage_single() {
        String msg = inspection.buildMessage(1);
        assertEquals("Non-null assertion operator ('!') used. This may hide null/undefined errors at runtime. Use proper null checks instead.", msg);
    }

    @Test
    public void testBuildMessage_multiple() {
        String msg = inspection.buildMessage(3);
        assertEquals("Non-null assertion operator ('!') used 3 times. This may hide null/undefined errors at runtime. Use proper null checks instead.", msg);
    }
}
