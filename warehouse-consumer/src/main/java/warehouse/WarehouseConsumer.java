package warehouse;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Warehouse Consumer - receives order messages from RabbitMQ and tracks:
 * 1. Total quantity ordered per product ID
 * 2. Total number of orders
 *
 * Uses manual acknowledgements and multithreaded consumption.
 */
public class WarehouseConsumer {

    private static final Logger logger = LoggerFactory.getLogger(WarehouseConsumer.class);

    private final int numThreads;
    private final int prefetchCount;
    private final String queueName;
    private final ConnectionFactory factory;

    // Thread-safe counters
    private final AtomicInteger totalOrders = new AtomicInteger(0);

    // ConcurrentHashMap: Thread-safe map for tracking quantity per product.
    // Key = productId (String), Value = AtomicInteger (thread-safe counter)
    private final ConcurrentHashMap<String, AtomicInteger> productQuantities = new ConcurrentHashMap<>();

    // Gson instance for JSON parsing.
    private final Gson gson = new Gson();

    /**
     * Constructs a new WarehouseConsumer with the specified configuration.
     *
     * @param numThreads    Number of consumer threads to spawn
     * @param prefetchCount RabbitMQ prefetch count (QoS)
     * @param queueName     Name of the queue to consume from
     * @param factory       Configured RabbitMQ ConnectionFactory
     */
    public WarehouseConsumer(int numThreads, int prefetchCount, String queueName, ConnectionFactory factory) {
        this.numThreads = numThreads;
        this.prefetchCount = prefetchCount;
        this.queueName = queueName;
        this.factory = factory;
    }

    /**
     * Main entry point for the Warehouse Consumer application.
     * Reads configuration from environment variables and starts the consumer.
     *
     * @param args Command line arguments (unused)
     * @throws Exception if an error occurs during startup
     */
    public static void main(String[] args) throws Exception {
        // Load configuration from environment variables
        int numThreads = Integer.parseInt(System.getenv().getOrDefault("NUM_CONSUMER_THREADS", "10"));
        int prefetchCount = Integer.parseInt(System.getenv().getOrDefault("PREFETCH_COUNT", "10"));
        String queueName = System.getenv().getOrDefault("QUEUE_NAME", "orders_queue");

        String rabbitHost = System.getenv().getOrDefault("RABBITMQ_HOST", "localhost");
        int rabbitPort = Integer.parseInt(System.getenv().getOrDefault("RABBITMQ_PORT", "5672"));
        String rabbitUser = System.getenv().getOrDefault("RABBITMQ_USER", "guest");
        String rabbitPass = System.getenv().getOrDefault("RABBITMQ_PASS", "guest");

        // Create RabbitMQ Connection Factory
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(rabbitHost);
        factory.setPort(rabbitPort);
        factory.setUsername(rabbitUser);
        factory.setPassword(rabbitPass);

        // Instantiate and start consumer
        WarehouseConsumer consumer = new WarehouseConsumer(numThreads, prefetchCount, queueName, factory);
        consumer.start();
    }

    /**
     * Starts the consumer threads and connection.
     * Blocks the current thread until the application is stopped.
     *
     * @throws Exception if connection fails
     */
    public void start() throws Exception {
        logger.info("Warehouse Consumer starting - threads={}, prefetch={}", numThreads, prefetchCount);
        logger.info("Connecting to RabbitMQ at {}:{}", factory.getHost(), factory.getPort());

        // Register shutdown hook to print totals
        Runtime.getRuntime().addShutdownHook(new Thread(this::printTotals));

        // Retry connection - RabbitMQ may not be ready when ECS starts this container
        Connection connection = createConnectionWithRetry();
        logger.info("Connected to RabbitMQ successfully");

        // Start consumer threads - each thread gets its own channel
        for (int i = 0; i < numThreads; i++) {
            final int threadNum = i;
            Thread thread = new Thread(() -> {
                try {
                    consume(connection, threadNum);
                } catch (IOException e) {
                    logger.error("Consumer thread {} failed", threadNum, e);
                }
            });
            // Daemon threads don't prevent JVM shutdown.
            thread.setDaemon(true);
            thread.start();
        }

        logger.info("All {} consumer threads started. Waiting for messages...", numThreads);

        // Block main thread - container stays alive until stopped
        Thread.currentThread().join();
    }

