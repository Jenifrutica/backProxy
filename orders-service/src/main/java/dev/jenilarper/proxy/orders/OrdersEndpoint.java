package dev.jenilarper.proxy.orders;

import dev.jenilarper.proxy.runtime.InvocationResponse;
import dev.jenilarper.proxy.runtime.MicroserviceProxy;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/services/orders")
public class OrdersEndpoint {

    private final MicroserviceProxy<Object> proxy;

    public OrdersEndpoint(@Qualifier("ordersProxy") MicroserviceProxy<Object> proxy) {
        this.proxy = proxy;
    }

    @PostMapping("/{operation}")
    public InvocationResponse invoke(
            @PathVariable String operation,
            @RequestBody(required = false) Map<String, Object> params
    ) {
        return new InvocationResponse(proxy.execute(operation, params == null ? Map.of() : params));
    }
}
