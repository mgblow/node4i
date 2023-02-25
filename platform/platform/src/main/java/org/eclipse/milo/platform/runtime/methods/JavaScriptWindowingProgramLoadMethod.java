package org.eclipse.milo.platform.runtime.methods;


import org.eclipse.milo.opcua.sdk.core.AccessLevel;
import org.eclipse.milo.opcua.sdk.core.Reference;
import org.eclipse.milo.opcua.sdk.core.ValueRanks;
import org.eclipse.milo.opcua.sdk.server.api.methods.AbstractMethodInvocationHandler;
import org.eclipse.milo.opcua.sdk.server.api.methods.InvalidArgumentException;
import org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaObjectNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaVariableNode;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.types.builtin.*;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.enumerated.NodeClass;
import org.eclipse.milo.opcua.stack.core.types.structured.Argument;
import org.eclipse.milo.platform.runtime.RunTime;
import org.eclipse.milo.platform.runtime.RuntimeIdentifier;
import org.eclipse.milo.platform.util.Props;
import org.eclipse.milo.platform.util.Utils;
import org.eclipse.milo.platform.validators.MethodInputValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JavaScriptWindowingProgramLoadMethod extends AbstractMethodInvocationHandler {

    public static final Argument name = new Argument("name", Identifiers.String, ValueRanks.Any, null, new LocalizedText("program name."));

    public static final Argument displayName = new Argument("displayName", Identifiers.String, ValueRanks.Any, null, new LocalizedText("program displayName"));

    public static final Argument location = new Argument("location", Identifiers.String, ValueRanks.Any, null, new LocalizedText("operation location."));

    public static final Argument samplingInterval = new Argument("samplingInterval", Identifiers.String, ValueRanks.Any, null, new LocalizedText("samplingInterval"));

    public static final Argument windowingLength = new Argument("windowingLength", Identifiers.String, ValueRanks.Any, null, new LocalizedText("windowingLength"));

    public static final Argument nodesToWindow = new Argument("nodesToWindow", Identifiers.String, ValueRanks.OneDimension, new UInteger[10], new LocalizedText("Nodes which you want to window"));

    public static final Argument Result = new Argument("result", Identifiers.String, ValueRanks.Any, null, new LocalizedText("result"));

    public static String APP_NAME = null;
    public static final int APP_NAMESPACE_INDEX = Integer.parseInt(Props.getProperty("app-namespace-index").toString());
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private UaMethodNode uaMethodNode;
    private RunTime runTime;

    public JavaScriptWindowingProgramLoadMethod(UaMethodNode uaMethodNode, RunTime runTime) {
        super(uaMethodNode);
        APP_NAME = uaMethodNode.getNodeContext().getServer().getConfig().getApplicationName().getText();
        this.uaMethodNode = uaMethodNode;
        this.runTime = runTime;
    }

    @Override
    public Argument[] getInputArguments() {
        return new Argument[]{name, displayName, location, nodesToWindow, samplingInterval, windowingLength};
    }

    @Override
    public Argument[] getOutputArguments() {
        return new Argument[]{Result};
    }

    @Override
    protected Variant[] invoke(InvocationContext invocationContext, Variant[] inputValues) {
        logger.debug("Invoking program() method of objectId={}", invocationContext.getObjectId());
        try {
            String name = (String) inputValues[0].getValue();
            String displayName = (String) inputValues[1].getValue();
            displayName = displayName == null ? name : displayName;
            String location = (String) inputValues[2].getValue();
            String[] nodesToWindow = (String[]) inputValues[3].getValue();
            String samplingInterval = (String) inputValues[4].getValue();
            String windowingLength = (String) inputValues[5].getValue();
            String WINDOWING_NODE_ID = RuntimeIdentifier.WINDOWING_IDENTIFIER.getIdentifier().replace("{app-name}", APP_NAME).replace("{windowing-name}", name);

            UaVariableNode outputAttribute = addOutPutToComponent(WINDOWING_NODE_ID);

            // check update
            UaObjectNode desiredLocation = (UaObjectNode) uaMethodNode.getNodeManager().get(new NodeId(APP_NAMESPACE_INDEX, location));
            // check update
            UaObjectNode windowingComponentNode = (UaObjectNode) uaMethodNode.getNodeManager().get(new NodeId(APP_NAMESPACE_INDEX, WINDOWING_NODE_ID));
            if (windowingComponentNode != null) {
                this.runTime.delete(windowingComponentNode);
            }

            windowingComponentNode = new UaObjectNode(uaMethodNode.getNodeContext(), new NodeId(APP_NAMESPACE_INDEX, WINDOWING_NODE_ID), new QualifiedName(2, WINDOWING_NODE_ID), new LocalizedText(displayName));
            windowingComponentNode.addReference(new Reference(windowingComponentNode.getNodeId(), Identifiers.HasProperty, outputAttribute.getNodeId().expanded(), true));
            uaMethodNode.getNodeManager().addNode(windowingComponentNode);

            addProgramNodeAttributes(inputValues, WINDOWING_NODE_ID, windowingComponentNode);

            final String nodesToWindowIdentifier = RuntimeIdentifier.RUNTIME_PROPERTY.getIdentifier().replace("{property-name}", "nodesToWindow").replace("{runtime-identifier}", WINDOWING_NODE_ID);
            final String nodesToWindowBrowseName = RuntimeIdentifier.PROPERTY_BROWSE_NAME.getIdentifier().replace("{property-name}", "nodesToWindow");
            UaVariableNode nodesToWindowAttribute = new UaVariableNode.UaVariableNodeBuilder(uaMethodNode.getNodeContext()).setNodeId(Utils.newNodeId(nodesToWindowIdentifier)).setAccessLevel(AccessLevel.READ_WRITE).setBrowseName(Utils.newQualifiedName(nodesToWindowBrowseName)).setDisplayName(LocalizedText.english("nodesToWindow")).setDataType(Identifiers.String).setTypeDefinition(Identifiers.PropertyType).setValue(new DataValue(new Variant(Utils.toJson(nodesToWindow)))).build();
            uaMethodNode.getNodeManager().addNode(nodesToWindowAttribute);
            windowingComponentNode.addReference(new Reference(windowingComponentNode.getNodeId(), Identifiers.HasProperty, nodesToWindowAttribute.getNodeId().expanded(), true));

            if (desiredLocation != null) {
                desiredLocation.addComponent(windowingComponentNode);
                desiredLocation.addReference(new Reference(desiredLocation.getNodeId(), Identifiers.HasChild, windowingComponentNode.getNodeId().expanded(), true));
            }

            String outPutIdentifier = RuntimeIdentifier.RUNTIME_PROPERTY.getIdentifier().replace("{runtime-identifier}", WINDOWING_NODE_ID).replace("{property-name}", "output");
            addNodesToWindow(windowingComponentNode, name, samplingInterval, windowingLength, nodesToWindow, Utils.newNodeId(outPutIdentifier));
            return new Variant[]{new Variant(true)};
        } catch (Exception e) {
            logger.error("Error invoking ProgramMethod of objectId={} with message {}", invocationContext.getObjectId(), e.getMessage());
            return new Variant[]{new Variant(false)};
        }
    }

    private void addNodesToWindow(UaObjectNode componentNode, String mainNode, String minimumSamplingInterval, String windowingLength, String[] nodesToWindow, NodeId outPutNode) {
        UaVariableNode node = (UaVariableNode) uaMethodNode.getNodeContext().getNodeManager().get(Utils.newNodeId("Scheduled"));
        this.runTime.windowingLoad(node, componentNode, mainNode, Long.valueOf(windowingLength), Long.valueOf(minimumSamplingInterval), nodesToWindow, outPutNode);
    }

    private void addProgramNodeAttributes(Variant[] inputValues, String windowingIdentifier, UaObjectNode programNode) {
        int index = 0;
        for (Argument input : getInputArguments()) {
            if (input.getName() == "displayName" || input.getName() == "nodesToWindow") {
                index++;
                continue;
            }
            Variant nodeValue = null;
            if (inputValues[index] != null) {
                nodeValue = new Variant(inputValues[index].getValue());
            }

            final String propertyIdentifier = RuntimeIdentifier.RUNTIME_PROPERTY.getIdentifier().replace("{property-name}", input.getName()).replace("{runtime-identifier}", windowingIdentifier);
            final String propertyBrowseName = RuntimeIdentifier.PROPERTY_BROWSE_NAME.getIdentifier().replace("{property-name}", input.getName());
            UaVariableNode attribute = new UaVariableNode.UaVariableNodeBuilder(uaMethodNode.getNodeContext()).setNodeId(Utils.newNodeId(propertyIdentifier)).setAccessLevel(AccessLevel.READ_WRITE).setBrowseName(Utils.newQualifiedName(propertyBrowseName)).setDisplayName(LocalizedText.english(input.getName())).setDataType(Identifiers.String).setTypeDefinition(Identifiers.PropertyType).setValue(DataValue.valueOnly(nodeValue)).build();
            uaMethodNode.getNodeManager().addNode(attribute);
            programNode.addReference(new Reference(programNode.getNodeId(), Identifiers.HasProperty, attribute.getNodeId().expanded(), true));
            index++;
        }
    }

    private UaVariableNode addOutPutToComponent(String COMPONENT_NODE_ID) {
        final String outputIdentifier = RuntimeIdentifier.RUNTIME_PROPERTY.getIdentifier().replace("{property-name}", "output").replace("{runtime-identifier}", COMPONENT_NODE_ID);
        final String outputBrowseName = RuntimeIdentifier.PROPERTY_BROWSE_NAME.getIdentifier().replace("{property-name}", "output");
        UaVariableNode outputAttribute = new UaVariableNode.UaVariableNodeBuilder(uaMethodNode.getNodeContext()).setNodeId(new NodeId(2, outputIdentifier)).setAccessLevel(AccessLevel.READ_WRITE).setBrowseName(new QualifiedName(2, outputBrowseName)).setDisplayName(LocalizedText.english("output")).setDataType(Identifiers.String).setTypeDefinition(Identifiers.PropertyType).setValue(DataValue.valueOnly(new Variant(""))).build();
        uaMethodNode.getNodeManager().addNode(outputAttribute);
        return outputAttribute;
    }

    protected void validateInputArgumentValues(Variant[] inputArgumentValues) throws InvalidArgumentException {
        MethodInputValidator.forbiddenCharacter(this, name);
        MethodInputValidator.NotNull(this, name, location, samplingInterval, nodesToWindow, windowingLength);
        MethodInputValidator.Exists(this, nodesToWindow,location);
        MethodInputValidator.isNumber(this, windowingLength);
        MethodInputValidator.checkRange(this, windowingLength, 1000l, ">");
        MethodInputValidator.checkRange(this, samplingInterval, 100L, ">");
        MethodInputValidator.checkType(this, location, NodeClass.Object);
        if (this.inputErrorMessages.size() != 0) {
            throw new InvalidArgumentException(null);
        }
    }
}

