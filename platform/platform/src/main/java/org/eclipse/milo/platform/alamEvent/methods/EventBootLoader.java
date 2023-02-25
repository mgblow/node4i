package org.eclipse.milo.platform.alamEvent.methods;

import org.eclipse.milo.opcua.sdk.core.ValueRanks;
import org.eclipse.milo.opcua.sdk.server.api.methods.AbstractMethodInvocationHandler;
import org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaVariableNode;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.structured.Argument;
import org.eclipse.milo.platform.alamEvent.AlarmEvent;
import org.eclipse.milo.platform.util.Props;
import org.eclipse.milo.platform.util.ScriptEngineUtil;
import org.eclipse.milo.platform.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

public class EventBootLoader extends AbstractMethodInvocationHandler {

    public static final Argument result = new Argument("result", Identifiers.String, ValueRanks.Any, null, new LocalizedText("result"));

    public static String APP_NAME = null;
    public static final int APP_NAMESPACE_INDEX = Integer.parseInt(Props.getProperty("app-namespace-index").toString());
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private UaMethodNode uaMethodNode;
    private AlarmEvent alarmEvent;

    public EventBootLoader(UaMethodNode uaMethodNode, AlarmEvent alarmEvent) {
        super(uaMethodNode);
        APP_NAME = uaMethodNode.getNodeContext().getServer().getConfig().getApplicationName().getText();
        this.uaMethodNode = uaMethodNode;
        this.alarmEvent = alarmEvent;
    }

    @Override
    public Argument[] getInputArguments() {
        return new Argument[]{};
    }

    @Override
    public Argument[] getOutputArguments() {
        return new Argument[]{result};
    }

    @Override
    protected Variant[] invoke(InvocationContext invocationContext, Variant[] inputValues) throws UaException {
        logger.debug("Invoking program() method of objectId={}", invocationContext.getObjectId());
        try {
            this.loadEvent();
            return new Variant[]{new Variant(true)};
        } catch (Exception e) {
            logger.error("Error invoking EventAcknowledgeMethod method of objectId={} with message {}", invocationContext.getObjectId(), e.getMessage());
            return new Variant[]{new Variant(false)};
        }
    }

    private void loadEvent() {
        LoggerFactory.getLogger(getClass()).info("[BootLoader] started loading events ...");
        // get Alarm&Events Folder
        UaNode alarmEventsFolder = uaMethodNode.getNodeContext().getNodeManager().get(new NodeId(APP_NAMESPACE_INDEX, APP_NAME + "/Alarm&Events"));
        alarmEventsFolder.getReferences().stream().forEach(reference -> {
            if (reference.getReferenceTypeId().equals(Identifiers.HasEventSource)) {
                UaNode event = uaMethodNode.getNodeContext().getNodeManager().get(new NodeId(reference.getTargetNodeId().getNamespaceIndex(), reference.getTargetNodeId().getIdentifier().toString()));
                try {
                    LoggerFactory.getLogger(getClass()).error("started loading  event source for node : {}", event.getNodeId());
                    uaMethodNode.getNodeContext().getServer().getEventFactory().createAlarmConditionTypeEvent(event, false);
                } catch (UaException e) {
                    LoggerFactory.getLogger(getClass()).error("error adding event source for node : {}", event.getNodeId());
                }
                String condition = new ScriptEngineUtil(ScriptEngineUtil.Engine.JavaScript).constructAlarmEventsScript(event);
                String triggerNodes = new String(Utils.getPropertyValue(event, "triggerNodes").getBytes(), StandardCharsets.UTF_8);
                addTriggerNodes(Utils.fromJsonToArrayOfString(triggerNodes), condition, event);
            }
        });
    }

    private void addTriggerNodes(String[] triggerNodes, String script, UaNode eventNode) {
        if (triggerNodes.length == 0) {
            UaNode node = uaMethodNode.getNodeContext().getNodeManager().get(Utils.newNodeId("TIME"));
            this.alarmEvent.addEventToNode(node, eventNode, script);
        }
        for (String identifier : triggerNodes) {
            UaVariableNode node = (UaVariableNode) this.uaMethodNode.getNodeManager().get(new NodeId(APP_NAMESPACE_INDEX, identifier));
            this.alarmEvent.addEventToNode(node, eventNode, script);
        }
    }
}
