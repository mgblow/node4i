package org.eclipse.milo.platform.gateway.io;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNodeContext;
import org.eclipse.milo.opcua.sdk.server.nodes.UaObjectNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaVariableNode;
import org.eclipse.milo.opcua.stack.core.types.builtin.*;
import org.eclipse.milo.platform.gateway.GatewayIdentifier;
import org.eclipse.milo.platform.gateway.interfaces.Device;
import org.eclipse.milo.platform.gateway.interfaces.OpcUaDeviceClient;
import org.eclipse.milo.platform.util.LogUtil;
import org.eclipse.milo.platform.util.Utils;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

public class KafkaDevice implements Device, OpcUaDeviceClient {
    private boolean state;
    private final UaNodeContext uaNodeContext;
    Properties properties = new Properties();
    private Map<String, Thread> subscriptions = new HashMap<>();
    HashMap<String, String> nodeNameAndTopic = new HashMap();
    private static String APP_NAME = null;
    Map<String, KafkaConsumer<String, String>> clients = new ConcurrentHashMap<>();
    Map<String, KafkaProducer<String, byte[]>> writeClients = new ConcurrentHashMap<>();

    public KafkaDevice(UaNodeContext uaNodeContext) {
        this.uaNodeContext = uaNodeContext;
        APP_NAME = uaNodeContext.getServer().getConfig().getApplicationName().getText();
    }

    @Override
    public void turnOnDevice(String ioName) {
        String deviceIdentifier = GatewayIdentifier.IO_IDENTIFIER.getIdentifier().replace("{app-name}", APP_NAME).replace("{io-name}", ioName);

        try {
            final String configProperty = GatewayIdentifier.IO_Property.getIdentifier().replace("{property-name}", "Config").replace("{io-identifier}", deviceIdentifier);
            UaObjectNode deviceConfig = (UaObjectNode) this.uaNodeContext.getNodeManager().get(new NodeId(2, configProperty));
            final String host = Utils.getPropertyValue(deviceConfig, "Property/URL");
            if (clients.get(host) == null) {
                properties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, host);
                properties.setProperty(ConsumerConfig.GROUP_ID_CONFIG, ioName);
                properties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
                properties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());

                clients.put(host, new KafkaConsumer<>(properties));

            }
            final KafkaConsumer<String, String> consumer = clients.get(host);

            final String attributeProperty = GatewayIdentifier.IO_Property.getIdentifier().replace("{property-name}", "Attributes").replace("{io-identifier}", deviceIdentifier);

            UaObjectNode deviceAttributesNode = (UaObjectNode) this.uaNodeContext.getNodeManager().get(new NodeId(2, attributeProperty));

            Objects.requireNonNull(deviceAttributesNode).getComponentNodes().forEach(node -> {
                String topic = Utils.getPropertyValue(node, "Property/topic");
                String nodeName = Utils.getPropertyValue(node, "Property/name");
                nodeNameAndTopic.put(nodeName, topic);
            });
            consumer.subscribe(nodeNameAndTopic.values());
            Thread thread = new Thread(() -> {
                while (isOn(host)) {
                    for (Map.Entry<String, String> entry : nodeNameAndTopic.entrySet()) {
                        try {
                            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(3600000L));
                            for (ConsumerRecord<String, String> record : records) {
                                final String tagId = GatewayIdentifier.KAFKA_TAG.getIdentifier().replace("{app-name}", APP_NAME).replace("{io-name}", ioName).replace("{tag-name}", entry.getKey());

                                final String kafkaValue = GatewayIdentifier.VALUE_PROPERTY_IDENTIFIER.getIdentifier().replace("{tag-identifier}", tagId);
                                final NodeId nodeID = Utils.newNodeId(kafkaValue);

                                if (nodeID != null) {
                                    DataValue nodeValue = new DataValue(new Variant(record.value()), StatusCode.GOOD);
                                    UaVariableNode node = (UaVariableNode) uaNodeContext.getNodeManager().get(nodeID);
                                    node.setValue(nodeValue);
                                }
                            }
                        }catch (Exception e){
                            LogUtil.getInstance().logAndFireEvent(getClass(),"error occurred about kafka device " + ioName,"KAFKADEVICE");
                        }
                    }
                }

            });
            executor.submit(thread);
            subscriptions.put(ioName, thread);
        } catch (Exception e) {
            LogUtil.getInstance().logAndFireEvent(getClass(),"error occurred about kafka device " + ioName,"KAFKADEVICE");
        }

    }

    @Override
    public void turnOffDevice(String ioName) {
        try {
            clients.remove(ioName);
            subscriptions.get(ioName).interrupt();
        }catch (Exception e){
            LogUtil.getInstance().logAndFireEvent(getClass(),"error occurred on turn off kafka device " + ioName,"KAFKADEVICE");
        }
    }

    public boolean isOn(String deviceName) {
        return clients.get(deviceName) != null;
    }

    public KafkaProducer<String, byte[]> getWriteClients(String host) {
        return writeClients.get(host);
    }

    public void addWriteClients(String host,  KafkaProducer<String, byte[]> producer) {
        writeClients.put(host, producer);
    }
}

