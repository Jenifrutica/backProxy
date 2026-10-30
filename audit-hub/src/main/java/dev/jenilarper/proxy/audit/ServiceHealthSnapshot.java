package dev.jenilarper.proxy.audit;

public record ServiceHealthSnapshot(
        String serviceId,
        long totalCalls,
        long errorCount,
        double errorRate,
        double avgResponseMs
) {
}
