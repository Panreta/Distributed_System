# Shopping Cart Service (SCS)

Shopping Cart Service (SCS) is a Spring Boot service that exposes a checkout API, calls an external CCA service synchronously for payment authorization, and publishes order messages to RabbitMQ for downstream processing (e.g., Warehouse Consumer).

## Architecture

```text
ALB ──HTTP──▶  SCS ── HTTP ──▶  CCA (authorize)
                    │
                    └──AMQP──▶  RabbitMQ  ──▶  WareHouse
                               (orders_queue)          

```

## What It Does

- Exposes a simple HTTP health endpoint for load balancers. 
- Accepts checkout requests and validates input (non-empty cart). 
- Calls CCA synchronously via `ShoppingService.processCheckout(...)` to obtain a `PaymentDecision`. 
- Publishes an `OrderMessage` to RabbitMQ for warehouse . 

## API Endpoints 

Base path: `/shopping-cart` ShoppingCartController

### 1) Health Check (for ALB)

- `GET /shopping-cart/health`
- Response: `200 OK`, `text/plain` body: `OK` 

### 2) Checkout

- `POST /shopping-cart/checkout`
- Consumes: `application/json`
- Produces: `text/plain` 

#### Request (example)

> Exact DTO fields depend on `CheckoutRequest`, but it must contain a non-empty `items` list. 

```
{
  "items": [
    { "productId": "p1", "quantity": 2 },
    { "productId": "p2", "quantity": 2 }
  ]
}
```

#### Responses (mapped from `PaymentDecision`)

- `200 OK` — `Order Placed Successfully` (AUTHORIZED) ShoppingCartController
- `402 Payment Required` — `Payment Declined` (DECLINED) ShoppingCartController
- `400 Bad Request` — `Invalid Credit Card` (INVALID_FORMAT) ShoppingCartController
- `503 Service Unavailable` — `Payment System Unavailable` (PAYMENT_SYSTEM_UNAVAILABLE) ShoppingCartController
- `400 Bad Request` — `Cart is empty` (items missing/empty) ShoppingCartController

## RabbitMQ Message Contract

Messages in this JSON :

```
{
  "orderId":"c1-1771374070480",
  "cartId":"c1",
  "items":[
    {"productId":"p1","quantity":2},
    {"productId":"p2","quantity":2}
  ]
}
```

Queue name used by the consumer side is `orders_queue` (SCS must publish to the same queue). 

## RabbitMQ Integration Notes

### Factory vs Channel (conceptual)

- **Connection factory** holds connection parameters and creates an AMQP **Connection**.
- **Channel** is created on top of a Connection and is where you do nearly all AMQP operations:
  - declare exchange/queue/bindings
  - publish (`basicPublish`)
  - consume (`basicConsume`) and ACK/NACK
- Best practice: keep **few Connections** and use **multiple Channels** (channels are lighter than connections).
   Also: channels are typically **not thread-safe**—prefer one channel per thread/consumer or use a channel pool.

### How this repo applies it

- `RMQConfig` centralizes RabbitMQ connection properties and channel-related settings.
- `RMQChannelFactory` maintains a long-lived AMQP **Connection** and creates/reuses **Channels** to reduce connection churn (TCP handshake overhead) and improve throughput.
- `RabbitMQService` encapsulates publish (and any consume/listen logic you add later), keeping RabbitMQ client calls out of controllers/services.

## Tech Stack

- Java 17
- Spring Boot
- RabbitMQ Java Client (AMQP)
- Jackson (customized via `JacksonConfig` for message serialization)
- Maven

## Core Project Structure

