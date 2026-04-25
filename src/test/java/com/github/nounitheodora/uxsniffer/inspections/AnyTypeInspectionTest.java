package com.github.nounitheodora.uxsniffer.inspections;

import org.junit.Test;

import static org.junit.Assert.*;

public class AnyTypeInspectionTest {

    private final AnyTypeInspection inspection = new AnyTypeInspection();

    @Test
    public void testCount_singleAny() {
        String script = "\nconst data = ref<any>(null)\n";
        assertEquals(1, inspection.countAnyUsages(script));
    }

    @Test
    public void testCount_parameterAndReturn() {
        String script = "\nfunction process(input: any): any {\n  return input\n}\n";
        assertEquals(2, inspection.countAnyUsages(script));
    }

    @Test
    public void testCount_noAny() {
        String script = "\nconst label = ref<string>('Hello')\nfunction greet(name: string): string { return name }\n";
        assertEquals(0, inspection.countAnyUsages(script));
    }

    @Test
    public void testCount_commentedOut() {
        String script = "\n// const data: any = null\n";
        assertEquals(0, inspection.countAnyUsages(script));
    }

    @Test
    public void testCount_anyInVariable() {
        String script = "\nlet result: any = fetchData()\n";
        assertEquals(1, inspection.countAnyUsages(script));
    }

    @Test
    public void testCount_anyWithSpaces() {
        String script = "\nconst data:  any = null\n";
        assertEquals(1, inspection.countAnyUsages(script));
    }

    @Test
    public void testCount_notAnySubstring() {
        String script = "\nconst anything = ref(0)\nconst company = 'Anywise'\n";
        assertEquals(0, inspection.countAnyUsages(script));
    }

    @Test
    public void testBuildMessage_single() {
        String msg = inspection.buildMessage(1);
        assertEquals("TypeScript 'any' type used. This disables type checking and may hide errors. Define explicit types instead.", msg);
    }

    @Test
    public void testBuildMessage_multiple() {
        String msg = inspection.buildMessage(3);
        assertEquals("TypeScript 'any' type used 3 times. This disables type checking and may hide errors. Define explicit types instead.", msg);
    }
}
