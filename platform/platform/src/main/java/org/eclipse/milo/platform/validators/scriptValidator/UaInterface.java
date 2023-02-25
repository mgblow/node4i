package org.eclipse.milo.platform.validators.scriptValidator;

import org.eclipse.milo.opcua.sdk.client.OpcUaClient;

import java.util.HashMap;
import java.util.Map;

public class UaInterface {

    public UaInterface() {

    }

    public String getNodeValue(String identifier) {
        return "";
    }

    public Map<String, String> getNodeValues(String[] identifiers) {
        return new HashMap<>();
    }

    public void saveNode(String location, String name, String value) throws Exception {
    }

    public void sendMessageToMqtt(String deviceName, String topic, Object message) {
    }

    public void sendMessageToActiveMQ(String deviceName, String queueName, String message) {
    }

    public void sendMessageToKafka(String deviceName, String topic, String message) {
    }

    public void writeValueToOpcUa(String deviceName, int namespaceIndex, String identifier, String value) {
    }

    public void writeValueToOpcUa(String deviceName, String identifier, int namespaceIndex, String value, String dataType) {
    }

    private OpcUaClient createOpcUaClient(String deviceName) {
        return null;
    }

    public String getSimpleArchivedValue(String identifier, Long startTime, Long endTime) {
        return "";
    }

    public void sendMessageToRabbitMQ(String deviceName, String queueName, String message) {
    }

    public void writeValueToOpcUa(String deviceName, String[] tags, Object[] values) {
    }

    public void writeValuesToS7(String deviceName, String[] tags, Object[] values) {
    }

    public void fire(String eventDefinitionId) {
    }

    public boolean state(String eventDefinitionId) throws Exception {
        return true;
    }

    public void ack(String eventId, String comment) throws Exception {
    }

    public String eventInfo(String eventDefinitionId) {
        return null;
    }


    public synchronized void loadComponents() {
    }

    public synchronized void loadComponents(String... componentIds) {
    }

    public synchronized void loadEvents() {
    }

    public synchronized void loadEvents(String... eventIds) {
    }
}
