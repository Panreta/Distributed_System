package com.cs6650.scsservice.rmq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;

public class RMQChannelFactory extends BasePooledObjectFactory<Channel> implements AutoCloseable {

    // final connection to limit resource cost
    private final Connection connection;

    // channels reuse only one connection
    public RMQChannelFactory(ConnectionFactory factory) throws Exception {
        this.connection = factory.newConnection(); // TCP connection
    }

    @Override
    public Channel create() throws Exception {
        Channel ch = connection.createChannel();
        // Publisher confirm
        ch.confirmSelect();
        return ch;
    }

    // Wrap channel
    @Override
    public PooledObject<Channel> wrap(Channel channel) {
        return new DefaultPooledObject<>(channel);
    }

    // We set method setTestOnBorrow(RMQconfig), here can check channel still open or not
    @Override
    public boolean validateObject(PooledObject<Channel> p) {
        Channel ch = p.getObject();
        return ch != null && ch.isOpen();
    }

    // destroy channel
    @Override
    public void destroyObject(PooledObject<Channel> p) throws Exception {
        Channel ch = p.getObject();
        if (ch != null && ch.isOpen()) ch.close();
    }

    //close factory
    @Override
    public void close() throws Exception {
        if (connection != null && connection.isOpen()) connection.close();
    }
}
