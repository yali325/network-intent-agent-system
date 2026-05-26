package com.yali.mactav.intent.tool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.yali.mactav.intent.tool.IntentExtractTool.IntentExtractionHints;
import org.junit.jupiter.api.Test;

/**
 * Offline tests for the IntentExtractTool deterministic hint boundary.
 *
 * <p>The tool test validates structured output only and does not create a mock
 * tool or invoke a model.</p>
 */
class IntentExtractToolTest {

    private final IntentExtractTool tool = new IntentExtractTool();

    @Test
    void extractIntentHintsShouldReturnBusinessLevelHints() {
        IntentExtractionHints hints = tool.extractIntentHints(
                "Office users can access the server, guest users cannot access the server, "
                        + "office and guest are isolated, use OSPF."
        );

        assertEquals(3, hints.businessObjects().size());
        assertEquals(3, hints.relations().size());
        assertEquals("OSPF", hints.preferences().get(0).value());
    }

    @Test
    void extractIntentHintsShouldIgnoreImplementationDetails() {
        IntentExtractionHints hints = tool.extractIntentHints(
                "Office can access server. Configure interface GigabitEthernet0/0/1 with ip address 10.1.1.1/24."
        );

        String output = hints.toString().toLowerCase();
        assertFalse(output.contains("10.1.1.1"));
        assertFalse(output.contains("gigabitethernet0/0/1"));
        assertTrue(hints.warnings().contains("Implementation details were ignored because IntentAgent only keeps business intent."));
    }
}
