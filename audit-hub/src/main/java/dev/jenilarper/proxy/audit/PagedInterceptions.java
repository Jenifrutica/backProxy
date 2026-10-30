package dev.jenilarper.proxy.audit;

import dev.jenilarper.proxy.runtime.InterceptionRecord;

import java.util.List;

public record PagedInterceptions(
        List<InterceptionRecord> content,
        int page,
        int size,
        int totalElements,
        int totalPages
) {
}
