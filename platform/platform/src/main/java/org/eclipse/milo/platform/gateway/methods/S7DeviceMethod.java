/*
 * Copyright (c) 2019 the Eclipse Milo Authors
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

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
import org.eclipse.milo.opcua.stack.core.types.structured.Argument;
import org.eclipse.milo.platform.gateway.GatewayIdentifier;
import org.eclipse.milo.platform.validators.MethodInputValidator;
import org.eclipse.milo.platform.util.Props;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

public class S7DeviceMethod extends AbstractMethodInvocationHandler {

    public static final Argument NAME = new Argument("NAME", Identifiers.String, ValueRanks.Any, null, new LocalizedText("NAME"));

    public static final Argument DISPLAY_NAME = new Argument("DISPLAY_NAME", Identifiers.String, ValueRanks.Any, null, new LocalizedText("DISPLAY_NAME"));

    public static final Argument URL = new Argument("URL", Identifiers.String, ValueRanks.Any, null, new LocalizedText("S7 URL"));

    public static final Argument RACK = new Argument("RACK", Identifiers.String, ValueRanks.Any, null, new LocalizedText("RACK Number"));

    public static final Argument SLOT = new Argument("SLOT", Identifiers.String, ValueRanks.Any, null, new LocalizedText("SLOT Number"));

    public static final Argument RESULT = new Argument("result", Identifiers.String, ValueRanks.Any, null, new LocalizedText("result"));

    public static String APP_NAME = Props.getProperty("app-name").toString();
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private UaMethodNode uaMethodNode;

    public S7DeviceMethod(UaMethodNode uaMethodNode) {
        super(uaMethodNode);
        APP_NAME = uaMethodNode.getNodeContext().getServer().getConfig().getApplicationName().getText();
        this.uaMethodNode = uaMethodNode;
    }

    @Override
    public Argument[] getInputArguments() {
        return new Argument[]{NAME, DISPLAY_NAME, URL, RACK, SLOT};
    }

    @Override
    public Argument[] getOutputArguments() {
        return new Argument[]{RESULT};
    }

    @Override
    protected Variant[] invoke(InvocationContext invocationContext, Variant[] inputValues) {
        logger.debug("Invoking s7() method of objectId={}", invocationContext.getObjectId());
        String name = (String) inputValues[0].getValue();
        String displayName = (String) inputValues[1].getValue();
        displayName = displayName == null ? name : displayName;

        String deviceIdentifier = GatewayIdentifier.IO_IDENTIFIER.getIdentifier().replace("{app-name}",APP_NAME).replace("{io-name}",name);

        // create s7 Object node
        UaObjectNode uaS7Node = new UaObjectNode(uaMethodNode.getNodeContext(), new NodeId(2, deviceIdentifier), new QualifiedName(2, deviceIdentifier), new LocalizedText(displayName));
        uaMethodNode.getNodeManager().addNode(uaS7Node);

        final String configProperty = GatewayIdentifier.IO_Property.getIdentifier().replace("{property-name}", "Config").replace("{io-identifier}", deviceIdentifier);
        final String configBrowseName = GatewayIdentifier.PROPERTY_BROWSE_NAME.getIdentifier().replace("{property-name}", "Config");
        UaObjectNode uaConfigNode = new UaObjectNode(uaMethodNode.getNodeContext(), new NodeId(2, configProperty), new QualifiedName(2, configBrowseName), new LocalizedText("Config"));
        uaMethodNode.getNodeManager().addNode(uaConfigNode);

        final String attributeProperty = GatewayIdentifier.IO_Property.getIdentifier().replace("{property-name}", "Attributes").replace("{io-identifier}", deviceIdentifier);
        final String attributeBrowseName = GatewayIdentifier.PROPERTY_BROWSE_NAME.getIdentifier().replace("{property-name}", "Attributes");
        UaObjectNode uaAttributeNode = new UaObjectNode(uaMethodNode.getNodeContext(), new NodeId(2, attributeProperty), new QualifiedName(2, attributeBrowseName), new LocalizedText("Attributes"));
        uaMethodNode.getNodeManager().addNode(uaAttributeNode);

        uaS7Node.addComponent(uaAttributeNode);

        AtomicInteger count = new AtomicInteger(-1);
        Arrays.stream(this.getInputArguments()).forEach(input -> {
            int index = count.addAndGet(1);
            if (input.getName() == "DISPLAY_NAME") return;
            Variant nodeValue = null;
            if (inputValues[index] != null) {
                nodeValue = new Variant(inputValues[index].getValue());
            }
            String properties = GatewayIdentifier.CONFIG_PROPERTIES.getIdentifier().replace("{io-identifier}", deviceIdentifier).replace("{property-name}", input.getName());
            final String propertyBrowseName=GatewayIdentifier.PROPERTY_BROWSE_NAME.getIdentifier().replace("{property-name}",input.getName());
            UaVariableNode attribute = new UaVariableNode.UaVariableNodeBuilder(uaMethodNode.getNodeContext()).setNodeId(new NodeId(2, properties)).setAccessLevel(AccessLevel.READ_WRITE).setBrowseName(new QualifiedName(2,propertyBrowseName)).setDisplayName(LocalizedText.english(input.getName())).setDataType(Identifiers.String).setTypeDefinition(Identifiers.PropertyType).setValue(DataValue.valueOnly(nodeValue)).build();
            uaMethodNode.getNodeManager().addNode(attribute);
            uaConfigNode.addReference(new Reference(uaConfigNode.getNodeId(), Identifiers.HasProperty, attribute.getNodeId().expanded(), true));
        });
        // add state node
        addPropertyNode(deviceIdentifier, uaS7Node, "STATE", "STATE", "OFF");


        addProtocolConfig(deviceIdentifier, uaConfigNode);
        uaS7Node.addComponent(uaConfigNode);
        UaFolderNode devicesFolder = (UaFolderNode) uaMethodNode.getNodeManager().get(new NodeId(2, APP_NAME + "/IO"));
        devicesFolder.addOrganizes(uaS7Node);
        return new Variant[]{new Variant(Boolean.TRUE)};
    }

    private void addPropertyNode(String deviceIdentifier, UaObjectNode uaAmqpNode, String propertyName, String state, String defaultValue) {
        final String propertyIdentifier = GatewayIdentifier.IO_Property.getIdentifier().replace("{io-identifier}", deviceIdentifier).replace("{property-name}", propertyName);
        final String propertyBrowseName=GatewayIdentifier.PROPERTY_BROWSE_NAME.getIdentifier().replace("{property-name}",state);
        UaVariableNode stateNode = new UaVariableNode.UaVariableNodeBuilder(uaMethodNode.getNodeContext()).setNodeId(new NodeId(2, propertyIdentifier)).setAccessLevel(AccessLevel.READ_WRITE).setBrowseName(new QualifiedName(2, propertyBrowseName)).setDisplayName(LocalizedText.english(state)).setDataType(Identifiers.String).setTypeDefinition(Identifiers.PropertyType).setValue(new DataValue(new Variant(defaultValue))).build();
        uaMethodNode.getNodeManager().addNode(stateNode);
        uaAmqpNode.addReference(new Reference(uaAmqpNode.getNodeId(), Identifiers.HasProperty, stateNode.getNodeId().expanded(), true));
    }

    private void addProtocolConfig(String deviceNodeId, UaObjectNode uaConfigNode) {
        // add config protocol, name prop
        final String protocolIdentifier = GatewayIdentifier.PROTOCOL_PROPERTY_IDENTIFIER.getIdentifier().replace("{io-identifier}", deviceNodeId);
        UaVariableNode protocolAttribute = new UaVariableNode.UaVariableNodeBuilder(uaMethodNode.getNodeContext()).setNodeId(new NodeId(2, protocolIdentifier)).setAccessLevel(AccessLevel.READ_WRITE).setBrowseName(new QualifiedName(2, "Property/PROTOCOL")).setDisplayName(LocalizedText.english("PROTOCOL")).setDataType(Identifiers.String).setTypeDefinition(Identifiers.PropertyType).setValue(DataValue.valueOnly(new Variant("S7"))).build();
        uaMethodNode.getNodeManager().addNode(protocolAttribute);
        uaConfigNode.addReference(new Reference(uaConfigNode.getNodeId(), Identifiers.HasProperty, protocolAttribute.getNodeId().expanded(), true));
        // end adding protocol, name prop
    }

    protected void validateInputArgumentValues(Variant[] inputArgumentValues) throws InvalidArgumentException {
        MethodInputValidator.forbiddenCharacter(this,NAME);
        MethodInputValidator.NotNull(this, NAME, URL,RACK,SLOT);
        MethodInputValidator.isNumber(this,RACK,SLOT);
        if (this.inputErrorMessages.size() != 0) {
            throw new InvalidArgumentException(null);
        }
    }
}
