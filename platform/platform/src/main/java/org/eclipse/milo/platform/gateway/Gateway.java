package org.eclipse.milo.platform.gateway;

import org.eclipse.milo.opcua.sdk.core.Reference;
import org.eclipse.milo.opcua.sdk.server.AbstractLifecycle;
import org.eclipse.milo.opcua.sdk.server.nodes.UaFolderNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNodeContext;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.platform.gateway.io.*;
import org.eclipse.milo.platform.gateway.methods.*;
import org.eclipse.milo.platform.util.Props;
import org.eclipse.milo.platform.util.Utils;
import org.slf4j.LoggerFactory;

public class Gateway extends AbstractLifecycle {
    private static String ROOT_APP_NAME = null;
    UaNodeContext uaNodeContext;
    MqttDevice mqttDevice;
    OpcUaDevice opcUaDevice;
    S7Device s7Device;
    RabbitMqDevice rabbitMqDevice;
    ActiveAMQPDevice activeAMQPDevice;
    KafkaDevice kafkaDevice;

    RestDevice restDevice;
    ModbusDevice modbusDevice;
    static Gateway gateWay;

    public static Gateway getInstance(UaNodeContext uaNodeContext) {
        if (gateWay == null) {
            gateWay = new Gateway(uaNodeContext);
        }
        return gateWay;
    }

    public static Gateway getInstance() {
        return gateWay;
    }

    private Gateway(UaNodeContext uaNodeContext) {
        this.uaNodeContext = uaNodeContext;
        this.mqttDevice = new MqttDevice(uaNodeContext);
        this.opcUaDevice = new OpcUaDevice(uaNodeContext);
        this.s7Device = new S7Device(uaNodeContext);
        this.rabbitMqDevice = new RabbitMqDevice(uaNodeContext);
        this.activeAMQPDevice = new ActiveAMQPDevice(uaNodeContext);
        this.restDevice = new RestDevice(uaNodeContext);
        this.modbusDevice = new ModbusDevice(uaNodeContext);
        this.kafkaDevice = new KafkaDevice(uaNodeContext);
        ROOT_APP_NAME = this.uaNodeContext.getServer().getConfig().getApplicationName().getText();
    }

    @Override
    protected void onStartup() {
        // root folder node
        UaFolderNode rootNode = (UaFolderNode) uaNodeContext.getNodeManager().get(new NodeId(Integer.parseInt((String) Props.getProperty("app-namespace-index")), ROOT_APP_NAME));
        // add module folder
        UaFolderNode devicesFolder = Utils.buildAndAddFolder(uaNodeContext, rootNode, "/IO", "IO");

        // bootstrap module methods
        injectMqttDeviceMethods(devicesFolder);
        injectMqttInputMethod(devicesFolder);

        injectRabbitAmqpDeviceMethod(devicesFolder);
        injectActiveAmqpDeviceMethod(devicesFolder);

        injectAmqpInputMethod(devicesFolder);

        injectS7DeviceMethods(devicesFolder);
        injectS7InputMethod(devicesFolder);

        injectOpcUaDeviceMethods(devicesFolder);
        injectOpcUaInputMethod(devicesFolder);

        injectNoInputMethod(devicesFolder);

        injectModbusDeviceMethods(devicesFolder);
        injectModbusInputMethod(devicesFolder);

        injectKafkaDeviceMethod(devicesFolder);
        injectKafkaInputMethod(devicesFolder);

        injectRestDeviceMethod(devicesFolder);
        injectRestInputMethod(devicesFolder);

        injectDeviceStateMethods(devicesFolder);


        LoggerFactory.getLogger(getClass()).info("Starting [Gateway] service might take a few seconds ...");
    }

    private void injectNoInputMethod(UaFolderNode rootNode) {
        UaMethodNode methodNode = UaMethodNode.builder(uaNodeContext).setNodeId(new NodeId(Integer.parseInt((String) Props.getProperty("app-namespace-index")), ROOT_APP_NAME + "/IO/NoDeviceInput")).setBrowseName(new QualifiedName(Integer.parseInt((String) Props.getProperty("app-namespace-index")), ROOT_APP_NAME + "/" + "IO/NoDeviceInput")).setDisplayName(new LocalizedText(null, "InternalInput")).setDescription(LocalizedText.english("InternalInput")).build();

        NoDeviceAttributeMethod noDeviceAttributeMethod = new NoDeviceAttributeMethod(methodNode);
        methodNode.setInputArguments(noDeviceAttributeMethod.getInputArguments());

        methodNode.setOutputArguments(noDeviceAttributeMethod.getOutputArguments());
        methodNode.setInvocationHandler(noDeviceAttributeMethod);
        uaNodeContext.getNodeManager().addNode(methodNode);
        methodNode.addReference(new Reference(methodNode.getNodeId(), Identifiers.HasComponent, rootNode.getNodeId().expanded(), false));

    }

