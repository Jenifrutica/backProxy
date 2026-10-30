package dev.jenilarper.proxy.runtime;

public class ServiceOperationException extends RuntimeException {

    private final int status;

    public ServiceOperationException(String message, int status) {
        super(message);
        this.status = status;
    }

    public int status() {
        return status;
    }
}
