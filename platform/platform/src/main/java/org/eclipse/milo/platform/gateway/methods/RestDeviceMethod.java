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
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.*;
import org.eclipse.milo.opcua.stack.core.types.structured.Argument;
import org.eclipse.milo.platform.gateway.GatewayIdentifier;
import org.eclipse.milo.platform.util.Utils;
import org.eclipse.milo.platform.validators.MethodInputValidator;

import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public class RestDeviceMethod extends AbstractMethodInvocationHandler {
    public static final Argument NAME = new Argument("NAME", Identifiers.String, ValueRanks.Any, null, new LocalizedText("NAME"));
    public static final Argument DISPLAY_NAME = new Argument("DISPLAY_NAME", Identifiers.String, ValueRanks.Any, null, new LocalizedText("DISPLAY_NAME"));
    public static final Argument URL = new Argument("URL", Identifiers.String, ValueRanks.Any, null, new LocalizedText("URL"));
    public static final Argument RESULT = new Argument("result", Identifiers.String, ValueRanks.Any, null, new LocalizedText("result"));
    public static String APP_NAME = null;
    UaMethodNode uaMethodNode;

    public RestDeviceMethod(UaMethodNode node) {
        super(node);
        this.uaMethodNode = node;
        APP_NAME = uaMethodNode.getNodeContext().getServer().getConfig().getApplicationName().getText();
    }

    @Override
    public Argument[] getInputArguments() {
        return new Argument[]{NAME, DISPLAY_NAME, URL};
    }

    @Override
    public Argument[] getOutputArguments() {
        return new Argument[]{RESULT};
    }

    @Override
    protected Variant[] invoke(InvocationContext invocationContext, Variant[] inputValues) throws UaException {
        String name = (String) inputValues[0].getValue();
        String displayName = (String) inputValues[1].getValue();
        displayName = displayName == null ? name : displayName;
        String deviceIdentifier = GatewayIdentifier.IO_IDENTIFIER.getIdentifier().replace("{app-name}", APP_NAME).replace("{io-name}", name);


        UaObjectNode restNode = new UaObjectNode(uaMethodNode.getNodeContext(), new NodeId(2, deviceIdentifier), new QualifiedName(2, deviceIdentifier), new LocalizedText(displayName));
        uaMethodNode.getNodeManager().addNode(restNode);


        final String configProperty = GatewayIdentifier.IO_Property.getIdentifier().replace("{property-name}", "Config").replace("{io-identifier}", deviceIdentifier);
        final String configBrowseName = GatewayIdentifier.PROPERTY_BROWSE_NAME.getIdentifier().replace("{property-name}", "Config");
        UaObjectNode uaConfigNode = new UaObjectNode(uaMethodNode.getNodeContext(), new NodeId(2, configProperty), new QualifiedName(2, configBrowseName), new LocalizedText("Config"));
        uaMethodNode.getNodeManager().addNode(uaConfigNode);

        final String attributeProperty = GatewayIdentifier.IO_Property.getIdentifier().replace("{property-name}", "Attributes").replace("{io-identifier}", deviceIdentifier);
        final String attributeBrowseName = GatewayIdentifier.PROPERTY_BROWSE_NAME.getIdentifier().replace("{property-name}", "Attributes");
        UaObjectNode uaAttributeNode = new UaObjectNode(uaMethodNode.getNodeContext(), new NodeId(2, attributeProperty), new QualifiedName(2, attributeBrowseName), new LocalizedText("Attributes"));
        uaMethodNode.getNodeManager().addNode(uaAttributeNode);


        restNode.addComponent(uaAttributeNode);
        AtomicInteger count = new AtomicInteger(-1);
        Arrays.stream(this.getInputArguments()).forEach(input -> {
            int index = count.addAndGet(1);
            if (Objects.equals(input.getName(), "DISPLAY_NAME")) return;
            Variant nodeValue = null;
            if (inputValues[index] != null) {
                nodeValue = new Variant(inputValues[index].getValue());
            }

            String properties = GatewayIdentifier.CONFIG_PROPERTIES.getIdentifier().replace("{io-identifier}", deviceIdentifier).replace("{property-name}", input.getName());
            String inputBrowseName = GatewayIdentifier.PROPERTY_BROWSE_NAME.getIdentifier().replace("{property-name}", input.getName());
            UaVariableNode attribute = new UaVariableNode.UaVariableNodeBuilder(uaMethodNode.getNodeContext()).setNodeId(new NodeId(2, properties)).setAccessLevel(AccessLevel.READ_WRITE).setBrowseName(new QualifiedName(2, inputBrowseName)).setDisplayName(LocalizedText.english(input.getName())).setDataType(Identifiers.String).setTypeDefinition(Identifiers.PropertyType).setValue(DataValue.valueOnly(nodeValue)).build();
            uaMethodNode.getNodeManager().addNode(attribute);
            uaConfigNode.addReference(new Reference(uaConfigNode.getNodeId(), Identifiers.HasProperty, attribute.getNodeId().expanded(), true));
        });
        // add state node
        addPropertyNode(deviceIdentifier, restNode, "STATE", "STATE", "OFF");


        addProtocolConfig(deviceIdentifier, uaConfigNode);
        restNode.addComponent(uaConfigNode);
        UaFolderNode devicesFolder = (UaFolderNode) uaMethodNode.getNodeManager().get(Utils.newNodeId(2, GatewayIdentifier.IO_FOLDER.getIdentifier().replace("{app-name}", APP_NAME)));
        devicesFolder.addOrganizes(restNode);
        return new Variant[]{new Variant(Boolean.TRUE)};
    }

    private void addPropertyNode(String deviceIdentifier, UaObjectNode uaAmqpNode, String propertyName, String state, String defaultValue) {
        final String propertyIdentifier = GatewayIdentifier.IO_Property.getIdentifier().replace("{io-identifier}", deviceIdentifier).replace("{property-name}", propertyName);
        String inputBrowseName = GatewayIdentifier.PROPERTY_BROWSE_NAME.getIdentifier().replace("{property-name}", state);
        UaVariableNode stateNode = new UaVariableNode.UaVariableNodeBuilder(uaMethodNode.getNodeContext()).setNodeId(new NodeId(2, propertyIdentifier)).setAccessLevel(AccessLevel.READ_WRITE).setBrowseName(new QualifiedName(2, inputBrowseName)).setDisplayName(LocalizedText.english(state)).setDataType(Identifiers.String).setTypeDefinition(Identifiers.PropertyType).setValue(new DataValue(new Variant(defaultValue))).build();
        uaMethodNode.getNodeManager().addNode(stateNode);
        uaAmqpNode.addReference(new Reference(uaAmqpNode.getNodeId(), Identifiers.HasProperty, stateNode.getNodeId().expanded(), true));
    }

    private void addProtocolConfig(String deviceNodeId, UaObjectNode uaConfigNode) {
        final String protocolIdentifier = GatewayIdentifier.PROTOCOL_PROPERTY_IDENTIFIER.getIdentifier().replace("{io-identifier}", deviceNodeId);
        UaVariableNode protocolAttribute = new UaVariableNode.UaVariableNodeBuilder(uaMethodNode.getNodeContext()).
                setNodeId(new NodeId(2, protocolIdentifier)).setAccessLevel(AccessLevel.READ_WRITE).
                setBrowseName(new QualifiedName(2, "Property/PROTOCOL"))
                .setDisplayName(LocalizedText.english("PROTOCOL")).setDataType(Identifiers.String).setTypeDefinition(Identifiers.PropertyType).setValue(DataValue.valueOnly(new Variant("REST"))).build();
        uaMethodNode.getNodeManager().addNode(protocolAttribute);
        uaConfigNode.addReference(new Reference(uaConfigNode.getNodeId(), Identifiers.HasProperty, protocolAttribute.getNodeId().expanded(), true));
    }

    protected void validateInputArgumentValues(Variant[] inputArgumentValues) throws InvalidArgumentException {
        MethodInputValidator.forbiddenCharacter(this, NAME);
        MethodInputValidator.NotNull(this, NAME, URL);
        if (this.inputErrorMessages.size() != 0) {
            throw new InvalidArgumentException(null);
        }
    }
}