    private void injectS7InputMethod(UaFolderNode rootNode) {
        UaMethodNode methodNode = UaMethodNode.builder(uaNodeContext).setNodeId(new NodeId(Integer.parseInt((String) Props.getProperty("app-namespace-index")), ROOT_APP_NAME + "/IO/S7DeviceInput")).setBrowseName(new QualifiedName(Integer.parseInt((String) Props.getProperty("app-namespace-index")), ROOT_APP_NAME + "/" + "IO/S7DeviceInput")).setDisplayName(new LocalizedText(null, "S7IOInput")).setDescription(LocalizedText.english("S7IOInput")).build();

        S7AttributeMethod s7TagMethod = new S7AttributeMethod(methodNode);
        methodNode.setInputArguments(s7TagMethod.getInputArguments());

        methodNode.setOutputArguments(s7TagMethod.getOutputArguments());
        methodNode.setInvocationHandler(s7TagMethod);
        uaNodeContext.getNodeManager().addNode(methodNode);
        methodNode.addReference(new Reference(methodNode.getNodeId(), Identifiers.HasComponent, rootNode.getNodeId().expanded(), false));
    }

    private void injectMqttInputMethod(UaFolderNode rootNode) {
        UaMethodNode methodNode = UaMethodNode.builder(uaNodeContext).setNodeId(new NodeId(Integer.parseInt((String) Props.getProperty("app-namespace-index")), ROOT_APP_NAME + "/IO/MqttDeviceInput")).setBrowseName(new QualifiedName(Integer.parseInt((String) Props.getProperty("app-namespace-index")), ROOT_APP_NAME + "/" + "IO/MqttDeviceInput")).setDisplayName(new LocalizedText(null, "MqttIOInput")).setDescription(LocalizedText.english("MqttIOInput")).build();

        MqttAttributeMethod mqttTagMethod = new MqttAttributeMethod(methodNode);
        methodNode.setInputArguments(mqttTagMethod.getInputArguments());
        methodNode.setOutputArguments(mqttTagMethod.getOutputArguments());
        methodNode.setInvocationHandler(mqttTagMethod);
        uaNodeContext.getNodeManager().addNode(methodNode);
        methodNode.addReference(new Reference(methodNode.getNodeId(), Identifiers.HasComponent, rootNode.getNodeId().expanded(), false));
    }

    private void injectAmqpInputMethod(UaFolderNode rootNode) {
        UaMethodNode methodNode = UaMethodNode.builder(uaNodeContext).setNodeId(new NodeId(Integer.parseInt((String) Props.getProperty("app-namespace-index")), ROOT_APP_NAME + "/IO/AmqpDeviceInput")).setBrowseName(new QualifiedName(Integer.parseInt((String) Props.getProperty("app-namespace-index")), ROOT_APP_NAME + "/" + "IO/AmqpDeviceInput")).setDisplayName(new LocalizedText(null, "AmqpIOInput")).setDescription(LocalizedText.english("AmqpIOInput")).build();
        AmqpAttributeMethod amqpTagMethod = new AmqpAttributeMethod(methodNode);
        methodNode.setInputArguments(amqpTagMethod.getInputArguments());
        methodNode.setOutputArguments(amqpTagMethod.getOutputArguments());
        methodNode.setInvocationHandler(amqpTagMethod);
        uaNodeContext.getNodeManager().addNode(methodNode);
        methodNode.addReference(new Reference(methodNode.getNodeId(), Identifiers.HasComponent, rootNode.getNodeId().expanded(), false));
    }

