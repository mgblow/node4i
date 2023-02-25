package org.eclipse.milo.platform.runtime.interfaces;

import com.google.gson.Gson;
import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient;
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client;
import com.hivemq.client.mqtt.mqtt5.Mqtt5ClientBuilder;
import com.rabbitmq.client.Channel;
import org.apache.activemq.pool.PooledConnectionFactory;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.core.AccessLevel;
import org.eclipse.milo.opcua.sdk.core.Reference;
import org.eclipse.milo.opcua.sdk.server.api.NodeManager;
import org.eclipse.milo.opcua.sdk.server.api.persistence.RedisCommunication;
import org.eclipse.milo.opcua.sdk.server.model.nodes.objects.AlarmConditionTypeNode;
import org.eclipse.milo.opcua.sdk.server.nodes.*;
import org.eclipse.milo.opcua.sdk.server.nodes.serializable.SerializableAlarmConditionTypeNode;
import org.eclipse.milo.opcua.sdk.server.nodes.serializable.SerializableComponent;
import org.eclipse.milo.opcua.sdk.server.nodes.serializable.SerializableHistorian;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.types.builtin.*;
import org.eclipse.milo.opcua.stack.core.types.enumerated.IdType;
import org.eclipse.milo.platform.alamEvent.AlarmEvent;
import org.eclipse.milo.platform.alamEvent.interfaces.UaAlarmEventInterface;
import org.eclipse.milo.platform.boot.loaders.EventLoader;
import org.eclipse.milo.platform.boot.loaders.RuntimeLoader;
import org.eclipse.milo.platform.gateway.Gateway;
import org.eclipse.milo.platform.gateway.GatewayIdentifier;
import org.eclipse.milo.platform.gateway.interfaces.OpcUaDeviceClient;
import org.eclipse.milo.platform.runtime.RunTime;
import org.eclipse.milo.platform.structs.siemens.S7Configuration;
import org.eclipse.milo.platform.structs.siemens.S7Controller;
import org.eclipse.milo.platform.structs.siemens.S7Tag;
import org.eclipse.milo.platform.util.Props;
import org.eclipse.milo.platform.util.Utils;
import org.slf4j.LoggerFactory;

