package dev.jenilarper.proxy.audit;

import dev.jenilarper.proxy.runtime.InvocationStatus;
import dev.jenilarper.proxy.runtime.ServiceOperationException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;

@RestController
@RequestMapping("/api/metrics")
public class MetricsController {

    private final AuditLedger ledger;
    private final SimulateLoadService simulator;

    public MetricsController(AuditLedger ledger, SimulateLoadService simulator) {
        this.ledger = ledger;
        this.simulator = simulator;
    }

    @GetMapping("/summary")
    public List<ServiceHealthSnapshot> summary() {
        return ledger.summary();
    }

    @GetMapping("/logs")
    public PagedInterceptions logs(
            @RequestParam(required = false) String service,
            @RequestParam(required = false) InvocationStatus status,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        return ledger.query(
                service,
                status,
                parseInstant(from, "from"),
                parseInstant(to, "to"),
                safePage,
                safeSize
        );
    }

    @PostMapping("/simulate-load")
    public LoadSimulationReport simulateLoad() {
        return simulator.run();
    }

    private Instant parseInstant(String value, String field) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException error) {
            throw new ServiceOperationException(
                    "El parámetro " + field + " no es una fecha ISO-8601 válida.",
                    400
            );
        }
    }
}