    private void injectRabbitAmqpDeviceMethod(UaFolderNode rootNode) {
        UaMethodNode methodNode = UaMethodNode.builder(uaNodeContext).setNodeId(new NodeId(Integer.parseInt((String) Props.getProperty("app-namespace-index")), ROOT_APP_NAME + "/IO/RabbitDevice")).setBrowseName(new QualifiedName(Integer.parseInt((String) Props.getProperty("app-namespace-index")), ROOT_APP_NAME + "/" + "IO/RabbitDevice")).setDisplayName(new LocalizedText(null, "RabbitIO")).setDescription(LocalizedText.english("RabbitIO")).build();
        RabbitAmqpMethod amqpInputMethod = new RabbitAmqpMethod(methodNode);
        methodNode.setInputArguments(amqpInputMethod.getInputArguments());
        methodNode.setOutputArguments(amqpInputMethod.getOutputArguments());
        methodNode.setInvocationHandler(amqpInputMethod);
        uaNodeContext.getNodeManager().addNode(methodNode);
        methodNode.addReference(new Reference(methodNode.getNodeId(), Identifiers.HasComponent, rootNode.getNodeId().expanded(), false));
    }

    private void injectActiveAmqpDeviceMethod(UaFolderNode rootNode) {
        UaMethodNode methodNode = UaMethodNode.builder(uaNodeContext).setNodeId(new NodeId(Integer.parseInt((String) Props.getProperty("app-namespace-index")), ROOT_APP_NAME + "/IO/ActiveDevice")).setBrowseName(new QualifiedName(Integer.parseInt((String) Props.getProperty("app-namespace-index")), ROOT_APP_NAME + "/" + "IO/ActiveDevice")).setDisplayName(new LocalizedText(null, "ActiveIO")).setDescription(LocalizedText.english("ActiveIO")).build();
        ActivemqMethod amqpInputMethod = new ActivemqMethod(methodNode);
        methodNode.setInputArguments(amqpInputMethod.getInputArguments());
        methodNode.setOutputArguments(amqpInputMethod.getOutputArguments());
        methodNode.setInvocationHandler(amqpInputMethod);
        uaNodeContext.getNodeManager().addNode(methodNode);
        methodNode.addReference(new Reference(methodNode.getNodeId(), Identifiers.HasComponent, rootNode.getNodeId().expanded(), false));
    }

    private void injectRestDeviceMethod(UaFolderNode rootNode) {
        UaMethodNode methodNode = UaMethodNode.builder(uaNodeContext).setNodeId(Utils.newNodeId(Integer.parseInt((String) Props.getProperty("app-namespace-index")), ROOT_APP_NAME + "/IO/RestDevice")).setBrowseName(new QualifiedName(Integer.parseInt((String) Props.getProperty("app-namespace-index")), ROOT_APP_NAME + "/" + "IO/RestDevice")).setDisplayName(new LocalizedText(null, "RestIO")).setDescription(LocalizedText.english("RestIO")).build();
        RestDeviceMethod restInputMethod = new RestDeviceMethod(methodNode);
        methodNode.setInputArguments(restInputMethod.getInputArguments());
        methodNode.setOutputArguments(restInputMethod.getOutputArguments());
        methodNode.setInvocationHandler(restInputMethod);
        uaNodeContext.getNodeManager().addNode(methodNode);
        methodNode.addReference(new Reference(methodNode.getNodeId(), Identifiers.HasComponent, rootNode.getNodeId().expanded(), false));
    }

    private void injectRestInputMethod(UaFolderNode rootNode) {
        UaMethodNode methodNode = UaMethodNode.builder(uaNodeContext).setNodeId(Utils.newNodeId(Integer.parseInt((String) Props.getProperty("app-namespace-index")), ROOT_APP_NAME + "/IO/RestDeviceInput")).setBrowseName(new QualifiedName(Integer.parseInt((String) Props.getProperty("app-namespace-index")), ROOT_APP_NAME + "/" + "IO/RestDeviceInput")).setDisplayName(new LocalizedText(null, "RestIOInput")).setDescription(LocalizedText.english("RestIOInput")).build();
        RestAttributeMethod restAttributeMethod = new RestAttributeMethod(methodNode);
        methodNode.setInputArguments(restAttributeMethod.getInputArguments());
        methodNode.setOutputArguments(restAttributeMethod.getOutputArguments());
        methodNode.setInvocationHandler(restAttributeMethod);
        uaNodeContext.getNodeManager().addNode(methodNode);
        methodNode.addReference(new Reference(methodNode.getNodeId(), Identifiers.HasComponent, rootNode.getNodeId().expanded(), false));
    }

