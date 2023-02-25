/*
 * Copyright (c) 2019 the Eclipse Milo Authors
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.milo.opcua.sdk.server.nodes.factories;

import com.google.gson.Gson;
import org.eclipse.milo.opcua.sdk.core.Reference;
import org.eclipse.milo.opcua.sdk.core.nodes.ObjectNode;
import org.eclipse.milo.opcua.sdk.core.nodes.ObjectTypeNode;
import org.eclipse.milo.opcua.sdk.server.*;
import org.eclipse.milo.opcua.sdk.server.api.NodeManager;
import org.eclipse.milo.opcua.sdk.server.api.persistence.RedisCommunication;
import org.eclipse.milo.opcua.sdk.server.api.persistence.caches.EventFactoryCache;
import org.eclipse.milo.opcua.sdk.server.drivers.MqttDriver;
import org.eclipse.milo.opcua.sdk.server.model.nodes.objects.AlarmConditionTypeNode;
import org.eclipse.milo.opcua.sdk.server.model.nodes.objects.BaseEventTypeNode;
import org.eclipse.milo.opcua.sdk.server.model.nodes.variables.TwoStateVariableTypeNode;
import org.eclipse.milo.opcua.sdk.server.model.types.objects.AlarmConditionType;
import org.eclipse.milo.opcua.sdk.server.model.types.objects.BaseEventType;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNodeContext;
import org.eclipse.milo.opcua.sdk.server.nodes.serializable.SerializableAlarmConditionTypeNode;
import org.eclipse.milo.opcua.sdk.server.util.Props;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.*;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UShort;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class EventFactory extends AbstractLifecycle {

    private final NodeManager<UaNode> nodeManager;

    private final OpcUaServer server;
    private final NodeFactory nodeFactory;
    MqttDriver mqttDriver;

    private BaseEventType sysErrorEvent;

    private UaNodeContext context;
    private static LocalizedText aTrueState = new LocalizedText("true");
    private static LocalizedText aFalseState = new LocalizedText("false");
    private static String ALARM_EVENT_DOCUMENT = "events";
    private static String Time_Pattern = "EEE MMM d HH:mm:ss z YYYY";
    private int APP_NAMESPACE_INDEX = Integer.parseInt(Props.getProperty("app-namespace-index").toString());
    private String APP_NAME;
    private EventFactoryCache eventFactoryCache;


    public EventFactory(OpcUaServer server) {
        this(server, server.getObjectTypeManager(), server.getVariableTypeManager());
    }

    public EventFactory(OpcUaServer server, ObjectTypeManager objectTypeManager, VariableTypeManager variableTypeManager) {
        this.server = server;
        this.nodeManager = new UaNodeManager(null);
        this.context = new EventNodeContext(server, nodeManager);
        nodeFactory = new NodeFactory(context, objectTypeManager, variableTypeManager);
        this.mqttDriver = new MqttDriver();
        eventFactoryCache = new EventFactoryCache(new RedisCommunication(server));
        APP_NAME = server.getConfig().getApplicationName().getText();

    }

    @Override
    protected void onStartup() {
        server.getAddressSpaceManager().register(nodeManager);
    }

    @Override
    protected void onShutdown() {
        server.getAddressSpaceManager().unregister(nodeManager);
    }

    /**
     * Create an Event instance of the type identified by {@code typeDefinitionId}.
     * <p>
     * Event Nodes must be deleted by the caller when once they have been posted to the event bus or their lifetime
     * has otherwise expired.
     *
     * @param nodeId           the {@link NodeId} to use for the Event {@link ObjectNode}.
     * @param typeDefinitionId the {@link NodeId} of the {@link ObjectTypeNode} representing the type definition.
     * @return an Event {@link ObjectNode} instance.
     * @throws UaException if an error occurs creating the Event instance.
     */
    public BaseEventTypeNode createEvent(NodeId nodeId, NodeId typeDefinitionId) throws UaException {
        return (BaseEventTypeNode) nodeFactory.createNode(nodeId, typeDefinitionId, new NodeFactory.InstantiationCallback() {
            @Override
            public boolean includeOptionalNode(NodeId typeDefinitionId, QualifiedName browseName) {
                return true;
            }
        });
    }

    public SerializableAlarmConditionTypeNode findNewestBySourceNode(UaNode uaNode, boolean clone) throws UaException {
        return eventFactoryCache.findNewestBySourceNode(uaNode.getNodeId().getIdentifier().toString());

    }
    public AlarmConditionTypeNode createAlarmConditionTypeEvent(UaNode sourceNode, boolean clone) throws UaException {
        NodeId nodeId = (clone) ? Utils.newNodeId(sourceNode.getNodeId().getIdentifier() + "/clone") : sourceNode.getNodeId();
        AlarmConditionTypeNode alarmConditionTypeNode =null;
        try{
            alarmConditionTypeNode = (AlarmConditionTypeNode) nodeFactory.createNode(nodeId, Identifiers.AlarmConditionType, new NodeFactory.InstantiationCallback() {
                @Override
                public boolean includeOptionalNode(NodeId typeDefinitionId, QualifiedName browseName) {
                    return true;
                }
            });
        }catch (Exception e){
            LoggerFactory.getLogger(getClass()).error("Error occurred in EventFactory createAlarmConditionTypeEvent Method ..");
        }
        alarmConditionTypeNode.setEventType(Identifiers.AlarmConditionType);
        alarmConditionTypeNode.setConditionName(sourceNode.getNodeId().getIdentifier().toString());
        String propertyUniqueIdentifier = sourceNode.getNodeId().getIdentifier().toString() + "/" + UUID.randomUUID();
        TwoStateVariableTypeNode ackedStateNode = new TwoStateVariableTypeNode(this.context, Utils.newNodeId(propertyUniqueIdentifier, "AckedState"), Utils.newQualifiedName("Property/AckedState"), new LocalizedText("AckedState"), new LocalizedText("AckedState"), UInteger.valueOf(1), UInteger.valueOf(1));
        TwoStateVariableTypeNode activeStateNode = new TwoStateVariableTypeNode(this.context, Utils.newNodeId(propertyUniqueIdentifier, "ActiveState"), Utils.newQualifiedName("Property/ActiveState"), new LocalizedText("ActiveState"), new LocalizedText("ActiveState"), UInteger.valueOf(1), UInteger.valueOf(1));
        TwoStateVariableTypeNode enabledStateNode = new TwoStateVariableTypeNode(this.context, Utils.newNodeId(propertyUniqueIdentifier, "EnabledState"), Utils.newQualifiedName("Property/EnabledState"), new LocalizedText("EnabledState"), new LocalizedText("EnabledState"), UInteger.valueOf(1), UInteger.valueOf(1));

        this.context.getNodeManager().addNode(ackedStateNode);
        this.context.getNodeManager().addNode(activeStateNode);
        this.context.getNodeManager().addNode(enabledStateNode);
        alarmConditionTypeNode.setSourceNode(sourceNode.getNodeId());
        alarmConditionTypeNode.setSourceName(sourceNode.getBrowseName().getName());
        alarmConditionTypeNode.addComponent(ackedStateNode);
        alarmConditionTypeNode.addComponent(activeStateNode);
        alarmConditionTypeNode.addComponent(enabledStateNode);
        alarmConditionTypeNode.addComponent(sourceNode);
        alarmConditionTypeNode.addReference(new Reference(alarmConditionTypeNode.getNodeId(), Identifiers.PropertyType, sourceNode.getNodeId().expanded(), true));
        alarmConditionTypeNode.getAckedStateNode().setTrueState(aFalseState);
        alarmConditionTypeNode.getAckedStateNode().setFalseState(aFalseState);
        alarmConditionTypeNode.getActiveStateNode().setTrueState(aFalseState);
        alarmConditionTypeNode.getActiveStateNode().setFalseState(aTrueState);
        alarmConditionTypeNode.getEnabledStateNode().setTrueState(aTrueState);
        alarmConditionTypeNode.getEnabledStateNode().setFalseState(aTrueState);
        alarmConditionTypeNode.setActiveState(new LocalizedText("false"));
        alarmConditionTypeNode.setAckedState(aFalseState);
        alarmConditionTypeNode.setEnabledState(aTrueState);
        alarmConditionTypeNode.setConfirmedState(aTrueState);
        alarmConditionTypeNode.setConditionClassName(new LocalizedText(Utils.getPropertyValue(sourceNode, "Property/conditionClass")));
        alarmConditionTypeNode.setMessage(new LocalizedText(Utils.getPropertyValue(sourceNode, "Property/message")));
        alarmConditionTypeNode.setSeverity(UShort.valueOf(Utils.getPropertyValue(sourceNode, "Property/severity")));
        if (Utils.getPropertyValue(sourceNode, "Property/enabled").equals("false")) {
            alarmConditionTypeNode.getEnabledStateNode().setTrueState(aFalseState);
            alarmConditionTypeNode.getEnabledStateNode().setFalseState(aTrueState);
        } else {
            alarmConditionTypeNode.getEnabledStateNode().setFalseState(aFalseState);
            alarmConditionTypeNode.getEnabledStateNode().setTrueState(aTrueState);
        }
        alarmConditionTypeNode.setRetain(false);
        this.context.getNodeManager().addNode(alarmConditionTypeNode);
        return alarmConditionTypeNode;
    }

    public void publishUncompletedLifeCycles(String[] eventNodeIds) {
        Arrays.stream(eventNodeIds).forEach(eventNodeId -> {
            List<SerializableAlarmConditionTypeNode> list = eventFactoryCache.findBySourceNode(eventNodeId);
            Iterator<SerializableAlarmConditionTypeNode> itr = list.iterator();
            while (itr.hasNext()) {
                // // loop through events
            }

            // post events
        });
    }


    public void post(AlarmConditionTypeNode event) {
        this.sendEventOverMqtt(event);
        this.server.getEventBus().post(event);
        this.persist(event);
    }

    public void post(BaseEventType event , boolean flag) {
        server.getEventBus().post(event);
    }
    public void sendSysErrorEvent(String location , String message){
        try {
            String NAME = "SYSERROR_" + location;
            if (sysErrorEvent == null) {
                sysErrorEvent = createEvent(new NodeId(APP_NAMESPACE_INDEX, NAME), Identifiers.BaseEventType);
                sysErrorEvent.setSeverity(UShort.valueOf(1000));
                sysErrorEvent.setEventType(Identifiers.BaseEventType);
                sysErrorEvent.setSourceNode(new NodeId(APP_NAMESPACE_INDEX, APP_NAME));
            }
            sysErrorEvent.setSourceName(NAME);
            sysErrorEvent.setMessage(new LocalizedText(message));
            sysErrorEvent.setTime(new DateTime());
            sysErrorEvent.setReceiveTime(new DateTime());
            this.post(sysErrorEvent,false);
        }catch (Exception e) {

        }

    }
    public void sendEventOverMqtt(AlarmConditionTypeNode event) {
        CompletableFuture.runAsync(() -> {
            this.mqttDriver.send("server/events/" + event.getSourceNode().getIdentifier().toString(), serializedAlarmConditionTypeNodeEvent(event));
        });
    }

    private void persist(AlarmConditionTypeNode event) {
        eventFactoryCache.save(event);
    }

    public void acknowledge(String eventId, LocalizedText comment) throws ParseException {
        // about to acknowledge stored event
        List<SerializableAlarmConditionTypeNode> events = eventFactoryCache.findByEventId(eventId);
        if (events.size() != 0) {
            boolean retainCompleted = false;
            Comparator<SerializableAlarmConditionTypeNode> comparator = Comparator.comparing(SerializableAlarmConditionTypeNode::getTime);
            SerializableAlarmConditionTypeNode event = events.stream().max(comparator).get();
            String sourceNodeId = event.getSourceNode();
            AlarmConditionTypeNode alarmConditionTypeNode = (AlarmConditionTypeNode) this.context.getNodeManager().get(Utils.newNodeId(sourceNodeId));
            if (alarmConditionTypeNode != null) {
                String acknowledgeState = event.getAcknowledgeState();
                String retain = event.getRetain();
                if (retain.equals("true") && acknowledgeState.equals("false")) {
                    DateTime acknowledgeTime = new DateTime();
                    String addressSpaceEventId = new String(alarmConditionTypeNode.getEventId().bytes(), StandardCharsets.UTF_8);
                    // current node in AddressSpace should be updated
                    if (addressSpaceEventId.equals(eventId)) {
                        // current node in AddressSpace should be updated
                        alarmConditionTypeNode.setAckedState(aTrueState);
                        alarmConditionTypeNode.getAckedStateNode().setTrueState(aTrueState);
                        alarmConditionTypeNode.getAckedStateNode().setFalseState(aFalseState);
                        alarmConditionTypeNode.getAckedStateNode().setTransitionTime(acknowledgeTime);
                        alarmConditionTypeNode.setTime(acknowledgeTime);
                        alarmConditionTypeNode.setComment(comment);
                        if (alarmConditionTypeNode.getActiveStateNode().getTrueState().equals(aFalseState)) {
                            alarmConditionTypeNode.setRetain(false);
                            retainCompleted = true;
                        }
                        CompletableFuture.runAsync(() -> {
                            this.post(alarmConditionTypeNode);
                        });
                        if (retainCompleted) {
                            CompletableFuture.runAsync(() -> {
                                retainCompleted(alarmConditionTypeNode);
                            });
                        }
                    } else {
                        AlarmConditionTypeNode clonedAlarmConditionTypeNode = alarmConditionTypeNode.clone();
                        clonedAlarmConditionTypeNode.setEventId(ByteString.of(eventId.getBytes()));
                        clonedAlarmConditionTypeNode.setAckedState(aTrueState);
                        clonedAlarmConditionTypeNode.getAckedStateNode().setTrueState(aTrueState);
                        clonedAlarmConditionTypeNode.getAckedStateNode().setFalseState(aFalseState);
                        clonedAlarmConditionTypeNode.getAckedStateNode().setTransitionTime(acknowledgeTime);
                        clonedAlarmConditionTypeNode.setComment(comment);
                        // time
                        Date time = new Date(event.getTime());
                        clonedAlarmConditionTypeNode.setTime(new DateTime(time));
                        // activeStateNode
                        Date activeTime = new Date(event.getActiveTime());

                        LocalizedText state = new LocalizedText(event.getActiveState());
                        if (state.equals(aTrueState)) {
                            clonedAlarmConditionTypeNode.getActiveStateNode().setTrueState(aTrueState);
                            clonedAlarmConditionTypeNode.getActiveStateNode().setFalseState(aFalseState);
                        } else {
                            clonedAlarmConditionTypeNode.getActiveStateNode().setFalseState(aTrueState);
                            clonedAlarmConditionTypeNode.getActiveStateNode().setTrueState(aFalseState);
                        }
                        clonedAlarmConditionTypeNode.getActiveStateNode().setTransitionTime(new DateTime(activeTime));
                        // activeState
                        clonedAlarmConditionTypeNode.setAckedState(new LocalizedText(event.getActiveState()));
                        if (clonedAlarmConditionTypeNode.getActiveStateNode().getTrueState().equals(aFalseState)) {
                            clonedAlarmConditionTypeNode.setRetain(false);
                            retainCompleted = true;
                        }
                        CompletableFuture.runAsync(() -> {
                            this.post(clonedAlarmConditionTypeNode);
                        }).thenApply(completed -> removeClonedObject(clonedAlarmConditionTypeNode));
                        if (retainCompleted) {
                            CompletableFuture.runAsync(() -> {
                                retainCompleted(clonedAlarmConditionTypeNode);
                            });
                        }
                    }
                }
            } else {
                // event sourceNode is not valid, something bad happened!
                LoggerFactory.getLogger(getClass()).error("error happened while trying to acknowledge event {}, sourceNode not found.");
            }
        }
    }

    private void retainCompleted(AlarmConditionTypeNode event) {
        String eid = new String(event.getEventId().bytes(), StandardCharsets.UTF_8);
        Map<String, SerializableAlarmConditionTypeNode> events = eventFactoryCache.findMapByEventId(eid);
        Boolean history = Boolean.valueOf(Utils.getPropertyValue(event.getSourceNodeNode(), "Property/history"));
        if (history) {
            for (Map.Entry<String, SerializableAlarmConditionTypeNode> entry : events.entrySet()) {
                entry.getValue().setRetain("false");
                eventFactoryCache.save(entry.getKey(), entry.getValue());
            }
        } else {
            eventFactoryCache.remove(eid);
        }
    }

    public List<SerializableAlarmConditionTypeNode> extractArchive(Map<String, List<String>> orMap, Map<String, String> andMap,Long offset , Long limit) {
        return eventFactoryCache.find(orMap, andMap,offset,limit);
    }

    private boolean removeClonedObject(AlarmConditionTypeNode clonedAlarmConditionTypeNode) {
        clonedAlarmConditionTypeNode.getReferences().stream().forEach(reference -> {
            this.context.getNodeManager().removeReference(reference);
        });
        this.context.getNodeManager().removeNode(clonedAlarmConditionTypeNode);
        return true;
    }

    public void process(UaNode sourceNode, boolean state) {
        AlarmConditionTypeNode alarmConditionTypeNode = (AlarmConditionTypeNode) this.context.getNodeManager().get(sourceNode.getNodeId());
        DateTime now = new DateTime();
        boolean retainCompleted = false;
        if (alarmConditionTypeNode.getEnabledStateNode().getFalseState().equals(aTrueState))
            return;
        if (state == true) {
            // active Alarm state is handled
            if (alarmConditionTypeNode.getRetain() == false && alarmConditionTypeNode.getActiveStateNode().getTrueState().equals(aFalseState)) {
                // a new active state is happened
                alarmConditionTypeNode.setRetain(true);
                alarmConditionTypeNode.setActiveState(aTrueState);
                alarmConditionTypeNode.getActiveStateNode().setTrueState(aTrueState);
                alarmConditionTypeNode.getActiveStateNode().setTransitionTime(now);
                alarmConditionTypeNode.setAckedState(aFalseState);
                alarmConditionTypeNode.getAckedStateNode().setTrueState(aFalseState);
                alarmConditionTypeNode.getAckedStateNode().setFalseState(aTrueState);
                alarmConditionTypeNode.getAckedStateNode().setTransitionTime(null);
                alarmConditionTypeNode.setTime(now);
                alarmConditionTypeNode.setEventId(ByteString.of(UUID.randomUUID().toString().getBytes()));
                // send now
                this.post(alarmConditionTypeNode);
            } else if (alarmConditionTypeNode.getRetain() == true && alarmConditionTypeNode.getActiveStateNode().getTrueState().equals(aFalseState)) {
                // a new active state is happened
                alarmConditionTypeNode.setActiveState(aTrueState);
                alarmConditionTypeNode.getActiveStateNode().setTrueState(aTrueState);
                alarmConditionTypeNode.getActiveStateNode().setFalseState(aFalseState);
                alarmConditionTypeNode.getActiveStateNode().setTransitionTime(now);
                alarmConditionTypeNode.setAckedState(aFalseState);
                alarmConditionTypeNode.getAckedStateNode().setTrueState(aFalseState);
                alarmConditionTypeNode.getAckedStateNode().setFalseState(aTrueState);
                alarmConditionTypeNode.getAckedStateNode().setTransitionTime(null);
                alarmConditionTypeNode.setTime(now);
                // change the eventId of current shot in AddressSpace
                alarmConditionTypeNode.setEventId(ByteString.of(UUID.randomUUID().toString().getBytes()));
                // send now
                this.post(alarmConditionTypeNode);
            }
        } else {
            // none active Alarm state is handled
            if (alarmConditionTypeNode.getRetain() == true && alarmConditionTypeNode.getActiveStateNode().getTrueState().equals(aTrueState)) {
                alarmConditionTypeNode.setActiveState(aFalseState);
                alarmConditionTypeNode.getActiveStateNode().setTrueState(aFalseState);
                alarmConditionTypeNode.getActiveStateNode().setFalseState(aTrueState);
//                alarmConditionTypeNode.getActiveStateNode().setTransitionTime(now);
                alarmConditionTypeNode.setTime(new DateTime());
                if (alarmConditionTypeNode.getAckedStateNode().getTrueState().equals(aTrueState)) {
                    alarmConditionTypeNode.setRetain(false);
                    retainCompleted = true;
                }
                // send now
                this.post(alarmConditionTypeNode);
            } else if (alarmConditionTypeNode.getRetain() == false && alarmConditionTypeNode.getActiveStateNode().getTrueState().equals(aTrueState)) {
                // a clear state is happening
                alarmConditionTypeNode.setActiveState(aFalseState);
                alarmConditionTypeNode.getActiveStateNode().setTrueState(aFalseState);
                alarmConditionTypeNode.getActiveStateNode().setFalseState(aTrueState);
                alarmConditionTypeNode.getActiveStateNode().setTransitionTime(now);
                // send now
                this.post(alarmConditionTypeNode);
            }
        }
        if (retainCompleted) {
            CompletableFuture.runAsync(() -> {
                retainCompleted(alarmConditionTypeNode);
            });
        }
    }

    public String serializedAlarmConditionTypeNodeEvent(AlarmConditionTypeNode event) {
        SerializableAlarmConditionTypeNode serializableAlarmConditionTypeNode = new SerializableAlarmConditionTypeNode(event);
        Gson gson = new Gson();
        return gson.toJson(serializableAlarmConditionTypeNode);

    }

    public NodeManager<UaNode> getNodeManager() {
        return this.context.getNodeManager();
    }

    public void updateAlarmConditionType(AlarmConditionTypeNode alarmConditionTypeNode, SerializableAlarmConditionTypeNode lastEvent) {
        LocalizedText aTrueState = new LocalizedText("true");
        LocalizedText aFalseState = new LocalizedText("false");

        boolean retain = Boolean.parseBoolean(lastEvent.getRetain());
        alarmConditionTypeNode.setRetain(retain);

        boolean active = Boolean.parseBoolean(lastEvent.getActiveState());
        if (active) {
            alarmConditionTypeNode.setActiveState(aTrueState);
            alarmConditionTypeNode.getActiveStateNode().setTrueState(aTrueState);
            alarmConditionTypeNode.getActiveStateNode().setFalseState(aFalseState);
        } else {
            alarmConditionTypeNode.setActiveState(aFalseState);
            alarmConditionTypeNode.getActiveStateNode().setTrueState(aFalseState);
            alarmConditionTypeNode.getActiveStateNode().setFalseState(aTrueState);
        }

        DateTime activeTime = new DateTime(new Date(lastEvent.getActiveTime()));
        alarmConditionTypeNode.getActiveStateNode().setTransitionTime(activeTime);

        DateTime time = new DateTime(new Date(lastEvent.getTime()));
        alarmConditionTypeNode.setTime(time);

        boolean ack = Boolean.parseBoolean(lastEvent.getAcknowledgeState());
        if (ack) {
            alarmConditionTypeNode.setAckedState(aTrueState);
            alarmConditionTypeNode.getAckedStateNode().setTrueState(aTrueState);
            alarmConditionTypeNode.getAckedStateNode().setFalseState(aFalseState);
            DateTime ackTime = new DateTime(new Date(lastEvent.getAcknowledgeTime()));
            alarmConditionTypeNode.getAckedStateNode().setTransitionTime(ackTime);
        } else {
            alarmConditionTypeNode.setAckedState(aFalseState);
            alarmConditionTypeNode.getAckedStateNode().setTrueState(aFalseState);
            alarmConditionTypeNode.getAckedStateNode().setFalseState(aTrueState);
            alarmConditionTypeNode.getAckedStateNode().setTransitionTime(null);
        }
        alarmConditionTypeNode.setEventId(ByteString.of((lastEvent.getEventId()).toString().getBytes()));
        this.context.getNodeManager().addNode(alarmConditionTypeNode);
    }


    private static class EventNodeContext implements UaNodeContext {

        private final OpcUaServer server;
        private final NodeManager<UaNode> nodeManager;


        EventNodeContext(OpcUaServer server, NodeManager<UaNode> nodeManager) {
            this.server = server;
            this.nodeManager = nodeManager;
        }

        @Override
        public OpcUaServer getServer() {
            return server;
        }

        @Override
        public NodeManager<UaNode> getNodeManager() {
            return nodeManager;
        }

    }

    private static class Utils {
        public static QualifiedName newQualifiedName(String qualifiedName) {
            return new QualifiedName(2, qualifiedName);
        }

        public static NodeId newNodeId(String identifier) {
            return new NodeId(2, identifier);
        }

        public static NodeId newNodeId(String sourceIdentifier, String identifier) {
            return new NodeId(2, sourceIdentifier + "/" + identifier);
        }

        public static String getPropertyValue(UaNode rootNode, String property) {
            try {
                return rootNode.getPropertyNode(property).get().getValue().getValue().getValue().toString();
            } catch (Exception e) {
                LoggerFactory.getLogger(Utils.class).info("the property value could not be found for : {}", rootNode.getNodeId());
                return null;
            }
        }
    }
}
