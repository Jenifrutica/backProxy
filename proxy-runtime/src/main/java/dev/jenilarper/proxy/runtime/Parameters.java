package dev.jenilarper.proxy.runtime;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Normalizes the varargs contract into a named parameter map and validates
 * the values the operations expect.
 */
public final class Parameters {

    private Parameters() {
    }

    public static Map<String, Object> input(Object... params) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (params != null && params.length == 1 && params[0] instanceof Map<?, ?> map) {
            map.forEach((key, value) -> result.put(String.valueOf(key), value));
            return result;
        }
        result.put("args", params == null ? List.of() : Arrays.asList(params));
        return result;
    }

    public static String string(Map<String, Object> params, String key) {
        Object value = params.get(key);
        if (value == null || value.toString().isBlank()) {
            throw new ServiceOperationException("Falta el parámetro: " + key, 400);
        }
        return value.toString();
    }

    public static int integer(Map<String, Object> params, String key) {
        Object value = params.get(key);
        if (value == null) {
            throw new ServiceOperationException("Falta el parámetro: " + key, 400);
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException error) {
            throw new ServiceOperationException("El parámetro " + key + " debe ser numérico.", 400);
        }
    }

    public static double decimal(Map<String, Object> params, String key) {
        Object value = params.get(key);
        if (value == null) {
            throw new ServiceOperationException("Falta el parámetro: " + key, 400);
        }
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException error) {
            throw new ServiceOperationException("El parámetro " + key + " debe ser numérico.", 400);
        }
    }

    @SuppressWarnings("unchecked")
    public static List<Map<String, Object>> listOfMaps(Map<String, Object> params, String key) {
        Object value = params.get(key);
        if (!(value instanceof List<?> list)) {
            throw new ServiceOperationException("El parámetro " + key + " debe ser una lista.", 400);
        }
        return (List<Map<String, Object>>) list;
    }
}