    private void injectOpcUaInputMethod(UaFolderNode rootNode) {
        UaMethodNode methodNode = UaMethodNode.builder(uaNodeContext).setNodeId(new NodeId(Integer.parseInt((String) Props.getProperty("app-namespace-index")), ROOT_APP_NAME + "/IO/OpcUaDeviceInput")).setBrowseName(new QualifiedName(Integer.parseInt((String) Props.getProperty("app-namespace-index")), ROOT_APP_NAME + "/" + "IO/OpcUaDeviceInput")).setDisplayName(new LocalizedText(null, "OpcUaIOInput")).setDescription(LocalizedText.english("OpcUaIOInput")).build();

        OpcUaAttributeMethod opcuaTagMethod = new OpcUaAttributeMethod(methodNode);
        methodNode.setInputArguments(opcuaTagMethod.getInputArguments());
        methodNode.setOutputArguments(opcuaTagMethod.getOutputArguments());
        methodNode.setInvocationHandler(opcuaTagMethod);
        uaNodeContext.getNodeManager().addNode(methodNode);
        methodNode.addReference(new Reference(methodNode.getNodeId(), Identifiers.HasComponent, rootNode.getNodeId().expanded(), false));
    }

    private void injectMqttDeviceMethods(UaFolderNode rootNode) {
        UaMethodNode methodNode = UaMethodNode.builder(uaNodeContext).setNodeId(new NodeId(Integer.parseInt((String) Props.getProperty("app-namespace-index")), ROOT_APP_NAME + "/IO/MqttDevice")).setBrowseName(new QualifiedName(Integer.parseInt((String) Props.getProperty("app-namespace-index")), ROOT_APP_NAME + "/" + "IO/MqttDevice")).setDisplayName(new LocalizedText(null, "MqttIO")).setDescription(LocalizedText.english("MqttIO")).build();
        MqttDeviceMethod mqttInputMethod = new MqttDeviceMethod(methodNode);
        methodNode.setInputArguments(mqttInputMethod.getInputArguments());
        methodNode.setOutputArguments(mqttInputMethod.getOutputArguments());
        methodNode.setInvocationHandler(mqttInputMethod);
        uaNodeContext.getNodeManager().addNode(methodNode);
        methodNode.addReference(new Reference(methodNode.getNodeId(), Identifiers.HasComponent, rootNode.getNodeId().expanded(), false));
    }

    private void injectAmqpDeviceMethod(UaFolderNode rootNode) {
        UaMethodNode methodNode = UaMethodNode.builder(uaNodeContext).setNodeId(new NodeId(Integer.parseInt((String) Props.getProperty("app-namespace-index")), ROOT_APP_NAME + "/IO/AmqpDevice")).setBrowseName(new QualifiedName(Integer.parseInt((String) Props.getProperty("app-namespace-index")), ROOT_APP_NAME + "/" + "IO/AmqpDevice")).setDisplayName(new LocalizedText(null, "AmqpIO")).setDescription(LocalizedText.english("AmqpIO")).build();
        RabbitAmqpMethod rabbitAmqpMethod = new RabbitAmqpMethod(methodNode);
        methodNode.setInputArguments(rabbitAmqpMethod.getInputArguments());
        methodNode.setOutputArguments(rabbitAmqpMethod.getOutputArguments());
        methodNode.setInvocationHandler(rabbitAmqpMethod);
        uaNodeContext.getNodeManager().addNode(methodNode);
        methodNode.addReference(new Reference(methodNode.getNodeId(), Identifiers.HasComponent, rootNode.getNodeId().expanded(), false));
    }

    private void injectS7DeviceMethods(UaFolderNode rootNode) {
        UaMethodNode methodNode = UaMethodNode.builder(uaNodeContext).setNodeId(new NodeId(Integer.parseInt((String) Props.getProperty("app-namespace-index")), ROOT_APP_NAME + "/IO/S7Device")).setBrowseName(new QualifiedName(Integer.parseInt((String) Props.getProperty("app-namespace-index")), ROOT_APP_NAME + "/IO/S7Device")).setDisplayName(new LocalizedText(null, "S7IO")).setDescription(LocalizedText.english("S7IO")).build();
        S7DeviceMethod s7InputMethod = new S7DeviceMethod(methodNode);
        methodNode.setInputArguments(s7InputMethod.getInputArguments());
        methodNode.setOutputArguments(s7InputMethod.getOutputArguments());
        methodNode.setInvocationHandler(s7InputMethod);
        uaNodeContext.getNodeManager().addNode(methodNode);
        methodNode.addReference(new Reference(methodNode.getNodeId(), Identifiers.HasComponent, rootNode.getNodeId().expanded(), false));
    }

