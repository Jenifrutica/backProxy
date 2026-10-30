package dev.jenilarper.proxy.audit;

import dev.jenilarper.proxy.runtime.InterceptionRecord;
import dev.jenilarper.proxy.runtime.InvocationStatus;

import java.time.Instant;
import java.util.List;

public interface AuditLedger {

    void append(InterceptionRecord record);

    List<ServiceHealthSnapshot> summary();

    PagedInterceptions query(
            String service,
            InvocationStatus status,
            Instant from,
            Instant to,
            int page,
            int size
    );
}
