package org.eclipse.milo.opcua.sdk.server.nodes.serializable;

import org.eclipse.milo.opcua.sdk.core.nodes.Node;
import org.eclipse.milo.opcua.sdk.server.nodes.UaVariableNode;
import org.slf4j.LoggerFactory;

public class SerializableUaVariableNode extends SerializableUaNode {
    private String value;
    private SerializableNodeId dataType;
    private int valueRank;
    private int accessLevel;
    private int userAccessLevel;
    private boolean historizing;

    public <T extends Node> SerializableUaVariableNode(T node) {
        super(node);
        UaVariableNode variableNode = (UaVariableNode) node;
        try {
            this.value = (variableNode.getValue().getValue().isNull()) ? null : variableNode.getValue().getValue().getValue().toString();
            this.dataType = new SerializableNodeId(variableNode.getDataType());
            this.valueRank = variableNode.getValueRank().intValue();
            this.accessLevel = variableNode.getAccessLevel().intValue();
            this.userAccessLevel = variableNode.getUserAccessLevel().intValue();
            this.historizing = variableNode.getHistorizing();
        } catch (Exception e) {
            LoggerFactory.getLogger(getClass()).error("error while trying to create a Serializable Node : {}", e.getMessage());
        }
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public SerializableNodeId getDataType() {
        return dataType;
    }

    public void setDataType(SerializableNodeId dataType) {
        this.dataType = dataType;
    }

    public int getValueRank() {
        return valueRank;
    }

    public void setValueRank(int valueRank) {
        this.valueRank = valueRank;
    }

    public int getAccessLevel() {
        return accessLevel;
    }

    public void setAccessLevel(int accessLevel) {
        this.accessLevel = accessLevel;
    }

    public int getUserAccessLevel() {
        return userAccessLevel;
    }

    public void setUserAccessLevel(int userAccessLevel) {
        this.userAccessLevel = userAccessLevel;
    }

    public boolean isHistorizing() {
        return historizing;
    }

    public void setHistorizing(boolean historizing) {
        this.historizing = historizing;
    }
}