    private void injectOpcUaDeviceMethods(UaFolderNode rootNode) {
        UaMethodNode methodNode = UaMethodNode.builder(uaNodeContext).setNodeId(new NodeId(Integer.parseInt((String) Props.getProperty("app-namespace-index")), ROOT_APP_NAME + "/IO/OpcUaDevice")).setBrowseName(new QualifiedName(Integer.parseInt((String) Props.getProperty("app-namespace-index")), ROOT_APP_NAME + "/IO/OpcUaDevice")).setDisplayName(new LocalizedText(null, "OpcUaIO")).setDescription(LocalizedText.english("OpcUaIO")).build();
        OpcUaDeviceMethod opcuaInputMethod = new OpcUaDeviceMethod(methodNode);
        methodNode.setInputArguments(opcuaInputMethod.getInputArguments());
        methodNode.setOutputArguments(opcuaInputMethod.getOutputArguments());
        methodNode.setInvocationHandler(opcuaInputMethod);
        uaNodeContext.getNodeManager().addNode(methodNode);
        methodNode.addReference(new Reference(methodNode.getNodeId(), Identifiers.HasComponent, rootNode.getNodeId().expanded(), false));
    }

    private void injectDeviceStateMethods(UaFolderNode rootNode) {
        UaMethodNode methodNode = UaMethodNode.builder(uaNodeContext).setNodeId(new NodeId(Integer.parseInt((String) Props.getProperty("app-namespace-index")), ROOT_APP_NAME + "/IO/State")).setBrowseName(new QualifiedName(Integer.parseInt((String) Props.getProperty("app-namespace-index")), ROOT_APP_NAME + "/IO/State")).setDisplayName(new LocalizedText(null, "State")).setDescription(LocalizedText.english("State")).build();
        ChangeDeviceState changeDeviceState = new ChangeDeviceState(methodNode, this);
        methodNode.setInputArguments(changeDeviceState.getInputArguments());
        methodNode.setOutputArguments(changeDeviceState.getOutputArguments());
        methodNode.setInvocationHandler(changeDeviceState);
        uaNodeContext.getNodeManager().addNode(methodNode);
        methodNode.addReference(new Reference(methodNode.getNodeId(), Identifiers.HasComponent, rootNode.getNodeId().expanded(), false));
    }


    private void injectModbusDeviceMethods(UaFolderNode rootNode) {
        UaMethodNode methodNode = UaMethodNode.builder(uaNodeContext).setNodeId(new NodeId(Integer.parseInt((String) Props.getProperty("app-namespace-index")), ROOT_APP_NAME + "/IO/ModbusDevice")).setBrowseName(new QualifiedName(Integer.parseInt((String) Props.getProperty("app-namespace-index")), ROOT_APP_NAME + "/IO/ModbusDevice")).setDisplayName(new LocalizedText(null, "ModbusIO")).setDescription(LocalizedText.english("ModbusIO")).build();
        ModbusDeviceMethod modbusInputMethod = new ModbusDeviceMethod(methodNode);
        methodNode.setInputArguments(modbusInputMethod.getInputArguments());
        methodNode.setOutputArguments(modbusInputMethod.getOutputArguments());
        methodNode.setInvocationHandler(modbusInputMethod);
        uaNodeContext.getNodeManager().addNode(methodNode);
        methodNode.addReference(new Reference(methodNode.getNodeId(), Identifiers.HasComponent, rootNode.getNodeId().expanded(), false));
    }

    private void injectModbusInputMethod(UaFolderNode rootNode) {
        UaMethodNode methodNode = UaMethodNode.builder(uaNodeContext).setNodeId(new NodeId(Integer.parseInt((String) Props.getProperty("app-namespace-index")), ROOT_APP_NAME + "/IO/ModbusDeviceInput")).setBrowseName(new QualifiedName(Integer.parseInt((String) Props.getProperty("app-namespace-index")), ROOT_APP_NAME + "/" + "IO/ModbusDeviceInput")).setDisplayName(new LocalizedText(null, "ModbusIOInput")).setDescription(LocalizedText.english("ModbusIOInput")).build();
        ModbusAttributeMethod opcuaTagMethod = new ModbusAttributeMethod(methodNode);
        methodNode.setInputArguments(opcuaTagMethod.getInputArguments());
        methodNode.setOutputArguments(opcuaTagMethod.getOutputArguments());
        methodNode.setInvocationHandler(opcuaTagMethod);
        uaNodeContext.getNodeManager().addNode(methodNode);
        methodNode.addReference(new Reference(methodNode.getNodeId(), Identifiers.HasComponent, rootNode.getNodeId().expanded(), false));
    }

