package dev.jenilarper.proxy.runtime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Logging proxy. It implements the same contract as the real subject, measures
 * the delegated call and publishes one audit record, whether it succeeds or fails.
 */
public final class LoggingProxy<T> implements MicroserviceProxy<T> {

    private static final Logger log = LoggerFactory.getLogger(LoggingProxy.class);

    private final String serviceId;
    private final MicroserviceProxy<T> delegate;
    private final AuditPublisher publisher;

    public LoggingProxy(String serviceId, MicroserviceProxy<T> delegate, AuditPublisher publisher) {
        this.serviceId = serviceId;
        this.delegate = delegate;
        this.publisher = publisher;
    }

    @Override
    public T execute(String operation, Object... params) {
        String requestId = UUID.randomUUID().toString();
        Instant timestamp = Instant.now();
        long startedAt = System.nanoTime();
        Map<String, Object> input = Parameters.input(params);

        try {
            T result = delegate.execute(operation, params);
            long durationMs = elapsedMillis(startedAt);
            publish(new InterceptionRecord(
                    requestId,
                    serviceId,
                    operation,
                    durationMs,
                    InvocationStatus.SUCCESS,
                    timestamp,
                    input,
                    JsonSupport.truncate(JsonSupport.toJson(result)),
                    null,
                    null,
                    null
            ));
            return result;
        } catch (RuntimeException error) {
            long durationMs = elapsedMillis(startedAt);
            publish(new InterceptionRecord(
                    requestId,
                    serviceId,
                    operation,
                    durationMs,
                    InvocationStatus.ERROR,
                    timestamp,
                    input,
                    null,
                    error.getClass().getSimpleName(),
                    JsonSupport.truncate(error.getMessage()),
                    JsonSupport.stackSummary(error)
            ));
            throw error;
        }
    }

    private long elapsedMillis(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000;
    }

    private void publish(InterceptionRecord record) {
        try {
            publisher.publish(record);
        } catch (RuntimeException error) {
            // Auditing must never break the business call.
            log.warn("Could not publish audit record for {}.{}", record.serviceId(), record.operation(), error);
        }
    }
}
