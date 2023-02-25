package org.eclipse.milo.platform.boot.loaders;

import org.eclipse.milo.opcua.sdk.core.Reference;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNodeContext;
import org.eclipse.milo.opcua.sdk.server.nodes.serializable.SerializableReference;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.platform.util.Props;
import org.eclipse.milo.platform.util.Utils;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class ReferenceLoader {


    private static final int ROOT_APP_NAMESPACE_INDEX = Integer.parseInt(Props.getProperty("app-namespace-index").toString());
    private final UaNodeContext uaNodeContext;

    public ReferenceLoader(UaNodeContext uaNodeContext) {
        this.uaNodeContext = uaNodeContext;
    }

    public void load() {
        loadReferences();
    }

    private void loadReferences() {
        LoggerFactory.getLogger(getClass()).info("[BootLoader] started loading references ...");
        Map<String, SerializableReference> references = uaNodeContext.getNodeManager().cacheNodeManager().getNodeFactoryCache().findReferencesByNamespaceIndex(ROOT_APP_NAMESPACE_INDEX, 0, 10000);
        references.forEach((ID, reference) -> {
            NodeId uaSourceNodeId = Utils.newNodeId(reference.getSourceNodeId().getNamespaceIndex(), reference.getSourceNodeId().getIdentifier());
            NodeId uaTargetNodeId = Utils.newNodeId(reference.getTargetNodeId().getNamespaceIndex(), reference.getTargetNodeId().getIdentifier());
            if (uaNodeContext.getNodeManager().get(uaSourceNodeId) != null) {
                UaNode uaSourceNode = this.uaNodeContext.getNodeManager().get(uaSourceNodeId);
                NodeId referenceTypeId = Identifiers.init(Integer.valueOf(reference.getReferenceTypeId().getIdentifier()));
                Reference.Direction referenceDirection = Reference.Direction.valueOf(reference.getDirection().name());
                Reference referenceToAdd = new Reference(uaSourceNodeId, referenceTypeId, uaTargetNodeId.expanded(), referenceDirection);
                if (!uaSourceNode.getReferences().contains(referenceToAdd)) {
                    uaSourceNode.addReference(referenceToAdd);
                }
            }
        });
    }
}
