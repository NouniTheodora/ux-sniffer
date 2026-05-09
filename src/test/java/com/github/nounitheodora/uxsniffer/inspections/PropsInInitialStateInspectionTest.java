package com.github.nounitheodora.uxsniffer.inspections;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class PropsInInitialStateInspectionTest {

    private final PropsInInitialStateInspection inspection = new PropsInInitialStateInspection();

    // --- detectPropsInState ---

    @Test
    public void testDetect_refWithProp() {
        String script = "\nconst props = defineProps({ initialName: String })\n" +
                "const name = ref(props.initialName)\n";
        List<String> found = inspection.detectPropsInState(script);
        assertEquals(1, found.size());
        assertEquals("initialName", found.getFirst());
    }

    @Test
    public void testDetect_refWithPropSpaces() {
        String script = "\nconst name = ref( props.title )\n";
        List<String> found = inspection.detectPropsInState(script);
        assertEquals(1, found.size());
        assertEquals("title", found.getFirst());
    }

    @Test
    public void testDetect_multipleProps() {
        String script = "\nconst name = ref(props.initialName)\n" +
                "const count = ref(props.initialCount)\n";
        List<String> found = inspection.detectPropsInState(script);
        assertEquals(2, found.size());
        assertEquals("initialName", found.get(0));
        assertEquals("initialCount", found.get(1));
    }

    @Test
    public void testDetect_reactiveWithProp() {
        String script = "\nconst state = reactive({ name: props.userName })\n";
        List<String> found = inspection.detectPropsInState(script);
        assertEquals(1, found.size());
        assertEquals("userName", found.getFirst());
    }

    @Test
    public void testDetect_noViolation_computed() {
        String script = "\nconst displayName = computed(() => props.initialName)\n";
        List<String> found = inspection.detectPropsInState(script);
        assertTrue(found.isEmpty());
    }

    @Test
    public void testDetect_noViolation_noProps() {
        String script = "\nconst count = ref(0)\nconst name = ref('hello')\n";
        List<String> found = inspection.detectPropsInState(script);
        assertTrue(found.isEmpty());
    }

    @Test
    public void testDetect_commentedOut() {
        String script = "\n// const name = ref(props.initialName)\n";
        List<String> found = inspection.detectPropsInState(script);
        assertTrue(found.isEmpty());
    }

    @Test
    public void testDetect_duplicatesNotRepeated() {
        String script = "\nconst a = ref(props.name)\nconst b = ref(props.name)\n";
        List<String> found = inspection.detectPropsInState(script);
        assertEquals(1, found.size());
    }

    // --- buildMessage ---

    @Test
    public void testBuildMessage_singleProp() {
        String msg = inspection.buildMessage(List.of("initialName"));
        assertEquals("Prop 'initialName' used to initialise reactive state. Use computed(() => props.initialName) to stay in sync.", msg);
    }

    @Test
    public void testBuildMessage_twoProps() {
        String msg = inspection.buildMessage(List.of("initialName", "initialCount"));
        assertEquals("Props 'initialName' and 'initialCount' used to initialise reactive state. Use computed() to stay in sync with prop changes.", msg);
    }

    @Test
    public void testBuildMessage_threeProps() {
        String msg = inspection.buildMessage(List.of("name", "count", "active"));
        assertEquals("Props 'name', 'count' and 'active' used to initialise reactive state. Use computed() to stay in sync with prop changes.", msg);
    }
}
