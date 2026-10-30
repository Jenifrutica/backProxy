package dev.jenilarper.proxy.runtime;

import java.util.Map;

@FunctionalInterface
public interface OperationHandler {

    Object handle(Map<String, Object> params);
}
