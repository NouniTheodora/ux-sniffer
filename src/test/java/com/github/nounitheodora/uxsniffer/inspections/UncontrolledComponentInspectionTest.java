package com.github.nounitheodora.uxsniffer.inspections;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class UncontrolledComponentInspectionTest {

    private final UncontrolledComponentInspection inspection = new UncontrolledComponentInspection();

    // --- extractTemplateContent ---

    @Test
    public void testExtractTemplate_basic() {
        String text = "<template>\n  <div>hello</div>\n</template>\n<script setup>\n</script>";
        assertEquals("\n  <div>hello</div>\n", inspection.extractTemplateContent(text));
    }

    @Test
    public void testExtractTemplate_noTemplate() {
        assertEquals("", inspection.extractTemplateContent("<script setup>\nconst x = 1\n</script>"));
    }

    // --- hasRef ---

    @Test
    public void testHasRef_present() {
        assertTrue(inspection.hasRef(" ref=\"nameInput\" type=\"text\""));
    }

    @Test
    public void testHasRef_absent() {
        assertFalse(inspection.hasRef(" type=\"text\" class=\"form-input\""));
    }

    // --- hasValueBinding ---

    @Test
    public void testHasValueBinding_vModel() {
        assertTrue(inspection.hasValueBinding(" v-model=\"name\" type=\"text\""));
    }

    @Test
    public void testHasValueBinding_colonValue() {
        assertTrue(inspection.hasValueBinding(" :value=\"name\" type=\"text\""));
    }

    @Test
    public void testHasValueBinding_vBindValue() {
        assertTrue(inspection.hasValueBinding(" v-bind:value=\"name\" type=\"text\""));
    }

    @Test
    public void testHasValueBinding_none() {
        assertFalse(inspection.hasValueBinding(" ref=\"nameInput\" type=\"text\""));
    }

    // --- detectUncontrolled ---

    @Test
    public void testDetect_uncontrolledInput() {
        String template = "\n  <input ref=\"nameInput\" type=\"text\" />\n";
        List<String> found = inspection.detectUncontrolled(template);
        assertEquals(1, found.size());
        assertEquals("input", found.get(0));
    }

    @Test
    public void testDetect_uncontrolledTextarea() {
        String template = "\n  <textarea ref=\"msgInput\"></textarea>\n";
        List<String> found = inspection.detectUncontrolled(template);
        assertEquals(1, found.size());
        assertEquals("textarea", found.get(0));
    }

    @Test
    public void testDetect_uncontrolledSelect() {
        String template = "\n  <select ref=\"roleSelect\">\n    <option>Admin</option>\n  </select>\n";
        List<String> found = inspection.detectUncontrolled(template);
        assertEquals(1, found.size());
        assertEquals("select", found.get(0));
    }

    @Test
    public void testDetect_controlledWithVModel() {
        String template = "\n  <input ref=\"nameInput\" v-model=\"name\" type=\"text\" />\n";
        List<String> found = inspection.detectUncontrolled(template);
        assertTrue(found.isEmpty());
    }

    @Test
    public void testDetect_controlledWithColonValue() {
        String template = "\n  <input ref=\"nameInput\" :value=\"name\" type=\"text\" />\n";
        List<String> found = inspection.detectUncontrolled(template);
        assertTrue(found.isEmpty());
    }

    @Test
    public void testDetect_inputWithoutRef() {
        String template = "\n  <input type=\"text\" />\n";
        List<String> found = inspection.detectUncontrolled(template);
        assertTrue(found.isEmpty());
    }

    @Test
    public void testDetect_multiple() {
        String template = "\n  <input ref=\"a\" type=\"text\" />\n  <textarea ref=\"b\"></textarea>\n";
        List<String> found = inspection.detectUncontrolled(template);
        assertEquals(2, found.size());
        assertEquals("input", found.get(0));
        assertEquals("textarea", found.get(1));
    }

    @Test
    public void testDetect_mixedControlledAndUncontrolled() {
        String template = "\n  <input ref=\"a\" type=\"text\" />\n  <input ref=\"b\" v-model=\"name\" />\n";
        List<String> found = inspection.detectUncontrolled(template);
        assertEquals(1, found.size());
        assertEquals("input", found.get(0));
    }

    // --- buildMessage ---

    @Test
    public void testBuildMessage_single() {
        String msg = inspection.buildMessage(List.of("input"));
        assertEquals("Uncontrolled <input>: uses a ref but has no v-model or :value binding. Bind the value reactively instead.", msg);
    }

    @Test
    public void testBuildMessage_singleTextarea() {
        String msg = inspection.buildMessage(List.of("textarea"));
        assertEquals("Uncontrolled <textarea>: uses a ref but has no v-model or :value binding. Bind the value reactively instead.", msg);
    }

    @Test
    public void testBuildMessage_multiple() {
        String msg = inspection.buildMessage(List.of("input", "textarea"));
        assertEquals("2 uncontrolled form elements use a ref but have no v-model or :value binding. Bind values reactively instead.", msg);
    }
}
