package org.eclipse.milo.platform.gateway;

public enum GatewayIdentifier {
    IO_FOLDER("{app-name}/IO"),
    IO_IDENTIFIER("{app-name}/IO/{io-name}"),
    INTERNAL_IDENTIFIER("{app-name}/IO/Internal"),
    AMQP_TAG("{app-name}/IO/AMQP/{io-name}/TAG/{tag-name}"),
    MODBUS_TAG("{app-name}/IO/Modbus/{io-name}/TAG/{tag-name}"),
    MQTT_TAG("{app-name}/IO/MQTT/{io-name}/TAG/{tag-name}"),
    OPC_TAG("{app-name}/IO/OPC/{io-name}/TAG/{tag-name}"),
    REST_TAG("{app-name}/IO/REST/{io-name}/TAG/{tag-name}"),
    S7_TAG("{app-name}/IO/S7/{io-name}/TAG/{tag-name}"),
    INTERNAL_TAG("{app-name}/IO/Internal/TAG/{node-class}/{tag-name}"),
    IO_Property("{io-identifier}/Property/{property-name}"),
    TAG_PROPERTY("{tag-identifier}/Property/{property-name}"),
    KAFKA_TAG("{app-name}/IO/Kafka/{io-name}/TAG/{tag-name}"),
    VALUE_PROPERTY_IDENTIFIER("{tag-identifier}/Property/value"),
    S7_AND_MODBUS_VALUE_PROPERTY_IDENTIFIER("{app-name}/IO/S7/{io-name}/TAG/Property/{topic}/value"),
    CONFIG_PROPERTIES("{io-identifier}/Property/Config/Property/{property-name}"),
    PROPERTY_BROWSE_NAME("Property/{property-name}"),
    PROTOCOL_PROPERTY_IDENTIFIER("{io-identifier}/Property/Config/Property/PROTOCOL");


    private final String identifier;

    GatewayIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getIdentifier() {
        return identifier;
    }
}
