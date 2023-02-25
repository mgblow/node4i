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
import org.eclipse.milo.opcua.sdk.server.nodes.*;
import org.eclipse.milo.opcua.sdk.server.nodes.serializable.SerializableParentTrigger;
import org.eclipse.milo.opcua.sdk.server.nodes.serializable.SerializableTriggerParent;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.*;
import org.eclipse.milo.opcua.stack.core.types.enumerated.NodeClass;
import org.eclipse.milo.opcua.stack.core.types.structured.Argument;
import org.eclipse.milo.platform.alamEvent.AlarmEvent;
import org.eclipse.milo.platform.alamEvent.EventIdentifier;
import org.eclipse.milo.platform.util.Props;
import org.eclipse.milo.platform.util.ScriptEngineUtil;
import org.eclipse.milo.platform.util.Utils;
import org.eclipse.milo.platform.validators.MethodInputValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

public class EventLoadMethod extends AbstractMethodInvocationHandler {

    public static final Argument name = new Argument("name", Identifiers.String, ValueRanks.Any, null, new LocalizedText("name"));

    public static final Argument displayName = new Argument("displayName", Identifiers.String, ValueRanks.Any, null, new LocalizedText("displayName"));

    public static final Argument location = new Argument("location", Identifiers.String, ValueRanks.Any, null, new LocalizedText("operation location."));

    public static final Argument conditionClassName = new Argument("conditionClass", Identifiers.String, ValueRanks.Any, null, new LocalizedText("operation condition class."));

    public static final Argument severity = new Argument("severity", Identifiers.String, ValueRanks.Any, null, new LocalizedText("severity"));

    public static final Argument message = new Argument("message", Identifiers.String, ValueRanks.Any, null, new LocalizedText("message"));

    public static final Argument condition = new Argument("condition", Identifiers.String, ValueRanks.Any, null, new LocalizedText("condition to compile (notice: compile language has been set to [javascript])"));

    public static final Argument offDelay = new Argument("offDelay", Identifiers.String, ValueRanks.Any, null, new LocalizedText("Operation OffDelay"));

    public static final Argument onDelay = new Argument("onDelay", Identifiers.String, ValueRanks.Any, null, new LocalizedText("Operation OnDelay"));

    public static final Argument maxTimeShelved = new Argument("maxTimeShelved", Identifiers.String, ValueRanks.Any, null, new LocalizedText("Operation MaxTimeShelved"));

    public static final Argument tensorflow = new Argument("tensorflow", Identifiers.String, ValueRanks.Any, null, new LocalizedText("set tensorflow to true if you want to use machine learning in your condition with tensorflow.js"));

    public static final Argument triggerNodes = new Argument("triggerNodes", Identifiers.String, ValueRanks.OneOrMoreDimensions, null, new LocalizedText("Alarm&Event's trigger nodes"));

    public static final Argument enabled = new Argument("enabled", Identifiers.Boolean, ValueRanks.Any, null, new LocalizedText("is It enabled?"));
    public static final Argument history = new Argument("history", Identifiers.Boolean, ValueRanks.Any, null, new LocalizedText("is It saved?"));

    public static final Argument Result = new Argument("result", Identifiers.String, ValueRanks.Any, null, new LocalizedText("result"));

    public static String APP_NAME = null;
    public static final int APP_NAMESPACE_INDEX = Integer.parseInt(Props.getProperty("app-namespace-index").toString());
    private final Logger logger = LoggerFactory.getLogger(getClass());
    public UaMethodNode uaMethodNode;
    private AlarmEvent alarmEvent;

    public EventLoadMethod(UaMethodNode uaMethodNode, AlarmEvent alarmEvent) {
        super(uaMethodNode);
        APP_NAME = uaMethodNode.getNodeContext().getServer().getConfig().getApplicationName().getText();
        this.uaMethodNode = uaMethodNode;
        this.alarmEvent = alarmEvent;
    }

    @Override
    public Argument[] getInputArguments() {

        return new Argument[]{name, displayName, location, conditionClassName, severity, message, condition, offDelay, onDelay, maxTimeShelved, triggerNodes, enabled, history};
    }

    @Override
    public Argument[] getOutputArguments() {
        return new Argument[]{Result};
    }

