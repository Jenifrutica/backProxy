# Backend · Proxy de auditoría y logging

## Cómo se aplican los requisitos

| Requisito | Cómo se aplica | Archivo |
|---|---|---|
| Interfaz genérica `MicroserviceProxy<T>` con `execute(String operation, Object... params): T` | Se declara una interfaz con un único método. Los servicios reales y los proxies la implementan, y los controladores dependen solo de la interfaz, nunca de una implementación concreta. | `proxy-runtime/.../MicroserviceProxy.java` |
| `InventoryService`, `OrderService` y `PaymentService` reales con operaciones implementadas | Cada servicio implementa la interfaz y despacha la operación con un `OperationCatalog` (`Map<String, OperationHandler>`). El estado vive en mapas concurrentes en memoria. | `InventoryService.java`, `OrderService.java`, `PaymentService.java` |
| `LoggingProxy` que envuelve cualquier servicio y registra inicio, duración, resultado y error con stack resumido | El proxy recibe un `MicroserviceProxy<T>` delegado. Genera un `requestId` UUID, toma `Instant` y `nanoTime`, delega la llamada y publica un `InterceptionRecord`. Si falla, captura tipo, mensaje y las primeras 8 frames del stack, y vuelve a lanzar la excepción. | `proxy-runtime/.../LoggingProxy.java` |
| Cada log incluye `serviceId`, `operation`, `durationMs`, `status`, `requestId` UUID y `timestamp` | `InterceptionRecord` es un record con esos seis campos obligatorios. `status` es el enum `SUCCESS`/`ERROR` y `timestamp` se toma al inicio de la llamada. | `proxy-runtime/.../InterceptionRecord.java` |
| `POST /api/services/inventory/{operation}` pasa por el logging proxy | El controlador inyecta el bean `inventoryProxy` (el `LoggingProxy`), no el servicio real. Cada petición ejecuta `proxy.execute(operation, params)`. | `InventoryEndpoint.java` |
| `POST /api/services/orders/{operation}` pasa por el logging proxy | Mismo esquema: el controlador depende de `ordersProxy` y toda llamada queda auditada. | `OrdersEndpoint.java` |
| `POST /api/services/payments/{operation}` pasa por el logging proxy | Mismo esquema con `paymentsProxy`; además el servicio inyecta fallas antes de despachar la operación. | `PaymentsEndpoint.java` |
| `GET /api/metrics/summary` con total, tasa de error y promedio por servicio | `InMemoryAuditLedger` agrupa los registros por `serviceId`, cuenta llamadas y errores, calcula `errorRate` y promedia `durationMs`. | `MetricsController.java` + `InMemoryAuditLedger.java` |
| `GET /api/metrics/logs?service=&status=&from=&to=` con filtros y paginación | El ledger filtra por servicio, estado e instantes ISO-8601, y pagina con `page`/`size`. Devuelve `content`, `totalElements` y `totalPages`. | `MetricsController.java` + `InMemoryAuditLedger.java` |
| 10% de fallas intencionales en `PaymentService` | `PaymentFailureInjector` lanza `PaymentDeclinedException` (HTTP 422) en el 10% de las llamadas. La falla vive en el servicio real, no en el proxy, para que el proxy la registre como `ERROR` con stack trace. | `PaymentFailureInjector.java` |
| `POST /api/metrics/simulate-load` con 50 llamadas aleatorias | `SimulateLoadService` elige servicio y operación al azar y ejecuta 50 POST contra los endpoints públicos. Al entrar por la API, cada llamada atraviesa el `LoggingProxy` y genera su log. | `SimulateLoadService.java` |

**Extras incluidos:** CORS para el dashboard, latencia de procesamiento simulada, `inputParams` y `responseJson` para el detalle del front, y `/health` para Render.

## Despliegue

El `render.yaml` de este repositorio define los cuatro servicios y también el gateway del front (repo `frontProxy`). Al sincronizar el Blueprint se crea una única URL pública:

```
https://jenilarper-proxy-app.onrender.com
```

### Local

```bash
mvn -DskipTests package
java -jar audit-hub/target/audit-hub-1.0.0.jar &
java -jar inventory-service/target/inventory-service-1.0.0.jar &
java -jar orders-service/target/orders-service-1.0.0.jar &
java -jar payments-service/target/payments-service-1.0.0.jar &
```

Puertos: 8081 inventario, 8082 pedidos, 8083 pagos, 8084 auditoría.
