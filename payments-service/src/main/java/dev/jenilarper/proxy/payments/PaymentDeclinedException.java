package dev.jenilarper.proxy.payments;

import dev.jenilarper.proxy.runtime.ServiceOperationException;

public class PaymentDeclinedException extends ServiceOperationException {

    public PaymentDeclinedException(String message) {
        super(message, 422);
    }
}
