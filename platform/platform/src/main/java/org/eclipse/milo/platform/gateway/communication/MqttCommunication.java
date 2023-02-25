package org.eclipse.milo.platform.gateway.communication;

import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient;
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client;
import com.hivemq.client.mqtt.mqtt5.Mqtt5ClientBuilder;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ObjectWriter;
import org.eclipse.milo.opcua.sdk.server.util.Props;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class MqttCommunication {
    String APP_NAME = Props.getProperty("app-name").toString();
    int APP_NAMESPACE_INDEX = Integer.parseInt(Props.getProperty("app-namespace-index").toString());
    String MQTT_BROKER_CLIENT_HOST = Props.getProperty("mqtt-broker-host").toString();
    String MQTT_BROKER_CLIENT_ID = UUID.randomUUID().toString();
    String MQTT_BROKER_PORT = Props.getProperty("mqtt-broker-port").toString();
    String MQTT_BROKER_USERNAME = Props.getProperty("mqtt-broker-username").toString();
    String MQTT_BROKER_PASSWORD = Props.getProperty("mqtt-broker-password").toString();

    private static Mqtt5AsyncClient client;

    public MqttCommunication() {
        this.client = createMqttClient();
    }

    public void send(String topic, Object data) {
        try {
            if (!client.getState().isConnected()) client.connect();
            CompletableFuture<Void> runAsync = CompletableFuture.runAsync(() -> {
                ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
                String json = "";
                try {
                    json = ow.writeValueAsString(data);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                ObjectMapper mapper = new ObjectMapper();
                client.publishWith().topic(topic).qos(MqttQos.AT_LEAST_ONCE).payload(json.getBytes()).send();
            });
            runAsync.join();
        } catch (Exception e) {
            LoggerFactory.getLogger(getClass()).error("could not send message to device with name : {} causing error : {}", topic, e.getMessage());
        }
    }

    public void send(String topic, String data) {
        try {
            if (!client.getState().isConnected()) client.connect();
            CompletableFuture<Void> runAsync = CompletableFuture.runAsync(() -> {
                client.publishWith().topic(topic).qos(MqttQos.EXACTLY_ONCE).payload(data.getBytes()).send();
            });
            runAsync.join();
        } catch (Exception e) {
            LoggerFactory.getLogger(getClass()).error("could not send message to device with name : {} causing error : {}", topic, e.getMessage());
        }
    }


    private Mqtt5AsyncClient createMqttClient() {

        Mqtt5ClientBuilder clientBuilder = Mqtt5Client.builder().identifier(MQTT_BROKER_CLIENT_ID).serverHost(MQTT_BROKER_CLIENT_HOST).serverPort(Integer.parseInt(MQTT_BROKER_PORT)).simpleAuth().username(MQTT_BROKER_USERNAME).password(MQTT_BROKER_PASSWORD.getBytes(StandardCharsets.UTF_8)).applySimpleAuth();
        Mqtt5AsyncClient client = clientBuilder.buildAsync();
        if (!client.getState().isConnected()) client.connect();
        return client;
    }
}
