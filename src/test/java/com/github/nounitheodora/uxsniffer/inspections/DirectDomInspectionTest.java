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
public class DirectDomInspectionTest {

    @RunWith(Parameterized.class)
    public static class SingleApiDetectionTest {

        private final DirectDomInspection inspection = new DirectDomInspection();

        @Parameterized.Parameter
        public String description;

        @Parameterized.Parameter(1)
        public String script;

        @Parameterized.Parameter(2)
        public String expectedApi;

        @Parameterized.Parameters(name = "{0}")
        public static Collection<Object[]> data() {
            return Arrays.asList(new Object[][]{
                    {"getElementById", "\nfunction update() {\n  document.getElementById('title').innerText = 'x'\n}\n", "getElementById"},
                    {"querySelector", "\nconst el = document.querySelector('.item')\n", "querySelector"},
                    {"querySelectorAll", "\nconst items = document.querySelectorAll('li')\n", "querySelectorAll"},
                    {"createElement", "\nconst div = document.createElement('div')\n", "createElement"},
                    {"appendChild", "\nparent.appendChild(child)\n", "appendChild"},
                    {"removeChild", "\nparent.removeChild(child)\n", "removeChild"},
                    {"replaceChild", "\nparent.replaceChild(newEl, oldEl)\n", "replaceChild"},
                    {"setAttribute", "\nel.setAttribute('class', 'active')\n", "setAttribute"},
                    {"innerHTML", "\nel.innerHTML = '<b>bold</b>'\n", "innerHTML"},
                    {"textContent", "\nel.textContent = 'hello'\n", "textContent"},
            });
        }

        @Test
        public void testDetectSingleApi() {
            List<String> found = inspection.detectDomApis(script);
            assertTrue("Expected to find " + expectedApi, found.contains(expectedApi));
        }
    }

    public static class NonParameterized {

        private final DirectDomInspection inspection = new DirectDomInspection();

        @Test
        public void testDetect_getElementById_alsoFindsInnerText() {
            String script = "\nfunction update() {\n  document.getElementById('title').innerText = 'x'\n}\n";
            List<String> found = inspection.detectDomApis(script);
            assertTrue(found.contains("getElementById"));
            assertTrue(found.contains("innerText"));
        }

        @Test
        public void testDetect_noApis() {
            String script = "\nconst name = ref('hello')\nconst double = computed(() => count.value * 2)\n";
            List<String> found = inspection.detectDomApis(script);
            assertTrue(found.isEmpty());
        }

        @Test
        public void testDetect_commentedOutCode() {
            String script = "\n// document.getElementById('x')\n";
            List<String> found = inspection.detectDomApis(script);
            assertTrue(found.isEmpty());
        }

        @Test
        public void testDetect_multipleApis() {
            String script = """
                    \nconst el = document.getElementById('x')
                    const item = document.createElement('li')
                    item.textContent = 'test'
                    el.appendChild(item)
                    """;
            List<String> found = inspection.detectDomApis(script);
            assertEquals(4, found.size());
            assertEquals("getElementById", found.get(0));
            assertEquals("createElement", found.get(1));
            assertEquals("textContent", found.get(2));
            assertEquals("appendChild", found.get(3));
        }

        @Test
        public void testDetect_duplicatesNotRepeated() {
            String script = "\ndocument.getElementById('a')\ndocument.getElementById('b')\n";
            List<String> found = inspection.detectDomApis(script);
            assertEquals(1, found.size());
            assertEquals("getElementById", found.getFirst());
        }

        @Test
        public void testBuildMessage_singleApi() {
            String msg = inspection.buildMessage(List.of("getElementById"));
            assertEquals("Direct DOM manipulation via 'getElementById'. Use framework-provided refs or reactive bindings instead.", msg);
        }

        @Test
        public void testBuildMessage_twoApis() {
            String msg = inspection.buildMessage(List.of("getElementById", "createElement"));
            assertEquals("Direct DOM manipulation via 'getElementById' and 'createElement'. Use framework-provided refs or reactive bindings instead.", msg);
        }

        @Test
        public void testBuildMessage_threeApis() {
            String msg = inspection.buildMessage(List.of("getElementById", "createElement", "appendChild"));
            assertEquals("Direct DOM manipulation via 'getElementById', 'createElement' and 'appendChild'. Use framework-provided refs or reactive bindings instead.", msg);
        }
    }
}
