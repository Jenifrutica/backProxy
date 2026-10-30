package dev.jenilarper.proxy.runtime;

public class UnknownOperationException extends ServiceOperationException {

    public UnknownOperationException(String operation) {
        super("Operación desconocida: " + operation, 400);
    }
}
