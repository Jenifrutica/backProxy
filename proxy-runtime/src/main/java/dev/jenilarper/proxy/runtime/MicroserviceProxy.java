package dev.jenilarper.proxy.runtime;

/**
 * Contract shared by the real services and every proxy that wraps them.
 * The caller never knows whether it is talking to the real subject or a proxy.
 */
public interface MicroserviceProxy<T> {

    T execute(String operation, Object... params);
}
