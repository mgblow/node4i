package org.eclipse.milo.opcua.sdk.server.nodes.serializable;

import org.eclipse.milo.opcua.sdk.core.nodes.Node;
import org.slf4j.LoggerFactory;

public class SerializableUaObjectNode extends SerializableUaNode {

    private int eventNotifier;

    public <T extends Node> SerializableUaObjectNode(T node) {
        super(node);
        try {
            org.eclipse.milo.opcua.sdk.server.nodes.UaObjectNode objectNode = (org.eclipse.milo.opcua.sdk.server.nodes.UaObjectNode) node;
            this.eventNotifier = objectNode.getEventNotifier().intValue();
        } catch (Exception e) {
            LoggerFactory.getLogger(getClass()).error("error while trying to create a Serializable Node : {}", e.getMessage());
        }
    }

    public int getEventNotifier() {
        return eventNotifier;
    }

    public void setEventNotifier(int eventNotifier) {
        this.eventNotifier = eventNotifier;
    }
}
