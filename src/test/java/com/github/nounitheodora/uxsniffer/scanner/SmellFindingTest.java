package com.github.nounitheodora.uxsniffer.scanner;

import org.junit.Test;

import static org.junit.Assert.*;

public class SmellFindingTest {

    @Test
    public void testRecordAccessors() {
        SmellFinding finding = new SmellFinding("Large file", "/project/src/App.vue", "App.vue", "Too many lines");
        assertEquals("Large file", finding.smellName());
        assertEquals("/project/src/App.vue", finding.filePath());
        assertEquals("App.vue", finding.fileName());
        assertEquals("Too many lines", finding.message());
    }

    @Test
    public void testEquality() {
        SmellFinding a = new SmellFinding("Large file", "/src/A.vue", "A.vue", "msg");
        SmellFinding b = new SmellFinding("Large file", "/src/A.vue", "A.vue", "msg");
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void testInequality_differentSmell() {
        SmellFinding a = new SmellFinding("Large file", "/src/A.vue", "A.vue", "msg");
        SmellFinding b = new SmellFinding("Too many props", "/src/A.vue", "A.vue", "msg");
        assertNotEquals(a, b);
    }

    @Test
    public void testInequality_differentFile() {
        SmellFinding a = new SmellFinding("Large file", "/src/A.vue", "A.vue", "msg");
        SmellFinding b = new SmellFinding("Large file", "/src/B.vue", "B.vue", "msg");
        assertNotEquals(a, b);
    }

    @Test
    public void testInequality_differentMessage() {
        SmellFinding a = new SmellFinding("Large file", "/src/A.vue", "A.vue", "msg1");
        SmellFinding b = new SmellFinding("Large file", "/src/A.vue", "A.vue", "msg2");
        assertNotEquals(a, b);
    }

    @Test
    public void testToString() {
        SmellFinding finding = new SmellFinding("Large file", "/src/A.vue", "A.vue", "msg");
        String str = finding.toString();
        assertTrue(str.contains("Large file"));
        assertTrue(str.contains("A.vue"));
        assertTrue(str.contains("msg"));
    }
}