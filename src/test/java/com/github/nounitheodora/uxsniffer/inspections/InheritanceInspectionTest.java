package com.github.nounitheodora.uxsniffer.inspections;

import org.junit.Test;

import static org.junit.Assert.*;

public class InheritanceInspectionTest {

    private final InheritanceInspection inspection = new InheritanceInspection();

    // --- detectExtends ---

    @Test
    public void testDetect_extendsBasic() {
        String script = "\nimport BaseComponent from './BaseComponent.vue'\n" +
                "export default {\n  extends: BaseComponent,\n}\n";
        assertEquals("BaseComponent", inspection.detectExtends(script));
    }

    @Test
    public void testDetect_extendsWithSpaces() {
        String script = "\nexport default {\n  extends :  MyMixin,\n}\n";
        assertEquals("MyMixin", inspection.detectExtends(script));
    }

    @Test
    public void testDetect_noExtends() {
        String script = "\nexport default {\n  data() { return {} },\n}\n";
        assertNull(inspection.detectExtends(script));
    }

    @Test
    public void testDetect_scriptSetup_noExtends() {
        String script = "\nimport { ref } from 'vue'\nconst count = ref(0)\n";
        assertNull(inspection.detectExtends(script));
    }

    @Test
    public void testDetect_commentedOut() {
        String script = "\n// extends: BaseComponent,\n";
        assertNull(inspection.detectExtends(script));
    }

    @Test
    public void testDetect_blockCommentedOut() {
        String script = "\n/* extends: BaseComponent, */\n";
        assertNull(inspection.detectExtends(script));
    }

    // --- buildMessage ---

    @Test
    public void testBuildMessage() {
        String msg = inspection.buildMessage("BaseComponent");
        assertEquals(
                "Inheritance via 'extends: BaseComponent' detected. Prefer composables or component composition over class-style inheritance.",
                msg);
    }

    @Test
    public void testBuildMessage_differentName() {
        String msg = inspection.buildMessage("MyMixin");
        assertEquals(
                "Inheritance via 'extends: MyMixin' detected. Prefer composables or component composition over class-style inheritance.",
                msg);
    }
}
