/*
 * Copyright (c) 2019 the Eclipse Milo Authors
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.milo.platform.alamEvent.methods;

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
import org.eclipse.milo.platform.alamEvent.EventIdentifier;
import org.eclipse.milo.platform.validators.MethodInputValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

public class CreateConditionClass extends AbstractMethodInvocationHandler {

    public static final Argument name = new Argument("name", Identifiers.String, ValueRanks.Any, null, new LocalizedText("Condition Class Name"));

    public static final Argument displayName = new Argument("displayName", Identifiers.String, ValueRanks.Any, null, new LocalizedText("DisplayName"));

    public static final Argument color = new Argument("color", Identifiers.String, ValueRanks.Any, null, new LocalizedText("Condition Class Occurrence Color"));

    public static final Argument acknowledgeable = new Argument("acknowledgeable", Identifiers.String, ValueRanks.Any, null, new LocalizedText("Condition Class Acknowledgeable"));


    public static final Argument Result = new Argument("result", Identifiers.String, ValueRanks.Any, null, new LocalizedText("result"));

    public static String APP_NAME = null;
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public UaMethodNode uaMethodNode;

    public CreateConditionClass(UaMethodNode uaMethodNode) {
        super(uaMethodNode);
        APP_NAME = uaMethodNode.getNodeContext().getServer().getConfig().getApplicationName().getText();
        this.uaMethodNode = uaMethodNode;
    }

    @Override
    public Argument[] getInputArguments() {
        return new Argument[]{name, displayName, color, acknowledgeable};
    }

    @Override
    public Argument[] getOutputArguments() {
        return new Argument[]{Result};
    }

    @Override
    protected Variant[] invoke(InvocationContext invocationContext, Variant[] inputValues) {
        logger.debug("Invoking CreateConditionClass() method of objectId={}", invocationContext.getObjectId());
        String name = (String) inputValues[0].getValue();
        String displayName = (String) inputValues[1].getValue();
        displayName = displayName == null ? name : displayName;
        String color = (String) inputValues[2].getValue();
        String CONDITION_CLASS_NODE_ID = EventIdentifier.CONDITION_CLASS_IDENTIFIER.getIdentifier().replace("{class-name}",name).replace("{app-name}",APP_NAME);
        UaObjectNode classNode = (UaObjectNode) uaMethodNode.getNodeManager().get(new NodeId(2, CONDITION_CLASS_NODE_ID));
        if (classNode == null) {
            classNode = new UaObjectNode(uaMethodNode.getNodeContext(), new NodeId(2, CONDITION_CLASS_NODE_ID), new QualifiedName(2, name), new LocalizedText(displayName));
            uaMethodNode.getNodeManager().addNode(classNode);
        }
        classNode.setDisplayName(new LocalizedText(displayName));
        AtomicInteger count = new AtomicInteger(-1);
        int index = -1;
        for (Argument input : this.getInputArguments()) {
            index++;
            if (input.getName() == "displayName") continue;

            final String conditionPropertyIdentifier = EventIdentifier.CONDITION_CLASS_PROPERTY_IDENTIFIER.getIdentifier().replace("{property-name}", input.getName()).replace("{condition-class-identifier}", CONDITION_CLASS_NODE_ID);
            UaVariableNode attribute = new UaVariableNode.UaVariableNodeBuilder(uaMethodNode.getNodeContext()).setNodeId(new NodeId(2, conditionPropertyIdentifier)).setAccessLevel(AccessLevel.READ_WRITE).setBrowseName(new QualifiedName(2, EventIdentifier.PROPERTY_BROWSE_NAME.getIdentifier().replace("{property-name}",input.getName()))).setDisplayName(LocalizedText.english(input.getName())).setDataType(Identifiers.String).setTypeDefinition(Identifiers.PropertyType).setValue(DataValue.valueOnly(new Variant(inputValues[index].getValue()))).build();
            classNode.addReference(new Reference(classNode.getNodeId(), Identifiers.HasProperty, attribute.getNodeId().expanded(), true));
            this.uaMethodNode.getNodeManager().addNode(attribute);
        }
        UaFolderNode conditionClassesFolder = (UaFolderNode) classNode.getNodeManager().get(new NodeId(2, APP_NAME + "/Alarm&Events/ConditionClasses"));
        conditionClassesFolder.addOrganizes(classNode);
        return new Variant[]{new Variant(Boolean.TRUE)};
}

    protected void validateInputArgumentValues(Variant[] inputArgumentValues) throws InvalidArgumentException {
        MethodInputValidator.forbiddenCharacter(this,name);
        MethodInputValidator.NotNull(this, name, color);
        MethodInputValidator.isBoolean(this, acknowledgeable);
        if (this.inputErrorMessages.size() != 0) {
            throw new InvalidArgumentException(null);
        }
    }
}
