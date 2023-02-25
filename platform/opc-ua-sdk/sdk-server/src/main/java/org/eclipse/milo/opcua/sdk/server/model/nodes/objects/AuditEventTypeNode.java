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
import org.eclipse.milo.opcua.sdk.server.model.types.objects.AuditEventType;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNodeContext;
import org.eclipse.milo.opcua.stack.core.types.builtin.DateTime;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UByte;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;

public class AuditEventTypeNode extends BaseEventTypeNode implements AuditEventType {
    public AuditEventTypeNode(UaNodeContext context, NodeId nodeId, QualifiedName browseName,
                              LocalizedText displayName, LocalizedText description, UInteger writeMask,
                              UInteger userWriteMask) {
        super(context, nodeId, browseName, displayName, description, writeMask, userWriteMask);
    }

    public AuditEventTypeNode(UaNodeContext context, NodeId nodeId, QualifiedName browseName,
                              LocalizedText displayName, LocalizedText description, UInteger writeMask,
                              UInteger userWriteMask, UByte eventNotifier) {
        super(context, nodeId, browseName, displayName, description, writeMask, userWriteMask, eventNotifier);
    }

    @Override
    public PropertyTypeNode getActionTimeStampNode() {
        Optional<VariableNode> propertyNode = getPropertyNode(AuditEventType.ACTION_TIME_STAMP);
        return (PropertyTypeNode) propertyNode.orElse(null);
    }

    @Override
    public DateTime getActionTimeStamp() {
        Optional<DateTime> propertyValue = getProperty(AuditEventType.ACTION_TIME_STAMP);
        return propertyValue.orElse(null);
    }

    @Override
    public void setActionTimeStamp(DateTime value) {
        setProperty(AuditEventType.ACTION_TIME_STAMP, value);
    }

    @Override
    public PropertyTypeNode getStatusNode() {
        Optional<VariableNode> propertyNode = getPropertyNode(AuditEventType.STATUS);
        return (PropertyTypeNode) propertyNode.orElse(null);
    }

    @Override
    public Boolean getStatus() {
        Optional<Boolean> propertyValue = getProperty(AuditEventType.STATUS);
        return propertyValue.orElse(null);
    }

    @Override
    public void setStatus(Boolean value) {
        setProperty(AuditEventType.STATUS, value);
    }

    @Override
    public PropertyTypeNode getServerIdNode() {
        Optional<VariableNode> propertyNode = getPropertyNode(AuditEventType.SERVER_ID);
        return (PropertyTypeNode) propertyNode.orElse(null);
    }

    @Override
    public String getServerId() {
        Optional<String> propertyValue = getProperty(AuditEventType.SERVER_ID);
        return propertyValue.orElse(null);
    }

    @Override
    public void setServerId(String value) {
        setProperty(AuditEventType.SERVER_ID, value);
    }

    @Override
    public PropertyTypeNode getClientAuditEntryIdNode() {
        Optional<VariableNode> propertyNode = getPropertyNode(AuditEventType.CLIENT_AUDIT_ENTRY_ID);
        return (PropertyTypeNode) propertyNode.orElse(null);
    }

    @Override
    public String getClientAuditEntryId() {
        Optional<String> propertyValue = getProperty(AuditEventType.CLIENT_AUDIT_ENTRY_ID);
        return propertyValue.orElse(null);
    }

    @Override
    public void setClientAuditEntryId(String value) {
        setProperty(AuditEventType.CLIENT_AUDIT_ENTRY_ID, value);
    }

    @Override
    public PropertyTypeNode getClientUserIdNode() {
        Optional<VariableNode> propertyNode = getPropertyNode(AuditEventType.CLIENT_USER_ID);
        return (PropertyTypeNode) propertyNode.orElse(null);
    }

    @Override
    public String getClientUserId() {
        Optional<String> propertyValue = getProperty(AuditEventType.CLIENT_USER_ID);
        return propertyValue.orElse(null);
    }

    @Override
    public void setClientUserId(String value) {
        setProperty(AuditEventType.CLIENT_USER_ID, value);
    }
}
