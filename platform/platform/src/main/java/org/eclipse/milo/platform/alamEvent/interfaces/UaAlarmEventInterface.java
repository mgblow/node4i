package org.eclipse.milo.platform.alamEvent.interfaces;

import org.eclipse.milo.opcua.sdk.core.AccessLevel;
import org.eclipse.milo.opcua.sdk.server.model.nodes.objects.BaseEventTypeNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaFolderNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNodeContext;
import org.eclipse.milo.opcua.sdk.server.nodes.UaVariableNode;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.*;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UShort;
import org.eclipse.milo.platform.util.Props;
import org.eclipse.milo.platform.util.Utils;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class UaAlarmEventInterface {
    UaNodeContext uaNodeContext;


    private static UaAlarmEventInterface uaAlarmEventInterface;

    public static UaAlarmEventInterface getInstance(UaNodeContext uaNodeContext) {
        if(uaAlarmEventInterface == null) {
            uaAlarmEventInterface = new UaAlarmEventInterface(uaNodeContext);
        }
        return uaAlarmEventInterface;
    }
    private UaAlarmEventInterface(UaNodeContext uaNodeContext) {
        this.uaNodeContext = uaNodeContext;
    }
    public synchronized void fireEvent(String location , String name , String message, String severity) throws UaException {
        fireEvent(new String[] {location,name},message , severity);
    }

    public synchronized void fireEvent(String[] self, String message, String severity) throws UaException {
        String LOCATION = self[0];
        String NAME = self[1];
        // send an event
        BaseEventTypeNode event = this.uaNodeContext.getServer().getEventFactory().createEvent(new NodeId(Integer.parseInt(Props.getProperty("app-namespace-index").toString()), NAME + UUID.randomUUID() /// :::: must be unique
        ), Identifiers.BaseEventType);
        event.setMessage(new LocalizedText(message));
        event.setTime(new DateTime());
        event.setReceiveTime(new DateTime());
        event.setSeverity(UShort.valueOf(severity));
        event.setEventType(Identifiers.BaseEventType);
        event.setSourceName(NAME);
        event.setSourceNode(new NodeId(Integer.parseInt(Props.getProperty("app-namespace-index").toString()), LOCATION));
        this.uaNodeContext.getServer().getEventBus().post(event);
        uaNodeContext.getNodeManager().removeNode(event);
    }
    public synchronized void alert(String identifier, boolean state) throws Exception {
        UaNode sourceNode = this.uaNodeContext.getNodeManager().get(Utils.newNodeId(identifier));
        if (sourceNode != null) {
            this.uaNodeContext.getServer().getEventFactory().process(sourceNode, state);
        } else {
            LoggerFactory.getLogger(getClass()).error("Unable to fetch sourceNode : {} with state: {} to generate event.", identifier, state);
        }
    }

    public synchronized String getNodeValue(String identifier) throws Exception {
            UaVariableNode variableNode = (UaVariableNode) uaNodeContext.getNodeManager().get(new NodeId(Integer.parseInt(Props.getProperty("app-namespace-index").toString()), identifier));
            if (variableNode != null)
                return variableNode.getValue().getValue().getValue().toString();
             throw new Exception("identifier not exist");
    }

    public synchronized Map<String, String> getNodeValues(String[] identifiers) {
        try {
            Map<String, String> values = new HashMap<>();
            for (String identifier : identifiers) {
                UaVariableNode variableNode = (UaVariableNode) uaNodeContext.getNodeManager().get(new NodeId(Integer.parseInt(Props.getProperty("app-namespace-index").toString()), identifier));
                if (variableNode != null) {
                    values.put(identifier, variableNode.getValue().getValue().getValue().toString());
                } else {
                    values.put(identifier, null);
                }
            }
            return values;
        } catch (Exception e) {
            LoggerFactory.getLogger(getClass()).error("could not read node value with identifier : {} causing error : {}", identifiers, e.getMessage());
        }

        return null;
    }

    public synchronized void saveNode(String location, String name, String value) throws Exception {
        try {
            UaFolderNode locationFolder = (UaFolderNode) uaNodeContext.getNodeManager().get(new NodeId(Integer.parseInt(Props.getProperty("app-namespace-index").toString()), location));
            if (locationFolder == null) {
                throw new Exception("error creating a node with name : " + name);
            }
            DataValue nodeValue = new DataValue(new Variant(value), StatusCode.GOOD);
            UaVariableNode uaNode = (UaVariableNode) uaNodeContext.getNodeManager().get(new NodeId(Integer.parseInt(Props.getProperty("app-namespace-index").toString()), Props.getProperty("app-name").toString() + "/" + name));
            if (uaNode == null) {
                UaVariableNode uaVariableNode = new UaVariableNode.UaVariableNodeBuilder(this.uaNodeContext).setNodeId(new NodeId(Integer.parseInt(Props.getProperty("app-namespace-index").toString()), Props.getProperty("app-name").toString() + "/" + name)).setAccessLevel(AccessLevel.READ_WRITE).setBrowseName(new QualifiedName(Integer.parseInt(Props.getProperty("app-namespace-index").toString()), Props.getProperty("app-name").toString() + "/" + name)).setDisplayName(LocalizedText.english(name)).setDataType(Identifiers.String).setTypeDefinition(Identifiers.VariableNode).build();
                uaVariableNode.setValue(nodeValue);
                uaNodeContext.getNodeManager().addNode(uaVariableNode);
                locationFolder.addOrganizes(uaVariableNode);
            } else {
                uaNode.setValue(nodeValue);
            }
        } catch (Exception e) {
            LoggerFactory.getLogger(getClass()).error("could not save node with name : {} causing error : {} with value : {}", name, e.getMessage(), value);
        }
    }

}
