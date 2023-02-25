package org.eclipse.milo.opcua.sdk.server.nodes.serializable;

import com.google.gson.Gson;
import org.eclipse.milo.opcua.sdk.core.Reference;

import java.io.Serializable;
import java.util.UUID;

public class SerializableReference implements Serializable {
    public enum Direction {
        FORWARD, INVERSE
    }

    private SerializableNodeId sourceNodeId;
    private SerializableNodeId referenceTypeId;
    private SerializableNodeId targetNodeId;
    private Direction direction;

    public SerializableReference(org.eclipse.milo.opcua.sdk.core.Reference reference) {
        this.sourceNodeId = new SerializableNodeId(reference.getSourceNodeId());
        this.referenceTypeId = new SerializableNodeId(reference.getReferenceTypeId());
        this.targetNodeId = new SerializableNodeId(new org.eclipse.milo.opcua.stack.core.types.builtin.NodeId(reference.getTargetNodeId().getNamespaceIndex().intValue(), reference.getTargetNodeId().getIdentifier().toString()));
        this.direction = Direction.valueOf(reference.getDirection().name());
    }

    public SerializableNodeId getSourceNodeId() {
        return sourceNodeId;
    }

    public void setSourceNodeId(SerializableNodeId sourceSerializableNodeId) {
        this.sourceNodeId = sourceSerializableNodeId;
    }

    public SerializableNodeId getReferenceTypeId() {
        return referenceTypeId;
    }

    public void setReferenceTypeId(SerializableNodeId referenceTypeId) {
        this.referenceTypeId = referenceTypeId;
    }

    public SerializableNodeId getTargetNodeId() {
        return targetNodeId;
    }

    public void setTargetNodeId(SerializableNodeId targetSerializableNodeId) {
        this.targetNodeId = targetSerializableNodeId;
    }

    public Direction getDirection() {
        return direction;
    }

    public void setDirection(Direction direction) {
        this.direction = direction;
    }

    public String toJson() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }

    public String getUniqueIdentifier() {
        String identifier = this.getSourceNodeId().getUniqueIdentifier() + "->" + this.getTargetNodeId().getUniqueIdentifier() + "]:" + this.getReferenceTypeId().getUniqueIdentifier();
        return UUID.nameUUIDFromBytes(identifier.getBytes()).toString();
    }

    public boolean equals(Reference reference) {
        if (
                (
                        this.getSourceNodeId().getIdentifier().equals(reference.getSourceNodeId().getIdentifier().toString()) &&
                                this.getSourceNodeId().getNamespaceIndex() == reference.getSourceNodeId().getNamespaceIndex().intValue()
                ) &&
                        (
                                this.getTargetNodeId().getIdentifier().equals(reference.getTargetNodeId().getIdentifier().toString()) &&
                                        this.getTargetNodeId().getNamespaceIndex() == reference.getTargetNodeId().getNamespaceIndex().intValue()
                        ) &&
                        (
                                this.getDirection().name().equals(reference.getDirection().name())
                        ) &&
                        (
                                this.getReferenceTypeId().getIdentifier().equals(reference.getReferenceTypeId().getIdentifier().toString()) &&
                                        this.getReferenceTypeId().getNamespaceIndex() == reference.getReferenceTypeId().getNamespaceIndex().intValue()
                        )
        ) {
            return true;
        }

        return false;
    }
}
