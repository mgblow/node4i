package org.eclipse.milo.platform.structs.server;

import org.eclipse.milo.opcua.sdk.server.nodes.serializable.SerializableNode;
import org.eclipse.milo.opcua.sdk.server.nodes.serializable.SerializableReference;

import java.util.List;
import java.util.Map;

public class DBObject {
    private List<SerializableNode> serializableNodes;
    private Map<String, SerializableReference> referenceMap;

    public DBObject(){

    }

    public DBObject(List<SerializableNode> serializableNodes, Map<String, SerializableReference> referenceMap) {
        this.serializableNodes = serializableNodes;
        this.referenceMap = referenceMap;
    }

    public List<SerializableNode> getSerializableNodes() {
        return serializableNodes;
    }

    public void setSerializableNodes(List<SerializableNode> serializableNodes) {
        this.serializableNodes = serializableNodes;
    }

    public Map<String, SerializableReference> getReferenceMap() {
        return referenceMap;
    }

    public void setReferenceMap(Map<String, SerializableReference> referenceMap) {
        this.referenceMap = referenceMap;
    }
}
