package dev.jenilarper.proxy.audit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "dev.jenilarper.proxy")
public class AuditHubApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuditHubApplication.class, args);
    }
}
