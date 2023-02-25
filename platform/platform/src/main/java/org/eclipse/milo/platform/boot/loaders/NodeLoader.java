package org.eclipse.milo.platform.boot.loaders;

import com.google.gson.Gson;
import org.eclipse.milo.opcua.sdk.core.AccessLevel;
import org.eclipse.milo.opcua.sdk.server.api.persistence.mapper.CacheMapper;
import org.eclipse.milo.opcua.sdk.server.nodes.UaFolderNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNodeContext;
import org.eclipse.milo.opcua.sdk.server.nodes.UaObjectNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaVariableNode;
import org.eclipse.milo.opcua.sdk.server.nodes.serializable.*;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.types.builtin.*;
import org.eclipse.milo.platform.util.Props;
import org.eclipse.milo.platform.util.Utils;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class NodeLoader {
    private final UaNodeContext uaNodeContext;
    CacheMapper cacheMapper;
    private static final int ROOT_APP_NAMESPACE_INDEX = Integer.parseInt(Props.getProperty("app-namespace-index").toString());

    private Gson gson = new Gson();

    public NodeLoader(UaNodeContext uaNodeContext) {
        this.cacheMapper = new CacheMapper();
        this.uaNodeContext = uaNodeContext;
    }

    public void load() {
        loadNodes();
    }

    private void loadNodes() {
        LoggerFactory.getLogger(getClass()).info("[BootLoader] started loading nodes ...");
        Map<SerializableNodeId, SerializableNode> nodes = uaNodeContext.getNodeManager().cacheNodeManager().getNodeFactoryCache().findNodesByNamespaceIndex(ROOT_APP_NAMESPACE_INDEX, 0, 1000);
        nodes.forEach((nodeId, node) -> {
            if (this.uaNodeContext.getNodeManager().getNode(Utils.newNodeId(nodeId.getIdentifier())).isEmpty()) {
                switch (Integer.valueOf(node.getNodeClass())) {
                    case 2: //  VariableNode
                        SerializableUaVariableNode serializableUaVariableNode = (SerializableUaVariableNode) node;
                        UaVariableNode uaVariableNode = new UaVariableNode.UaVariableNodeBuilder(uaNodeContext).setNodeId(Utils.newNodeId(nodeId.getIdentifier()))
                                .setAccessLevel(AccessLevel.fromValue(serializableUaVariableNode.getAccessLevel()))
                                .setBrowseName(new QualifiedName(nodeId.getNamespaceIndex(), serializableUaVariableNode.getBrowseName()))
                                .setDisplayName(LocalizedText.english(serializableUaVariableNode.getDisplayName()))
                                .setDataType(Identifiers.init(Integer.valueOf(serializableUaVariableNode.getDataType().getIdentifier())))
                                .setValue(DataValue.valueOnly(new Variant(serializableUaVariableNode.getValue()))).build();
                        uaNodeContext.getNodeManager().addNode(uaVariableNode);
                        break;
                    case 1: // ObjectNode or FolderNode
                        SerializableUaObjectNode serializableUaObjectNode = (SerializableUaObjectNode) node;
                        if (serializableUaObjectNode.getNodeId().getIdentifier().contains("Internal")) {
                            UaFolderNode uaObjectNode = new UaFolderNode(uaNodeContext, Utils.newNodeId(serializableUaObjectNode.getNodeId().getIdentifier()), new QualifiedName(serializableUaObjectNode.getNodeId().getNamespaceIndex(), serializableUaObjectNode.getBrowseName()), new LocalizedText(serializableUaObjectNode.getDisplayName()));
                            uaNodeContext.getNodeManager().addNode(uaObjectNode);
                        } else {
                            UaObjectNode uaObjectNode = new UaObjectNode(uaNodeContext, Utils.newNodeId(serializableUaObjectNode.getNodeId().getIdentifier()), new QualifiedName(serializableUaObjectNode.getNodeId().getNamespaceIndex(), serializableUaObjectNode.getBrowseName()), new LocalizedText(serializableUaObjectNode.getDisplayName()));
                            uaNodeContext.getNodeManager().addNode(uaObjectNode);
                        }
                        break;
                }
            }
        });
    }
}
