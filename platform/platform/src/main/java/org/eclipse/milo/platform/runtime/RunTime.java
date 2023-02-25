package org.eclipse.milo.platform.runtime;

import org.eclipse.milo.opcua.sdk.core.Reference;
import org.eclipse.milo.opcua.sdk.server.AbstractLifecycle;
import org.eclipse.milo.opcua.sdk.server.nodes.UaFolderNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNodeContext;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.platform.runtime.methods.*;
import org.eclipse.milo.platform.runtime.methods.uaInterfaces.*;
import org.eclipse.milo.platform.runtime.observers.JavascriptRuntimeObserver;
import org.eclipse.milo.platform.runtime.observers.JavascriptWindowingRuntimeObserver;
import org.eclipse.milo.platform.util.Props;
import org.eclipse.milo.platform.util.Utils;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class RunTime extends AbstractLifecycle {
    private static String ROOT_APP_NAME = null;
    private Map<String, Map<NodeId, JavascriptRuntimeObserver>> javascriptRuntimeObserverListMap = new ConcurrentHashMap<>();
    private Map<String, Map<NodeId, JavascriptWindowingRuntimeObserver>> javascriptWindowingRuntimeObserverListMap = new ConcurrentHashMap<>();
    UaNodeContext uaNodeContext;

    public void load(UaNode uaNode, UaNode component, String script) {
        JavascriptRuntimeObserver javascriptRuntimeObserver = new JavascriptRuntimeObserver(this.uaNodeContext, script);
        uaNode.addAttributeObserver(javascriptRuntimeObserver);
        if (javascriptRuntimeObserverListMap.get(uaNode.getNodeId().getIdentifier().toString()) == null) {
            javascriptRuntimeObserverListMap.put(component.getNodeId().getIdentifier().toString(), new HashMap<>());
        }
        javascriptRuntimeObserverListMap.get(component.getNodeId().getIdentifier().toString()).put(uaNode.getNodeId(), javascriptRuntimeObserver);
    }

    public void unload(UaNode component) throws Exception {
        if (this.javascriptRuntimeObserverListMap.get(component.getNodeId().getIdentifier().toString()) != null) {
            Objects.requireNonNull(this.javascriptRuntimeObserverListMap.get(component.getNodeId().getIdentifier().toString())).forEach((nodeId, javascriptRuntimeObserver) -> {
                this.uaNodeContext.getNodeManager().get(nodeId).removeAttributeObserver(javascriptRuntimeObserver);
                this.javascriptRuntimeObserverListMap.remove(nodeId);
            });
        }
        getUaNodeContext().getNodeManager().cacheNodeManager().getTriggerParentFactoryCache().deleteParent(component.getNodeId().getIdentifier().toString());
    }

    public void delete(UaNode componentNode) throws Exception {
        this.unload(componentNode);
        componentNode.delete();
    }

    public void windowingLoad(UaNode uaNode, UaNode componentNode, String mainNode, long windowingLength, Long sampleInterval, String[] nodesToWindow, NodeId outPutNodeId) {
        JavascriptWindowingRuntimeObserver javascriptWindowingRuntimeObserver = new JavascriptWindowingRuntimeObserver(this.uaNodeContext, mainNode, windowingLength, sampleInterval, nodesToWindow, outPutNodeId);
        uaNode.addAttributeObserver(javascriptWindowingRuntimeObserver);
        if (javascriptWindowingRuntimeObserverListMap.get(javascriptWindowingRuntimeObserver) == null) {
            javascriptWindowingRuntimeObserverListMap.put(componentNode.getNodeId().getIdentifier().toString(), new HashMap<>());
        }
        javascriptWindowingRuntimeObserverListMap.get(componentNode.getNodeId().getIdentifier().toString()).put(uaNode.getNodeId(), javascriptWindowingRuntimeObserver);
    }

    public RunTime(UaNodeContext uaNodeContext) {
        this.uaNodeContext = uaNodeContext;
        ROOT_APP_NAME = this.uaNodeContext.getServer().getConfig().getApplicationName().getText();
    }

    @Override
    protected void onStartup() {
        try {
            LoggerFactory.getLogger(getClass()).info("Starting [RunTime] service, it might take a few seconds ...");
            // root folder node
            @Nullable UaFolderNode rootNode = (UaFolderNode) uaNodeContext.getNodeManager().get(new NodeId(Integer.parseInt((String) Props.getProperty("app-namespace-index")), ROOT_APP_NAME));
            // add module folder
            UaFolderNode programsFolder = Utils.buildAndAddFolder(uaNodeContext, rootNode, "/Runtime", "Runtime");
            UaFolderNode interfacesFolder = Utils.buildAndAddFolder(uaNodeContext, programsFolder, "/Runtime/Interfaces", "Interfaces");
            injectMethods(programsFolder);
            injectInterfacesMethods(interfacesFolder);
        } catch (Exception e) {
            LoggerFactory.getLogger(getClass()).error("[Runtime] service, went through errors : {}", e.getMessage());
        }
    }

    private void injectInterfacesMethods(UaFolderNode interfacesFolder) {
        UaMethodNode getArchivedValueMethodNode = UaMethodNode.builder(uaNodeContext).setNodeId(new NodeId(Integer.parseInt((String) Props.getProperty("app-namespace-index")), ROOT_APP_NAME + "/Runtime/JavaScript/GetArchivedValue")).setBrowseName(new QualifiedName(Integer.parseInt((String) Props.getProperty("app-namespace-index")), ROOT_APP_NAME + "/Runtime/JavaScript/GetArchivedValue")).setDisplayName(new LocalizedText(null, "GetArchivedValue")).setDescription(LocalizedText.english("GetArchivedValue")).build();
        GetSimpleArchivedValue getArchivedValueMethod = new GetSimpleArchivedValue(getArchivedValueMethodNode);
        getArchivedValueMethodNode.setInputArguments(getArchivedValueMethod.getInputArguments());
        getArchivedValueMethodNode.setOutputArguments(getArchivedValueMethod.getOutputArguments());
        getArchivedValueMethodNode.setInvocationHandler(getArchivedValueMethod);
        uaNodeContext.getNodeManager().addNode(getArchivedValueMethodNode);
        getArchivedValueMethodNode.addReference(new Reference(getArchivedValueMethodNode.getNodeId(), Identifiers.HasComponent, interfacesFolder.getNodeId().expanded(), false));

        UaMethodNode fireMethodNode = UaMethodNode.builder(uaNodeContext).setNodeId(new NodeId(Integer.parseInt((String) Props.getProperty("app-namespace-index")), ROOT_APP_NAME + "/Runtime/JavaScript/Fire")).setBrowseName(new QualifiedName(Integer.parseInt((String) Props.getProperty("app-namespace-index")), ROOT_APP_NAME + "/Runtime/JavaScript/Fire")).setDisplayName(new LocalizedText(null, "Fire")).setDescription(LocalizedText.english("Fire")).build();
        Fire fire = new Fire(fireMethodNode);
        fireMethodNode.setInputArguments(fire.getInputArguments());
        fireMethodNode.setOutputArguments(fire.getOutputArguments());
        fireMethodNode.setInvocationHandler(fire);
        uaNodeContext.getNodeManager().addNode(fireMethodNode);
        fireMethodNode.addReference(new Reference(fireMethodNode.getNodeId(), Identifiers.HasComponent, interfacesFolder.getNodeId().expanded(), false));


        UaMethodNode ackMethodNode = UaMethodNode.builder(uaNodeContext).setNodeId(new NodeId(Integer.parseInt((String) Props.getProperty("app-namespace-index")), ROOT_APP_NAME + "/Runtime/JavaScript/Ack")).setBrowseName(new QualifiedName(Integer.parseInt((String) Props.getProperty("app-namespace-index")), ROOT_APP_NAME + "/Runtime/JavaScript/Ack")).setDisplayName(new LocalizedText(null, "Ack")).setDescription(LocalizedText.english("Ack")).build();
        Ack ack = new Ack(ackMethodNode);
        ackMethodNode.setInputArguments(ack.getInputArguments());
        ackMethodNode.setOutputArguments(ack.getOutputArguments());
        ackMethodNode.setInvocationHandler(ack);
        uaNodeContext.getNodeManager().addNode(ackMethodNode);
        ackMethodNode.addReference(new Reference(ackMethodNode.getNodeId(), Identifiers.HasComponent, interfacesFolder.getNodeId().expanded(), false));

        UaMethodNode eventInfoMethodNode = UaMethodNode.builder(uaNodeContext).setNodeId(new NodeId(Integer.parseInt((String) Props.getProperty("app-namespace-index")), ROOT_APP_NAME + "/Runtime/JavaScript/EventInfo")).setBrowseName(new QualifiedName(Integer.parseInt((String) Props.getProperty("app-namespace-index")), ROOT_APP_NAME + "/Runtime/JavaScript/EventInfo")).setDisplayName(new LocalizedText(null, "EventInfo")).setDescription(LocalizedText.english("EventInfo")).build();
        EventInfo eventInfo = new EventInfo(eventInfoMethodNode);
        eventInfoMethodNode.setInputArguments(eventInfo.getInputArguments());
        eventInfoMethodNode.setOutputArguments(eventInfo.getOutputArguments());
        eventInfoMethodNode.setInvocationHandler(eventInfo);
        uaNodeContext.getNodeManager().addNode(eventInfoMethodNode);
        eventInfoMethodNode.addReference(new Reference(eventInfoMethodNode.getNodeId(), Identifiers.HasComponent, interfacesFolder.getNodeId().expanded(), false));

        UaMethodNode stateMethodNode = UaMethodNode.builder(uaNodeContext).setNodeId(new NodeId(Integer.parseInt((String) Props.getProperty("app-namespace-index")), ROOT_APP_NAME + "/Runtime/JavaScript/State")).setBrowseName(new QualifiedName(Integer.parseInt((String) Props.getProperty("app-namespace-index")), ROOT_APP_NAME + "/Runtime/JavaScript/State")).setDisplayName(new LocalizedText(null, "State")).setDescription(LocalizedText.english("State")).build();
        State state = new State(stateMethodNode);
        stateMethodNode.setInputArguments(state.getInputArguments());
        stateMethodNode.setOutputArguments(state.getOutputArguments());
        stateMethodNode.setInvocationHandler(state);
        uaNodeContext.getNodeManager().addNode(stateMethodNode);
        stateMethodNode.addReference(new Reference(stateMethodNode.getNodeId(), Identifiers.HasComponent, interfacesFolder.getNodeId().expanded(), false));


        UaMethodNode GetNodeValueMethodNode = UaMethodNode.builder(uaNodeContext).setNodeId(new NodeId(Integer.parseInt((String) Props.getProperty("app-namespace-index")), ROOT_APP_NAME + "/Runtime/JavaScript/GetNodeValue")).setBrowseName(new QualifiedName(Integer.parseInt((String) Props.getProperty("app-namespace-index")), ROOT_APP_NAME + "/Runtime/JavaScript/GetNodeValue")).setDisplayName(new LocalizedText(null, "GetNodeValue")).setDescription(LocalizedText.english("GetNodeValue")).build();
        GetNodeValue getNodeValue = new GetNodeValue(GetNodeValueMethodNode);
        GetNodeValueMethodNode.setInputArguments(getNodeValue.getInputArguments());
        GetNodeValueMethodNode.setOutputArguments(getNodeValue.getOutputArguments());
        GetNodeValueMethodNode.setInvocationHandler(getNodeValue);
        uaNodeContext.getNodeManager().addNode(GetNodeValueMethodNode);
        GetNodeValueMethodNode.addReference(new Reference(GetNodeValueMethodNode.getNodeId(), Identifiers.HasComponent, interfacesFolder.getNodeId().expanded(), false));

        UaMethodNode GetNodeValuesMethodNode = UaMethodNode.builder(uaNodeContext).setNodeId(new NodeId(Integer.parseInt((String) Props.getProperty("app-namespace-index")), ROOT_APP_NAME + "/Runtime/JavaScript/GetNodeValues")).setBrowseName(new QualifiedName(Integer.parseInt((String) Props.getProperty("app-namespace-index")), ROOT_APP_NAME + "/Runtime/JavaScript/GetNodeValues")).setDisplayName(new LocalizedText(null, "GetNodeValues")).setDescription(LocalizedText.english("GetNodeValues")).build();
        GetNodeValues getNodeValues = new GetNodeValues(GetNodeValuesMethodNode);
        GetNodeValuesMethodNode.setInputArguments(getNodeValues.getInputArguments());
        GetNodeValuesMethodNode.setOutputArguments(getNodeValues.getOutputArguments());
        GetNodeValuesMethodNode.setInvocationHandler(getNodeValues);
        uaNodeContext.getNodeManager().addNode(GetNodeValuesMethodNode);
        GetNodeValuesMethodNode.addReference(new Reference(GetNodeValuesMethodNode.getNodeId(), Identifiers.HasComponent, interfacesFolder.getNodeId().expanded(), false));

        UaMethodNode SaveNodeMethodNode = UaMethodNode.builder(uaNodeContext).setNodeId(new NodeId(Integer.parseInt((String) Props.getProperty("app-namespace-index")), ROOT_APP_NAME + "/Runtime/JavaScript/SaveNode")).setBrowseName(new QualifiedName(Integer.parseInt((String) Props.getProperty("app-namespace-index")), ROOT_APP_NAME + "/Runtime/JavaScript/SaveNode")).setDisplayName(new LocalizedText(null, "SaveNode")).setDescription(LocalizedText.english("SaveNode")).build();
        SaveNode saveNode = new SaveNode(SaveNodeMethodNode);
        SaveNodeMethodNode.setInputArguments(saveNode.getInputArguments());
        SaveNodeMethodNode.setOutputArguments(saveNode.getOutputArguments());
        SaveNodeMethodNode.setInvocationHandler(saveNode);
        uaNodeContext.getNodeManager().addNode(SaveNodeMethodNode);
        SaveNodeMethodNode.addReference(new Reference(SaveNodeMethodNode.getNodeId(), Identifiers.HasComponent, interfacesFolder.getNodeId().expanded(), false));

        UaMethodNode sendMessageToAMPQMethodNode = UaMethodNode.builder(uaNodeContext).setNodeId(new NodeId(Integer.parseInt((String) Props.getProperty("app-namespace-index")), ROOT_APP_NAME + "/Runtime/JavaScript/SendMessageToRabbitMQ")).setBrowseName(new QualifiedName(Integer.parseInt((String) Props.getProperty("app-namespace-index")), ROOT_APP_NAME + "/Runtime/JavaScript/SendMessageToRabbitMQ")).setDisplayName(new LocalizedText(null, "sendMessageToRabbitMQ")).setDescription(LocalizedText.english("SendMessageToRabbitMQ")).build();
        SendMessageToRabbitMQ sendMessageToAMQPDevice = new SendMessageToRabbitMQ(sendMessageToAMPQMethodNode);
        sendMessageToAMPQMethodNode.setInputArguments(sendMessageToAMQPDevice.getInputArguments());
        sendMessageToAMPQMethodNode.setOutputArguments(sendMessageToAMQPDevice.getOutputArguments());
        sendMessageToAMPQMethodNode.setInvocationHandler(sendMessageToAMQPDevice);
        uaNodeContext.getNodeManager().addNode(sendMessageToAMPQMethodNode);
        sendMessageToAMPQMethodNode.addReference(new Reference(sendMessageToAMPQMethodNode.getNodeId(), Identifiers.HasComponent, interfacesFolder.getNodeId().expanded(), false));

        UaMethodNode activemqMethodNode = UaMethodNode.builder(uaNodeContext).setNodeId(new NodeId(Integer.parseInt((String) Props.getProperty("app-namespace-index")), ROOT_APP_NAME + "/Runtime/JavaScript/sendMessageToActiveMQ")).setBrowseName(new QualifiedName(Integer.parseInt((String) Props.getProperty("app-namespace-index")), ROOT_APP_NAME + "/Runtime/JavaScript/sendMessageToActiveMQ")).setDisplayName(new LocalizedText(null, "sendMessageToActiveMQ")).setDescription(LocalizedText.english("sendMessageToActiveMQ")).build();
        SendMessageToActivemq sendMessageToActivemqDevice = new SendMessageToActivemq(activemqMethodNode);
        activemqMethodNode.setInputArguments(sendMessageToActivemqDevice.getInputArguments());
        activemqMethodNode.setOutputArguments(sendMessageToActivemqDevice.getOutputArguments());
        activemqMethodNode.setInvocationHandler(sendMessageToActivemqDevice);
        uaNodeContext.getNodeManager().addNode(activemqMethodNode);
        activemqMethodNode.addReference(new Reference(activemqMethodNode.getNodeId(), Identifiers.HasComponent, interfacesFolder.getNodeId().expanded(), false));

        UaMethodNode sendMessageToMqttMethodNode = UaMethodNode.builder(uaNodeContext).setNodeId(new NodeId(Integer.parseInt((String) Props.getProperty("app-namespace-index")), ROOT_APP_NAME + "/Runtime/JavaScript/SendMessageToMqtt")).setBrowseName(new QualifiedName(Integer.parseInt((String) Props.getProperty("app-namespace-index")), ROOT_APP_NAME + "/Runtime/JavaScript/SendMessageToMqtt")).setDisplayName(new LocalizedText(null, "sendMessageToMqtt")).setDescription(LocalizedText.english("SendMessageToMqtt")).build();
        SendMessageToMqtt sendMessageToMqttDevice = new SendMessageToMqtt(sendMessageToMqttMethodNode);
        sendMessageToMqttMethodNode.setInputArguments(sendMessageToMqttDevice.getInputArguments());
        sendMessageToMqttMethodNode.setOutputArguments(sendMessageToMqttDevice.getOutputArguments());
        sendMessageToMqttMethodNode.setInvocationHandler(sendMessageToMqttDevice);
        uaNodeContext.getNodeManager().addNode(sendMessageToMqttMethodNode);
        sendMessageToMqttMethodNode.addReference(new Reference(sendMessageToMqttMethodNode.getNodeId(), Identifiers.HasComponent, interfacesFolder.getNodeId().expanded(), false));

        UaMethodNode sendMessageToKafkaMethodNode = UaMethodNode.builder(uaNodeContext).setNodeId(new NodeId(Integer.parseInt((String) Props.getProperty("app-namespace-index")), ROOT_APP_NAME + "/Runtime/JavaScript/SendMessageToKafka")).setBrowseName(new QualifiedName(Integer.parseInt((String) Props.getProperty("app-namespace-index")), ROOT_APP_NAME + "/Runtime/JavaScript/SendMessageToKafka")).setDisplayName(new LocalizedText(null, "sendMessageToKafka")).setDescription(LocalizedText.english("SendMessageToKafka")).build();
        SendMessageToKafka sendMessageToKafka = new SendMessageToKafka(sendMessageToKafkaMethodNode);
        sendMessageToKafkaMethodNode.setInputArguments(sendMessageToKafka.getInputArguments());
        sendMessageToKafkaMethodNode.setOutputArguments(sendMessageToKafka.getOutputArguments());
        sendMessageToKafkaMethodNode.setInvocationHandler(sendMessageToKafka);
        uaNodeContext.getNodeManager().addNode(sendMessageToKafkaMethodNode);
        sendMessageToKafkaMethodNode.addReference(new Reference(sendMessageToKafkaMethodNode.getNodeId(), Identifiers.HasComponent, interfacesFolder.getNodeId().expanded(), false));


        UaMethodNode writeValueToOpcUaMethodNode = UaMethodNode.builder(uaNodeContext).setNodeId(new NodeId(Integer.parseInt((String) Props.getProperty("app-namespace-index")), ROOT_APP_NAME + "/Runtime/JavaScript/WriteValueToOpcUa")).setBrowseName(new QualifiedName(Integer.parseInt((String) Props.getProperty("app-namespace-index")), ROOT_APP_NAME + "/Runtime/JavaScript/WriteValueToOpcUa")).setDisplayName(new LocalizedText(null, "writeValueToOpcUa")).setDescription(LocalizedText.english("WriteValueToOpcUa")).build();
        WriteValueToOpcUa writeValueToOpcUaDevice = new WriteValueToOpcUa(writeValueToOpcUaMethodNode);
        writeValueToOpcUaMethodNode.setInputArguments(writeValueToOpcUaDevice.getInputArguments());
        writeValueToOpcUaMethodNode.setOutputArguments(writeValueToOpcUaDevice.getOutputArguments());
        writeValueToOpcUaMethodNode.setInvocationHandler(writeValueToOpcUaDevice);
        uaNodeContext.getNodeManager().addNode(writeValueToOpcUaMethodNode);
        writeValueToOpcUaMethodNode.addReference(new Reference(writeValueToOpcUaMethodNode.getNodeId(), Identifiers.HasComponent, interfacesFolder.getNodeId().expanded(), false));


        UaMethodNode writeBatchValuesToOpcUaMethodNode = UaMethodNode.builder(uaNodeContext).setNodeId(new NodeId(Integer.parseInt((String) Props.getProperty("app-namespace-index")), ROOT_APP_NAME + "/Runtime/JavaScript/WriteBatchValuesToOpcUa")).setBrowseName(new QualifiedName(Integer.parseInt((String) Props.getProperty("app-namespace-index")), ROOT_APP_NAME + "/Runtime/JavaScript/WriteBatchValuesToOpcUa")).setDisplayName(new LocalizedText(null, "WriteBatchValuesToOpcUa")).setDescription(LocalizedText.english("WriteBatchValuesToOpcUa")).build();
        writeBatchValueToOpcUa writeBatchValuesToOpcUaDevice = new writeBatchValueToOpcUa(writeBatchValuesToOpcUaMethodNode);
        writeBatchValuesToOpcUaMethodNode.setInputArguments(writeBatchValuesToOpcUaDevice.getInputArguments());
        writeBatchValuesToOpcUaMethodNode.setOutputArguments(writeBatchValuesToOpcUaDevice.getOutputArguments());
        writeBatchValuesToOpcUaMethodNode.setInvocationHandler(writeBatchValuesToOpcUaDevice);
        uaNodeContext.getNodeManager().addNode(writeBatchValuesToOpcUaMethodNode);
        writeBatchValuesToOpcUaMethodNode.addReference(new Reference(writeBatchValuesToOpcUaMethodNode.getNodeId(), Identifiers.HasComponent, interfacesFolder.getNodeId().expanded(), false));


        UaMethodNode GetOutputFromComponentMethodNode = UaMethodNode.builder(uaNodeContext).setNodeId(new NodeId(Integer.parseInt((String) Props.getProperty("app-namespace-index")), ROOT_APP_NAME + "/Runtime/JavaScript/GetOutputFromComponent")).setBrowseName(new QualifiedName(Integer.parseInt((String) Props.getProperty("app-namespace-index")), ROOT_APP_NAME + "/Runtime/JavaScript/GetOutputFromComponent")).setDisplayName(new LocalizedText(null, "GetOutputFromComponent")).setDescription(LocalizedText.english("GetOutputFromComponent")).build();
        GetOutputFromComponent getOutputFromComponent = new GetOutputFromComponent(GetOutputFromComponentMethodNode);
        GetOutputFromComponentMethodNode.setInputArguments(getOutputFromComponent.getInputArguments());
        GetOutputFromComponentMethodNode.setOutputArguments(getOutputFromComponent.getOutputArguments());
        GetOutputFromComponentMethodNode.setInvocationHandler(getOutputFromComponent);
        uaNodeContext.getNodeManager().addNode(GetOutputFromComponentMethodNode);
        GetOutputFromComponentMethodNode.addReference(new Reference(GetOutputFromComponentMethodNode.getNodeId(), Identifiers.HasComponent, interfacesFolder.getNodeId().expanded(), false));
    }

    private void injectMethods(UaFolderNode programsFolder) {

        UaMethodNode testNloadMethodNode = UaMethodNode.builder(uaNodeContext).setNodeId(new NodeId(Integer.parseInt((String) Props.getProperty("app-namespace-index")), ROOT_APP_NAME + "/Runtime/JavaScript/TestNLoad")).setBrowseName(new QualifiedName(Integer.parseInt((String) Props.getProperty("app-namespace-index")), ROOT_APP_NAME + "/Runtime/JavaScript/TestNLoad")).setDisplayName(new LocalizedText(null, "TestNJavaScriptProgramLoad")).setDescription(LocalizedText.english("TestNJavaScriptProgramLoad")).build();
        TestNJavaScriptProgramLoadMethod testNjavaScriptProgramLoadMethod = new TestNJavaScriptProgramLoadMethod(testNloadMethodNode, this);
        testNloadMethodNode.setInputArguments(testNjavaScriptProgramLoadMethod.getInputArguments());
        testNloadMethodNode.setOutputArguments(testNjavaScriptProgramLoadMethod.getOutputArguments());
        testNloadMethodNode.setInvocationHandler(testNjavaScriptProgramLoadMethod);

        // bootstrap module methods
        UaMethodNode loadMethodNode = UaMethodNode.builder(uaNodeContext).setNodeId(new NodeId(Integer.parseInt((String) Props.getProperty("app-namespace-index")), ROOT_APP_NAME + "/Runtime/JavaScript/Load")).setBrowseName(new QualifiedName(Integer.parseInt((String) Props.getProperty("app-namespace-index")), ROOT_APP_NAME + "/Runtime/JavaScript/Load")).setDisplayName(new LocalizedText(null, "JavaScriptProgramLoad")).setDescription(LocalizedText.english("JavaScriptProgramLoad")).build();
        JavaScriptProgramLoadMethod javaScriptProgramLoadMethod = new JavaScriptProgramLoadMethod(loadMethodNode, this);
        loadMethodNode.setInputArguments(javaScriptProgramLoadMethod.getInputArguments());
        loadMethodNode.setOutputArguments(javaScriptProgramLoadMethod.getOutputArguments());
        loadMethodNode.setInvocationHandler(javaScriptProgramLoadMethod);

        UaMethodNode unLoadMethodNode = UaMethodNode.builder(uaNodeContext).setNodeId(new NodeId(Integer.parseInt((String) Props.getProperty("app-namespace-index")), ROOT_APP_NAME + "/Runtime/JavaScript/UnLoad")).setBrowseName(new QualifiedName(Integer.parseInt((String) Props.getProperty("app-namespace-index")), ROOT_APP_NAME + "/Runtime/JavaScript/UnLoad")).setDisplayName(new LocalizedText(null, "JavaScriptProgramUnLoad")).setDescription(LocalizedText.english("JavaScriptProgramUnLoad")).build();
        JavascriptProgramUnLoadMethod javascriptProgramUnLoadMethod = new JavascriptProgramUnLoadMethod(unLoadMethodNode, this);
        unLoadMethodNode.setInputArguments(javascriptProgramUnLoadMethod.getInputArguments());
        unLoadMethodNode.setOutputArguments(javascriptProgramUnLoadMethod.getOutputArguments());
        unLoadMethodNode.setInvocationHandler(javascriptProgramUnLoadMethod);

        UaMethodNode getMethodNode = UaMethodNode.builder(uaNodeContext).setNodeId(new NodeId(Integer.parseInt((String) Props.getProperty("app-namespace-index")), ROOT_APP_NAME + "/Runtime/JavaScript/Get")).setBrowseName(new QualifiedName(Integer.parseInt((String) Props.getProperty("app-namespace-index")), ROOT_APP_NAME + "/Runtime/JavaScript/Get")).setDisplayName(new LocalizedText(null, "JavaScriptProgramGet")).setDescription(LocalizedText.english("JavaScriptProgramGet")).build();
        ComponentGetMethod javaScriptProgramGetMethod = new ComponentGetMethod(getMethodNode, this);
        getMethodNode.setInputArguments(javaScriptProgramGetMethod.getInputArguments());
        getMethodNode.setOutputArguments(javaScriptProgramGetMethod.getOutputArguments());
        getMethodNode.setInvocationHandler(javaScriptProgramGetMethod);

        UaMethodNode deleteMethodNode = UaMethodNode.builder(uaNodeContext).setNodeId(new NodeId(Integer.parseInt((String) Props.getProperty("app-namespace-index")), ROOT_APP_NAME + "/Runtime/JavaScript/Delete")).setBrowseName(new QualifiedName(Integer.parseInt((String) Props.getProperty("app-namespace-index")), ROOT_APP_NAME + "/Runtime/JavaScript/Delete")).setDisplayName(new LocalizedText(null, "JavaScriptProgramDelete")).setDescription(LocalizedText.english("JavaScriptProgramDelete")).build();
        JavaScriptProgramDeleteMethod javaScriptProgramDeleteMethod = new JavaScriptProgramDeleteMethod(deleteMethodNode, this);
        deleteMethodNode.setInputArguments(javaScriptProgramDeleteMethod.getInputArguments());
        deleteMethodNode.setOutputArguments(javaScriptProgramDeleteMethod.getOutputArguments());
        deleteMethodNode.setInvocationHandler(javaScriptProgramDeleteMethod);


        UaMethodNode deleteNodeMethodNode = UaMethodNode.builder(uaNodeContext).setNodeId(new NodeId(Integer.parseInt((String) Props.getProperty("app-namespace-index")), ROOT_APP_NAME + "/Runtime/Node/Delete")).setBrowseName(new QualifiedName(Integer.parseInt((String) Props.getProperty("app-namespace-index")), ROOT_APP_NAME + "/Runtime/Node/Delete")).setDisplayName(new LocalizedText(null, "NodeDelete")).setDescription(LocalizedText.english("NodeDelete")).build();
        NodeDeleteMethod nodeDeleteMethod = new NodeDeleteMethod(unLoadMethodNode, this);
        deleteNodeMethodNode.setInputArguments(nodeDeleteMethod.getInputArguments());
        deleteNodeMethodNode.setOutputArguments(nodeDeleteMethod.getOutputArguments());
        deleteNodeMethodNode.setInvocationHandler(nodeDeleteMethod);

        UaMethodNode windowingMethodNode = UaMethodNode.builder(uaNodeContext).setNodeId(new NodeId(Integer.parseInt((String) Props.getProperty("app-namespace-index")), ROOT_APP_NAME + "/Runtime/JavaScript/Windowing")).setBrowseName(new QualifiedName(Integer.parseInt((String) Props.getProperty("app-namespace-index")), ROOT_APP_NAME + "/Runtime/JavaScript/Windowing")).setDisplayName(new LocalizedText(null, "JavaScriptProgramWindowingLoad")).setDescription(LocalizedText.english("JavaScriptProgramWindowingLoad")).build();
        JavaScriptWindowingProgramLoadMethod javaScriptWindowingProgramLoadMethod = new JavaScriptWindowingProgramLoadMethod(unLoadMethodNode, this);
        windowingMethodNode.setInputArguments(javaScriptWindowingProgramLoadMethod.getInputArguments());
        windowingMethodNode.setOutputArguments(javaScriptWindowingProgramLoadMethod.getOutputArguments());
        windowingMethodNode.setInvocationHandler(javaScriptWindowingProgramLoadMethod);

        uaNodeContext.getNodeManager().addNode(unLoadMethodNode);
        uaNodeContext.getNodeManager().addNode(loadMethodNode);
        uaNodeContext.getNodeManager().addNode(deleteMethodNode);
        uaNodeContext.getNodeManager().addNode(deleteNodeMethodNode);
        uaNodeContext.getNodeManager().addNode(windowingMethodNode);
        uaNodeContext.getNodeManager().addNode(getMethodNode);
        uaNodeContext.getNodeManager().addNode(testNloadMethodNode);

        unLoadMethodNode.addReference(new Reference(unLoadMethodNode.getNodeId(), Identifiers.HasComponent, programsFolder.getNodeId().expanded(), false));
        testNloadMethodNode.addReference(new Reference(testNloadMethodNode.getNodeId(), Identifiers.HasComponent, programsFolder.getNodeId().expanded(), false));
        loadMethodNode.addReference(new Reference(loadMethodNode.getNodeId(), Identifiers.HasComponent, programsFolder.getNodeId().expanded(), false));
        deleteMethodNode.addReference(new Reference(deleteMethodNode.getNodeId(), Identifiers.HasComponent, programsFolder.getNodeId().expanded(), false));
        deleteNodeMethodNode.addReference(new Reference(deleteNodeMethodNode.getNodeId(), Identifiers.HasComponent, programsFolder.getNodeId().expanded(), false));
        windowingMethodNode.addReference(new Reference(windowingMethodNode.getNodeId(), Identifiers.HasComponent, programsFolder.getNodeId().expanded(), false));
        getMethodNode.addReference(new Reference(getMethodNode.getNodeId(), Identifiers.HasComponent, programsFolder.getNodeId().expanded(), false));
    }


    @Override
    protected void onShutdown() {
        LoggerFactory.getLogger(getClass()).info("[RunTime] service has been shutdown.");
    }

    public UaNodeContext getUaNodeContext() {
        return uaNodeContext;
    }
}
