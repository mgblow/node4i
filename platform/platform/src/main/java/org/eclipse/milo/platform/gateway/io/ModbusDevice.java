package org.eclipse.milo.platform.gateway.io;

import com.google.gson.Gson;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNodeContext;
import org.eclipse.milo.opcua.sdk.server.nodes.UaObjectNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaVariableNode;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.platform.gateway.interfaces.Device;
import org.eclipse.milo.platform.structs.modbus.ModbusConfiguration;
import org.eclipse.milo.platform.structs.modbus.ModbusController;
import org.eclipse.milo.platform.structs.modbus.ModbusTag;
import org.eclipse.milo.platform.util.LogUtil;
import org.eclipse.milo.platform.util.Utils;
import org.eclipse.paho.client.mqttv3.*;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ModbusDevice implements Device {

    private static String APP_NAME = null;

    private static String MQTT_BROKER_HOST = org.eclipse.milo.opcua.sdk.server.util.Props.getProperty("mqtt-broker-host").toString();
    private static String MQTT_BROKER_CLIENT_ID = org.eclipse.milo.opcua.sdk.server.util.Props.getProperty("mqtt-broker-client-id").toString();
    private static String MQTT_BROKER_PORT = org.eclipse.milo.opcua.sdk.server.util.Props.getProperty("mqtt-broker-port").toString();
    private static String MQTT_BROKER_USERNAME = org.eclipse.milo.opcua.sdk.server.util.Props.getProperty("mqtt-broker-username").toString();
    private static String MQTT_BROKER_PASSWORD = org.eclipse.milo.opcua.sdk.server.util.Props.getProperty("mqtt-broker-password").toString();
    private UaNodeContext uaNodeContext;

    private Map<String, MqttClient> clients = new HashMap<>();
    Gson gson = new Gson();

    public ModbusDevice(UaNodeContext uaNodeContext) {
        APP_NAME = uaNodeContext.getServer().getConfig().getApplicationName().getText();
        this.uaNodeContext = uaNodeContext;
    }

    @Override
    public void turnOnDevice(String ioName) {
        try {
            this.addDeviceClient(ioName);
        } catch (MqttException e) {
            LoggerFactory.getLogger(getClass()).error("error happened while trying to add mqtt client for device : {}", ioName);
        }
        UaObjectNode deviceConfigNode = (UaObjectNode) this.uaNodeContext.getNodeManager().get(new NodeId(2, APP_NAME + "/IO/" + ioName + "/Config"));
        UaObjectNode deviceAttributesNode = (UaObjectNode) this.uaNodeContext.getNodeManager().get(new NodeId(2, APP_NAME + "/IO/" + ioName + "/Attributes"));
        ModbusConfiguration modbusConfiguration = new ModbusConfiguration();
        ModbusController modbusController = new ModbusController();
        modbusController.setConnection_name(ioName);
        String url = Utils.getPropertyValue(deviceConfigNode, "Property/URL");
        modbusController.setHost(url.split(":")[0]);
        modbusController.setPort(url.split(":")[1]);
        modbusController.setRtu(Boolean.parseBoolean(Utils.getPropertyValue(deviceConfigNode,"Property/RTU")));
        modbusController.setType("modbusTcp");

        ModbusConfiguration.Config config = new ModbusConfiguration.Config();
        config.setController(modbusController);
        config.setPullingInterval(1000);
        modbusConfiguration.setConfig(config);
        modbusConfiguration.setTags(new ArrayList<ModbusTag>());
        deviceAttributesNode.getComponentNodes().forEach(node -> {
            String name = Utils.getPropertyValue((UaObjectNode) node, "Property/name");
            String address = Utils.getPropertyValue((UaObjectNode) node, "Property/address");
            ModbusTag modbusTag = new ModbusTag();
            modbusTag.setName(name);
            modbusTag.setAddress(address);
            modbusConfiguration.getTags().add(modbusTag);
        });

        this.publish(ioName, "scouting/modbus/tcp/subscribe", modbusConfiguration);
        try {
            this.clients.get(ioName).subscribe("devices/" + ioName + "/out");
        } catch (MqttException e) {
            LoggerFactory.getLogger(getClass()).error("error {} while subscribing the mqtt client for device : {}", e.getMessage(), ioName);
        }
    }

    @Override
    public void turnOffDevice(String ioName) {
        this.publish(ioName, "scouting/modbus/tcp/unsubscribe", ioName);
        try {
            this.clients.get(ioName).disconnect();
        } catch (MqttException e) {
            LoggerFactory.getLogger(getClass()).error("error {} while disconnecting the modbus client for device : {}", e.getMessage(), ioName);
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
        client.setCallback(new ModbusDeviceMqttCallBack(deviceName));
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
            LogUtil.getInstance().logAndFireEvent(getClass(),errorMessage,"MODBUSDEVICE");
        }


    }

    private class ModbusDeviceMqttCallBack implements MqttCallback {

        private String deviceName;

        ModbusDeviceMqttCallBack(String deviceName) {
            this.deviceName = deviceName;
        }

        @Override
        public void connectionLost(Throwable throwable) {
            LoggerFactory.getLogger(getClass()).error("connection lost in modbus device mqtt client.");
        }

        @Override
        public void messageArrived(String topic, MqttMessage mqttMessage) throws Exception {
            try {
                String message = new String(mqttMessage.getPayload(), StandardCharsets.UTF_8);
                HashMap values = gson.fromJson(message, HashMap.class);
                values.forEach((key, value) -> {
                    try {
                        NodeId nodeId = Utils.newNodeId(APP_NAME + "/IO/Modbus/"  + deviceName +  "/TAG/" + key + "/value");
                        DataValue nodeValue = new DataValue(new Variant(value), StatusCode.GOOD);
                        UaVariableNode node = (UaVariableNode) uaNodeContext.getNodeManager().get(nodeId);
                        if (node == null) {
                            LogUtil.getInstance().logAndFireEvent(getClass(),"no definition in topic : " + key,"MODBUSDEVICE");
                        } else {
                            node.setValue(nodeValue);
                        }
                    } catch (Exception e) {
                        LogUtil.getInstance().logAndFireEvent(getClass(),e.getMessage(),"MODBUSDEVICE");
                    }
                });
            }catch (Exception e){
                logger.error("error to discover message that generate with modbus");
            }
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
        }
    }

}
