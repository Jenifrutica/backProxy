package dev.jenilarper.proxy.audit;

import dev.jenilarper.proxy.runtime.InterceptionRecord;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuditIngestController {

    private final AuditLedger ledger;

    public AuditIngestController(AuditLedger ledger) {
        this.ledger = ledger;
    }

    @PostMapping("/internal/audit")
    public ResponseEntity<Void> ingest(@RequestBody InterceptionRecord record) {
        ledger.append(record);
        return ResponseEntity.accepted().build();
    }
}
