package dev.jenilarper.proxy.runtime;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoggingProxyTest {

    private final List<InterceptionRecord> records = new ArrayList<>();

    @Test
    void recordsSuccessfulCall() {
        MicroserviceProxy<Object> real = (operation, params) -> Map.of("available", 12);
        LoggingProxy<Object> proxy = new LoggingProxy<>("inventory", real, records::add);

        Object result = proxy.execute("checkStock", Map.of("itemId", "SKU-1"));

        assertEquals(Map.of("available", 12), result);
        assertEquals(1, records.size());
        InterceptionRecord record = records.getFirst();
        assertEquals("inventory", record.serviceId());
        assertEquals("checkStock", record.operation());
        assertEquals(InvocationStatus.SUCCESS, record.status());
        assertNotNull(record.requestId());
        assertNotNull(record.timestamp());
        assertEquals("SKU-1", record.inputParams().get("itemId"));
    }

    @Test
    void recordsFailureAndRethrows() {
        MicroserviceProxy<Object> real = (operation, params) -> {
            throw new IllegalStateException("payment provider down");
        };
        LoggingProxy<Object> proxy = new LoggingProxy<>("payments", real, records::add);

        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> proxy.execute("charge", Map.of("amount", 10))
        );

        assertEquals("payment provider down", error.getMessage());
        assertEquals(1, records.size());
        InterceptionRecord record = records.getFirst();
        assertEquals(InvocationStatus.ERROR, record.status());
        assertEquals("IllegalStateException", record.errorType());
        assertTrue(record.stackTraceSummary().contains("IllegalStateException"));
    }
}
