package dev.jenilarper.proxy.orders;

import dev.jenilarper.proxy.runtime.AuditPublisher;
import dev.jenilarper.proxy.runtime.HttpAuditPublisher;
import dev.jenilarper.proxy.runtime.LoggingProxy;
import dev.jenilarper.proxy.runtime.MicroserviceProxy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ServiceConfig {

    @Bean
    public AuditPublisher auditPublisher(@Value("${audit.hub.url}") String hubUrl) {
        return new HttpAuditPublisher(hubUrl);
    }

    @Bean
    public MicroserviceProxy<Object> ordersProxy(AuditPublisher publisher) {
        return new LoggingProxy<>("orders", new OrderService(), publisher);
    }
}
