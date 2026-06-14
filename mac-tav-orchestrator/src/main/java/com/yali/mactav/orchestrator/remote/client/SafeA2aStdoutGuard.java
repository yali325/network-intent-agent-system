package com.yali.mactav.orchestrator.remote.client;

import java.io.PrintStream;
import java.util.concurrent.Callable;

/**
 * Temporarily protects stdout around SAA A2aRemoteAgent invocation only.
 */
final class SafeA2aStdoutGuard {

    private static final Object LOCK = new Object();

    private SafeA2aStdoutGuard() {
    }

    static <T> T call(Callable<T> action) throws Exception {
        synchronized (LOCK) {
            PrintStream original = System.out;
            try {
                System.setOut(new RedactingPrintStream(original));
                return action.call();
            }
            finally {
                System.setOut(original);
            }
        }
    }
}
