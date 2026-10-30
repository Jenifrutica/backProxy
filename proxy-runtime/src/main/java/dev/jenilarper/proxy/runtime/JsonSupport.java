package dev.jenilarper.proxy.runtime;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public final class JsonSupport {

    private static final int MAX_FIELD_LENGTH = 2000;

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    private JsonSupport() {
    }

    public static ObjectMapper mapper() {
        return MAPPER;
    }

    public static String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            return String.valueOf(value);
        }
    }

    public static String truncate(String value) {
        if (value == null) {
            return null;
        }
        return value.length() <= MAX_FIELD_LENGTH
                ? value
                : value.substring(0, MAX_FIELD_LENGTH) + "…";
    }

    public static String stackSummary(Throwable error) {
        StringBuilder summary = new StringBuilder(error.getClass().getName());
        if (error.getMessage() != null) {
            summary.append(": ").append(error.getMessage());
        }
        StackTraceElement[] frames = error.getStackTrace();
        int limit = Math.min(frames.length, 8);
        for (int index = 0; index < limit; index++) {
            summary.append("\n\tat ").append(frames[index]);
        }
        return truncate(summary.toString());
    }
}
