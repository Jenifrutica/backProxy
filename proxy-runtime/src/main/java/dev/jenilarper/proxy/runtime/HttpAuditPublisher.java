package dev.jenilarper.proxy.runtime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Fire-and-forget transport to the audit hub. The business response never waits
 * for the audit call, and a hub outage never breaks a service.
 */
public final class HttpAuditPublisher implements AuditPublisher {

    private static final Logger log = LoggerFactory.getLogger(HttpAuditPublisher.class);

    private final RestClient client;
    private final ExecutorService executor;

    public HttpAuditPublisher(String hubUrl) {
        this.client = RestClient.builder().baseUrl(hubUrl).build();
        this.executor = new ThreadPoolExecutor(
                2,
                2,
                0L,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(2000),
                runnable -> {
                    Thread thread = new Thread(runnable, "audit-publisher");
                    thread.setDaemon(true);
                    return thread;
                },
                new ThreadPoolExecutor.DiscardOldestPolicy()
        );
    }

    @Override
    public void publish(InterceptionRecord record) {
        executor.submit(() -> {
            try {
                client.post()
                        .uri("/internal/audit")
                        .body(record)
                        .retrieve()
                        .toBodilessEntity();
            } catch (RuntimeException error) {
                log.warn("Audit hub rejected record {} from {}", record.requestId(), record.serviceId(), error);
            }
        });
    }
}