    @Override
    protected Variant[] invoke(InvocationContext invocationContext, Variant[] inputValues) throws UaException {
        logger.debug("Invoking program() method of objectId={}", invocationContext.getObjectId());
        String name = (String) inputValues[0].getValue();
        String displayName = (String) inputValues[1].getValue();
        displayName = displayName == null ? name : displayName;
        String location = (String) inputValues[2].getValue();
        String className = (String) inputValues[3].getValue();
        String severity = (String) inputValues[4].getValue();
        String message = (String) inputValues[5].getValue();
        String[] triggerNodes = (String[]) inputValues[10].getValue();
        Boolean enabled = (Boolean) inputValues[11].getValue();
        String ALARM_EVENT_NODE_ID = EventIdentifier.ALARM_IDENTIFIER.getIdentifier().replace("{app-name}", APP_NAME).replace("{alarm-name}", name);

        if (name.equals("load")) {
            this.loadEvent();
            return new Variant[]{new Variant("done loading")};
        }
        try {
            final String classIdentifier = EventIdentifier.CONDITION_CLASS_IDENTIFIER.getIdentifier().replace("{app-name}", APP_NAME).replace("{class-name}", className);
            UaObjectNode classNode = (UaObjectNode) uaMethodNode.getNodeManager().get(new NodeId(2, classIdentifier));

            if (classNode == null) {
                throw new UaException(StatusCode.BAD);
            }
            UaObjectNode alarmConditionTypeEvent = (UaObjectNode) this.uaMethodNode.getNodeContext().getNodeManager().get(new NodeId(Integer.parseInt(Props.getProperty("app-namespace-index").toString()), ALARM_EVENT_NODE_ID));

            if (alarmConditionTypeEvent == null) {
                // send an alarmEvent
                alarmConditionTypeEvent = new UaObjectNode(uaMethodNode.getNodeContext(), new NodeId(Integer.parseInt(Props.getProperty("app-namespace-index").toString()), ALARM_EVENT_NODE_ID), new QualifiedName(Integer.parseInt(Props.getProperty("app-namespace-index").toString()), ALARM_EVENT_NODE_ID), new LocalizedText(displayName));


            } else {

                List<Reference> references = alarmConditionTypeEvent.getReferences().stream().filter(c -> c.getDirection() == Reference.Direction.INVERSE).collect(Collectors.toList());
                //remove only current location reference
                UaVariableNode locationNode = (UaVariableNode) this.uaMethodNode.getNodeContext().getNodeManager().get(new NodeId(Integer.parseInt(Props.getProperty("app-namespace-index").toString()), EventIdentifier.ALARM_PROPERTY_IDENTIFIER.getIdentifier().replace("{alarm-identifier}",ALARM_EVENT_NODE_ID).replace("{property-name}","location")));
                String locationValue = locationNode.getValue().getValue().getValue().toString();
                for (Reference reference : references) {
                    if (reference.getTargetNodeId().getIdentifier().equals(locationValue)) {
                        alarmConditionTypeEvent.removeReference(reference);
                    }
                }
                alarmConditionTypeEvent.setDisplayName(new LocalizedText(displayName));
            }

            alarmConditionTypeEvent.setDisplayName(new LocalizedText(displayName));
            invocationContext.getMethodNode().getNodeManager().addNode(alarmConditionTypeEvent);
            classNode.addComponent(alarmConditionTypeEvent);
            int index = -1;
            for (Argument input : this.getInputArguments()) {
                index++;
                if (input.getName() == "displayName") continue;
                if (input.getName() == "triggerNodes") continue;
                final String alarmPropertyIdentifier = EventIdentifier.ALARM_PROPERTY_IDENTIFIER.getIdentifier().replace("{property-name}", input.getName()).replace("{alarm-identifier}", ALARM_EVENT_NODE_ID);
                UaVariableNode attribute = new UaVariableNode.UaVariableNodeBuilder(uaMethodNode.getNodeContext()).setNodeId(Utils.newNodeId(alarmPropertyIdentifier)).setAccessLevel(AccessLevel.READ_WRITE).setBrowseName(Utils.newQualifiedName(EventIdentifier.PROPERTY_BROWSE_NAME.getIdentifier().replace("{property-name}",input.getName()))).setDisplayName(LocalizedText.english(input.getName())).setDataType(Identifiers.String).setTypeDefinition(Identifiers.PropertyType).setValue(DataValue.valueOnly(new Variant(inputValues[index].getValue()))).build();
                alarmConditionTypeEvent.addReference(new Reference(alarmConditionTypeEvent.getNodeId(), Identifiers.HasProperty, attribute.getNodeId().expanded(), true));
                this.uaMethodNode.getNodeManager().addNode(attribute);
            }

            final String alarmTriggerIdentifier = EventIdentifier.ALARM_PROPERTY_IDENTIFIER.getIdentifier().replace("{property-name}", "triggerNodes").replace("{alarm-identifier}", ALARM_EVENT_NODE_ID);
            UaVariableNode triggerNodesAttribute = new UaVariableNode.UaVariableNodeBuilder(uaMethodNode.getNodeContext()).setNodeId(Utils.newNodeId(alarmTriggerIdentifier)).setAccessLevel(AccessLevel.READ_WRITE).setBrowseName(Utils.newQualifiedName(EventIdentifier.PROPERTY_BROWSE_NAME.getIdentifier().replace("{property-name}","triggerNodes"))).setDisplayName(LocalizedText.english("triggerNodes")).setDataType(Identifiers.String).setTypeDefinition(Identifiers.PropertyType).setValue(new DataValue(new Variant(Utils.toJson(triggerNodes)))).build();
            uaMethodNode.getNodeManager().addNode(triggerNodesAttribute);
            alarmConditionTypeEvent.addReference(new Reference(alarmConditionTypeEvent.getNodeId(), Identifiers.HasProperty, triggerNodesAttribute.getNodeId().expanded(), true));


            UaNode desiredLocation = uaMethodNode.getNodeManager().get(new NodeId(2, location));
            if (desiredLocation != null) {
                desiredLocation.addReference(new Reference(desiredLocation.getNodeId(), Identifiers.HasComponent, alarmConditionTypeEvent.getNodeId().expanded(), true));
            }

            final String ALARM_FOLDER_IDENTIFIER = EventIdentifier.ALARM_EVENT_FOLDER.getIdentifier().replace("{app-name}", APP_NAME);
            UaFolderNode alarmEventFolder = (UaFolderNode) uaMethodNode.getNodeManager().get(new NodeId(2,ALARM_FOLDER_IDENTIFIER));
            alarmEventFolder.addReference(new Reference(alarmEventFolder.getNodeId(), Identifiers.HasEventSource, alarmConditionTypeEvent.getNodeId().expanded(), true));
            // generate eventSource
            this.uaMethodNode.getNodeContext().getServer().getEventFactory().createAlarmConditionTypeEvent(alarmConditionTypeEvent, false);
            // final condition
            String finalCondition = new ScriptEngineUtil(ScriptEngineUtil.Engine.JavaScript).constructAlarmEventsScript(alarmConditionTypeEvent);
            // triggerNodes
            addTriggerNodes(triggerNodes, finalCondition, alarmConditionTypeEvent);
            return new Variant[]{new Variant(true)};
        } catch (Exception e) {
            logger.error("Error invoking condition() method of objectId={} with message {}", invocationContext.getObjectId(), e.getMessage());
            return new Variant[]{new Variant(false)};
        }
    }

