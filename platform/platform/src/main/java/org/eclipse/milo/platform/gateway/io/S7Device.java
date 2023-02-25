package org.eclipse.milo.platform.gateway.io;

import com.google.gson.Gson;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNodeContext;
import org.eclipse.milo.opcua.sdk.server.nodes.UaObjectNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaVariableNode;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.platform.gateway.GatewayIdentifier;
import org.eclipse.milo.platform.gateway.interfaces.Device;
import org.eclipse.milo.platform.structs.siemens.S7Configuration;
import org.eclipse.milo.platform.structs.siemens.S7Controller;
import org.eclipse.milo.platform.structs.siemens.S7Tag;
import org.eclipse.milo.platform.util.LogUtil;
import org.eclipse.milo.platform.util.Utils;
import org.eclipse.paho.client.mqttv3.*;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class S7Device implements Device {

    private static String APP_NAME = null;

    private static String MQTT_BROKER_HOST = org.eclipse.milo.opcua.sdk.server.util.Props.getProperty("mqtt-broker-host").toString();
    private static String MQTT_BROKER_CLIENT_ID = UUID.randomUUID().toString();
    private static String MQTT_BROKER_PORT = org.eclipse.milo.opcua.sdk.server.util.Props.getProperty("mqtt-broker-port").toString();
    private static String MQTT_BROKER_USERNAME = org.eclipse.milo.opcua.sdk.server.util.Props.getProperty("mqtt-broker-username").toString();
    private static String MQTT_BROKER_PASSWORD = org.eclipse.milo.opcua.sdk.server.util.Props.getProperty("mqtt-broker-password").toString();
    private UaNodeContext uaNodeContext;

    private Map<String, MqttClient> clients = new HashMap<>();
    Gson gson = new Gson();

    public S7Device(UaNodeContext uaNodeContext) {
        APP_NAME = uaNodeContext.getServer().getConfig().getApplicationName().getText();
        this.uaNodeContext = uaNodeContext;
    }

    public MqttClient getClients(String deviceName) {
        return clients.get(deviceName);
    }

    @Override
    public void turnOnDevice(String ioName) {
        String deviceIdentifier = GatewayIdentifier.IO_IDENTIFIER.getIdentifier().replace("{app-name}", APP_NAME).replace("{io-name}", ioName);

        try {
            this.addDeviceClient(ioName);
        } catch (MqttException e) {
            LoggerFactory.getLogger(getClass()).error("error happened while trying to add mqtt client for device : {}", ioName);
        }
        final String configProperty = GatewayIdentifier.IO_Property.getIdentifier().replace("{property-name}", "Config").replace("{io-identifier}", deviceIdentifier);
        UaObjectNode deviceConfigNode = (UaObjectNode) this.uaNodeContext.getNodeManager().get(new NodeId(2, configProperty));

        final String attributeProperty = GatewayIdentifier.IO_Property.getIdentifier().replace("{property-name}", "Attributes").replace("{io-identifier}", deviceIdentifier);
        UaObjectNode deviceAttributesNode = (UaObjectNode) this.uaNodeContext.getNodeManager().get(new NodeId(2, attributeProperty));


        S7Configuration s7Configuration = new S7Configuration();
        S7Controller s7Controller = new S7Controller();
        s7Controller.setConnection_name(ioName);
        String url = Utils.getPropertyValue(deviceConfigNode, "Property/URL");
        String ip = url.split(":")[0];
        String port = url.split(":")[1];
        s7Controller.setHost(ip);
        s7Controller.setPort(port);
        s7Controller.setRack(Utils.getPropertyValue(deviceConfigNode, "Property/RACK"));
        s7Controller.setSlot(Utils.getPropertyValue(deviceConfigNode, "Property/SLOT"));
        s7Controller.setType("nodes7");

        S7Configuration.Config config = new S7Configuration.Config();
        config.setController(s7Controller);
        config.setPullingInterval(1000);
        s7Configuration.setConfig(config);
        s7Configuration.setTags(new ArrayList<S7Tag>());
        deviceAttributesNode.getComponentNodes().forEach(node -> {
            String name = Utils.getPropertyValue((UaObjectNode) node, "Property/name");
            String address = Utils.getPropertyValue((UaObjectNode) node, "Property/address");
            S7Tag s7Tag = new S7Tag();
            s7Tag.setName(name);
            s7Tag.setAddress(address);
            s7Configuration.getTags().add(s7Tag);
        });

        this.publish(ioName, "scouting/plc/siemens/subscribe", s7Configuration);
        try {
            this.clients.get(ioName).subscribe("devices/plc/siemens/" + ioName + "/out");
        } catch (MqttException e) {
            LoggerFactory.getLogger(getClass()).error("error {} while subscribing the mqtt client for device : {}", e.getMessage(), ioName);
        }
    }

    @Override
    public void turnOffDevice(String ioName) {
        this.publish(ioName, "scouting/plc/siemens/unsubscribe", ioName);
        try {
            this.clients.get(ioName).disconnect();
        } catch (MqttException e) {
            LoggerFactory.getLogger(getClass()).error("error {} while disconnecting the mqtt client for device : {}", e.getMessage(), ioName);
        }
        this.clients.put(ioName, null);
    }

    @Override
    public void restartDevice(String deviceName) {
        this.turnOffDevice(deviceName);
        this.turnOnDevice(deviceName);
    }

    private void addDeviceClient(String deviceName) throws MqttException {
        MqttClient client = new MqttClient("tcp://" + MQTT_BROKER_HOST + ":" + MQTT_BROKER_PORT, MQTT_BROKER_CLIENT_ID, null);
        MqttConnectOptions connOpts = new MqttConnectOptions();
        connOpts.setUserName(MQTT_BROKER_USERNAME);
        connOpts.setPassword(MQTT_BROKER_PASSWORD.toCharArray());
        connOpts.setCleanSession(true);
        client.setCallback(new S7DeviceMqttCallBack(deviceName));
        client.connect(connOpts);
        this.clients.put(deviceName, client);
    }

    public void publish(String device_name, String topic, Object data) {
        try {
            String message = gson.toJson(data);
            MqttMessage payload = new MqttMessage(message.getBytes());
            payload.setQos(2);
            this.clients.get(device_name).publish(topic, payload);
        }catch (Exception e){
            String errorMessage ="Error occurred when change " + device_name + "state.";
            if(e instanceof MqttException )
                 errorMessage = String.format("error %s in publish of mqtt message for device %s", e.getMessage(), device_name);
            else if (e instanceof MqttPersistenceException)
                errorMessage = String.format("error %s in persistence of mqtt message.", e.getMessage());
            LogUtil.getInstance().logAndFireEvent(getClass(),errorMessage,"S7DEVICE");
        }

    }

    private class S7DeviceMqttCallBack implements MqttCallback {

        private String deviceName;

        S7DeviceMqttCallBack(String deviceName) {
            this.deviceName = deviceName;
        }

        @Override
        public void connectionLost(Throwable throwable) {
            LoggerFactory.getLogger(getClass()).error("connection lost in S7 device mqtt client.");
            if (!clients.get(deviceName).isConnected()) {
                try {
                    clients.get(deviceName).connect();
                } catch (MqttException e) {
                    LoggerFactory.getLogger(getClass()).error("reconnection failed , in connection lost in S7 device mqtt client.");
                }
            }

        }

        @Override
        public void messageArrived(String topic, MqttMessage mqttMessage) throws Exception {
            try {
                String message = new String(mqttMessage.getPayload(), StandardCharsets.UTF_8);
                HashMap values = gson.fromJson(message, HashMap.class);
                values.forEach((key, value) -> {
                    try {
                        final String tagId = GatewayIdentifier.S7_TAG.getIdentifier().replace("{app-name}", APP_NAME).replace("{io-name}", deviceName).replace("{tag-name}", key.toString());
                        final String valueId = GatewayIdentifier.VALUE_PROPERTY_IDENTIFIER.getIdentifier().replace("{tag-identifier}", tagId);
                        NodeId nodeId = Utils.newNodeId(valueId);
                        DataValue nodeValue = new DataValue(new Variant(value), StatusCode.GOOD);
                        UaVariableNode node = (UaVariableNode) uaNodeContext.getNodeManager().get(nodeId);
                        if (node == null) {
                            LogUtil.getInstance().logAndFireEvent(getClass(), "no definition in topic : " + key, "S7DEVICE");
                        } else {
                            node.setValue(nodeValue);
                        }
                    } catch (Exception e) {
                        LogUtil.getInstance().logAndFireEvent(getClass(), e.getMessage(), "S7DEVICE");
                    }
                });
            }catch (Exception e){
                LogUtil.getInstance().logAndFireEvent(getClass(), e.getMessage(), "S7DEVICE");
            }
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
        }

    }

}
