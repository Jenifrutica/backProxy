package dev.jenilarper.proxy.audit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Fires 50 random calls through the public endpoints, so every call really goes
 * through the service's logging proxy. It never touches the services directly.
 */
@Service
public class SimulateLoadService {

    private static final int CALLS = 50;

    private final RestClient inventory;
    private final RestClient orders;
    private final RestClient payments;
    private final ExecutorService executor;
    private final ThreadLocalRandom random = ThreadLocalRandom.current();

    public SimulateLoadService(
            @Value("${services.inventory.url}") String inventoryUrl,
            @Value("${services.orders.url}") String ordersUrl,
            @Value("${services.payments.url}") String paymentsUrl
    ) {
        this.inventory = client(inventoryUrl);
        this.orders = client(ordersUrl);
        this.payments = client(paymentsUrl);

        AtomicInteger counter = new AtomicInteger();
        this.executor = Executors.newFixedThreadPool(8, runnable -> {
            Thread thread = new Thread(runnable, "simulate-load-" + counter.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        });
    }

    public LoadSimulationReport run() {
        List<Callable<Boolean>> tasks = new ArrayList<>(CALLS);
        for (int index = 0; index < CALLS; index++) {
            tasks.add(this::callRandom);
        }

        int succeeded = 0;
        int failed = 0;
        try {
            List<Future<Boolean>> futures = executor.invokeAll(tasks);
            for (Future<Boolean> future : futures) {
                try {
                    if (future.get()) {
                        succeeded++;
                    } else {
                        failed++;
                    }
                } catch (ExecutionException error) {
                    failed++;
                }
            }
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            failed += CALLS - succeeded - failed;
        }
        return new LoadSimulationReport(CALLS, succeeded, failed);
    }

    private boolean callRandom() {
        try {
            int pick = random.nextInt(3);
            if (pick == 0) {
                call(inventory, "inventory", randomInventoryCall());
            } else if (pick == 1) {
                call(orders, "orders", randomOrdersCall());
            } else {
                call(payments, "payments", randomPaymentsCall());
            }
            return true;
        } catch (RuntimeException error) {
            return false;
        }
    }

    private void call(RestClient client, String serviceId, Call call) {
        client.post()
                .uri("/api/services/" + serviceId + "/" + call.operation())
                .body(call.params())
                .retrieve()
                .toBodilessEntity();
    }

    private Call randomInventoryCall() {
        int pick = random.nextInt(3);
        if (pick == 0) {
            return new Call("checkStock", Map.of("itemId", "SKU-" + (1 + random.nextInt(4))));
        }
        if (pick == 1) {
            return new Call("reserveStock", Map.of(
                    "itemId", "SKU-" + (1 + random.nextInt(3)),
                    "quantity", 1 + random.nextInt(2)
            ));
        }
        return new Call("listItems", Map.of());
    }

    private Call randomOrdersCall() {
        if (random.nextBoolean()) {
            int lineCount = 1 + random.nextInt(3);
            List<Map<String, Object>> items = new ArrayList<>(lineCount);
            for (int index = 0; index < lineCount; index++) {
                items.add(Map.of(
                        "sku", "SKU-" + (1 + random.nextInt(5)),
                        "quantity", 1 + random.nextInt(4),
                        "unitPrice", 10 + random.nextInt(90)
                ));
            }
            return new Call("createOrder", Map.of(
                    "customerId", "CUS-" + (100 + random.nextInt(900)),
                    "items", items
            ));
        }
        return new Call("listOrders", Map.of());
    }

    private Call randomPaymentsCall() {
        if (random.nextInt(100) < 80) {
            return new Call("charge", Map.of(
                    "orderId", "ORD-SIM-" + (1000 + random.nextInt(9000)),
                    "amount", 10 + random.nextInt(490)
            ));
        }
        return new Call("listPayments", Map.of());
    }

    private RestClient client(String baseUrl) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(60_000);
        factory.setReadTimeout(60_000);
        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .build();
    }

    private record Call(String operation, Map<String, Object> params) {
    }
}
