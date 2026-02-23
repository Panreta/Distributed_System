# Warehouse Consumer

RabbitMQ consumer that simulates a warehouse receiving shipping orders. Part of the CS6650 Assignment 3 distributed system.

## Architecture

```
Shopping Cart Service  ──▶  RabbitMQ Broker  ──▶  Warehouse Consumer (this)
     (publisher)              (queue)              (consumer)
```


## What It Does

- Consumes order messages from `orders_queue` (RabbitMQ broker)
- Tracks total quantity ordered per product ID (thread-safe)
- Tracks total number of orders
- Prints total order count on shutdown (for grading screenshot)

## Message Format

Expected JSON format:
```json
{
   "orderId":orderId,
   "cartId":cartId,
   "items":[
      {"productId":productId,"quantity":quantity},
      {"productId":productId,"quantity":quantity}
   ]
}
```

Example:
```json
{
  "orderId":"c1-1771374070480",
  "cartId":"c1",
  "items":[
    {"productId":"p1","quantity":2},
    {"productId":"p2","quantity":2}
  ]
}
```

## Assignment Requirements Met

| Requirement | How |
|---|---|
| Manual Consumer Acknowledgements | `basicConsume(queue, autoAck=false)` + explicit `basicAck()` |
| Multithreaded consumption | Configurable N threads, each with own channel |
| Thread-safe shared data | `ConcurrentHashMap<String, AtomicInteger>` + `AtomicInteger` |
| Count total orders | `AtomicInteger.incrementAndGet()` per message |
| Count quantity per productId | `computeIfAbsent().addAndGet()` |
| Print totals on shutdown | JVM shutdown hook via `Runtime.addShutdownHook()` |
| Own container | Multi-stage Dockerfile, deployed as ECS Fargate task |

## Tech Stack

- Java 17 (plain — no Spring Boot)
- RabbitMQ Java Client 5.20.0
- Gson 2.10.1
- SLF4J + Logback
- Maven (shade plugin for fat JAR)
- Docker (multi-stage build)
- Terraform (AWS ECS Fargate deployment)

## Project Structure

```
warehouse-consumer/
├── src/main/java/warehouse/
│   └── WarehouseConsumer.java    # All consumer logic
├── src/main/resources/
│   └── logback.xml               # Logging config
├── pom.xml                       # Maven dependencies + shade plugin
├── Dockerfile                    # Multi-stage build
├── docker-compose.yml            # Local testing with RabbitMQ
├── deploy.sh                     # Build → push → redeploy to ECS
├── terraform/
│   ├── main.tf                   # Provider, VPC/subnet data sources
│   ├── variables.tf              # All configurable inputs
│   ├── terraform.tfvars          # Your deployment values
│   ├── ecr.tf                    # ECR repository
│   ├── ecs.tf                    # Cluster, task def, service, security group
│   └── outputs.tf                # ECR URL, log group, CLI commands
└── README.md
```

## Configuration

All configuration is via environment variables (set in `docker-compose.yml` for local, `terraform.tfvars` for AWS):

| Variable | Default | Description |
|---|---|---|
| `RABBITMQ_HOST` | `localhost` | RabbitMQ broker hostname |
| `RABBITMQ_PORT` | `5672` | AMQP port |
| `RABBITMQ_USER` | `guest` | RabbitMQ username |
| `RABBITMQ_PASS` | `guest` | RabbitMQ password |
| `NUM_CONSUMER_THREADS` | `10` | Number of consumer threads |
| `PREFETCH_COUNT` | `10` | Messages per thread before ack required |
| `RABBITMQ_RETRY_COUNT` | `30` | Number of connection retries |
| `RABBITMQ_RETRY_WAIT_S` | `2` | Seconds to wait between retries |

## Local Development

### Prerequisites

- Java 17
- Maven
- Docker + Docker Compose

### Run locally

```bash
# Start RabbitMQ + consumer
docker-compose up --build

# RabbitMQ Management UI
open http://localhost:15672   # guest/guest
```

The consumer will sit idle until messages appear on `orders_queue`. You can publish test messages via the RabbitMQ Management UI or a separate producer script.

### Build without Docker

```bash
mvn clean package -DskipTests
java -jar target/warehouse-consumer-1.0-SNAPSHOT.jar
```

## AWS Deployment

See [DEPLOY.md](./DEPLOY.md) for step-by-step AWS deployment instructions.

### Quick deploy (after initial setup)

```bash
./deploy.sh
```

## Load Test Tuning

During the 200,000 checkout load test, adjust these in `terraform.tfvars` and redeploy:

```hcl
num_consumer_threads = 50      # start at 10, increase if queue grows
prefetch_count       = 30      # start at 10, try up to 50-100
task_cpu             = "1024"  # bump if CPU-bound
task_memory          = "2048"
rabbitmq_retry_count = 60      # increase if RabbitMQ takes >1min to start
rabbitmq_retry_wait_s = 2
```

Goal: keep the RabbitMQ queue length near zero (under 1,000 messages). Monitor via the RabbitMQ Management Console.

## Shutdown & Screenshot

To capture the required shutdown screenshot:

```bash
# Stop the ECS task
aws ecs update-service --cluster cs6650-cluster --service warehouse-consumer --desired-count 0 --region us-west-2

# View the shutdown output in CloudWatch
aws logs tail /ecs/warehouse-consumer --region us-west-2 | tail -20
```

Output will show:
```
========================================
Total number of orders: 200000
========================================
```

## Team Coordination

- **Queue name**: `orders_queue` — must match what the Shopping Cart Service publishes to
- **Message format**: see above — must match what the SCS sends
- **Cluster name**: set in `terraform.tfvars` — coordinate with team so all services use the same cluster
- **RabbitMQ host**: update `terraform.tfvars` with the broker's private IP after RabbitMQ is deployed
