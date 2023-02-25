package org.eclipse.milo.platform.gateway.communication;

import org.apache.activemq.pool.PooledConnectionFactory;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNodeContext;
import org.eclipse.milo.opcua.sdk.server.nodes.UaObjectNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaVariableNode;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.platform.gateway.GatewayIdentifier;
import org.eclipse.milo.platform.util.Utils;

import javax.jms.*;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import static org.eclipse.milo.platform.gateway.interfaces.Device.logger;

public class ActiveMQCommunication {
    private static String APP_NAME = null;
    private final UaNodeContext uaNodeContext;
    Map<String, PooledConnectionFactory> activeMqConnectionPool;

    public ActiveMQCommunication(UaNodeContext uaNodeContext) {
        APP_NAME = uaNodeContext.getServer().getConfig().getApplicationName().getText();
        this.uaNodeContext = uaNodeContext;
        activeMqConnectionPool = new HashMap();

    }

    public Map<String, PooledConnectionFactory> getActiveMqConnectionPool() {
        return activeMqConnectionPool;
    }

    public Connection createConnection(String url) throws Exception {
        if (activeMqConnectionPool.isEmpty() || !activeMqConnectionPool.containsKey(url)) {
            PooledConnectionFactory connectionFactory = new PooledConnectionFactory(url);
            activeMqConnectionPool.put(url, connectionFactory);
        }
        final javax.jms.Connection connection = activeMqConnectionPool.get(url).createConnection();
        return connection;

    }

    public void receive(String deviceName, String queue, String topicName) throws IOException, TimeoutException, JMSException {
        final String deviceId = GatewayIdentifier.IO_IDENTIFIER.getIdentifier().replace("{app-name}", APP_NAME).replace("{io-name}", deviceName);
        final String configId = GatewayIdentifier.IO_Property.getIdentifier().replace("{property-name}", "Config").replace("{io-identifier}", deviceId);
        UaObjectNode deviceConfig = (UaObjectNode) this.uaNodeContext.getNodeManager().get(new NodeId(2, configId));

        final String url = Utils.getPropertyValue(deviceConfig, "Property/URL");
        final PooledConnectionFactory connectionFactory = this.activeMqConnectionPool.get(url);
        final Connection connection = connectionFactory.createConnection();
        connection.start();
        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Destination destination = session.createQueue(queue);
        MessageConsumer consumer = session.createConsumer(destination);

        final String tagId = GatewayIdentifier.AMQP_TAG.getIdentifier().replace("{app-name}", APP_NAME).replace("{io-name}", deviceName).replace("{tag-name}", topicName);
        final String valueId = GatewayIdentifier.VALUE_PROPERTY_IDENTIFIER.getIdentifier().replace("{tag-identifier}", tagId);

        NodeId nodeId = Utils.newNodeId(2, valueId);
        UaVariableNode node = (UaVariableNode) uaNodeContext.getNodeManager().get(nodeId);
        consumer.setMessageListener(new ConsumerMessageListener(queue, node));
    }


    public void closeConnection(String deviceName) {
        try {
            activeMqConnectionPool.remove(deviceName);
        } catch (Exception e) {
            logger.error(String.format("error happened closing the RedisCommunication for device : %s with error : %s", deviceName, e.getMessage()));
        }
    }

}