    /**
     * Sets up a RabbitMQ consumer on its own Channel.
     * Called once per thread — the DeliverCallback runs asynchronously
     * whenever a message arrives on that channel.
     *
     * @param connection Shared TCP connection to RabbitMQ
     * @param threadNum  Thread identifier for logging
     * @throws IOException if channel creation fails
     */
    private void consume(Connection connection, int threadNum) throws IOException {
        Channel channel = connection.createChannel();
        channel.queueDeclare(queueName, true, false, false, null);
        channel.basicQos(prefetchCount);

        logger.info("Consumer thread {} ready, prefetch={}", threadNum, prefetchCount);

        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            long deliveryTag = delivery.getEnvelope().getDeliveryTag();
            try {
                String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
                // Process the order/message (updates our counters)
                processOrder(message);
                // MANUAL ACKNOWLEDGEMENT - after processing.
                channel.basicAck(deliveryTag, false);
            } catch (Exception e) {
                logger.error("Error processing message, nacking", e);
                // Reject and requeue on failure
                channel.basicNack(deliveryTag, false, true);
            }
        };

        // Start consuming
        channel.basicConsume(queueName, false, deliverCallback, consumerTag -> {
            logger.warn("Consumer {} cancelled", consumerTag);
        });
    }

    /**
     * Parse the order JSON and update thread-safe counters.
     * Expected format:
     * {
     * "orderId": "c1-1771374070480",
     * "cartId": "c1",
     * "items": [
     * {"productId": "p1", "quantity": 2},
     * {"productId": "p2", "quantity": 2}
     * ]
     * }
     *
     * @param message Raw JSON string from RabbitMQ
     */
    private void processOrder(String message) {
        JsonObject order = gson.fromJson(message, JsonObject.class);
        JsonArray items = order.getAsJsonArray("items");

        logger.info(">>> Received order: {}", order.get("cartId").getAsString());

        Integer ORDER_COUNT_INTERVAL = 10000;

        for (JsonElement element : items) {
            JsonObject item = element.getAsJsonObject();
            String productId = item.get("productId").getAsString();
            int quantity = item.get("quantity").getAsInt();

            // Thread-safe: computeIfAbsent + addAndGet are both atomic
            productQuantities
                    .computeIfAbsent(productId, k -> new AtomicInteger(0))
                    .addAndGet(quantity);
        }

        int orderCount = totalOrders.incrementAndGet();

        // Progress logging every 10,000 orders.
        if (orderCount % ORDER_COUNT_INTERVAL == 0) {
            logger.info("Processed {} orders so far", orderCount);
        }
    }

    /**
     * Retry connection to RabbitMQ with backoff.
     * Handles ECS timing where RabbitMQ container may not be ready yet.
     *
     * @return An open Connection to RabbitMQ
     * @throws RuntimeException if all retries are exhausted
     */
    private Connection createConnectionWithRetry() {
        int RABBITMQ_RETRY_COUNT_DEFAULT = 30;
        int RABBITMQ_RETRY_WAIT_S_DEFAULT = 2;
        int maxRetries = Integer.parseInt(
                System.getenv().getOrDefault("RABBITMQ_RETRY_COUNT", String.valueOf(RABBITMQ_RETRY_COUNT_DEFAULT)));
        int waitSeconds = Integer.parseInt(
                System.getenv().getOrDefault("RABBITMQ_RETRY_WAIT_S", String.valueOf(RABBITMQ_RETRY_WAIT_S_DEFAULT)));
        for (int i = 1; i <= maxRetries; i++) {
            try {
                return factory.newConnection();
            } catch (Exception e) {
                logger.warn("Connection attempt {}/{} failed: {}. Retrying in {}s...",
                        i, maxRetries, e.getMessage(), waitSeconds);
                try {
                    Thread.sleep(waitSeconds * 1000L);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted during connection retry", ie);
                }
            }
        }
        throw new RuntimeException("Failed to connect to RabbitMQ after " + maxRetries + " attempts");
    }

    /**
     * Called on shutdown - prints the total number of orders.
     * Triggered by: docker stop (SIGTERM), ECS task stop, or Ctrl+C.
     */
    private void printTotals() {
        logger.info("=== Warehouse Consumer Shutting Down ===");
        logger.info("Total number of orders: {}", totalOrders.get());
        logger.info("Unique products tracked: {}", productQuantities.size());

        // Also print to stdout to ensure it appears in container logs / screenshots
        System.out.println("========================================");
        System.out.println("Total number of orders: " + totalOrders.get());
        System.out.println("========================================");
    }
}
