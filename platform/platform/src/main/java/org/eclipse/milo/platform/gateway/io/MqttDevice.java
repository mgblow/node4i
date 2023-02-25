package org.eclipse.milo.platform.gateway.io;

import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient;
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client;
import com.hivemq.client.mqtt.mqtt5.Mqtt5ClientBuilder;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNodeContext;
import org.eclipse.milo.opcua.sdk.server.nodes.UaObjectNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaVariableNode;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.platform.gateway.GatewayIdentifier;
import org.eclipse.milo.platform.gateway.interfaces.Device;
import org.eclipse.milo.platform.util.LogUtil;
import org.eclipse.milo.platform.util.Props;
import org.eclipse.milo.platform.util.Utils;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class MqttDevice implements Device {

    private static String APP_NAME = null;
    private UaNodeContext uaNodeContext;
    private Map<String, Mqtt5AsyncClient> clients = new ConcurrentHashMap<>();
    private Map<String, Mqtt5AsyncClient> writeClients = new ConcurrentHashMap<>();
    private Map<String, Thread> subscriptions = new HashMap<>();

    public MqttDevice(UaNodeContext uaNodeContext) {

        this.uaNodeContext = uaNodeContext;
        APP_NAME = uaNodeContext.getServer().getConfig().getApplicationName().getText();
    }

    @Override
    public void turnOnDevice(String ioName) {
        try {
            if (this.clients.get(ioName) == null) {
                this.addDeviceClient(ioName);
            } else {
                this.turnOffDevice(ioName);
            }

            String deviceId = GatewayIdentifier.IO_IDENTIFIER.getIdentifier().replace("{app-name}", APP_NAME).replace("{io-name}", ioName);
            final String attributeProperty = GatewayIdentifier.IO_Property.getIdentifier().replace("{property-name}", "Attributes").replace("{io-identifier}", deviceId);
            UaObjectNode deviceAttributesNode = (UaObjectNode) this.uaNodeContext.getNodeManager().get(new NodeId(Integer.parseInt(Props.getProperty("app-namespace-index").toString()), attributeProperty));

            HashMap<String, String> nameAndtopics = new HashMap();
            deviceAttributesNode.getComponentNodes().forEach(node -> {
                String topic = Utils.getPropertyValue(node, "Property/topic");
                String name = Utils.getPropertyValue(node, "Property/name");
                nameAndtopics.put(name, topic);
            });
            Thread thread = new Thread(() -> {
                for (String name : nameAndtopics.keySet()) {
                    final String topic = nameAndtopics.get(name);
                    CompletableFuture.runAsync(() -> {
                        clients.get(ioName).toAsync().subscribeWith().topicFilter(topic).qos(MqttQos.EXACTLY_ONCE).callback(publish -> {
                            String message = new String(publish.getPayloadAsBytes(), StandardCharsets.UTF_8);
                            try {
                                String tagIdentifier = GatewayIdentifier.MQTT_TAG.getIdentifier().replace("{app-name}", APP_NAME).replace("{io-name}", ioName).replace("{tag-name}",name);
                                final String valueId = GatewayIdentifier.VALUE_PROPERTY_IDENTIFIER.getIdentifier().replace("{tag-identifier}", tagIdentifier);
                                NodeId nodeId = Utils.newNodeId(valueId);
                                DataValue nodeValue = new DataValue(new Variant(message), StatusCode.GOOD);
                                UaVariableNode node = (UaVariableNode) uaNodeContext.getNodeManager().get(nodeId);
                                if (node == null) {
                                    logger.error("no definition in topic : " + topic);
                                } else {
                                    node.setValue(nodeValue);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }).send();
                    });
                }
            });
            executor.submit(thread);
            this.subscriptions.put(ioName, thread);
        } catch (Exception e) {
            LogUtil.getInstance().logAndFireEvent(getClass(),"error happened turning device : "+ ioName +" [ON], error : " + e.getMessage(),"MQTTDEVICE");
        }
    }

    @Override
    public void turnOffDevice(String ioName) {
        try {
            if (this.clients.get(ioName) != null) {
                if (this.subscriptions.get(ioName) != null) {
                    this.subscriptions.get(ioName).interrupt();
                    this.subscriptions.remove(ioName);
                }
                this.clients.get(ioName).disconnect();
                this.clients.remove(ioName);
            }
        } catch (Exception e) {
            LogUtil.getInstance().logAndFireEvent(getClass(),"error happened turning device : "+ ioName +" [OFF], error : "+e.getMessage(),"MQTTDEVICE");

        }
    }

    @Override
    public void restartDevice(String deviceName) {
        this.turnOffDevice(deviceName);
        this.turnOnDevice(deviceName);
    }

    public void addDeviceClient(String deviceName) {
        try {
            final String deviceId = GatewayIdentifier.IO_IDENTIFIER.getIdentifier().replace("{app-name}", APP_NAME).replace("{io-name}", deviceName);
            final String configId = GatewayIdentifier.IO_Property.getIdentifier().replace("{property-name}", "Config").replace("{io-identifier}", deviceId);
            UaObjectNode deviceConfigNode = (UaObjectNode) uaNodeContext.getNodeManager().get(new NodeId(APP_NAMESPACE_INDEX, configId));
            if (clients.get(deviceName) == null) {
                String url = Utils.getPropertyValue(deviceConfigNode, "Property/URL");
                String ip = url.split(":")[0];
                String port = url.split(":")[1];
                Mqtt5ClientBuilder clientBuilder = Mqtt5Client.builder().identifier
                        (Utils.getPropertyValue(deviceConfigNode, "Property/CLIENT_ID"))
                        .serverHost(ip).
                        serverPort(Integer.parseInt(port));

                if (Utils.getPropertyValue(deviceConfigNode, "Property/USERNAME") != null && Utils.getPropertyValue(deviceConfigNode, "Property/PASSWORD") != null) {
                    clientBuilder.simpleAuth().username(Utils.getPropertyValue(deviceConfigNode, "Property/USERNAME")).password(Utils.getPropertyValue(deviceConfigNode, "Property/PASSWORD").getBytes(StandardCharsets.UTF_8)).applySimpleAuth();
                }
                Mqtt5AsyncClient client = clientBuilder.buildAsync();
                client.connect();
//                if(!client.getState().isConnected())
//                    throw new Exception("cant turn on mqtt device");
                clients.put(deviceName, client);
            }
        } catch (Exception e) {
            LogUtil.getInstance().logAndFireEvent(getClass(),"error happened creating MQTT client connection, device name : "+deviceName+" with error : "+e.getMessage(),"MQTTDEVICE");
        }
    }

    public Mqtt5AsyncClient getClients(String deviceName) {
        return clients.get(deviceName);
    }

    public Mqtt5AsyncClient getWriteClients(String deviceName) {
        return writeClients.get(deviceName);
    }

    public void addClient(String deviceName, Mqtt5AsyncClient mqttClient) {
        clients.put(deviceName, mqttClient);
    }

    public void addWriteClient(String deviceName, Mqtt5AsyncClient mqttClient) {
        writeClients.put(deviceName, mqttClient);
    }
}
