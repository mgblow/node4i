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
import org.eclipse.milo.opcua.sdk.server.model.types.objects.AuditUpdateStateEventType;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNodeContext;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UByte;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;

public class AuditUpdateStateEventTypeNode extends AuditUpdateMethodEventTypeNode implements AuditUpdateStateEventType {
    public AuditUpdateStateEventTypeNode(UaNodeContext context, NodeId nodeId,
                                         QualifiedName browseName, LocalizedText displayName, LocalizedText description,
                                         UInteger writeMask, UInteger userWriteMask) {
        super(context, nodeId, browseName, displayName, description, writeMask, userWriteMask);
    }

    public AuditUpdateStateEventTypeNode(UaNodeContext context, NodeId nodeId,
                                         QualifiedName browseName, LocalizedText displayName, LocalizedText description,
                                         UInteger writeMask, UInteger userWriteMask, UByte eventNotifier) {
        super(context, nodeId, browseName, displayName, description, writeMask, userWriteMask, eventNotifier);
    }

    @Override
    public PropertyTypeNode getOldStateIdNode() {
        Optional<VariableNode> propertyNode = getPropertyNode(AuditUpdateStateEventType.OLD_STATE_ID);
        return (PropertyTypeNode) propertyNode.orElse(null);
    }

    @Override
    public Object getOldStateId() {
        Optional<Object> propertyValue = getProperty(AuditUpdateStateEventType.OLD_STATE_ID);
        return propertyValue.orElse(null);
    }

    @Override
    public void setOldStateId(Object value) {
        setProperty(AuditUpdateStateEventType.OLD_STATE_ID, value);
    }

    @Override
    public PropertyTypeNode getNewStateIdNode() {
        Optional<VariableNode> propertyNode = getPropertyNode(AuditUpdateStateEventType.NEW_STATE_ID);
        return (PropertyTypeNode) propertyNode.orElse(null);
    }

    @Override
    public Object getNewStateId() {
        Optional<Object> propertyValue = getProperty(AuditUpdateStateEventType.NEW_STATE_ID);
        return propertyValue.orElse(null);
    }

    @Override
    public void setNewStateId(Object value) {
        setProperty(AuditUpdateStateEventType.NEW_STATE_ID, value);
    }
}
