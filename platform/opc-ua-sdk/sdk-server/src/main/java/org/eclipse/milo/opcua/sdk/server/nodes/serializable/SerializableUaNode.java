package org.eclipse.milo.opcua.sdk.server.nodes.serializable;

import com.google.gson.Gson;
import org.eclipse.milo.opcua.sdk.core.nodes.Node;
import org.slf4j.LoggerFactory;

public class SerializableUaNode implements SerializableNode {
    SerializableNodeId nodeId;
    private int nodeClass;
    private String browseName;
    private String displayName;
    private String description;

    private int writeMask;
    private int userWriteMask;


    public <T extends Node> SerializableUaNode(T node) {
        try {
            this.nodeId = new SerializableNodeId(node.getNodeId());
            this.nodeClass = node.getNodeClass().getValue();
            this.browseName = node.getBrowseName().getName();
            this.displayName = node.getDisplayName().getText();
            this.description = node.getDescription().getText();
            this.writeMask = node.getWriteMask().intValue();
            this.userWriteMask = node.getUserWriteMask().intValue();
        } catch (Exception e) {
            LoggerFactory.getLogger(getClass()).error("error while trying to create a Serializable Node : {}", e.getMessage());
        }
    }

    public SerializableNodeId getNodeId() {
        return nodeId;
    }

    public void setNodeId(SerializableNodeId serializableNodeId) {
        this.nodeId = serializableNodeId;
    }

    public int getNodeClass() {
        return nodeClass;
    }

    public void setNodeClass(int nodeClass) {
        this.nodeClass = nodeClass;
    }

    public String getBrowseName() {
        return browseName;
    }

    public void setBrowseName(String browseName) {
        this.browseName = browseName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getWriteMask() {
        return writeMask;
    }

    public void setWriteMask(int writeMask) {
        this.writeMask = writeMask;
    }

    public int getUserWriteMask() {
        return userWriteMask;
    }

    public void setUserWriteMask(int userWriteMask) {
        this.userWriteMask = userWriteMask;
    }

    public String toJson() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }
}
