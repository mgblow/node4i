package org.eclipse.milo.platform.gateway.methods;

import org.eclipse.milo.opcua.sdk.core.AccessLevel;
import org.eclipse.milo.opcua.sdk.core.Reference;
import org.eclipse.milo.opcua.sdk.core.ValueRanks;
import org.eclipse.milo.opcua.sdk.server.api.methods.AbstractMethodInvocationHandler;
import org.eclipse.milo.opcua.sdk.server.api.methods.InvalidArgumentException;
import org.eclipse.milo.opcua.sdk.server.nodes.UaFolderNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaObjectNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaVariableNode;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.types.builtin.*;
import org.eclipse.milo.opcua.stack.core.types.enumerated.IdType;
import org.eclipse.milo.opcua.stack.core.types.enumerated.NodeClass;
import org.eclipse.milo.opcua.stack.core.types.structured.Argument;
import org.eclipse.milo.platform.gateway.GatewayIdentifier;
import org.eclipse.milo.platform.util.Props;
import org.eclipse.milo.platform.util.Utils;
import org.eclipse.milo.platform.validators.MethodInputValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

public class OpcUaAttributeMethod extends AbstractMethodInvocationHandler {

    public static final Argument DISPLAY_NAME = new Argument("displayName", Identifiers.String, ValueRanks.Any, null, new LocalizedText("displayName"));
    public static final Argument LOCATION = new Argument("location", Identifiers.String, ValueRanks.Any, null, new LocalizedText("location"));
    public static final Argument IO_Name = new Argument("IOName", Identifiers.String, ValueRanks.Any, null, new LocalizedText("IOName"));
    public static final Argument Identifier = new Argument("identifier", Identifiers.String, ValueRanks.Any, null, new LocalizedText("identifier"));
    public static final Argument NODE_NAMESPACE_INDEX = new Argument("namespaceIndex", Identifiers.String, ValueRanks.Any, null, new LocalizedText("namespaceIndex"));
    public static final Argument IDENTIFIER_DATA_TYPE = new Argument("identifierDataType", Identifiers.String, ValueRanks.Any, null, new LocalizedText("identifierDataType"));
    public static final Argument DESCRIPTION = new Argument("description", Identifiers.String, ValueRanks.Any, null, new LocalizedText("description"));
    public static final Argument RESULT = new Argument("result", Identifiers.String, ValueRanks.Any, null, new LocalizedText("result"));
    public static String APP_NAME = null;
    private final Logger logger = LoggerFactory.getLogger(getClass());
    UaMethodNode uaMethodNode;
    private int appNamespaceIndex = Integer.parseInt(Props.getProperty("app-namespace-index").toString());

    public OpcUaAttributeMethod(UaMethodNode uaMethodNode) {
        super(uaMethodNode);
        APP_NAME = uaMethodNode.getNodeContext().getServer().getConfig().getApplicationName().getText();
        this.uaMethodNode = uaMethodNode;
    }

    @Override
    public Argument[] getInputArguments() {
        return new Argument[]{DISPLAY_NAME, LOCATION, IO_Name, Identifier, NODE_NAMESPACE_INDEX, IDENTIFIER_DATA_TYPE, DESCRIPTION};
    }

    @Override
    public Argument[] getOutputArguments() {
        return new Argument[]{RESULT};
    }

