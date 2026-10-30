# Proxy de auditoría y logging

Backend del taller **Sistema de monitoreo de microservicios con Proxy de auditoría y logging**.

Tres microservicios de negocio (Inventario, Pedidos y Pagos), cada uno envuelto por un
`LoggingProxy` escrito a mano, y un hub de auditoría que centraliza las métricas y sirve
los endpoints de observabilidad.

## Arquitectura

```
                    ┌──────────────────────────────┐
                    │        Panel web (SPA)       │
                    └──────────────┬───────────────┘
                                   │ HTTP
          ┌────────────────────────┼────────────────────────┐
          │                        │                        │
   ┌──────▼───────┐        ┌───────▼──────┐         ┌───────▼──────┐
   │  Inventario  │        │   Pedidos    │         │    Pagos     │
   │  LoggingProxy│        │  LoggingProxy│         │  LoggingProxy│
   └──────┬───────┘        └───────┬──────┘         └───────┬──────┘
          │  InterceptionRecord    │                        │
          └────────────────────────┼────────────────────────┘
                                   │
                          ┌────────▼────────┐
                          │    Audit Hub    │
                          │ ledger + métricas│
                          └─────────────────┘
```

Cada microservicio es un deployable independiente. El hub es infraestructura de
observabilidad, no un cuarto microservicio de negocio.

## Módulos

| Módulo | Puerto local | Responsabilidad |
|---|---|---|
| `proxy-runtime` | — | Contrato `MicroserviceProxy<T>`, `LoggingProxy<T>`, DTO de auditoría, publicación HTTP y manejo de errores |
| `inventory-service` | 8081 | Inventario real + su proxy |
| `orders-service` | 8082 | Pedidos reales + su proxy |
| `payments-service` | 8083 | Pagos reales + su proxy, con 10% de fallas simuladas |
| `audit-hub` | 8084 | Ledger en memoria, `/api/metrics/*` y simulación de carga |

## El patrón

La interfaz es la que exige el taller:

```java
public interface MicroserviceProxy<T> {
    T execute(String operation, Object... params);
}
```

Cada servicio real la implementa. `LoggingProxy<T>` también la implementa y envuelve al
servicio real, de modo que el controlador no distingue si habla con el sujeto real o con
el proxy:

```java
MicroserviceProxy<Object> inventory =
        new LoggingProxy<>("inventory", new InventoryService(), auditPublisher);
```

Antes de delegar genera un `requestId` UUID y toma el `Instant`. Después registra
`durationMs` (medida solo sobre la llamada real), el resultado serializado y, si falla,
el tipo de error, el mensaje y un resumen del stack trace. La publicación al hub es
asíncrona: una caída del hub no rompe la llamada de negocio.

Las 10% de fallas de Pagos viven dentro de `PaymentService`, no en el proxy. Así el
proxy las registra como `ERROR` con su stack trace, que es exactamente lo que se quiere
observar.

## Endpoints

| Método | Ruta | Descripción |
|---|---|---|
| `POST` | `/api/services/inventory/{operation}` | Pasa por el proxy de Inventario |
| `POST` | `/api/services/orders/{operation}` | Pasa por el proxy de Pedidos |
| `POST` | `/api/services/payments/{operation}` | Pasa por el proxy de Pagos |
| `GET` | `/api/metrics/summary` | Total, errores, tasa de error y promedio por servicio |
| `GET` | `/api/metrics/logs` | Logs filtrados y paginados |
| `POST` | `/api/metrics/simulate-load` | 50 llamadas aleatorias a través de los proxies |
| `POST` | `/internal/audit` | Ingesta interna de los proxies |
| `GET` | `/health` | Health check para Render |

### Operaciones disponibles

- **Inventario:** `checkStock`, `reserveStock`, `releaseStock`, `listItems`
- **Pedidos:** `createOrder`, `getOrder`, `cancelOrder`, `listOrders`
- **Pagos:** `charge`, `refund`, `getPayment`, `listPayments`

### Ejemplos

```bash
curl -X POST http://localhost:8081/api/services/inventory/checkStock \
  -H 'Content-Type: application/json' \
  -d '{"itemId":"SKU-1"}'
```

```json
{"result":{"itemId":"SKU-1","available":120}}
```

