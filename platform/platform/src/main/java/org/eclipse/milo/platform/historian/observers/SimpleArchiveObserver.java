package org.eclipse.milo.platform.historian.observers;

import org.eclipse.milo.opcua.sdk.server.OpcUaServer;
import org.eclipse.milo.opcua.sdk.server.api.persistence.RedisCommunication;
import org.eclipse.milo.opcua.sdk.server.nodes.AttributeObserver;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNode;
import org.eclipse.milo.opcua.stack.core.AttributeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.json.simple.JSONObject;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class SimpleArchiveObserver implements AttributeObserver {
    private RedisCommunication redisCommunication;

    public SimpleArchiveObserver(OpcUaServer opcUaServer) {
        this.redisCommunication = new RedisCommunication(opcUaServer);
    }

    @Override
    public void attributeChanged(UaNode node, AttributeId attributeId, Object value) {
        if (attributeId.equals(AttributeId.Value)) {
            // archive data in long term databases : mode simple
            try {

                LoggerFactory.getLogger(getClass()).debug("value changed for node : {}, with value : {}", node.getNodeId().getIdentifier(), value);
                JSONObject jsonObject = new JSONObject();
                final String identifier = node.getNodeId().getIdentifier().toString();
                final String displayName = node.getDisplayName().getText();
                final String browseName = node.getBrowseName().getName();
                final long currentTimeMillis = System.currentTimeMillis();
                final String savedValue = ((DataValue) value).getValue().getValue().toString();
                jsonObject.put("displayName", displayName);
                jsonObject.put("browseName", browseName);
                jsonObject.put("identifier", identifier);
                jsonObject.put("value", savedValue);
                jsonObject.put("time", currentTimeMillis);
                this.redisCommunication.getClient().jsonSet("simpleArchive:" + identifier + "_" + UUID.randomUUID(), jsonObject);
            } catch (Exception e) {
                LoggerFactory.getLogger(getClass()).error("error {} SimpleArchive in value changed for node : {}, with value : {}", e.getMessage(), node.getNodeId().getIdentifier(), value);
            }
        }
    }

    public RedisCommunication getRedisCommunication() {
        return redisCommunication;
    }

    public SimpleArchiveObserver setRedisCommunication(RedisCommunication redisCommunication) {
        this.redisCommunication = redisCommunication;
        return this;
    }
}