    @Override
    protected Variant[] invoke(InvocationContext invocationContext, Variant[] inputValues) {

        logger.debug("Invoking tag method of objectId={}", invocationContext.getObjectId());
        String displayName = inputValues[0].getValue().toString();
        String location = inputValues[1].getValue().toString();
        String ioName = inputValues[2].getValue().toString();
        String identifier = inputValues[3].getValue().toString();
        String identifierDataType = inputValues[5].getValue().toString();

        String deviceIdentifier = GatewayIdentifier.IO_IDENTIFIER.getIdentifier().replace("{app-name}", APP_NAME).replace("{io-name}", ioName);
        String tagId = GatewayIdentifier.OPC_TAG.getIdentifier().replace("{tag-name}", identifier).replace("{app-name}",APP_NAME).replace("{io-name}",ioName);

        UaObjectNode deviceNode = (UaObjectNode) uaMethodNode.getNodeManager().get(new NodeId(2, deviceIdentifier));
        if (deviceNode == null) return new Variant[]{new Variant(false)};

        final String attributeProperty = GatewayIdentifier.IO_Property.getIdentifier().replace("{property-name}", "Attributes").replace("{io-identifier}", deviceIdentifier);
        UaObjectNode deviceAttributesNode = (UaObjectNode) uaMethodNode.getNodeManager().get(Utils.newNodeId(attributeProperty));

        UaObjectNode uaOpcUaTagNode = (UaObjectNode) invocationContext.getMethodNode().getNodeManager().get(new NodeId(appNamespaceIndex, tagId));
        //Check whether it is saving or updating
        if (uaOpcUaTagNode == null) {
            uaOpcUaTagNode = new UaObjectNode(invocationContext.getMethodNode().getNodeContext(), new NodeId(appNamespaceIndex, tagId), new QualifiedName(appNamespaceIndex, tagId), new LocalizedText(displayName));
        } else {
            removeFromLastLocation(tagId, uaOpcUaTagNode);
        }

        int index = -1;
        for (Argument input : this.getInputArguments()) {
            index++;
            if (input.getName() == "displayName") continue;
            Variant nodeValue = null;
            if (inputValues[index] != null) {
                nodeValue = new Variant(inputValues[index].getValue());
            }
            addProperty(tagId, uaOpcUaTagNode, input.getName(), nodeValue);
        }
        uaOpcUaTagNode.setDisplayName(new LocalizedText(displayName));
        invocationContext.getMethodNode().getNodeManager().addNode(uaOpcUaTagNode);

        addProperty(tagId, uaOpcUaTagNode, "value", new Variant(""));

        deviceNode.addReference(new Reference(deviceNode.getNodeId(), Identifiers.HasProperty, uaOpcUaTagNode.getNodeId().expanded(), true));
        UaFolderNode locationFolder = (UaFolderNode) uaMethodNode.getNodeManager().get(new NodeId(2, location));
        locationFolder.addOrganizes(uaOpcUaTagNode);
        deviceAttributesNode.addComponent(uaOpcUaTagNode);
        deviceNode.addReference(new Reference(deviceNode.getNodeId(), Identifiers.HasProperty, uaOpcUaTagNode.getNodeId().expanded(), true));

        return new Variant[]{new Variant(true)};
    }


    protected void validateInputArgumentValues(Variant[] inputArgumentValues) throws InvalidArgumentException {
        MethodInputValidator.forbiddenCharacter(this, IO_Name);
        MethodInputValidator.NotNull(this, LOCATION, Identifier, NODE_NAMESPACE_INDEX, IO_Name);
        MethodInputValidator.Exists(this, LOCATION);
        MethodInputValidator.Exists(this, IO_Name, APP_NAME + "/IO/");
        MethodInputValidator.isNumber(this, NODE_NAMESPACE_INDEX);
        MethodInputValidator.checkType(this, LOCATION, NodeClass.Object);
        MethodInputValidator.isValidEnum(this, IdType.class,IDENTIFIER_DATA_TYPE);

        if (this.inputErrorMessages.size() != 0) {
            throw new InvalidArgumentException(null);
        }
    }

    private void removeFromLastLocation(String tagId, UaObjectNode uaOpcUaTagNode) {
        List<Reference> references = uaOpcUaTagNode.getReferences().stream().filter(c -> c.getDirection() == Reference.Direction.INVERSE).collect(Collectors.toList());

        final String locationIdentifier = GatewayIdentifier.TAG_PROPERTY.getIdentifier().replace("{tag-identifier}", tagId).replace("{property-name}", "location");
        UaVariableNode locationNode = (UaVariableNode) uaOpcUaTagNode.getNodeContext().getNodeManager().get(new NodeId(appNamespaceIndex, locationIdentifier));

        String locationValue = locationNode.getValue().getValue().getValue().toString();
        for (Reference reference : references) {
            if (reference.getTargetNodeId().getIdentifier().equals(locationValue)) {
                uaOpcUaTagNode.removeReference(reference);
            }
        }
    }

    private void addProperty(String tagId, UaObjectNode uaOpcUaTagNode, String propertyName, Variant nodeValue) {
        String propertyIdentifier = GatewayIdentifier.TAG_PROPERTY.getIdentifier().replace("{tag-identifier}", tagId).replace("{property-name}", propertyName);
        String inputBrowseName = GatewayIdentifier.PROPERTY_BROWSE_NAME.getIdentifier().replace("{property-name}", propertyName);
        UaVariableNode property = new UaVariableNode.UaVariableNodeBuilder(uaMethodNode.getNodeContext()).setNodeId(new NodeId(appNamespaceIndex, propertyIdentifier)).setAccessLevel(AccessLevel.READ_WRITE).setBrowseName(new QualifiedName(appNamespaceIndex, inputBrowseName)).setDisplayName(LocalizedText.english(propertyName)).setDataType(Identifiers.String).setTypeDefinition(Identifiers.PropertyType).setValue(DataValue.valueOnly(nodeValue)).build();
        uaMethodNode.getNodeManager().addNode(property);

        uaOpcUaTagNode.addReference(new Reference(uaOpcUaTagNode.getNodeId(), Identifiers.HasProperty, property.getNodeId().expanded(), true));
    }
}