```bash
curl -X POST http://localhost:8082/api/services/orders/createOrder \
  -H 'Content-Type: application/json' \
  -d '{"customerId":"CUS-1","items":[{"sku":"SKU-1","quantity":2,"unitPrice":25.5}]}'
```

```bash
curl -X POST http://localhost:8083/api/services/payments/charge \
  -H 'Content-Type: application/json' \
  -d '{"orderId":"ORD-1","amount":51}'
```

```bash
curl http://localhost:8084/api/metrics/summary
curl 'http://localhost:8084/api/metrics/logs?service=payments&status=ERROR&page=0&size=20'
curl -X POST http://localhost:8084/api/metrics/simulate-load
```

### Filtros de logs

`GET /api/metrics/logs` acepta:

| Parámetro | Valores |
|---|---|
| `service` | `inventory`, `orders`, `payments` (vacío = todos) |
| `status` | `SUCCESS`, `ERROR` |
| `from`, `to` | ISO-8601, por ejemplo `2026-10-30T14:00:00Z` |
| `page` | desde 0 |
| `size` | 1 a 100 |

Respuesta:

```json
{
  "content": [
    {
      "requestId": "9f1c...",
      "serviceId": "payments",
      "operation": "charge",
      "durationMs": 8,
      "status": "ERROR",
      "timestamp": "2026-10-30T14:02:11Z",
      "inputParams": {"orderId": "ORD-1", "amount": 51},
      "errorType": "PaymentDeclinedException",
      "errorMessage": "Pago rechazado por el proveedor (falla simulada del 10%).",
      "stackTraceSummary": "..."
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 50,
  "totalPages": 3
}
```

## Ejecución local

Requisitos: Java 21 y Maven 3.9+.

```bash
mvn -DskipTests package

java -jar audit-hub/target/audit-hub-1.0.0.jar &
java -jar inventory-service/target/inventory-service-1.0.0.jar &
java -jar orders-service/target/orders-service-1.0.0.jar &
java -jar payments-service/target/payments-service-1.0.0.jar &
```

Verificación:

```bash
curl http://localhost:8081/health
curl http://localhost:8082/health
curl http://localhost:8083/health
curl http://localhost:8084/health
```

## Pruebas

```bash
mvn test
```

`LoggingProxyTest` cubre el camino exitoso y el camino de error: verifica que se emite un
registro por llamada, que el `requestId` existe, que el estado es `SUCCESS` o `ERROR` y
que el error se vuelve a lanzar con su stack resumido.

## Variables de entorno

| Variable | Servicio | Valor por defecto |
|---|---|---|
| `PORT` | Todos | 8081 / 8082 / 8083 / 8084 |
| `AUDIT_HUB_URL` | Los 3 servicios | `http://localhost:8084` |
| `INVENTORY_URL` | audit-hub | `http://localhost:8081` |
| `ORDERS_URL` | audit-hub | `http://localhost:8082` |
| `PAYMENTS_URL` | audit-hub | `http://localhost:8083` |

## Despliegue en Render

El repositorio incluye `render.yaml` con los cuatro Web Services en plan gratuito y un
`Dockerfile` por servicio.

1. En Render: **New + → Blueprint**.
2. Seleccionar este repositorio (`Jenifrutica/backProxy`).
3. Render crea los cuatro servicios y aplica las variables de entorno.

Los nombres de servicio definen las URLs:

- `https://jenilarper-proxy-inventory.onrender.com`
- `https://jenilarper-proxy-orders.onrender.com`
- `https://jenilarper-proxy-payments.onrender.com`
- `https://jenilarper-proxy-audit.onrender.com`

Los servicios gratuitos se suspenden tras 15 minutos sin tráfico y tardan alrededor de
50 segundos en despertar. Para la demo conviene un monitor externo que los mantenga
calientes y un calentamiento previo.

## Notas

- El ledger es en memoria, con un máximo de 10 000 registros; se pierde al reiniciar.
- `simulate-load` entra por los endpoints públicos, así que cada llamada pasa realmente
  por el `LoggingProxy`; no hay atajos.
- Los logs guardan los parámetros de entrada y la respuesta truncada a 2000 caracteres,
  que es lo que necesita el detalle expandible del panel.
