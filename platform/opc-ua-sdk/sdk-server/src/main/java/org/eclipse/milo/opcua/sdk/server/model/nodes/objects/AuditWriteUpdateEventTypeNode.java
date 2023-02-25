/*
 * Copyright (c) 2019 the Eclipse Milo Authors
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.milo.opcua.sdk.server.model.nodes.objects;

import java.util.Optional;

import org.eclipse.milo.opcua.sdk.core.nodes.VariableNode;
import org.eclipse.milo.opcua.sdk.server.model.nodes.variables.PropertyTypeNode;
import org.eclipse.milo.opcua.sdk.server.model.types.objects.AuditWriteUpdateEventType;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNodeContext;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UByte;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;

public class AuditWriteUpdateEventTypeNode extends AuditUpdateEventTypeNode implements AuditWriteUpdateEventType {
    public AuditWriteUpdateEventTypeNode(UaNodeContext context, NodeId nodeId,
                                         QualifiedName browseName, LocalizedText displayName, LocalizedText description,
                                         UInteger writeMask, UInteger userWriteMask) {
        super(context, nodeId, browseName, displayName, description, writeMask, userWriteMask);
    }

    public AuditWriteUpdateEventTypeNode(UaNodeContext context, NodeId nodeId,
                                         QualifiedName browseName, LocalizedText displayName, LocalizedText description,
                                         UInteger writeMask, UInteger userWriteMask, UByte eventNotifier) {
        super(context, nodeId, browseName, displayName, description, writeMask, userWriteMask, eventNotifier);
    }

    @Override
    public PropertyTypeNode getAttributeIdNode() {
        Optional<VariableNode> propertyNode = getPropertyNode(AuditWriteUpdateEventType.ATTRIBUTE_ID);
        return (PropertyTypeNode) propertyNode.orElse(null);
    }

    @Override
    public UInteger getAttributeId() {
        Optional<UInteger> propertyValue = getProperty(AuditWriteUpdateEventType.ATTRIBUTE_ID);
        return propertyValue.orElse(null);
    }

    @Override
    public void setAttributeId(UInteger value) {
        setProperty(AuditWriteUpdateEventType.ATTRIBUTE_ID, value);
    }

    @Override
    public PropertyTypeNode getIndexRangeNode() {
        Optional<VariableNode> propertyNode = getPropertyNode(AuditWriteUpdateEventType.INDEX_RANGE);
        return (PropertyTypeNode) propertyNode.orElse(null);
    }

    @Override
    public String getIndexRange() {
        Optional<String> propertyValue = getProperty(AuditWriteUpdateEventType.INDEX_RANGE);
        return propertyValue.orElse(null);
    }

    @Override
    public void setIndexRange(String value) {
        setProperty(AuditWriteUpdateEventType.INDEX_RANGE, value);
    }

    @Override
    public PropertyTypeNode getOldValueNode() {
        Optional<VariableNode> propertyNode = getPropertyNode(AuditWriteUpdateEventType.OLD_VALUE);
        return (PropertyTypeNode) propertyNode.orElse(null);
    }

    @Override
    public Object getOldValue() {
        Optional<Object> propertyValue = getProperty(AuditWriteUpdateEventType.OLD_VALUE);
        return propertyValue.orElse(null);
    }

    @Override
    public void setOldValue(Object value) {
        setProperty(AuditWriteUpdateEventType.OLD_VALUE, value);
    }

    @Override
    public PropertyTypeNode getNewValueNode() {
        Optional<VariableNode> propertyNode = getPropertyNode(AuditWriteUpdateEventType.NEW_VALUE);
        return (PropertyTypeNode) propertyNode.orElse(null);
    }

    @Override
    public Object getNewValue() {
        Optional<Object> propertyValue = getProperty(AuditWriteUpdateEventType.NEW_VALUE);
        return propertyValue.orElse(null);
    }

    @Override
    public void setNewValue(Object value) {
        setProperty(AuditWriteUpdateEventType.NEW_VALUE, value);
    }
}