    private void injectKafkaInputMethod(UaFolderNode rootNode) {
        UaMethodNode methodNode = UaMethodNode.builder(uaNodeContext).setNodeId(new NodeId(Integer.parseInt((String) Props.getProperty("app-namespace-index")), ROOT_APP_NAME + "/IO/KafkaDeviceInput")).setBrowseName(new QualifiedName(Integer.parseInt((String) Props.getProperty("app-namespace-index")), ROOT_APP_NAME + "/" + "IO/KafkaDeviceInput")).setDisplayName(new LocalizedText(null, "KafkaIOInput")).setDescription(LocalizedText.english("KafkaIOInput")).build();
        KafkaAttributeMethod kafkaAttributeMethod = new KafkaAttributeMethod(methodNode);
        methodNode.setInputArguments(kafkaAttributeMethod.getInputArguments());
        methodNode.setOutputArguments(kafkaAttributeMethod.getOutputArguments());
        methodNode.setInvocationHandler(kafkaAttributeMethod);
        uaNodeContext.getNodeManager().addNode(methodNode);
        methodNode.addReference(new Reference(methodNode.getNodeId(), Identifiers.HasComponent, rootNode.getNodeId().expanded(), false));
    }

    private void injectKafkaDeviceMethod(UaFolderNode rootNode) {
        UaMethodNode methodNode = UaMethodNode.builder(uaNodeContext).setNodeId(new NodeId(Integer.parseInt((String) Props.getProperty("app-namespace-index")), ROOT_APP_NAME + "/IO/KafkaDevice")).setBrowseName(new QualifiedName(Integer.parseInt((String) Props.getProperty("app-namespace-index")), ROOT_APP_NAME + "/" + "IO/KafkaDevice")).setDisplayName(new LocalizedText(null, "KafkaIO")).setDescription(LocalizedText.english("KafkaIO")).build();
        KafkaDeviceMethod kafkaDeviceMethod = new KafkaDeviceMethod(methodNode);
        methodNode.setInputArguments(kafkaDeviceMethod.getInputArguments());
        methodNode.setOutputArguments(kafkaDeviceMethod.getOutputArguments());
        methodNode.setInvocationHandler(kafkaDeviceMethod);
        uaNodeContext.getNodeManager().addNode(methodNode);
        methodNode.addReference(new Reference(methodNode.getNodeId(), Identifiers.HasComponent, rootNode.getNodeId().expanded(), false));
    }

    @Override
    protected void onShutdown() {
        LoggerFactory.getLogger(getClass()).info("Gateway service has been shutdown.");
    }

    public MqttDevice getMqttDevice() {
        return mqttDevice;
    }

    public RabbitMqDevice getRabbitMQ() {
        return rabbitMqDevice;
    }

    public ActiveAMQPDevice getActiveMQ() {
        return activeAMQPDevice;
    }

    public RestDevice getRestDevice() {
        return restDevice;
    }

    public void setRabbitMqDevice(RabbitMqDevice rabbitMqDevice) {
        this.rabbitMqDevice = rabbitMqDevice;
    }

    public void setActiveMqDevice(ActiveAMQPDevice activeMqDevice) {
        this.activeAMQPDevice = activeMqDevice;
    }

    public ModbusDevice getModbusDevice() {
        return modbusDevice;
    }

    public void setModbusDevice(ModbusDevice modbusDevice) {
        this.modbusDevice = modbusDevice;
    }

    public void setMqttDevice(MqttDevice mqttDevice) {
        this.mqttDevice = mqttDevice;
    }

    public OpcUaDevice getOpcUaDevice() {
        return opcUaDevice;
    }

    public void setOpcUaDevice(OpcUaDevice opcUaDevice) {
        this.opcUaDevice = opcUaDevice;
    }

    public UaNodeContext getUaNodeContext() {
        return uaNodeContext;
    }

    public void setUaNodeContext(UaNodeContext uaNodeContext) {
        this.uaNodeContext = uaNodeContext;
    }

    public S7Device getS7Device() {
        return s7Device;
    }

    public void setS7Device(S7Device s7Device) {
        this.s7Device = s7Device;
    }

    public KafkaDevice getKafkaDevice() {
        return kafkaDevice;
    }
}
