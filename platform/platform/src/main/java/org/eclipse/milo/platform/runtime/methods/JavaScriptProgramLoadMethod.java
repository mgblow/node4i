/*
 * Copyright (c) 2019 the Eclipse Milo Authors
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.milo.platform.runtime.methods;
/*
 * Copyright (c) 2019 the Eclipse Milo Authors
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */


import org.eclipse.milo.opcua.sdk.core.AccessLevel;
import org.eclipse.milo.opcua.sdk.core.Reference;
import org.eclipse.milo.opcua.sdk.core.ValueRanks;
import org.eclipse.milo.opcua.sdk.server.api.methods.AbstractMethodInvocationHandler;
import org.eclipse.milo.opcua.sdk.server.api.methods.InvalidArgumentException;
import org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaObjectNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaVariableNode;
import org.eclipse.milo.opcua.sdk.server.nodes.serializable.SerializableParentTrigger;
import org.eclipse.milo.opcua.sdk.server.nodes.serializable.SerializableTriggerParent;
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

import java.util.List;

public class JavaScriptProgramLoadMethod extends AbstractMethodInvocationHandler {

    public static final Argument name = new Argument("name", Identifiers.String, ValueRanks.Any, null, new LocalizedText("program name."));

    public static final Argument displayName = new Argument("displayName", Identifiers.String, ValueRanks.Any, null, new LocalizedText("program displayName"));

    public static final Argument location = new Argument("location", Identifiers.String, ValueRanks.Any, null, new LocalizedText("operation location."));

    public static final Argument triggerNodes = new Argument("triggerNodes", Identifiers.String, ValueRanks.OneDimension, new UInteger[10], new LocalizedText("program trigger nodes"));

    public static final Argument script = new Argument("script", Identifiers.String, ValueRanks.Any, null, new LocalizedText("program script to compile (notice: compile language has been set to [javascript])"));

    public static final Argument tensorflow = new Argument("tensorflow", Identifiers.Boolean, ValueRanks.Any, null, new LocalizedText("set tensorflow to true if you want to use machine learning programs with tensorflow.js"));

    public static final Argument Result = new Argument("result", Identifiers.String, ValueRanks.Any, null, new LocalizedText("result"));

    public static String APP_NAME = null;
    public static final int APP_NAMESPACE_INDEX = Integer.parseInt(Props.getProperty("app-namespace-index").toString());
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private UaMethodNode uaMethodNode;
    private RunTime runTime;

    public JavaScriptProgramLoadMethod(UaMethodNode uaMethodNode, RunTime runTime) {
        super(uaMethodNode);
        APP_NAME = uaMethodNode.getNodeContext().getServer().getConfig().getApplicationName().getText();
        this.uaMethodNode = uaMethodNode;
        this.runTime = runTime;
    }

