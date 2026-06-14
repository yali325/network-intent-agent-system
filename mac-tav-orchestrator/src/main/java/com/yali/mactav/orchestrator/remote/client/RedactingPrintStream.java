package com.yali.mactav.orchestrator.remote.client;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * PrintStream wrapper that redacts known SAA A2A request leaks from stdout.
 */
final class RedactingPrintStream extends PrintStream {

    private static final String A2A_REQUEST_REDACTED = "[SAA_A2A_REQUEST_REDACTED]";

    private static final String A2A_REQUEST_URL_REDACTED = "[SAA_A2A_REQUEST_URL_REDACTED]";

    private final PrintStream delegate;

    RedactingPrintStream(PrintStream delegate) {
        super(delegate, true, StandardCharsets.UTF_8);
        this.delegate = delegate;
    }

    @Override
    public void println(String value) {
        delegate.println(redact(String.valueOf(value)));
    }

    @Override
    public void print(String value) {
        delegate.print(redact(String.valueOf(value)));
    }

    @Override
    public void println(Object value) {
        delegate.println(redact(String.valueOf(value)));
    }

    @Override
    public void print(Object value) {
        delegate.print(redact(String.valueOf(value)));
    }

    private String redact(String value) {
        if (value == null) {
            return "null";
        }
        if (isSensitiveA2aRequest(value)) {
            return A2A_REQUEST_REDACTED;
        }
        if (isA2aUrl(value)) {
            return A2A_REQUEST_URL_REDACTED;
        }
        return value;
    }

    private boolean isSensitiveA2aRequest(String value) {
        return value.contains("\"method\":\"message/send\"")
                || value.contains("\"method\":\"message/stream\"")
                || value.contains("\"payloadJson\"")
                || value.contains("\"workspaceSnapshot\"")
                || value.contains("\"rawText\"");
    }

    private boolean isA2aUrl(String value) {
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return (normalized.startsWith("http://") || normalized.startsWith("https://"))
                && normalized.contains("/a2a");
    }
}
