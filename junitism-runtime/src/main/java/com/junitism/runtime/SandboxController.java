package com.junitism.runtime;

/**
 * Intercepts System.exit, Runtime.halt, etc. when sandbox is active.
 */
public final class SandboxController {

    private SandboxController() {}

    public static void interceptExit(int status) {
        throw new SecurityException("System.exit(" + status + ") blocked by Junitism sandbox");
    }

    public static void interceptHalt(int status) {
        throw new SecurityException("Runtime.halt(" + status + ") blocked by Junitism sandbox");
    }
}
