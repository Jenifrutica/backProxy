package dev.jenilarper.proxy.payments;

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

public class PaymentService implements MicroserviceProxy<Object> {

    private final Map<String, Payment> payments = new ConcurrentHashMap<>();
    private final Map<String, OperationHandler> operations;
    private final PaymentFailureInjector failureInjector = new PaymentFailureInjector();

    public PaymentService() {
        Map<String, OperationHandler> catalog = new LinkedHashMap<>();
        catalog.put("charge", this::charge);
        catalog.put("refund", this::refund);
        catalog.put("getPayment", this::getPayment);
        catalog.put("listPayments", this::listPayments);
        this.operations = Map.copyOf(catalog);
    }

    @Override
    public Object execute(String operation, Object... params) {
        OperationHandler handler = operations.get(operation);
        if (handler == null) {
            throw new UnknownOperationException(operation);
        }
        failureInjector.maybeFail();
        return handler.handle(Parameters.input(params));
    }

    private Object charge(Map<String, Object> params) {
        String orderId = Parameters.string(params, "orderId");
        double amount = Parameters.decimal(params, "amount");
        if (amount <= 0) {
            throw new ServiceOperationException("El monto debe ser mayor que cero.", 400);
        }

        String paymentId = "PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Payment payment = new Payment(paymentId, orderId, round(amount), "APPROVED", Instant.now());
        payments.put(paymentId, payment);
        return payment;
    }

    private Object refund(Map<String, Object> params) {
        String paymentId = Parameters.string(params, "paymentId");
        Payment payment = payments.get(paymentId);
        if (payment == null) {
            throw new ServiceOperationException("Pago no encontrado: " + paymentId, 404);
        }
        if ("REFUNDED".equals(payment.status())) {
            throw new ServiceOperationException("El pago ya fue reembolsado: " + paymentId, 409);
        }
        Payment refunded = new Payment(
                payment.paymentId(),
                payment.orderId(),
                payment.amount(),
                "REFUNDED",
                payment.createdAt()
        );
        payments.put(paymentId, refunded);
        return refunded;
    }

    private Object getPayment(Map<String, Object> params) {
        String paymentId = Parameters.string(params, "paymentId");
        Payment payment = payments.get(paymentId);
        if (payment == null) {
            throw new ServiceOperationException("Pago no encontrado: " + paymentId, 404);
        }
        return payment;
    }

    private Object listPayments(Map<String, Object> params) {
        List<Payment> sorted = payments.values().stream()
                .sorted((left, right) -> right.createdAt().compareTo(left.createdAt()))
                .toList();
        return Map.of("payments", sorted);
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    public record Payment(
            String paymentId,
            String orderId,
            double amount,
            String status,
            Instant createdAt
    ) {
    }
}