```
scsservice/
├── src/main/java/com/cs6650/scsservice/
│   ├── config/
│   │   ├── JacksonConfig.java        # Configures ObjectMapper. RabbitMQ traffic is non-HTTP, so custom serialization is needed.
│   │   ├── RestTemplateConfig.java   # Customizes the HTTP client (e.g., sets timeout limits, etc.).
│   │   └── RMQConfig.java            # Manages basic RabbitMQ connection properties, channel settings, etc.
│   ├── controller/
│   │   └── ShoppingCartController.java  # Entry point for the shopping cart business APIs.
│   ├── dto/
│   │   ├── CartItem.java             # Data model for a single cart item.
│   │   └── CheckoutRequest.java      # Request payload structure for checkout.
│   ├── model/
│   │   ├── OrderMessage.java         # Order message entity.
│   │   └── PaymentDecision.java      # CCA payment status enum.
│   ├── rmq/
│   │   └── RMQChannelFactory.java    # Connection factory. Channels are reused on top of it to reduce TCP handshake overhead.
│   ├── service/
│   │   ├── RabbitMQService.java      # Implements message queue producer and consumer logic.
│   │   └── ShoppingService.java      # Handles synchronous communication with the external CCA system.
│   └── ScsServiceApplication.java    # Application bootstrap class.
├── src/main/resources/
│   └── application.properties        # Environment configuration.
└── pom.xml                           # Dependency management.
```

## Configuration (`application.properties`)

This service is configured via `application.properties` with environment-variable overrides.

### Server

| Property      | Env Override | Default |
| ------------- | ------------ | ------- |
| `server.port` | `SCS_PORT`   | `8081`  |

### CCA 

| Property                 | Env Override             | Default                  | Notes                |
| ------------------------ | ------------------------ | ------------------------ | -------------------- |
| `cca.base-url`           | `CCA_BASE_URL`           | `http://localhost:8080`  | Base URL             |
| `cca.authorize-path`     | (N/A)                    | `/credit-card/authorize` | Endpoint path        |
| `cca.connect-timeout-ms` | `CCA_CONNECT_TIMEOUT_MS` | `200`                    | Connect timeout (ms) |
| `cca.read-timeout-ms`    | `CCA_READ_TIMEOUT_MS`    | `800`                    | Read timeout (ms)    |

### RabbitMQ

| Property                 | Env Override             | Default        |
| ------------------------ | ------------------------ | -------------- |
| `rmq.host`               | `RMQ_HOST`               | `localhost`    |
| `rmq.port`               | `RMQ_PORT`               | `5672`         |
| `rmq.username`           | `RMQ_USER`               | `guest`        |
| `rmq.password`           | `RMQ_PASS`               | `guest`        |
| `rmq.vhost`              | `RMQ_VHOST`              | `/`            |
| `rmq.queue`              | `RMQ_QUEUE`              | `orders_queue` |
| `rmq.confirm-timeout-ms` | `RMQ_CONFIRM_TIMEOUT_MS` | `5000`         |
| `rmq.pool.max-total`     | `RMQ_POOL_MAX_TOTAL`     | `50`           |
| `rmq.pool.max-wait-ms`   | `RMQ_POOL_MAX_WAIT_MS`   | `1000`         |

### Misc

- `spring.jmx.enabled=false` (avoids JMX conflicts)

## Local Development

### Prerequisites

- Java 17
- Maven
- Docker (for RabbitMQ)

### Run RabbitMQ locally

```
docker run --rm -it \
  -p 5672:5672 -p 15672:15672 \
  --name rabbitmq \
  rabbitmq:3-management

# http://localhost:15672  (guest/guest)
```

### Run SCS

```
mvn clean package -DskipTests
mvn spring-boot:run
```

Default base URL: `http://localhost:8081`

### Test

Health:

```
curl -i http://localhost:8081/shopping-cart/health
```

Checkout:

```
curl -i -X POST "http://localhost:8081/shopping-cart/checkout" \
  -H "Content-Type: application/json" \
  -d '{"items":[{"productId":"p1","quantity":2},{"productId":"p2","quantity":2}]}'
```

### Overriding config with environment variables (example)

```
export SCS_PORT=8081
export CCA_BASE_URL=http://localhost:8080
export RMQ_HOST=localhost
export RMQ_QUEUE=orders_queue

mvn spring-boot:run
```

## Team Checklist

- **Queue name**: `orders_queue` — must match what the consumer is reading. 
- **Message schema**: must match the consumer’s expected JSON structure. 
- **RabbitMQ broker address**: ensure all services point to the same broker host/VPC endpoint in AWS.