package dev.jenilarper.proxy.runtime;

@FunctionalInterface
public interface AuditPublisher {

    void publish(InterceptionRecord record);
}
