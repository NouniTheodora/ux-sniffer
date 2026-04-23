package com.github.nounitheodora.uxsniffer.inspections;

import org.junit.Test;

import static org.junit.Assert.*;

public class TooManyPropsInspectionTest {

    private final TooManyPropsInspection inspection = new TooManyPropsInspection();

    // --- countObjectProps ---

    @Test
    public void testCountObjectProps_simpleTypes() {
        String block = "\n  title: String,\n  count: Number,\n  active: Boolean\n";
        assertEquals(3, inspection.countObjectProps(block));
    }

    @Test
    public void testCountObjectProps_optionalProps() {
        String block = "\n  title?: String,\n  count?: Number\n";
        assertEquals(2, inspection.countObjectProps(block));
    }

    @Test
    public void testCountObjectProps_complexNestedType() {
        // nested object type should NOT add extra counts
        String block =
                "\n  title: String,\n" +
                "  config: {\n" +
                "    type: Object,\n" +
                "    required: true\n" +
                "  },\n" +
                "  active: Boolean\n";
        assertEquals(3, inspection.countObjectProps(block));
    }

    @Test
    public void testCountObjectProps_belowThreshold() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < TooManyPropsInspection.DEFAULT_PROPS_THRESHOLD; i++) {
            sb.append("  prop").append(i).append(": String,\n");
        }
        assertEquals(TooManyPropsInspection.DEFAULT_PROPS_THRESHOLD, inspection.countObjectProps(sb.toString()));
    }

    @Test
    public void testCountObjectProps_exceedsThreshold() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < TooManyPropsInspection.DEFAULT_PROPS_THRESHOLD + 2; i++) {
            sb.append("  prop").append(i).append(": String,\n");
        }
        assertTrue(inspection.countObjectProps(sb.toString()) > TooManyPropsInspection.DEFAULT_PROPS_THRESHOLD);
    }

    // --- countArrayItems ---

    @Test
    public void testCountArrayItems_basic() {
        String block = "'title', 'count', 'active'";
        assertEquals(3, inspection.countArrayItems(block));
    }

    @Test
    public void testCountArrayItems_empty() {
        assertEquals(0, inspection.countArrayItems("  "));
    }

    @Test
    public void testCountArrayItems_exceedsThreshold() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < TooManyPropsInspection.DEFAULT_PROPS_THRESHOLD + 2; i++) {
            if (i > 0) sb.append(", ");
            sb.append("'prop").append(i).append("'");
        }
        assertTrue(inspection.countArrayItems(sb.toString()) > TooManyPropsInspection.DEFAULT_PROPS_THRESHOLD);
    }

    // --- countProps (full script) ---

    @Test
    public void testCountProps_objectSyntax() {
        String script = "\ndefineProps({\n  title: String,\n  count: Number\n})\n";
        assertEquals(2, inspection.countProps(script));
    }

    @Test
    public void testCountProps_arraySyntax() {
        String script = "\ndefineProps(['title', 'count', 'active'])\n";
        assertEquals(3, inspection.countProps(script));
    }

    @Test
    public void testCountProps_typeScriptGeneric() {
        String script = "\ndefineProps<{\n  title: string\n  count?: number\n  active: boolean\n}>()\n";
        assertEquals(3, inspection.countProps(script));
    }

    @Test
    public void testCountProps_noDefineProps() {
        String script = "\nconst x = ref(0)\n";
        assertEquals(0, inspection.countProps(script));
    }

    @Test
    public void testCountProps_exceedsThreshold() {
        StringBuilder sb = new StringBuilder("defineProps({\n");
        for (int i = 0; i < TooManyPropsInspection.DEFAULT_PROPS_THRESHOLD + 2; i++) {
            sb.append("  prop").append(i).append(": String,\n");
        }
        sb.append("})\n");
        assertTrue(inspection.countProps(sb.toString()) > TooManyPropsInspection.DEFAULT_PROPS_THRESHOLD);
    }
}
