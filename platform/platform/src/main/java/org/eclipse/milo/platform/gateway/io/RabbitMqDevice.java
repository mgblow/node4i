package org.eclipse.milo.platform.gateway.io;

import org.eclipse.milo.opcua.sdk.server.nodes.UaNodeContext;
import org.eclipse.milo.opcua.sdk.server.nodes.UaObjectNode;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.platform.gateway.GatewayIdentifier;
import org.eclipse.milo.platform.gateway.communication.RabbitMqCommunication;
import org.eclipse.milo.platform.gateway.interfaces.Device;
import org.eclipse.milo.platform.gateway.interfaces.OpcUaDeviceClient;
import org.eclipse.milo.platform.util.Props;
import org.eclipse.milo.platform.util.Utils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;

public class RabbitMqDevice implements Device, OpcUaDeviceClient {
    private final UaNodeContext uaNodeContext;
    private final RabbitMqCommunication rabbitMqCommunication;
    private Map<String, Thread> subscriptions = new ConcurrentHashMap<>();

    private static String APP_NAME = null;

    public RabbitMqDevice(UaNodeContext uaNodeContext) {
        this.uaNodeContext = uaNodeContext;
        APP_NAME = uaNodeContext.getServer().getConfig().getApplicationName().getText();
        this.rabbitMqCommunication = new RabbitMqCommunication(uaNodeContext);
    }

    @Override
    public void turnOnDevice(String ioName) {
        final String deviceId = GatewayIdentifier.IO_IDENTIFIER.getIdentifier().replace("{app-name}", APP_NAME).replace("{io-name}", ioName);

        try {
            if (rabbitMqCommunication.getClient(ioName) == null) {
                rabbitMqCommunication.createClient(ioName);
            }
            final String attributeId = GatewayIdentifier.IO_Property.getIdentifier().replace("{property-name}", "Attributes").replace("{io-identifier}", deviceId);
            UaObjectNode deviceAttributesNode = (UaObjectNode) this.uaNodeContext.getNodeManager().get(new NodeId(Integer.parseInt(Props.getProperty("app-namespace-index").toString()), attributeId));

            Map<String, String> topicAndTopicNames = new HashMap<>();
            Objects.requireNonNull(deviceAttributesNode).getComponentNodes().forEach(node -> {
                String topic = Utils.getPropertyValue(node, "Property/queueName");
                String topicName = Utils.getPropertyValue(node, "Property/name");
                topicAndTopicNames.put(topicName, topic);
            });
            Thread thread = new Thread(() -> {
                for (Map.Entry<String, String> entry : topicAndTopicNames.entrySet()) {
                    String topic = entry.getValue();
                    String topicName = entry.getKey();
                    try {
                        rabbitMqCommunication.receive(ioName, topic, topicName);
                    } catch (IOException | TimeoutException e) {
                        e.printStackTrace();
                    }
                }
            });
            executor.submit(thread);
            this.subscriptions.put(ioName, thread);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public RabbitMqCommunication getRabbitMqCommunication() {
        return rabbitMqCommunication;
    }

    @Override
    public void turnOffDevice(String ioName) {
        if (this.subscriptions.get(ioName) != null) {
            this.subscriptions.get(ioName).interrupt();
            this.subscriptions.remove(ioName);
            if (rabbitMqCommunication.getClients().get(ioName) != null && rabbitMqCommunication.getClients().get(ioName).isOpen())

                rabbitMqCommunication.getClients().remove(ioName);
        }
    }
}