    private void loadEvent() {
        final String ALARM_FOLDER_IDENTIFIER = EventIdentifier.ALARM_EVENT_FOLDER.getIdentifier().replace("{app-name}", APP_NAME);
        LoggerFactory.getLogger(getClass()).info("[BootLoader] started loading events ...");
        // get Alarm&Events Folder
        UaNode alarmEventsFolder = uaMethodNode.getNodeContext().getNodeManager().get(new NodeId(APP_NAMESPACE_INDEX, ALARM_FOLDER_IDENTIFIER));
        alarmEventsFolder.getReferences().stream().forEach(reference -> {
            if (reference.getReferenceTypeId().equals(Identifiers.HasEventSource)) {
                UaNode event = uaMethodNode.getNodeContext().getNodeManager().get(new NodeId(reference.getTargetNodeId().getNamespaceIndex(), reference.getTargetNodeId().getIdentifier().toString()));
                try {
                    uaMethodNode.getNodeContext().getServer().getEventFactory().createAlarmConditionTypeEvent(event, false);
                } catch (UaException e) {
                    LoggerFactory.getLogger(getClass()).error("error adding event source for node : {}", event.getNodeId());
                }
                String condition = new ScriptEngineUtil(ScriptEngineUtil.Engine.JavaScript).constructAlarmEventsScript(event);
                String triggerNodes = new String(Utils.getPropertyValue(event, EventIdentifier.PROPERTY_BROWSE_NAME.getIdentifier().replace("{property-name}","triggerNodes")).getBytes(), StandardCharsets.UTF_8);
                addTriggerNodes(Utils.fromJsonToArrayOfString(triggerNodes), condition, event);
            }
        });
    }

