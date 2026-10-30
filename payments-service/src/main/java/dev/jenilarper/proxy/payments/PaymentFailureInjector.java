package dev.jenilarper.proxy.payments;

import java.util.concurrent.ThreadLocalRandom;

/**
 * The rubric asks for a 10% of intentional failures on this service. The failure
 * lives in the real service, never in the proxy, so the proxy records it as ERROR.
 */
public class PaymentFailureInjector {

    private static final int FAILURE_PERCENT = 10;

    public void maybeFail() {
        if (ThreadLocalRandom.current().nextInt(100) < FAILURE_PERCENT) {
            throw new PaymentDeclinedException("Pago rechazado por el proveedor (falla simulada del 10%).");
        }
    }
}
