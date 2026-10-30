package dev.jenilarper.proxy.runtime;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Adds a small, bounded processing time so the dashboard shows realistic
 * latency instead of a wall of zero milliseconds. The delay belongs to the
 * real service, not to the proxy.
 */
public final class ProcessingTime {

    private ProcessingTime() {
    }

    public static void simulate() {
        try {
            Thread.sleep(ThreadLocalRandom.current().nextInt(4, 22));
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
        }
    }
}
