package org.eclipse.milo.platform.gateway.communication;


import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNodeContext;
import org.eclipse.milo.opcua.sdk.server.nodes.UaObjectNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaVariableNode;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.platform.gateway.GatewayIdentifier;
import org.eclipse.milo.platform.util.LogUtil;
import org.eclipse.milo.platform.util.Utils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

public class RabbitMqCommunication {
    private static String APP_NAME = null;
    private final UaNodeContext uaNodeContext;
    Map<String, Channel> clients = new HashMap();

    public RabbitMqCommunication(UaNodeContext uaNodeContext) {
        APP_NAME = uaNodeContext.getServer().getConfig().getApplicationName().getText();
        this.uaNodeContext = uaNodeContext;

    }

    public void createClient(String deviceName) {
        try {
            final String deviceId = GatewayIdentifier.IO_IDENTIFIER.getIdentifier().replace("{app-name}", APP_NAME).replace("{io-name}", deviceName);
            final String configId = GatewayIdentifier.IO_Property.getIdentifier().replace("{property-name}", "Config").replace("{io-identifier}", deviceId);
            UaObjectNode deviceConfig = (UaObjectNode) this.uaNodeContext.getNodeManager().get(new NodeId(2, configId));

            final String url = Utils.getPropertyValue(deviceConfig, "Property/URL");
            final Channel client = createConnection(url.split(":")[0]);
            this.clients.put(deviceName, client);
        } catch (Exception e) {
            LogUtil.getInstance().logAndFireEvent(getClass(), "error happened creating the RabbitMQCommunication for device : " + deviceName + "With error :" + e.getMessage(), "RABBITMQDEVICE");
        }
    }

    public Map<String, Channel> getClients() {
        return clients;
    }

    public Channel getClient(String deviceName) {
        return clients.get(deviceName);
    }

    public Channel setClient(String deviceName) {
        return clients.get(deviceName);
    }

    private Channel createConnection(String ip) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(ip);
        Connection connection = factory.newConnection();
        return connection.createChannel();
    }

    public void receive(String deviceName, String topic, String topicName) throws IOException, TimeoutException {
        final Channel channel = this.clients.get(deviceName);
        channel.queueDeclare(topic, false, false, false, null);
        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
            final String rabbitMQTagIdentifier = GatewayIdentifier.AMQP_TAG.getIdentifier().replace("{tag-name}", topicName).replace("{io-name}", deviceName).replace("{app-name}", APP_NAME);
            final String rabbitValue = GatewayIdentifier.VALUE_PROPERTY_IDENTIFIER.getIdentifier().replace("{tag-identifier}", rabbitMQTagIdentifier);

            NodeId nodeId = new NodeId(2, rabbitValue);
            DataValue nodeValue = new DataValue(new Variant(message), StatusCode.GOOD);
            UaVariableNode node = (UaVariableNode) uaNodeContext.getNodeManager().get(nodeId);
            node.setValue(nodeValue);
        };
        channel.basicConsume(topic, true, deliverCallback, consumerTag -> {
        });
    }


    public void closeConnection(String deviceName) {
        try {
            clients.get(deviceName).close();
        } catch (Exception e) {
            LogUtil.getInstance().logAndFireEvent(getClass(), "error happened closing the RedisCommunication for device : " + deviceName + " with error : " + e.getMessage(), "RABBITMQDEVICE");

        }
    }
}
