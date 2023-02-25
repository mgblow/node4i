package org.eclipse.milo.platform.boot.loaders;

import org.eclipse.milo.opcua.sdk.server.api.persistence.RedisCommunication;
import org.eclipse.milo.opcua.sdk.server.api.persistence.caches.NodeFactoryCache;
import org.eclipse.milo.opcua.sdk.server.model.nodes.objects.AlarmConditionTypeNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNodeContext;
import org.eclipse.milo.opcua.sdk.server.nodes.UaVariableNode;
import org.eclipse.milo.opcua.sdk.server.nodes.serializable.SerializableAlarmConditionTypeNode;
import org.eclipse.milo.opcua.sdk.server.nodes.serializable.SerializableUaNode;
import org.eclipse.milo.opcua.sdk.server.nodes.serializable.SerializableUaVariableNode;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.platform.alamEvent.AlarmEvent;
import org.eclipse.milo.platform.util.Props;
import org.eclipse.milo.platform.util.ScriptEngineUtil;
import org.eclipse.milo.platform.util.Utils;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class EventLoader {

    public static String ROOT_APP_NAME = null;

    public static final int APP_NAMESPACE_INDEX = Integer.parseInt(Props.getProperty("app-namespace-index").toString());
    private final UaNodeContext uaNodeContext;
    private final AlarmEvent alarmEvent;
    NodeFactoryCache nodeFactoryCache;


    public EventLoader(UaNodeContext uaNodeContext, AlarmEvent alarmEvent) {
        ROOT_APP_NAME = uaNodeContext.getServer().getConfig().getApplicationName().getText();
        this.uaNodeContext = uaNodeContext;
        this.alarmEvent = alarmEvent;
        nodeFactoryCache = new NodeFactoryCache(new RedisCommunication(uaNodeContext.getServer()));
    }

    public void load() {
        List<SerializableUaNode> events = this.uaNodeContext.getNodeManager().cacheNodeManager().getNodeFactoryCache().findAll("Alarm");
        if (events != null && events.size() > 0)
            loadEvents(events);
    }

    public void load(String... eventIds) {
        List<String> idList = Arrays.asList(eventIds);
        Map<String, SerializableUaNode> eventMap = this.uaNodeContext.getNodeManager().cacheNodeManager().getNodeFactoryCache().findSerializeUaNodes(idList, APP_NAMESPACE_INDEX);
        if (eventMap != null && eventMap.size() > 0) {
            List<SerializableUaNode> eventList = new ArrayList<SerializableUaNode>(eventMap.values());
            loadEvents(eventList);
        }
    }

    private String getSerializeMapValue(Map<String, SerializableUaNode> map, String identifier) {
        SerializableUaNode script = map.get("UaNode:" + identifier);
        return ((SerializableUaVariableNode) script).getValue();
    }

    private void loadEvents(List<SerializableUaNode> events) {
        LoggerFactory.getLogger(getClass()).info("[EventLoader] started loading events ...");
        List<String> eventIdentifiers = events.stream().map(i -> i.getNodeId().getIdentifier()).collect(Collectors.toList());
        List<String> conditionIdentifiers = eventIdentifiers.stream().map(i -> i + "/Property/condition").collect(Collectors.toList());
        List<String> triggerNodeIdentifiers = eventIdentifiers.stream().map(i -> i + "/Property/triggerNodes").collect(Collectors.toList());
        Map<String, SerializableUaNode> scriptMap = this.uaNodeContext.getNodeManager().cacheNodeManager().getNodeFactoryCache().findSerializeUaNodes(conditionIdentifiers, APP_NAMESPACE_INDEX);
        Map<String, SerializableUaNode> triggersMap = this.uaNodeContext.getNodeManager().cacheNodeManager().getNodeFactoryCache().findSerializeUaNodes(triggerNodeIdentifiers, APP_NAMESPACE_INDEX);
        events.forEach(e -> {
            String conditionValue = getSerializeMapValue(scriptMap, e.getNodeId().getIdentifier() + "/Property/condition");
            String triggerValue = getSerializeMapValue(triggersMap, e.getNodeId().getIdentifier() + "/Property/triggerNodes");
            UaNode event = uaNodeContext.getNodeManager().get(new NodeId(APP_NAMESPACE_INDEX, e.getNodeId().getIdentifier()));
            conditionValue = new ScriptEngineUtil(ScriptEngineUtil.Engine.JavaScript).constructAlarmEventsScript(event);
            try {
                SerializableAlarmConditionTypeNode lastEvent = uaNodeContext.getServer().getEventFactory().findNewestBySourceNode(event, false);
                AlarmConditionTypeNode alarmConditionTypeNode = uaNodeContext.getServer().getEventFactory().createAlarmConditionTypeEvent(event, false);
                if (lastEvent != null) {
                   uaNodeContext.getServer().getEventFactory().updateAlarmConditionType(alarmConditionTypeNode,lastEvent);
                }
            } catch (Exception exception) {
                LoggerFactory.getLogger(getClass()).error("error adding event source for node : {}", event.getNodeId().getIdentifier());
            }
            addAlarmEventsTriggerNodes(Utils.fromJsonToArrayOfString(triggerValue), conditionValue, event);
        });
    }

    private void addAlarmEventsTriggerNodes(String[] triggerNodes, String script, UaNode eventNode) {
        if (triggerNodes.length == 0) {
            UaNode node = uaNodeContext.getNodeManager().get(Utils.newNodeId("TIME"));
            alarmEvent.addEventToNode(node, eventNode, script);
        }
        for (String identifier : triggerNodes) {
            UaVariableNode node = (UaVariableNode) uaNodeContext.getNodeManager().get(new NodeId(APP_NAMESPACE_INDEX, identifier));
            alarmEvent.addEventToNode(node, eventNode, script);
        }
    }

}
