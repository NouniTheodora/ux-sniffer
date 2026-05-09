package com.github.nounitheodora.uxsniffer.inspections;

import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.*;

@RunWith(Enclosed.class)
public class ForceUpdateInspectionTest {

    @RunWith(Parameterized.class)
    public static class DetectForceUpdates {

        private final ForceUpdateInspection inspection = new ForceUpdateInspection();

        @Parameterized.Parameter(0)
        public String description;

        @Parameterized.Parameter(1)
        public String script;

        @Parameterized.Parameter(2)
        public String expectedItem;

        @Parameterized.Parameters(name = "{0}")
        public static Collection<Object[]> data() {
            return Arrays.asList(new Object[][]{
                    {"$forceUpdate()", """
                            \nimport { getCurrentInstance } from 'vue'
                            const instance = getCurrentInstance()
                            function refresh() {
                              instance.proxy.$forceUpdate()
                            }
                            """, "$forceUpdate()"},
                    {"location.reload()", "\nfunction hardRefresh() {\n  location.reload()\n}\n", "location.reload()"},
                    {"window.location.reload()", "\nfunction hardRefresh() {\n  window.location.reload()\n}\n", "location.reload()"},
            });
        }

        @Test
        public void testDetectForceUpdate() {
            List<String> found = inspection.detectForceUpdates(script);
            assertEquals(1, found.size());
            assertEquals(expectedItem, found.getFirst());
        }
    }

    @RunWith(Parameterized.class)
    public static class DetectNoViolations {

        private final ForceUpdateInspection inspection = new ForceUpdateInspection();

        @Parameterized.Parameter(0)
        public String description;

        @Parameterized.Parameter(1)
        public String script;

        @Parameterized.Parameters(name = "{0}")
        public static Collection<Object[]> data() {
            return Arrays.asList(new Object[][]{
                    {"clean reactive code", "\nconst count = ref(0)\nconst doubled = computed(() => count.value * 2)\n"},
                    {"commented out $forceUpdate", "\n// instance.proxy.$forceUpdate()\n"},
                    {"commented out location.reload", "\n// location.reload()\n"},
            });
        }

        @Test
        public void testDetectNoViolation() {
            List<String> found = inspection.detectForceUpdates(script);
            assertTrue(found.isEmpty());
        }
    }

    public static class NonParameterized {

        private final ForceUpdateInspection inspection = new ForceUpdateInspection();

        @Test
        public void testDetect_both() {
            String script = "\ninstance.proxy.$forceUpdate()\nlocation.reload()\n";
            List<String> found = inspection.detectForceUpdates(script);
            assertEquals(2, found.size());
            assertEquals("$forceUpdate()", found.get(0));
            assertEquals("location.reload()", found.get(1));
        }

        @Test
        public void testDetect_duplicatesNotRepeated() {
            String script = "\ninstance.proxy.$forceUpdate()\ninstance.proxy.$forceUpdate()\n";
            List<String> found = inspection.detectForceUpdates(script);
            assertEquals(1, found.size());
        }

        @Test
        public void testBuildMessage_forceUpdateOnly() {
            String msg = inspection.buildMessage(List.of("$forceUpdate()"));
            assertEquals("Avoid '$forceUpdate()'. Redesign state so the framework's reactivity handles updates automatically.", msg);
        }

        @Test
        public void testBuildMessage_reloadOnly() {
            String msg = inspection.buildMessage(List.of("location.reload()"));
            assertEquals("Avoid 'location.reload()'. Use the framework's reactivity or programmatic navigation instead of full page reloads.", msg);
        }

        @Test
        public void testBuildMessage_both() {
            String msg = inspection.buildMessage(List.of("$forceUpdate()", "location.reload()"));
            assertEquals("Avoid '$forceUpdate()' and 'location.reload()'. Redesign state so the framework's reactivity handles updates automatically.", msg);
        }
    }
}