    @Override
    public Argument[] getInputArguments() {
        return new Argument[]{name, displayName, location, triggerNodes, script, tensorflow};
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
            String[] triggerNodes = (String[]) inputValues[3].getValue();
            String script = (String) inputValues[4].getValue();
            String COMPONENT_NODE_ID = RuntimeIdentifier.COMPONENT_IDENTIFIER.getIdentifier().replace("{app-name}", APP_NAME).replace("{component-name}", name);

            UaObjectNode desiredLocation = (UaObjectNode) uaMethodNode.getNodeManager().get(new NodeId(APP_NAMESPACE_INDEX, location));
            // check update
            UaObjectNode componentNode = (UaObjectNode) uaMethodNode.getNodeManager().get(new NodeId(APP_NAMESPACE_INDEX, COMPONENT_NODE_ID));
            if (componentNode != null) {
                this.runTime.delete(componentNode);
            }
            componentNode = new UaObjectNode(uaMethodNode.getNodeContext(), Utils.newNodeId(COMPONENT_NODE_ID), Utils.newQualifiedName(COMPONENT_NODE_ID), new LocalizedText(displayName));
            UaVariableNode outputAttribute = addOutPutToComponent(COMPONENT_NODE_ID);
            componentNode.addReference(new Reference(componentNode.getNodeId(), Identifiers.HasProperty, outputAttribute.getNodeId().expanded(), true));
            uaMethodNode.getNodeManager().addNode(componentNode);


            addComponentNodeAttributes(inputValues, COMPONENT_NODE_ID, componentNode);

            final String triggerNodesIdentifier = RuntimeIdentifier.RUNTIME_PROPERTY.getIdentifier().replace("{property-name}", "triggerNodes").replace("{runtime-identifier}", COMPONENT_NODE_ID);
            final String triggerNodesBrowseName = RuntimeIdentifier.PROPERTY_BROWSE_NAME.getIdentifier().replace("{property-name}", "triggerNodes");
            UaVariableNode triggerNodesAttribute = new UaVariableNode.UaVariableNodeBuilder(uaMethodNode.getNodeContext()).setNodeId(Utils.newNodeId(triggerNodesIdentifier)).setAccessLevel(AccessLevel.READ_WRITE).setBrowseName(Utils.newQualifiedName(triggerNodesBrowseName)).setDisplayName(LocalizedText.english("triggerNodes")).setDataType(Identifiers.String).setTypeDefinition(Identifiers.PropertyType).setValue(new DataValue(new Variant(Utils.toJson(triggerNodes)))).build();
            uaMethodNode.getNodeManager().addNode(triggerNodesAttribute);
            componentNode.addReference(new Reference(componentNode.getNodeId(), Identifiers.HasProperty, triggerNodesAttribute.getNodeId().expanded(), true));

            addTriggerNodes(triggerNodes, script, componentNode);


            if (desiredLocation != null) {
                desiredLocation.addComponent(componentNode);
                desiredLocation.addReference(new Reference(desiredLocation.getNodeId(), Identifiers.HasChild, componentNode.getNodeId().expanded(), true));
            }
            return new Variant[]{new Variant(true)};
        } catch (Exception e) {
            logger.error("Error invoking ProgramMethod of objectId={} with message {}", invocationContext.getObjectId(), e.getMessage());
            return new Variant[]{new Variant(false)};
        }
    }

    private UaVariableNode addOutPutToComponent(String COMPONENT_NODE_ID) {
        final String outputIdentifier = RuntimeIdentifier.RUNTIME_PROPERTY.getIdentifier().replace("{property-name}", "output").replace("{runtime-identifier}", COMPONENT_NODE_ID);
        final String outputBrowseName = RuntimeIdentifier.PROPERTY_BROWSE_NAME.getIdentifier().replace("{property-name}", "output");
        UaVariableNode outputAttribute = new UaVariableNode.UaVariableNodeBuilder(uaMethodNode.getNodeContext()).setNodeId(new NodeId(2, outputIdentifier)).setAccessLevel(AccessLevel.READ_WRITE).setBrowseName(new QualifiedName(2, outputBrowseName)).setDisplayName(LocalizedText.english("output")).setDataType(Identifiers.String).setTypeDefinition(Identifiers.PropertyType).setValue(DataValue.valueOnly(new Variant(""))).build();
        uaMethodNode.getNodeManager().addNode(outputAttribute);
        return outputAttribute;
    }

    private void addTriggerNodes(String[] triggerNodes, String script, UaObjectNode componentNode) {
        final List<String> triggers = getParentTriggers(componentNode.getNodeId().getIdentifier().toString()).getTriggers();
        if (!(triggers == null || triggers.size() == 0)) {
            for (String triggerIdentifier : triggers) {
                this.runTime.getUaNodeContext().getNodeManager().cacheNodeManager().getTriggerParentFactoryCache().deleteParentFromTriggerParents(triggerIdentifier, componentNode.getNodeId().getIdentifier().toString());
            }
        }
        if (triggerNodes.length == 0) {
            UaVariableNode node = (UaVariableNode) uaMethodNode.getNodeContext().getNodeManager().get(Utils.newNodeId("TIME"));
            this.runTime.load(node, componentNode, script);
        }
        for (String triggerIdentifier : triggerNodes) {
            this.runTime.load(this.uaMethodNode.getNodeManager().get(new NodeId(APP_NAMESPACE_INDEX, triggerIdentifier)), componentNode, script);
            addToTriggerParent(triggerIdentifier, componentNode.getNodeId().getIdentifier().toString());
        }
        addToParentTrigger(triggerNodes, componentNode.getNodeId().getIdentifier().toString());
    }

    private void addToTriggerParent(String triggerIdentifier, String parentIdentifier) {
        SerializableTriggerParent serializableTriggerParent = new SerializableTriggerParent();
        serializableTriggerParent.setIdentifier(triggerIdentifier);
        serializableTriggerParent.setParents(List.of(parentIdentifier));
        this.runTime.getUaNodeContext().getNodeManager().cacheNodeManager().getTriggerParentFactoryCache().addTriggerParents(serializableTriggerParent);
    }

    private SerializableParentTrigger getParentTriggers(String parentIdentifier) {
        SerializableParentTrigger serializableParentTrigger = new SerializableParentTrigger();
        serializableParentTrigger.setIdentifier(parentIdentifier);
        return this.runTime.getUaNodeContext().getNodeManager().cacheNodeManager().getTriggerParentFactoryCache().findParentTriggersByParentIdentifier(serializableParentTrigger.getIdentifier());

    }

    private void addToParentTrigger(String[] triggerNodes, String parentIdentifier) {
        SerializableParentTrigger serializableParentTrigger = new SerializableParentTrigger();
        serializableParentTrigger.setIdentifier(parentIdentifier);
        serializableParentTrigger.setTriggers(List.of(triggerNodes));
        this.runTime.getUaNodeContext().getNodeManager().cacheNodeManager().getTriggerParentFactoryCache().addParentTrigger(serializableParentTrigger);
    }

    private void addComponentNodeAttributes(Variant[] inputValues, String PROGRAM_NODE_ID, UaObjectNode programNode) {
        int index = 0;
        for (Argument input : getInputArguments()) {
            if (input.getName() == "displayName" || input.getName() == "triggerNodes") {
                index++;
                continue;
            }
            Variant nodeValue = null;
            if (inputValues[index] != null) {
                nodeValue = new Variant(inputValues[index].getValue());
            }
            final String propertyIdentifier = RuntimeIdentifier.RUNTIME_PROPERTY.getIdentifier().replace("{property-name}", input.getName()).replace("{runtime-identifier}", PROGRAM_NODE_ID);
            final String propertyBrowseName = RuntimeIdentifier.PROPERTY_BROWSE_NAME.getIdentifier().replace("{property-name}", input.getName());
            UaVariableNode attribute = new UaVariableNode.UaVariableNodeBuilder(uaMethodNode.getNodeContext()).setNodeId(Utils.newNodeId(propertyIdentifier)).setAccessLevel(AccessLevel.READ_WRITE).setBrowseName(Utils.newQualifiedName(propertyBrowseName)).setDisplayName(LocalizedText.english(input.getName())).setDataType(Identifiers.String).setTypeDefinition(Identifiers.PropertyType).setValue(DataValue.valueOnly(nodeValue)).build();
            uaMethodNode.getNodeManager().addNode(attribute);
            programNode.addReference(new Reference(programNode.getNodeId(), Identifiers.HasProperty, attribute.getNodeId().expanded(), true));
            index++;
        }
    }

    protected void validateInputArgumentValues(Variant[] inputArgumentValues) throws InvalidArgumentException {
        MethodInputValidator.forbiddenCharacter(this, name);
        MethodInputValidator.NotNull(this, name, location, script);
        MethodInputValidator.Exists(this, location, triggerNodes);
        MethodInputValidator.isBoolean(this, tensorflow);
        MethodInputValidator.validateScript(this, script);
        MethodInputValidator.checkType(this, location, NodeClass.Object);

        if (!this.inputErrorMessages.isEmpty()) {
            throw new InvalidArgumentException(null);
        }
    }

}
