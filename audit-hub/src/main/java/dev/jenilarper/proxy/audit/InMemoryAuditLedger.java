package dev.jenilarper.proxy.audit;

import dev.jenilarper.proxy.runtime.InterceptionRecord;
import dev.jenilarper.proxy.runtime.InvocationStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.stream.Collectors;

@Component
public class InMemoryAuditLedger implements AuditLedger {

    private static final int MAX_RECORDS = 10_000;

    private final ConcurrentLinkedDeque<InterceptionRecord> records = new ConcurrentLinkedDeque<>();
    private final List<String> knownServices;

    public InMemoryAuditLedger(@Value("${audit.known-services:inventory,orders,payments}") String knownServices) {
        this.knownServices = List.of(knownServices.split(","));
    }

    @Override
    public void append(InterceptionRecord record) {
        records.addFirst(record);
        while (records.size() > MAX_RECORDS) {
            records.removeLast();
        }
    }

    @Override
    public List<ServiceHealthSnapshot> summary() {
        List<InterceptionRecord> snapshot = List.copyOf(records);
        Map<String, List<InterceptionRecord>> byService = snapshot.stream()
                .collect(Collectors.groupingBy(InterceptionRecord::serviceId));

        Set<String> serviceIds = new LinkedHashSet<>(knownServices);
        serviceIds.addAll(byService.keySet());

        List<ServiceHealthSnapshot> result = new ArrayList<>();
        for (String serviceId : serviceIds) {
            List<InterceptionRecord> serviceRecords = byService.getOrDefault(serviceId, List.of());
            long total = serviceRecords.size();
            long errors = serviceRecords.stream()
                    .filter(record -> record.status() == InvocationStatus.ERROR)
                    .count();
            double average = serviceRecords.stream()
                    .mapToLong(InterceptionRecord::durationMs)
                    .average()
                    .orElse(0);
            double errorRate = total == 0 ? 0 : (double) errors / total;
            result.add(new ServiceHealthSnapshot(
                    serviceId,
                    total,
                    errors,
                    round(errorRate),
                    round(average)
            ));
        }
        return result;
    }

    @Override
    public PagedInterceptions query(
            String service,
            InvocationStatus status,
            Instant from,
            Instant to,
            int page,
            int size
    ) {
        List<InterceptionRecord> filtered = records.stream()
                .filter(record -> matchesService(record, service))
                .filter(record -> status == null || record.status() == status)
                .filter(record -> from == null || !record.timestamp().isBefore(from))
                .filter(record -> to == null || !record.timestamp().isAfter(to))
                .toList();

        int total = filtered.size();
        long offset = (long) page * size;
        int fromIndex = (int) Math.min(offset, total);
        int toIndex = (int) Math.min(offset + size, total);
        List<InterceptionRecord> content = filtered.subList(fromIndex, toIndex);
        int totalPages = size == 0 ? 0 : (int) Math.ceil((double) total / size);

        return new PagedInterceptions(content, page, size, total, totalPages);
    }

    private boolean matchesService(InterceptionRecord record, String service) {
        return service == null || service.isBlank() || "all".equalsIgnoreCase(service)
                || record.serviceId().equalsIgnoreCase(service);
    }

    private double round(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }
}
