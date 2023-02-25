package org.eclipse.milo.platform.nodes;

import org.eclipse.milo.opcua.sdk.server.nodes.UaNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNodeContext;
import org.eclipse.milo.opcua.sdk.server.nodes.UaVariableNode;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.enumerated.NodeClass;

import javax.xml.bind.JAXB;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExportList {

    // [SerializableNode, References[indexes]]
    private Map<SerializableNode, List<String>> nodes = new HashMap<>();
    private UaNodeContext uaNodeContext;

    public ExportList(UaNodeContext nodeContext) {
        this.uaNodeContext = nodeContext;
    }

    public void add(List<UaNode> nodes) {
        nodes.forEach(node -> {
            // return on method nodes
            if (node.getNodeClass().equals(NodeClass.Method)) return;
            // return on InputArguments or OutputArguments
            if (node.getBrowseName().getNamespaceIndex().intValue() != 2) return;
            SerializableNode serializableNode = new SerializableNode();
            serializableNode.setIdentifier(node.getNodeId().getIdentifier().toString());
            serializableNode.setIndex(Integer.parseInt(node.getNodeId().getNamespaceIndex().toString()));
            serializableNode.setNodeClass(node.getNodeClass().name());
            if (node.getNodeClass().equals(NodeClass.Variable)) {
                serializableNode.setValue(((UaVariableNode) node).getValue().getValue().toString());
            } else {
                serializableNode.setValue(null);
            }

            if (this.nodes.get(serializableNode) == null) {
                this.nodes.put(serializableNode, new ArrayList<>());
                addReferences(serializableNode);
            } else {
                addReferences(serializableNode);
            }
        });
    }

    private void addReferences(SerializableNode node) {
        this.uaNodeContext.getNodeManager().get(new NodeId(node.getIndex(), node.identifier)).getReferences().forEach(reference -> {
            if (reference.isForward()) {
                this.nodes.get(node).add(reference.getTargetNodeId().getIdentifier().toString());
            }
        });
    }

    public String getBytes() {
        StringWriter stringWriter = new StringWriter();
        JAXB.marshal(this.nodes,stringWriter);
        return stringWriter.toString();
    }

    class SerializableNode implements Serializable {
        private String identifier;
        private int index;
        private String nodeClass;
        private String value;

        public String getIdentifier() {
            return identifier;
        }

        public void setIdentifier(String identifier) {
            this.identifier = identifier;
        }

        public int getIndex() {
            return index;
        }

        public void setIndex(int index) {
            this.index = index;
        }

        public String getNodeClass() {
            return nodeClass;
        }

        public void setNodeClass(String nodeClass) {
            this.nodeClass = nodeClass;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }
}