import javax.jms.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class UaInterface implements OpcUaDeviceClient {
    String APP_NAME;
    Gateway gateway;
    String MQTT_BROKER_CLIENT_HOST = org.eclipse.milo.opcua.sdk.server.util.Props.getProperty("mqtt-broker-host").toString();
    String MQTT_BROKER_CLIENT_ID = org.eclipse.milo.opcua.sdk.server.util.Props.getProperty("mqtt-broker-client-id").toString();
    String MQTT_BROKER_PORT = org.eclipse.milo.opcua.sdk.server.util.Props.getProperty("mqtt-broker-port").toString();
    String MQTT_BROKER_USERNAME = org.eclipse.milo.opcua.sdk.server.util.Props.getProperty("mqtt-broker-username").toString();
    String MQTT_BROKER_PASSWORD = org.eclipse.milo.opcua.sdk.server.util.Props.getProperty("mqtt-broker-password").toString();
    int APP_NAMESPACE_INDEX = Integer.parseInt(Props.getProperty("app-namespace-index").toString());
    private RedisCommunication redisCommunication;
    UaNodeContext uaNodeContext;


    public UaInterface(UaNodeContext uaNodeContext) {
        APP_NAME = uaNodeContext.getServer().getConfig().getApplicationName().getText();
        this.uaNodeContext = uaNodeContext;
        gateway = Gateway.getInstance(uaNodeContext);
        redisCommunication = new RedisCommunication(uaNodeContext.getServer());
    }

    public static UaInterface getInstance(UaNodeContext uaNodeContext) {
        return new UaInterface(uaNodeContext);
    }

    public static UaInterface getNewInstance(UaNodeContext uaNodeContext) {
        return new UaInterface(uaNodeContext);
    }

    public synchronized String getSimpleArchivedValue(List<String> identifiers, Long startTime, Long endTime, Long offset, Long limit) {
        final List<SerializableHistorian> list = this.uaNodeContext.getNodeManager().cacheNodeManager().getNodeFactoryCache().findSimpleArchive(identifiers, startTime, endTime, offset, limit);
        return Utils.toJson(list);
    }

    public synchronized String findComponents(Long offset, Long limit) {
        final List<SerializableComponent> list = this.uaNodeContext.getNodeManager().cacheNodeManager().getNodeFactoryCache().findComponents(offset, limit);
        return Utils.toJson(list);
    }

    public synchronized String getOutputFromComponent(String identifier) {
        UaNode uaNode = uaNodeContext.getNodeManager().get(Utils.newNodeId(identifier));
        if (uaNode == null)
            return null;
        UaVariableNode uaVariableNode = (UaVariableNode) uaNodeContext.getNodeManager().get(new NodeId(Integer.parseInt(Props.getProperty("app-namespace-index").toString()), identifier + "/output"));
        return uaVariableNode.getValue().getValue().getValue().toString();
    }

    public synchronized String getNodeValue(String identifier) throws Exception {
        UaVariableNode variableNode = (UaVariableNode) uaNodeContext.getNodeManager().get(new NodeId(Integer.parseInt(Props.getProperty("app-namespace-index").toString()), identifier));
        if (variableNode != null)
            return variableNode.getValue().getValue().getValue().toString();
        throw new Exception("identifier not exist");
    }

    public synchronized Map<String, String> getNodeValues(String[] identifiers) {
        try {
            Map<String, String> values = new HashMap<>();
            for (String identifier : identifiers) {
                NodeManager<UaNode> nodeNodeManager = uaNodeContext.getNodeManager();
                UaVariableNode variableNode = (UaVariableNode) nodeNodeManager.get(new NodeId(APP_NAMESPACE_INDEX, identifier));
                if (variableNode != null) {
                    values.put(identifier, variableNode.getValue().getValue().getValue().toString());
                } else {
                    values.put(identifier, null);
                }
            }
            return values;
        } catch (Exception e) {
            LoggerFactory.getLogger(getClass()).error("could not read node value with identifier : {} causing error : {}", identifiers, e.getMessage());
        }

        return Collections.emptyMap();
    }

    public synchronized void saveNode(String location, String name, String value) throws Exception {
        try {
            UaFolderNode locationFolder = (UaFolderNode) uaNodeContext.getNodeManager().get(new NodeId(Integer.parseInt(Props.getProperty("app-namespace-index").toString()), location));
            if (locationFolder == null) {
                throw new Exception("error creating a node with name : " + name);
            }
            DataValue nodeValue = new DataValue(new Variant(value), StatusCode.GOOD);
            UaVariableNode uaNode = (UaVariableNode) uaNodeContext.getNodeManager().get(new NodeId(Integer.parseInt(Props.getProperty("app-namespace-index").toString()), APP_NAME + "/" + name));
            if (uaNode == null) {
                UaVariableNode uaVariableNode = new UaVariableNode.UaVariableNodeBuilder(this.uaNodeContext).setNodeId(new NodeId(Integer.parseInt(Props.getProperty("app-namespace-index").toString()), APP_NAME + "/" + name)).setAccessLevel(AccessLevel.READ_WRITE).setBrowseName(new QualifiedName(Integer.parseInt(Props.getProperty("app-namespace-index").toString()), APP_NAME + "/" + name)).setDisplayName(LocalizedText.english(name)).setDataType(Identifiers.String).setTypeDefinition(Identifiers.VariableNode).build();
                uaVariableNode.setValue(nodeValue);
                uaNodeContext.getNodeManager().addNode(uaVariableNode);
                locationFolder.addOrganizes(uaVariableNode);
            } else if (!uaNode.getReferences().stream().filter(c -> c.getDirection() == Reference.Direction.INVERSE).collect(Collectors.toList()).get(0).getTargetNodeId().getIdentifier().equals(location)) {
                List<Reference> references = uaNode.getReferences().stream().filter(c -> c.getDirection() == Reference.Direction.INVERSE).collect(Collectors.toList());
                for (Reference reference : references) {
                    uaNode.removeReference(reference);
                }
                locationFolder.addOrganizes(uaNode);

            }
            uaNode.setValue(nodeValue);
        } catch (Exception e) {
            LoggerFactory.getLogger(getClass()).error("could not save node with name : {} causing error : {} with value : {}", name, e.getMessage(), value);
        }
    }

    public synchronized void sendMessageToActiveMQ(String ioName, String queueName, String message) {
        try {
            String deviceId = GatewayIdentifier.IO_IDENTIFIER.getIdentifier().replace("{app-name}", APP_NAME).replace("{io-name}", ioName);
            final String attributeId = GatewayIdentifier.IO_Property.getIdentifier().replace("{property-name}", "Attributes").replace("{io-identifier}", deviceId);
            UaObjectNode deviceAttributesNode = (UaObjectNode) this.uaNodeContext.getNodeManager().get(new NodeId(Integer.parseInt(Props.getProperty("app-namespace-index").toString()), attributeId));
            if (deviceAttributesNode == null) {
                throw new RuntimeException("can not find [ActiveMQ] device : " + ioName + "to send message.");
            }

            final String configId = GatewayIdentifier.IO_Property.getIdentifier().replace("{property-name}", "Config").replace("{io-identifier}", deviceId);
            UaObjectNode deviceConfig = (UaObjectNode) this.uaNodeContext.getNodeManager().get(new NodeId(2, configId));
            String url = Utils.getPropertyValue(deviceConfig, "Property/URL");
            if (gateway.getActiveMQ().getActiveMqCommunication().getActiveMqConnectionPool().isEmpty() || !gateway.getActiveMQ().getActiveMqCommunication().getActiveMqConnectionPool().containsKey(url)) {
                PooledConnectionFactory connectionFactory = new PooledConnectionFactory(url);
                gateway.getActiveMQ().getActiveMqCommunication().getActiveMqConnectionPool().put(url, connectionFactory);
            }
            final javax.jms.Connection connection = gateway.getActiveMQ().getActiveMqCommunication().getActiveMqConnectionPool().get(url).createConnection();

            try {
                connection.start();
                Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
                Destination destination = session.createQueue(queueName);
                MessageProducer producer = session.createProducer(destination);
                TextMessage textMessage = session.createTextMessage(message);
                producer.send(textMessage);
                producer.close();
                session.close();
                connection.stop();
            } catch (JMSException e) {
                LoggerFactory.getLogger(getClass()).error("An error happened in starting connection Activemq with message :" + e.getMessage());
            }
        } catch (Exception e) {
            LoggerFactory.getLogger(getClass()).error("An error happened in Activemq with message :" + e.getMessage());
        }
    }

    public synchronized void sendMessageToRabbitMQ(String deviceName, String queueName, String message) {
        try {
            String deviceId = GatewayIdentifier.IO_IDENTIFIER.getIdentifier().replace("{app-name}", APP_NAME).replace("{io-name}", deviceName);
            final String attributeId = GatewayIdentifier.IO_Property.getIdentifier().replace("{property-name}", "Attributes").replace("{io-identifier}", deviceId);
            UaObjectNode deviceAttributesNode = (UaObjectNode) this.uaNodeContext.getNodeManager().get(new NodeId(Integer.parseInt(Props.getProperty("app-namespace-index").toString()), attributeId));

            if (deviceAttributesNode == null) {
                throw new RuntimeException("can not find [AMQP] device : " + deviceName + "to send message.");
            }
            Channel rabbitChanel;
            if (gateway.getRabbitMQ().getRabbitMqCommunication().getClient(deviceName) == null) {
                gateway.getRabbitMQ().getRabbitMqCommunication().createClient(deviceName);
            }
            rabbitChanel = gateway.getRabbitMQ().getRabbitMqCommunication().getClient(deviceName);
            try {
                rabbitChanel.queueDeclare(queueName, false, false, false, null);
                CompletableFuture<Void> runAsync = CompletableFuture.runAsync(() -> {
                    try {
                        rabbitChanel.basicPublish("", queueName, null, message.getBytes(StandardCharsets.UTF_8));
                    } catch (IOException e) {
                        LoggerFactory.getLogger(getClass()).error("An error happened in RabbitMQ while sending message , :" + e.getMessage());
                    }
                });
                runAsync.join();
            } catch (IOException e) {
                LoggerFactory.getLogger(getClass()).error("An error happened in RabbitMQ while sending message , :" + e.getMessage());
            }

        } catch (Exception e) {
            LoggerFactory.getLogger(getClass()).error("An error happened in RabbitMQ while sending message , :" + e.getMessage());
        }
    }

    public synchronized void sendMessageToMqtt(String ioName, String topic, Object message) {
        try {

            String deviceId = GatewayIdentifier.IO_IDENTIFIER.getIdentifier().replace("{app-name}", APP_NAME).replace("{io-name}", ioName);
            final String attributeProperty = GatewayIdentifier.IO_Property.getIdentifier().replace("{property-name}", "Attributes").replace("{io-identifier}", deviceId);

            UaObjectNode deviceAttributesNode = (UaObjectNode) this.uaNodeContext.getNodeManager().get(new NodeId(Integer.parseInt(Props.getProperty("app-namespace-index").toString()), attributeProperty));
            if (deviceAttributesNode == null) {
                throw new RuntimeException("can not find [Mqtt] device : " + ioName + "to send message.");
            }
            String payload = Utils.toJson(message);
            Mqtt5AsyncClient client = gateway.getMqttDevice().getWriteClients(ioName) != null ? gateway.getMqttDevice().getWriteClients(ioName) : createMqttClient(ioName);
            CompletableFuture<Void> runAsync = CompletableFuture.runAsync(() -> {
                // publish message over mqtt
                while (!client.getConfig().getState().isConnected())
                    client.connect();
                client.publishWith().topic(topic).qos(MqttQos.EXACTLY_ONCE).payload(payload.getBytes()).send();
            }).whenComplete((result, ex) -> client.disconnect());
            runAsync.join();
        } catch (Exception e) {
            LoggerFactory.getLogger(getClass()).error("could not send message to MQTT device with name : {} causing error : {}", ioName, e.getMessage());
        }

    }

    public synchronized void sendMessageToKafka(String ioName, String topic, String message) {
        try {
            final String deviceId = GatewayIdentifier.IO_IDENTIFIER.getIdentifier().replace("{io-name}", ioName).replace("{app-name}", APP_NAME);

            final String attributeProperty = GatewayIdentifier.IO_Property.getIdentifier().replace("{property-name}", "Attributes").replace("{io-identifier}", deviceId);
            UaObjectNode deviceAttributesNode = (UaObjectNode) this.uaNodeContext.getNodeManager().get(new NodeId(2, attributeProperty));
            if (deviceAttributesNode == null) {
                throw new Exception("can not find [AMQP] device : " + ioName + "to send message.");
            }

            final String configProperty = GatewayIdentifier.IO_Property.getIdentifier().replace("{property-name}", "Config").replace("{io-identifier}", deviceId);
            UaObjectNode deviceConfig = (UaObjectNode) this.uaNodeContext.getNodeManager().get(new NodeId(2, configProperty));
            final String host = Utils.getPropertyValue(deviceConfig, "Property/URL");

            if (gateway.getKafkaDevice().getWriteClients(host) == null) {
                Properties properties = new Properties();
                properties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, host);

                gateway.getKafkaDevice().addWriteClients(host, new KafkaProducer<>(properties, new StringSerializer(), new ByteArraySerializer()));

            }

            KafkaProducer<String, byte[]> producer = gateway.getKafkaDevice().getWriteClients(host);
            ProducerRecord<String, byte[]> record = new ProducerRecord<>(topic, message.getBytes());
            producer.send(record);
        } catch (Exception e) {
            LoggerFactory.getLogger(getClass()).error("could not send message to device with name : {} causing error : {}", ioName, e.getMessage());
        }

    }

    public synchronized void writeValueToOpcUa(String deviceName, String identifier, int namespaceIndex, String value, String dataType) {
        try {
            String deviceIdentifier = GatewayIdentifier.IO_IDENTIFIER.getIdentifier().replace("{app-name}", APP_NAME).replace("{io-name}", deviceName);
            String opcTagIdentifier = GatewayIdentifier.OPC_TAG.getIdentifier().replace("{app-name}", APP_NAME).replace("{io-name}", deviceName)
                    .replace("{tag-name}", identifier);
            final UaObjectNode opcTag = (UaObjectNode) this.uaNodeContext.getNodeManager().get(new NodeId(Integer.parseInt(Props.getProperty("app-namespace-index").toString()), opcTagIdentifier));
            if (opcTag == null) {
                LoggerFactory.getLogger(getClass()).error("can not find config object for device with name : {}, writing on node : {}", deviceName, identifier);
                throw new Exception("can not find [OPC] Tag : " + deviceName + "to send message.");
            }
            final String attributeProperty = GatewayIdentifier.IO_Property.getIdentifier().replace("{property-name}", "Attributes").replace("{io-identifier}", deviceIdentifier);
            UaObjectNode deviceAttributesNode = (UaObjectNode) this.uaNodeContext.getNodeManager().get(new NodeId(Integer.parseInt(Props.getProperty("app-namespace-index").toString()), attributeProperty));

            final String identifierDataTypeBrowseName = GatewayIdentifier.PROPERTY_BROWSE_NAME.getIdentifier().replace("{property-name}", "identifierDataType");
            final String identifierDataType = opcTag.getProperty(new QualifiedName(APP_NAMESPACE_INDEX, identifierDataTypeBrowseName)).get().toString();

            IdType idType = IdType.valueOf(identifierDataType);
            if (deviceAttributesNode == null) {
                LoggerFactory.getLogger(getClass()).error("can not find config object for device with name : {}, writing on node : {}", deviceName, identifier);
                throw new RuntimeException("can not find [OPC] device : " + deviceName + "to send message.");
            }
            if (gateway.getOpcUaDevice().getWriteClients(deviceName) == null) {
                OpcUaClient opcUaClient = createOpcUaClient(deviceName);
                gateway.getOpcUaDevice().setWriteClients(deviceName, opcUaClient);
            }
            OpcUaClient client = gateway.getOpcUaDevice().getWriteClients(deviceName);
            client.connect().get();
            List<NodeId> nodeIds = List.of(new NodeId(namespaceIndex, identifier, idType));
            Variant variant = Utils.changeDataType(value, dataType);
            // don't write status or timestamps
            DataValue dataValue = new DataValue(variant, null, null);
            CompletableFuture<Void> runAsync = CompletableFuture.runAsync(() -> {
                // write asynchronously....
                client.writeValues(nodeIds, List.of(dataValue));
                client.disconnect();
            });
            runAsync.join();
        } catch (Exception e) {
            LoggerFactory.getLogger(getClass()).error("could not send message to device with name : {} causing error : {}", deviceName, e.getMessage());
        }
    }

    public synchronized void writeValuesToS7(String deviceName, String[] tags, Object[] values) {
        UaObjectNode deviceConfigNode = (UaObjectNode) this.uaNodeContext.getNodeManager().get(new NodeId(2, APP_NAME + "/IO/" + deviceName + "/Config"));
        UaObjectNode deviceAttributesNode = (UaObjectNode) this.uaNodeContext.getNodeManager().get(new NodeId(2, APP_NAME + "/IO/" + deviceName + "/Attributes"));
        if (deviceConfigNode == null || deviceAttributesNode == null) {
            LoggerFactory.getLogger(getClass()).error("could not find device attributes and config for device : {}", deviceName);
        }
        S7Configuration s7Configuration = new S7Configuration();
        S7Controller s7Controller = new S7Controller();
        s7Controller.setConnection_name(deviceName);
        String url = Utils.getPropertyValue(deviceConfigNode, "Property/URL");
        s7Controller.setHost(url.split(":")[0]);
        s7Controller.setPort(url.split(":")[1]);
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
            if (Arrays.asList(tags).contains(name)) {
                S7Tag s7Tag = new S7Tag();
                s7Tag.setName(name);
                s7Tag.setAddress(address);
                s7Configuration.getTags().add(s7Tag);
            }
        });
        // append values to config
        s7Configuration.setValues(values);
        // send message
        String payload = Utils.toJson(s7Configuration);
        Mqtt5AsyncClient client = createS7MqttClient(deviceName);
        CompletableFuture<Void> runAsync = CompletableFuture.runAsync(() -> {
            // publish message over mqtt
            client.publishWith().topic("scouting/plc/siemens/write").qos(MqttQos.EXACTLY_ONCE).payload(payload.getBytes()).send();
            client.disconnect();
        }).whenComplete((result, ex) -> {
            client.disconnect();
        });
        runAsync.join();
    }

    private Mqtt5AsyncClient createMqttClient(String deviceName) {
        String deviceId = GatewayIdentifier.IO_IDENTIFIER.getIdentifier().replace("{app-name}", APP_NAME).replace("{io-name}", deviceName);

        final String configId = GatewayIdentifier.IO_Property.getIdentifier().replace("{property-name}", "Config").replace("{io-identifier}", deviceId);

        UaObjectNode deviceConfigNode = (UaObjectNode) uaNodeContext.getNodeManager().get(new NodeId(APP_NAMESPACE_INDEX, configId));

        String url = Utils.getPropertyValue(deviceConfigNode, "Property/URL");
        String ip = url.split(":")[0];
        String port = url.split(":")[1];
        Mqtt5ClientBuilder clientBuilder = Mqtt5Client.builder().identifier(UUID.randomUUID().toString()).serverHost(ip).serverPort(Integer.parseInt(port));
        if (Utils.getPropertyValue(deviceConfigNode, "Property/USERNAME") != null && Utils.getPropertyValue(deviceConfigNode, "Property/PASSWORD") != null) {
            clientBuilder.simpleAuth().username(Utils.getPropertyValue(deviceConfigNode, "Property/USERNAME")).password(Utils.getPropertyValue(deviceConfigNode, "Property/PASSWORD").getBytes(StandardCharsets.UTF_8)).applySimpleAuth();
        }
        Mqtt5AsyncClient client = clientBuilder.buildAsync();

        while (!client.getState().isConnected()) client.connect();
        gateway.getMqttDevice().addWriteClient(deviceName, client);
        return client;
    }

    private Mqtt5AsyncClient createS7MqttClient(String deviceName) {
        Mqtt5ClientBuilder clientBuilder = Mqtt5Client.builder().identifier(UUID.randomUUID().toString())
                .serverHost(MQTT_BROKER_CLIENT_HOST)
                .serverPort(Integer.parseInt(MQTT_BROKER_PORT))
                .simpleAuth().username(MQTT_BROKER_USERNAME)
                .password(MQTT_BROKER_PASSWORD.getBytes(StandardCharsets.UTF_8))
                .applySimpleAuth();

        Mqtt5AsyncClient client = clientBuilder.buildAsync();
        while (!client.getState().isConnected()) client.connect();
        return client;
    }

    public synchronized void sleep(int time) throws InterruptedException {
        Thread.sleep(time);
    }

    private OpcUaClient createOpcUaClient(String deviceName) {
        try {
            final String deviceIdentifier = GatewayIdentifier.IO_IDENTIFIER.getIdentifier().replace("{io-name}", deviceName).replace("{app-name}", APP_NAME);
            final String configProperty = GatewayIdentifier.IO_Property.getIdentifier().replace("{property-name}", "Config").replace("{io-identifier}", deviceIdentifier);
            UaObjectNode deviceConfigNode = (UaObjectNode) uaNodeContext.getNodeManager().get(new NodeId(2, configProperty));
            OpcUaClient client = createOpcUaClient(Utils.getPropertyValue(deviceConfigNode, "Property/URL"), Utils.getPropertyValue(deviceConfigNode, "Property/POLICY"), Utils.getPropertyValue(deviceConfigNode, "Property/USERNAME"), Utils.getPropertyValue(deviceConfigNode, "Property/PASSWORD"));
            return client;
        } catch (Exception e) {
            LoggerFactory.getLogger(getClass()).error("could not create client for device with name : {} causing error : {}", deviceName, e.getMessage());
            return null;
        }
    }

    public synchronized void fire(String eventDefinitionId) throws Exception {
        AlarmConditionTypeNode alarmConditionTypeNode = (AlarmConditionTypeNode) uaNodeContext.getServer().getEventFactory().getNodeManager().get(Utils.newNodeId(eventDefinitionId));
        if (alarmConditionTypeNode.getActiveState().getText().equals("false")) {
            UaAlarmEventInterface.getInstance(uaNodeContext).alert(eventDefinitionId, true);
        }
    }

    public synchronized boolean state(String eventDefinitionId) throws Exception {
        AlarmConditionTypeNode alarmConditionTypeNode = (AlarmConditionTypeNode) uaNodeContext.getServer().getEventFactory().getNodeManager().get(Utils.newNodeId(eventDefinitionId));
        return Boolean.parseBoolean(alarmConditionTypeNode.getActiveState().getText().toString());
    }

    public synchronized void ack(String eventId, String comment) throws Exception {
        uaNodeContext.getServer().getEventFactory().acknowledge(eventId, new LocalizedText(comment));
    }

    public synchronized String eventInfo(String eventDefinitionId) {
        AlarmConditionTypeNode alarmConditionTypeNode = (AlarmConditionTypeNode) uaNodeContext.getServer().getEventFactory().getNodeManager().get(Utils.newNodeId(eventDefinitionId));
        SerializableAlarmConditionTypeNode serializableAlarmConditionTypeNode = new SerializableAlarmConditionTypeNode(alarmConditionTypeNode);
        Gson gson = new Gson();
        return gson.toJson(serializableAlarmConditionTypeNode);
    }

    public synchronized void loadComponents() {
        RunTime runtime = new RunTime(uaNodeContext);
        ;
        new RuntimeLoader(uaNodeContext, runtime).load();
    }

    public synchronized void loadComponents(String... componentIds) {
        RunTime runtime = new RunTime(uaNodeContext);
        ;
        new RuntimeLoader(uaNodeContext, runtime).load(componentIds);
    }

    public synchronized void loadEvents() {
        AlarmEvent alarmEvent = new AlarmEvent(uaNodeContext);
        new EventLoader(uaNodeContext, alarmEvent).load();
    }

    public synchronized void loadEvents(String... eventIds) {
        AlarmEvent alarmEvent = new AlarmEvent(uaNodeContext);
        new EventLoader(uaNodeContext, alarmEvent).load(eventIds);
    }

}