package dev.jenilarper.proxy.inventory;

import dev.jenilarper.proxy.runtime.MicroserviceProxy;
import dev.jenilarper.proxy.runtime.OperationHandler;
import dev.jenilarper.proxy.runtime.Parameters;
import dev.jenilarper.proxy.runtime.ServiceOperationException;
import dev.jenilarper.proxy.runtime.UnknownOperationException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class InventoryService implements MicroserviceProxy<Object> {

    private final Map<String, Integer> stock = new ConcurrentHashMap<>();
    private final Map<String, Reservation> reservations = new ConcurrentHashMap<>();
    private final Map<String, OperationHandler> operations;

    public InventoryService() {
        stock.put("SKU-1", 120);
        stock.put("SKU-2", 64);
        stock.put("SKU-3", 18);
        stock.put("SKU-4", 7);
        stock.put("SKU-5", 0);

        Map<String, OperationHandler> catalog = new LinkedHashMap<>();
        catalog.put("checkStock", this::checkStock);
        catalog.put("reserveStock", this::reserveStock);
        catalog.put("releaseStock", this::releaseStock);
        catalog.put("listItems", this::listItems);
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

    private Object checkStock(Map<String, Object> params) {
        String itemId = Parameters.string(params, "itemId");
        int available = stock.getOrDefault(itemId, 0);
        return Map.of("itemId", itemId, "available", available);
    }

    private Object reserveStock(Map<String, Object> params) {
        String itemId = Parameters.string(params, "itemId");
        int quantity = Parameters.integer(params, "quantity");
        if (quantity <= 0) {
            throw new ServiceOperationException("La cantidad debe ser mayor que cero.", 400);
        }

        int[] remaining = new int[1];
        stock.compute(itemId, (key, current) -> {
            int available = current == null ? 0 : current;
            if (available < quantity) {
                throw new ServiceOperationException("Stock insuficiente para " + itemId + ".", 409);
            }
            remaining[0] = available - quantity;
            return remaining[0];
        });

        String reservationId = "RES-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        reservations.put(reservationId, new Reservation(reservationId, itemId, quantity));
        return Map.of(
                "reservationId", reservationId,
                "itemId", itemId,
                "quantity", quantity,
                "remaining", remaining[0]
        );
    }

    private Object releaseStock(Map<String, Object> params) {
        String reservationId = Parameters.string(params, "reservationId");
        Reservation reservation = reservations.remove(reservationId);
        if (reservation == null) {
            throw new ServiceOperationException("Reserva no encontrada: " + reservationId, 404);
        }
        stock.merge(reservation.itemId(), reservation.quantity(), Integer::sum);
        return Map.of(
                "reservationId", reservationId,
                "released", reservation.quantity(),
                "available", stock.getOrDefault(reservation.itemId(), 0)
        );
    }

    private Object listItems(Map<String, Object> params) {
        List<Map<String, Object>> items = stock.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> Map.<String, Object>of("itemId", entry.getKey(), "available", entry.getValue()))
                .toList();
        return Map.of("items", items);
    }

    private record Reservation(String reservationId, String itemId, int quantity) {
    }
}
