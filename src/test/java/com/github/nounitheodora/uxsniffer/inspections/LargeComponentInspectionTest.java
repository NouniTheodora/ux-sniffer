package com.github.nounitheodora.uxsniffer.inspections;

import org.junit.Test;

import static org.junit.Assert.*;

public class LargeComponentInspectionTest {

    private final LargeComponentInspection inspection = new LargeComponentInspection();

    // --- extractScriptContent ---

    @Test
    public void testExtractScriptContent_scriptSetup() {
        String content = "<template><div>Hello</div></template>\n<script setup>\nimport { ref } from 'vue'\nconst x = ref(0)\n</script>";
        String script = inspection.extractScriptContent(content);
        assertTrue(script.contains("import { ref }"));
        assertTrue(script.contains("const x = ref(0)"));
    }

    @Test
    public void testExtractScriptContent_noScript() {
        String content = "<template><div>Hello</div></template>";
        assertEquals("", inspection.extractScriptContent(content));
    }

    @Test
    public void testExtractScriptContent_doesNotIncludeTag() {
        String content = "<script setup>\nconst x = 1\n</script>";
        String script = inspection.extractScriptContent(content);
        assertFalse(script.contains("<script"));
        assertFalse(script.contains("</script>"));
    }

    // --- countFunctions ---

    @Test
    public void testCountFunctions_standardFunction() {
        String script = "\nfunction handleClick() {\n  console.log('click')\n}\n";
        assertEquals(1, inspection.countFunctions(script));
    }

    @Test
    public void testCountFunctions_asyncFunction() {
        String script = "\nasync function fetchData() {\n  return await api.get()\n}\n";
        assertEquals(1, inspection.countFunctions(script));
    }

    @Test
    public void testCountFunctions_arrowFunction() {
        String script = "\nconst handleClick = () => {\n  console.log('click')\n}\n";
        assertEquals(1, inspection.countFunctions(script));
    }

    @Test
    public void testCountFunctions_asyncArrowFunction() {
        String script = "\nconst fetchData = async () => {\n  return await api.get()\n}\n";
        assertEquals(1, inspection.countFunctions(script));
    }

    @Test
    public void testCountFunctions_atThreshold() {
        String script = "function a() {}\nfunction b() {}\nfunction c() {}\nfunction d() {}\n";
        assertEquals(LargeComponentInspection.DEFAULT_FUNCTIONS_THRESHOLD, inspection.countFunctions(script));
        assertFalse(inspection.countFunctions(script) > LargeComponentInspection.DEFAULT_FUNCTIONS_THRESHOLD);
    }

    @Test
    public void testCountFunctions_exceedsThreshold() {
        String script = "function a() {}\nfunction b() {}\nfunction c() {}\nfunction d() {}\nfunction e() {}\n";
        assertTrue(inspection.countFunctions(script) > LargeComponentInspection.DEFAULT_FUNCTIONS_THRESHOLD);
    }

    @Test
    public void testCountFunctions_ignoresComputedCallbacks() {
        String script = "\nconst doubled = computed(() => {\n  return count.value * 2\n})\n";
        assertEquals(0, inspection.countFunctions(script));
    }

    @Test
    public void testCountFunctions_ignoresWatchCallbacks() {
        String script = "\nconst stop = watch(source, () => {\n  doSomething()\n})\n";
        assertEquals(0, inspection.countFunctions(script));
    }

    @Test
    public void testCountFunctions_ignoresRefAndReactive() {
        String script = "\nconst count = ref(0)\nconst state = reactive({ name: '' })\n";
        assertEquals(0, inspection.countFunctions(script));
    }

    @Test
    public void testCountFunctions_mixedPatterns() {
        String script =
                "import { ref, computed } from 'vue'\n" +
                "const count = ref(0)\n" +
                "const doubled = computed(() => count.value * 2)\n" +
                "function handleClick() { count.value++ }\n" +
                "const fetchData = async () => {\n  return await api.get()\n}\n";
        assertEquals(2, inspection.countFunctions(script));
    }

    // --- isArrowFunctionAssignment ---

    @Test
    public void testIsArrowFunctionAssignment_simpleArrow() {
        assertTrue(inspection.isArrowFunctionAssignment("const fn = () => {"));
    }

    @Test
    public void testIsArrowFunctionAssignment_arrowWithParams() {
        assertTrue(inspection.isArrowFunctionAssignment("const fn = (a, b) => {"));
    }

    @Test
    public void testIsArrowFunctionAssignment_notAnAssignment() {
        assertFalse(inspection.isArrowFunctionAssignment("function fn() {"));
    }

    @Test
    public void testIsArrowFunctionAssignment_computed() {
        assertFalse(inspection.isArrowFunctionAssignment("const x = computed(() => {"));
    }

    // --- countLines (script block) ---

    @Test
    public void testCountLines_exceedsThreshold() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < LargeComponentInspection.DEFAULT_SCRIPT_LOC_THRESHOLD + 5; i++) {
            sb.append("const x").append(i).append(" = ").append(i).append("\n");
        }
        assertTrue(inspection.countLines(sb.toString()) > LargeComponentInspection.DEFAULT_SCRIPT_LOC_THRESHOLD);
    }

    // --- buildMessage ---

    @Test
    public void testBuildMessage_locOnly() {
        String msg = inspection.buildMessage("", true, false);
        assertTrue(msg.contains("lines"));
        assertFalse(msg.contains("functions"));
    }

    @Test
    public void testBuildMessage_functionsOnly() {
        String msg = inspection.buildMessage("", false, true);
        assertTrue(msg.contains("functions"));
        assertFalse(msg.contains("lines"));
    }

    @Test
    public void testBuildMessage_both() {
        String script = "function a() {}\n";
        String msg = inspection.buildMessage(script, true, true);
        assertTrue(msg.contains("lines"));
        assertTrue(msg.contains("functions"));
    }
}
