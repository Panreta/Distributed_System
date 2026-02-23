package com.cs6650.scsservice.config;

import com.cs6650.scsservice.rmq.RMQChannelFactory;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConnectionFactory;
// thread pools
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class RMQConfig {

    // read settings from application.properties
    @Value("${rmq.host}") private String host;
    @Value("${rmq.port}") private int port;
    @Value("${rmq.username}") private String username;
    @Value("${rmq.password}") private String password;
    @Value("${rmq.vhost}") private String vhost;

    @Value("${rmq.pool.max-total}") private int maxTotal;
    @Value("${rmq.pool.max-wait-ms}") private long maxWaitMs;

    @Bean(destroyMethod = "close")
    public GenericObjectPool<Channel> rabbitMQChannelPool() {
        try {
            ConnectionFactory factory = new ConnectionFactory();
            // set basic RabbitMQ factory
            factory.setHost(host);
            factory.setPort(port);
            factory.setUsername(username);
            factory.setPassword(password);
            factory.setVirtualHost(vhost);

            // Stable improvement
            // Enable automatic connection recovery
            factory.setAutomaticRecoveryEnabled(true);
            // 2 seconds to reconnect
            factory.setNetworkRecoveryInterval(2000);
            factory.setRequestedHeartbeat(30);

            // Non-blocking I/O to improve
            factory.useNio();

            // set Pool config
            RMQChannelFactory channelFactory = new RMQChannelFactory(factory);

            GenericObjectPoolConfig<Channel> cfg = new GenericObjectPoolConfig<>();
            // thread numbers, block setting, wait time
            cfg.setMaxTotal(maxTotal);
            cfg.setBlockWhenExhausted(true);
            cfg.setMaxWait(Duration.ofMillis(maxWaitMs));

            // check the net-connecting about pool after borrow
            cfg.setTestOnBorrow(true);

            // Improve performance
            cfg.setJmxEnabled(false);


            return new GenericObjectPool<>(channelFactory, cfg);

        } catch (Exception e) {// Fail Fast
            throw new RuntimeException("Failed to init RabbitMQ channel pool", e);
        }
    }
}

