package com.github.nounitheodora.uxsniffer.inspections;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class DirectDomInspectionTest {

    private final DirectDomInspection inspection = new DirectDomInspection();

    // --- detectDomApis ---

    @Test
    public void testDetect_getElementById() {
        String script = "\nfunction update() {\n  document.getElementById('title').innerText = 'x'\n}\n";
        List<String> found = inspection.detectDomApis(script);
        assertTrue(found.contains("getElementById"));
        assertTrue(found.contains("innerText"));
    }

    @Test
    public void testDetect_querySelector() {
        String script = "\nconst el = document.querySelector('.item')\n";
        List<String> found = inspection.detectDomApis(script);
        assertEquals(1, found.size());
        assertEquals("querySelector", found.get(0));
    }

    @Test
    public void testDetect_querySelectorAll() {
        String script = "\nconst items = document.querySelectorAll('li')\n";
        List<String> found = inspection.detectDomApis(script);
        assertEquals(1, found.size());
        assertEquals("querySelectorAll", found.get(0));
    }

    @Test
    public void testDetect_createElement() {
        String script = "\nconst div = document.createElement('div')\n";
        List<String> found = inspection.detectDomApis(script);
        assertEquals(1, found.size());
        assertEquals("createElement", found.get(0));
    }

    @Test
    public void testDetect_appendChild() {
        String script = "\nparent.appendChild(child)\n";
        List<String> found = inspection.detectDomApis(script);
        assertEquals(1, found.size());
        assertEquals("appendChild", found.get(0));
    }

    @Test
    public void testDetect_removeChild() {
        String script = "\nparent.removeChild(child)\n";
        List<String> found = inspection.detectDomApis(script);
        assertEquals(1, found.size());
        assertEquals("removeChild", found.get(0));
    }

    @Test
    public void testDetect_replaceChild() {
        String script = "\nparent.replaceChild(newEl, oldEl)\n";
        List<String> found = inspection.detectDomApis(script);
        assertEquals(1, found.size());
        assertEquals("replaceChild", found.get(0));
    }

    @Test
    public void testDetect_setAttribute() {
        String script = "\nel.setAttribute('class', 'active')\n";
        List<String> found = inspection.detectDomApis(script);
        assertEquals(1, found.size());
        assertEquals("setAttribute", found.get(0));
    }

    @Test
    public void testDetect_innerHTML() {
        String script = "\nel.innerHTML = '<b>bold</b>'\n";
        List<String> found = inspection.detectDomApis(script);
        assertEquals(1, found.size());
        assertEquals("innerHTML", found.get(0));
    }

    @Test
    public void testDetect_textContent() {
        String script = "\nel.textContent = 'hello'\n";
        List<String> found = inspection.detectDomApis(script);
        assertEquals(1, found.size());
        assertEquals("textContent", found.get(0));
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
        String script = "\nconst el = document.getElementById('x')\n" +
                "const item = document.createElement('li')\n" +
                "item.textContent = 'test'\n" +
                "el.appendChild(item)\n";
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
        assertEquals("getElementById", found.get(0));
    }

    // --- buildMessage ---

    @Test
    public void testBuildMessage_singleApi() {
        String msg = inspection.buildMessage(List.of("getElementById"));
        assertEquals("Direct DOM manipulation via 'getElementById'. Use Vue template refs instead.", msg);
    }

    @Test
    public void testBuildMessage_twoApis() {
        String msg = inspection.buildMessage(List.of("getElementById", "createElement"));
        assertEquals("Direct DOM manipulation via 'getElementById' and 'createElement'. Use Vue template refs instead.", msg);
    }

    @Test
    public void testBuildMessage_threeApis() {
        String msg = inspection.buildMessage(List.of("getElementById", "createElement", "appendChild"));
        assertEquals("Direct DOM manipulation via 'getElementById', 'createElement' and 'appendChild'. Use Vue template refs instead.", msg);
    }
}