    private void addTriggerNodes(String[] triggerNodes, String script, UaNode eventNode) {
        final List<String> triggers = getParentTriggers(eventNode.getNodeId().getIdentifier().toString()).getTriggers();
        if (!(triggers == null || triggers.size() == 0)) {
            for (String triggerIdentifier : triggers) {
                this.alarmEvent.getUaNodeContext().getNodeManager().cacheNodeManager().getTriggerParentFactoryCache().deleteParentFromTriggerParents(triggerIdentifier, eventNode.getNodeId().getIdentifier().toString());
            }
        }
        if (triggerNodes.length == 0) {
            UaNode node = uaMethodNode.getNodeContext().getNodeManager().get(Utils.newNodeId("TIME"));
            this.alarmEvent.addEventToNode(node, eventNode, script);
        }
        for (String triggerIdentifier : triggerNodes) {
            UaVariableNode node = (UaVariableNode) this.uaMethodNode.getNodeManager().get(new NodeId(APP_NAMESPACE_INDEX, triggerIdentifier));
            this.alarmEvent.addEventToNode(node, eventNode, script);
            addToTriggerParent(triggerIdentifier, eventNode.getNodeId().getIdentifier().toString());
        }
        addToParentTrigger(triggerNodes, eventNode.getNodeId().getIdentifier().toString());
    }

    private void addToTriggerParent(String triggerIdentifier, String parentIdentifier) {
        SerializableTriggerParent serializableTriggerParent = new SerializableTriggerParent();
        serializableTriggerParent.setIdentifier(triggerIdentifier);
        serializableTriggerParent.setParents(List.of(parentIdentifier));
        this.alarmEvent.getUaNodeContext().getNodeManager().cacheNodeManager().getTriggerParentFactoryCache().addTriggerParents(serializableTriggerParent);
    }

    private SerializableParentTrigger getParentTriggers(String parentIdentifier) {
        SerializableParentTrigger serializableParentTrigger = new SerializableParentTrigger();
        serializableParentTrigger.setIdentifier(parentIdentifier);
        return this.alarmEvent.getUaNodeContext().getNodeManager().cacheNodeManager().getTriggerParentFactoryCache().findParentTriggersByParentIdentifier(serializableParentTrigger.getIdentifier());

    }

    private void addToParentTrigger(String[] triggerNodes, String parentIdentifier) {
        SerializableParentTrigger serializableParentTrigger = new SerializableParentTrigger();
        serializableParentTrigger.setIdentifier(parentIdentifier);
        serializableParentTrigger.setTriggers(List.of(triggerNodes));
        this.alarmEvent.getUaNodeContext().getNodeManager().cacheNodeManager().getTriggerParentFactoryCache().addParentTrigger(serializableParentTrigger);
    }

    protected void validateInputArgumentValues(Variant[] inputArgumentValues) throws InvalidArgumentException {
        MethodInputValidator.forbiddenCharacter(this, name);
        MethodInputValidator.NotNull(this, name, location, conditionClassName, severity, message, condition, enabled);
        MethodInputValidator.Exists(this, location, triggerNodes);
        MethodInputValidator.checkType(this, location, NodeClass.Object);
        MethodInputValidator.Exists(this, conditionClassName, APP_NAME + "/Alarm&Events/ConditionClasses/");
        MethodInputValidator.isNumber(this, severity, offDelay, onDelay, maxTimeShelved);
        MethodInputValidator.checkRange(this, severity, 1000l, "<");
        MethodInputValidator.isBoolean(this, enabled, tensorflow, history);
        MethodInputValidator.validateScript(this, condition);
        MethodInputValidator.checkType(this, location, NodeClass.Object);
        if (this.inputErrorMessages.size() != 0) {
            throw new InvalidArgumentException(null);
        }
    }
}
