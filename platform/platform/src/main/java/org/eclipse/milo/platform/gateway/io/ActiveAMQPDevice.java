package org.eclipse.milo.platform.gateway.io;


import org.eclipse.milo.opcua.sdk.server.nodes.UaNodeContext;
import org.eclipse.milo.opcua.sdk.server.nodes.UaObjectNode;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.platform.gateway.GatewayIdentifier;
import org.eclipse.milo.platform.gateway.communication.ActiveMQCommunication;
import org.eclipse.milo.platform.gateway.interfaces.Device;
import org.eclipse.milo.platform.gateway.interfaces.OpcUaDeviceClient;
import org.eclipse.milo.platform.util.LogUtil;
import org.eclipse.milo.platform.util.Props;
import org.eclipse.milo.platform.util.Utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class ActiveAMQPDevice implements Device, OpcUaDeviceClient {
    private final UaNodeContext uaNodeContext;
    private final ActiveMQCommunication activeMqCommunication;
    private Map<String, Thread> subscriptions = new ConcurrentHashMap<>();

    private static String APP_NAME = null;

    public ActiveAMQPDevice(UaNodeContext uaNodeContext) {
        this.uaNodeContext = uaNodeContext;
        APP_NAME = uaNodeContext.getServer().getConfig().getApplicationName().getText();
        this.activeMqCommunication = new ActiveMQCommunication(uaNodeContext);
    }

    @Override
    public void turnOnDevice(String ioName) {
        try {
            final String deviceId = GatewayIdentifier.IO_IDENTIFIER.getIdentifier().replace("{app-name}", APP_NAME).replace("{io-name}", ioName);
            final String configId = GatewayIdentifier.IO_Property.getIdentifier().replace("{property-name}", "Config").replace("{io-identifier}", deviceId);
            UaObjectNode deviceConfig = (UaObjectNode) this.uaNodeContext.getNodeManager().get(new NodeId(2, configId));


            final String url = Utils.getPropertyValue(deviceConfig, "Property/URL");

            final String attributeId = GatewayIdentifier.IO_Property.getIdentifier().replace("{property-name}", "Attributes").replace("{io-identifier}", deviceId);
            UaObjectNode deviceAttributesNode = (UaObjectNode) this.uaNodeContext.getNodeManager().get(new NodeId(Integer.parseInt(Props.getProperty("app-namespace-index").toString()), attributeId));


            Map<String, String> topicAndTopicNames = new HashMap<>();
            Objects.requireNonNull(deviceAttributesNode).getComponentNodes().forEach(node -> {
                String topic = Utils.getPropertyValue(node, "Property/queueName");
                String topicName = Utils.getPropertyValue((UaObjectNode) node, "Property/name");
                topicAndTopicNames.put(topicName, topic);
            });
            activeMqCommunication.createConnection(url);

            Thread thread = new Thread(() -> {
                for (String topicName : topicAndTopicNames.keySet()) {
                    String queue = topicAndTopicNames.get(topicName);
                    try {
                        activeMqCommunication.receive(ioName, queue, topicName);
                    } catch (Exception e) {
                        LogUtil.getInstance().logAndFireEvent(getClass(),"error occurred in turning off activemq device " + ioName + ",message: " + e.getMessage(),"ACTIVEMQDEVICE");
                    }

                }
            });
            executor.submit(thread);
            this.subscriptions.put(ioName, thread);
        } catch (Exception e) {
            LogUtil.getInstance().logAndFireEvent(getClass(),"error occurred in turning off activemq device " + ioName + ",message: " + e.getMessage(),"ACTIVEMQDEVICE");
        }

    }

    @Override
    public void turnOffDevice(String ioName) {
        final String deviceId = GatewayIdentifier.IO_IDENTIFIER.getIdentifier().replace("{app-name}", APP_NAME).replace("{io-name}", ioName);
        final String configId = GatewayIdentifier.IO_Property.getIdentifier().replace("{property-name}", "Config").replace("{io-identifier}", deviceId);
        UaObjectNode deviceConfig = (UaObjectNode) this.uaNodeContext.getNodeManager().get(new NodeId(2, configId));

        String url = Utils.getPropertyValue(deviceConfig, "Property/URL");
        if (this.subscriptions.get(ioName) != null) {
            this.subscriptions.remove(ioName);
            try {
                if (activeMqCommunication.getActiveMqConnectionPool().get(url) != null)
                    activeMqCommunication.getActiveMqConnectionPool().get(url).clear();
                activeMqCommunication.getActiveMqConnectionPool().remove(url);
            } catch (Exception e) {
                LogUtil.getInstance().logAndFireEvent(getClass(),"error occurred in turning off activemq device " + ioName + ",message: " + e.getMessage(),"ACTIVEMQDEVICE");
            }
        }
    }

    @Override
    public void restartDevice(String deviceName) {
        this.turnOffDevice(deviceName);
        this.turnOnDevice(deviceName);
    }

    public ActiveMQCommunication getActiveMqCommunication() {
        return activeMqCommunication;
    }
}

