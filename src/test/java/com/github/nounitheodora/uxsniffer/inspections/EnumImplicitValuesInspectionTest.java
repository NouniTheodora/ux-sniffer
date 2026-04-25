package com.github.nounitheodora.uxsniffer.inspections;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class EnumImplicitValuesInspectionTest {

    private final EnumImplicitValuesInspection inspection = new EnumImplicitValuesInspection();

    @Test
    public void testDetect_implicitEnum() {
        String script = "\nenum Color {\n  Red,\n  Green,\n  Blue,\n}\n";
        List<String> found = inspection.detectImplicitEnums(script);
        assertEquals(1, found.size());
        assertEquals("Color", found.get(0));
    }

    @Test
    public void testDetect_explicitStringEnum() {
        String script = "\nenum Color {\n  Red = 'Red',\n  Green = 'Green',\n  Blue = 'Blue',\n}\n";
        List<String> found = inspection.detectImplicitEnums(script);
        assertTrue(found.isEmpty());
    }

    @Test
    public void testDetect_explicitNumberEnum() {
        String script = "\nenum Status {\n  Active = 1,\n  Inactive = 0,\n}\n";
        List<String> found = inspection.detectImplicitEnums(script);
        assertTrue(found.isEmpty());
    }

    @Test
    public void testDetect_noEnum() {
        String script = "\nconst color = ref('red')\n";
        List<String> found = inspection.detectImplicitEnums(script);
        assertTrue(found.isEmpty());
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
        assertEquals("Size", found.get(0));
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
