package org.eclipse.milo.platform.historian.observers;

import org.eclipse.milo.opcua.sdk.server.OpcUaServer;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNode;
import org.eclipse.milo.opcua.stack.core.AttributeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.platform.compression.SwingDoor;
import org.json.simple.JSONObject;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class CompressedSimpleArchiveObserver extends SimpleArchiveObserver {
    private SwingDoor swingDoor;

    public CompressedSimpleArchiveObserver(OpcUaServer opcUaServer, Double tolerance) {
        super(opcUaServer);
        swingDoor = new SwingDoor(tolerance);
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
                final boolean accepted = swingDoor.isAccepted(currentTimeMillis, Double.valueOf(savedValue));
                System.out.println(value + ": accepted" + accepted);
                if (accepted) {
                    jsonObject.put("displayName", displayName);
                    jsonObject.put("browseName", browseName);
                    jsonObject.put("identifier", identifier);
                    jsonObject.put("value", savedValue);
                    jsonObject.put("time", currentTimeMillis);
                    getRedisCommunication().getClient().jsonSet("simpleArchiveSwingDoor:" + identifier + "_" + UUID.randomUUID(), jsonObject);
                }
            } catch (Exception e) {
                LoggerFactory.getLogger(getClass()).error("error {} SimpleArchive in value changed for node : {}, with value : {}", e.getMessage(), node.getNodeId().getIdentifier(), value);
            }
        }
    }
}


