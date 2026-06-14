package com.yali.mactav.orchestrator.remote.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for temporary stdout redaction around SAA A2A invocation.
 */
class SafeA2aStdoutGuardTest {

    @Test
    void redactingPrintStreamShouldRedactSensitiveJsonAndPassThroughNormalLines() throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        PrintStream capture = new PrintStream(bytes, true, StandardCharsets.UTF_8);
        RedactingPrintStream redacting = new RedactingPrintStream(capture);

        redacting.println("plain startup line");
        redacting.print("{\"jsonrpc\":\"2.0\",\"method\":\"message/send\",\"params\":{\"payloadJson\":\"secret\"}}");
        redacting.println();
        redacting.println((Object) "{\"workspaceSnapshot\":\"secret\",\"rawText\":\"secret\"}");
        redacting.print((Object) "http://127.0.0.1:18082/a2a");

        String output = bytes.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("plain startup line"));
        assertTrue(output.contains("[SAA_A2A_REQUEST_REDACTED]"));
        assertTrue(output.contains("[SAA_A2A_REQUEST_URL_REDACTED]"));
        assertEquals(-1, output.indexOf("payloadJson"));
        assertEquals(-1, output.indexOf("workspaceSnapshot"));
        assertEquals(-1, output.indexOf("rawText"));
        assertEquals(-1, output.indexOf("message/send"));
        assertEquals(-1, output.indexOf("http://127.0.0.1:18082/a2a"));
    }

    @Test
    void guardShouldRestoreSystemOutWhenActionThrows() {
        PrintStream original = System.out;

        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> SafeA2aStdoutGuard.call(() -> {
                    throw new IllegalStateException("boom");
                }));

        assertEquals("boom", error.getMessage());
        assertSame(original, System.out);
    }
}
