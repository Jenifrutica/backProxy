# Backend · Proxy de auditoría y logging

## Cómo se aplica

### Render

1. **New + → Blueprint**.
2. Repositorio: `Jenifrutica/backProxy`.
3. **Blueprint Name:** `proxy-backend`.
4. **Branch:** `main`.
5. **Blueprint Path:** `render.yaml`.
6. **Deploy Blueprint**.

Crea cuatro Web Services en plan gratuito: `jenilarper-proxy-inventory`, `jenilarper-proxy-orders`, `jenilarper-proxy-payments` y `jenilarper-proxy-audit`. Las variables de entorno ya están en `render.yaml`.

Verificación:

```bash
curl https://jenilarper-proxy-audit.onrender.com/health
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

## Cumplimiento de requisitos

| Requisito | ¿Incluido? | Dónde |
|---|---|---|
| Interfaz genérica `MicroserviceProxy<T>` con `execute(String operation, Object... params): T` | ✅ | `proxy-runtime/.../MicroserviceProxy.java` |
| `InventoryService`, `OrderService`, `PaymentService` reales con operaciones implementadas | ✅ | `inventory-service`, `orders-service`, `payments-service` |
| `LoggingProxy` que envuelve cualquier servicio, mide duración y registra resultado o error con stack resumido | ✅ | `proxy-runtime/.../LoggingProxy.java` |
| Cada log incluye `serviceId`, `operation`, `durationMs`, `status`, `requestId` UUID y `timestamp` | ✅ | `proxy-runtime/.../InterceptionRecord.java` |
| `POST /api/services/inventory/{operation}` pasa por el proxy | ✅ | `InventoryEndpoint.java` |
| `POST /api/services/orders/{operation}` pasa por el proxy | ✅ | `OrdersEndpoint.java` |
| `POST /api/services/payments/{operation}` pasa por el proxy | ✅ | `PaymentsEndpoint.java` |
| `GET /api/metrics/summary` con total, tasa de error y promedio por servicio | ✅ | `MetricsController.java` |
| `GET /api/metrics/logs?service=&status=&from=&to=` con filtros y paginación | ✅ | `MetricsController.java` + `InMemoryAuditLedger.java` |
| 10% de fallas intencionales en `PaymentService` | ✅ | `PaymentFailureInjector.java` |
| `POST /api/metrics/simulate-load` con 50 llamadas aleatorias | ✅ | `SimulateLoadService.java` |

**Extras incluidos:** `inputParams` y `responseJson` para el detalle del front, CORS para el dashboard, latencia de procesamiento simulada y `/health` para Render.
