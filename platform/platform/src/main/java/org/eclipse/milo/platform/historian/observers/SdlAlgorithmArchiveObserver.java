package org.eclipse.milo.platform.historian.observers;

import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.eclipse.milo.opcua.sdk.server.nodes.AttributeObserver;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNode;
import org.eclipse.milo.opcua.stack.core.AttributeId;
import org.slf4j.LoggerFactory;

public class SdlAlgorithmArchiveObserver implements AttributeObserver {

    private MongoCollection mongoCollection;

    public SdlAlgorithmArchiveObserver(MongoCollection mongoCollection) {
        this.mongoCollection = mongoCollection;
    }

    @Override
    public void attributeChanged(UaNode node, AttributeId attributeId, Object value) {
        if (attributeId.equals(AttributeId.Value)) {
            // archive data in long term databases : mode simple
            LoggerFactory.getLogger(getClass()).debug("value changed for node : {}, with value : {}", node.getNodeId().getIdentifier(), value);
        }
    }
}
