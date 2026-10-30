package dev.jenilarper.proxy.runtime;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.Map;

/**
 * One audited call. This is the record the dashboard consumes.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record InterceptionRecord(
        String requestId,
        String serviceId,
        String operation,
        long durationMs,
        InvocationStatus status,
        Instant timestamp,
        Map<String, Object> inputParams,
        String responseJson,
        String errorType,
        String errorMessage,
        String stackTraceSummary
) {
}
