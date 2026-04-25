package com.github.nounitheodora.uxsniffer.inspections;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class ForceUpdateInspectionTest {

    private final ForceUpdateInspection inspection = new ForceUpdateInspection();

    // --- detectForceUpdates ---

    @Test
    public void testDetect_forceUpdate() {
        String script = "\nimport { getCurrentInstance } from 'vue'\n" +
                "const instance = getCurrentInstance()\n" +
                "function refresh() {\n" +
                "  instance.proxy.$forceUpdate()\n" +
                "}\n";
        List<String> found = inspection.detectForceUpdates(script);
        assertEquals(1, found.size());
        assertEquals("$forceUpdate()", found.get(0));
    }

    @Test
    public void testDetect_locationReload() {
        String script = "\nfunction hardRefresh() {\n  location.reload()\n}\n";
        List<String> found = inspection.detectForceUpdates(script);
        assertEquals(1, found.size());
        assertEquals("location.reload()", found.get(0));
    }

    @Test
    public void testDetect_windowLocationReload() {
        String script = "\nfunction hardRefresh() {\n  window.location.reload()\n}\n";
        List<String> found = inspection.detectForceUpdates(script);
        assertEquals(1, found.size());
        assertEquals("location.reload()", found.get(0));
    }

    @Test
    public void testDetect_both() {
        String script = "\ninstance.proxy.$forceUpdate()\nlocation.reload()\n";
        List<String> found = inspection.detectForceUpdates(script);
        assertEquals(2, found.size());
        assertEquals("$forceUpdate()", found.get(0));
        assertEquals("location.reload()", found.get(1));
    }

    @Test
    public void testDetect_noViolation() {
        String script = "\nconst count = ref(0)\nconst doubled = computed(() => count.value * 2)\n";
        List<String> found = inspection.detectForceUpdates(script);
        assertTrue(found.isEmpty());
    }

    @Test
    public void testDetect_commentedOutForceUpdate() {
        String script = "\n// instance.proxy.$forceUpdate()\n";
        List<String> found = inspection.detectForceUpdates(script);
        assertTrue(found.isEmpty());
    }

    @Test
    public void testDetect_commentedOutReload() {
        String script = "\n// location.reload()\n";
        List<String> found = inspection.detectForceUpdates(script);
        assertTrue(found.isEmpty());
    }

    @Test
    public void testDetect_duplicatesNotRepeated() {
        String script = "\ninstance.proxy.$forceUpdate()\ninstance.proxy.$forceUpdate()\n";
        List<String> found = inspection.detectForceUpdates(script);
        assertEquals(1, found.size());
    }

    // --- buildMessage ---

    @Test
    public void testBuildMessage_forceUpdateOnly() {
        String msg = inspection.buildMessage(List.of("$forceUpdate()"));
        assertEquals("Avoid '$forceUpdate()'. Redesign state so Vue's reactivity handles updates automatically.", msg);
    }

    @Test
    public void testBuildMessage_reloadOnly() {
        String msg = inspection.buildMessage(List.of("location.reload()"));
        assertEquals("Avoid 'location.reload()'. Use Vue's reactivity or router navigation instead of full page reloads.", msg);
    }

    @Test
    public void testBuildMessage_both() {
        String msg = inspection.buildMessage(List.of("$forceUpdate()", "location.reload()"));
        assertEquals("Avoid '$forceUpdate()' and 'location.reload()'. Redesign state so Vue's reactivity handles updates automatically.", msg);
    }
}
