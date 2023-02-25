package org.eclipse.milo.opcua.sdk.server.nodes.serializable;

import com.google.gson.Gson;

import java.io.Serializable;

public interface SerializableNode extends Serializable {
    SerializableNodeId getNodeId();

    void setNodeId(SerializableNodeId serializableNodeId);

    int getNodeClass();

    void setNodeClass(int nodeClass);

    String getBrowseName();

    void setBrowseName(String browseName);

    String getDisplayName();

    void setDisplayName(String displayName);

    String getDescription();

    void setDescription(String description);

    int getWriteMask();

    void setWriteMask(int writeMask);

    int getUserWriteMask();

    void setUserWriteMask(int userWriteMask);

    String toJson();
}
