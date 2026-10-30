package dev.jenilarper.proxy.orders;

import dev.jenilarper.proxy.runtime.MicroserviceProxy;
import dev.jenilarper.proxy.runtime.OperationHandler;
import dev.jenilarper.proxy.runtime.Parameters;
import dev.jenilarper.proxy.runtime.ServiceOperationException;
import dev.jenilarper.proxy.runtime.UnknownOperationException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class OrderService implements MicroserviceProxy<Object> {

    private final Map<String, Order> orders = new ConcurrentHashMap<>();
    private final Map<String, OperationHandler> operations;

    public OrderService() {
        Map<String, OperationHandler> catalog = new LinkedHashMap<>();
        catalog.put("createOrder", this::createOrder);
        catalog.put("getOrder", this::getOrder);
        catalog.put("cancelOrder", this::cancelOrder);
        catalog.put("listOrders", this::listOrders);
        this.operations = Map.copyOf(catalog);
    }

    @Override
    public Object execute(String operation, Object... params) {
        OperationHandler handler = operations.get(operation);
        if (handler == null) {
            throw new UnknownOperationException(operation);
        }
        return handler.handle(Parameters.input(params));
    }

    private Object createOrder(Map<String, Object> params) {
        String customerId = Parameters.string(params, "customerId");
        List<Map<String, Object>> rawItems = Parameters.listOfMaps(params, "items");
        if (rawItems.isEmpty()) {
            throw new ServiceOperationException("El pedido debe tener al menos un ítem.", 400);
        }

        List<OrderLine> lines = rawItems.stream().map(this::toLine).toList();
        double total = lines.stream()
                .mapToDouble(line -> line.quantity() * line.unitPrice())
                .sum();

        String orderId = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Order order = new Order(orderId, customerId, "CREATED", round(total), Instant.now(), lines);
        orders.put(orderId, order);
        return order;
    }

    private Object getOrder(Map<String, Object> params) {
        String orderId = Parameters.string(params, "orderId");
        Order order = orders.get(orderId);
        if (order == null) {
            throw new ServiceOperationException("Pedido no encontrado: " + orderId, 404);
        }
        return order;
    }

    private Object cancelOrder(Map<String, Object> params) {
        String orderId = Parameters.string(params, "orderId");
        Order order = orders.get(orderId);
        if (order == null) {
            throw new ServiceOperationException("Pedido no encontrado: " + orderId, 404);
        }
        Order cancelled = new Order(
                order.orderId(),
                order.customerId(),
                "CANCELLED",
                order.total(),
                order.createdAt(),
                order.lines()
        );
        orders.put(orderId, cancelled);
        return cancelled;
    }

    private Object listOrders(Map<String, Object> params) {
        List<Order> sorted = orders.values().stream()
                .sorted((left, right) -> right.createdAt().compareTo(left.createdAt()))
                .toList();
        return Map.of("orders", sorted);
    }

    private OrderLine toLine(Map<String, Object> raw) {
        String sku = Parameters.string(raw, "sku");
        int quantity = Parameters.integer(raw, "quantity");
        double unitPrice = Parameters.decimal(raw, "unitPrice");
        if (quantity <= 0 || unitPrice < 0) {
            throw new ServiceOperationException("Ítem inválido para el SKU " + sku, 400);
        }
        return new OrderLine(sku, quantity, unitPrice);
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    public record OrderLine(String sku, int quantity, double unitPrice) {
    }

    public record Order(
            String orderId,
            String customerId,
            String status,
            double total,
            Instant createdAt,
            List<OrderLine> lines
    ) {
    }
}
