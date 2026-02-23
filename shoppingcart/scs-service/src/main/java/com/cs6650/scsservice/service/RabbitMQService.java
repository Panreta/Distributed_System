package com.cs6650.scsservice.service;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
public class RabbitMQService {

    private final GenericObjectPool<Channel> channelPool;
    private final String queueName;
    private final long confirmTimeoutMs;

    public RabbitMQService(GenericObjectPool<Channel> channelPool,
                           @Value("${rmq.queue}") String queueName,
                           @Value("${rmq.confirm-timeout-ms}") long confirmTimeoutMs) {
        this.channelPool = channelPool;
        this.queueName = queueName;
        this.confirmTimeoutMs = confirmTimeoutMs;

        // Queue setting
        Channel channel = null;
        try {
            channel = channelPool.borrowObject();
            channel.queueDeclare(queueName, true, false, false, null);
            channelPool.returnObject(channel);
//            channel = null;
        } catch (Exception e) {
            if (channel != null) {
                try { channelPool.invalidateObject(channel); } catch (Exception ignore) {}
            }
            throw new RuntimeException("Queue declare failed", e);
        }
    }

    private AMQP.BasicProperties props() {
        return new AMQP.BasicProperties.Builder()
                .contentType("application/json")
                .contentEncoding("utf-8")
                .deliveryMode(2) // persistent
                .build();
    }

    public void publishAndConfirm(String json) {
        Channel channel = null;
        boolean ok = false;
        try {
            channel = channelPool.borrowObject();

            //
            channel.basicPublish("", queueName, props(), json.getBytes(StandardCharsets.UTF_8));

            if (!channel.waitForConfirms(confirmTimeoutMs)) {
                throw new RuntimeException("RabbitMQ did not confirm (nack/timeout)");
            }
            ok = true;
        } catch (Exception e) {
            throw new RuntimeException("RabbitMQ publish failed", e);
        } finally {
            if (channel != null) {
                try {
                    if (ok) channelPool.returnObject(channel);
                    else channelPool.invalidateObject(channel);
                } catch (Exception ignore) {}
            }
        }
    }
}
