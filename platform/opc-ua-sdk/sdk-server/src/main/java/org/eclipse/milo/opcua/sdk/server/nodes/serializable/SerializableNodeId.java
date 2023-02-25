package org.eclipse.milo.opcua.sdk.server.nodes.serializable;

import com.google.gson.Gson;

import java.io.Serializable;
import java.util.UUID;

public class SerializableNodeId implements Serializable {
    private int namespaceIndex;
    private String identifier;

    public SerializableNodeId(org.eclipse.milo.opcua.stack.core.types.builtin.NodeId nodeId) {
        this.namespaceIndex = nodeId.getNamespaceIndex().intValue();
        this.identifier = nodeId.getIdentifier().toString();
    }

    public int getNamespaceIndex() {
        return namespaceIndex;
    }

    public void setNamespaceIndex(int namespaceIndex) {
        this.namespaceIndex = namespaceIndex;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String toJson() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }

    public String getUniqueIdentifier() {
        String identifier = "#" + this.getNamespaceIndex() + "@" + this.getIdentifier();
        return UUID.nameUUIDFromBytes(identifier.getBytes()).toString();
    }
}
