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

import org.eclipse.milo.opcua.sdk.core.nodes.ObjectNode;
import org.eclipse.milo.opcua.sdk.core.nodes.VariableNode;
import org.eclipse.milo.opcua.sdk.server.model.nodes.variables.PropertyTypeNode;
import org.eclipse.milo.opcua.sdk.server.model.types.objects.ServerCapabilitiesType;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNodeContext;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UByte;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UShort;
import org.eclipse.milo.opcua.stack.core.types.structured.SignedSoftwareCertificate;

public class ServerCapabilitiesTypeNode extends BaseObjectTypeNode implements ServerCapabilitiesType {
    public ServerCapabilitiesTypeNode(UaNodeContext context, NodeId nodeId, QualifiedName browseName,
                                      LocalizedText displayName, LocalizedText description, UInteger writeMask,
                                      UInteger userWriteMask) {
        super(context, nodeId, browseName, displayName, description, writeMask, userWriteMask);
    }

    public ServerCapabilitiesTypeNode(UaNodeContext context, NodeId nodeId, QualifiedName browseName,
                                      LocalizedText displayName, LocalizedText description, UInteger writeMask,
                                      UInteger userWriteMask, UByte eventNotifier) {
        super(context, nodeId, browseName, displayName, description, writeMask, userWriteMask, eventNotifier);
    }

    @Override
    public PropertyTypeNode getServerProfileArrayNode() {
        Optional<VariableNode> propertyNode = getPropertyNode(ServerCapabilitiesType.SERVER_PROFILE_ARRAY);
        return (PropertyTypeNode) propertyNode.orElse(null);
    }

    @Override
    public String[] getServerProfileArray() {
        Optional<String[]> propertyValue = getProperty(ServerCapabilitiesType.SERVER_PROFILE_ARRAY);
        return propertyValue.orElse(null);
    }

    @Override
    public void setServerProfileArray(String[] value) {
        setProperty(ServerCapabilitiesType.SERVER_PROFILE_ARRAY, value);
    }

    @Override
    public PropertyTypeNode getLocaleIdArrayNode() {
        Optional<VariableNode> propertyNode = getPropertyNode(ServerCapabilitiesType.LOCALE_ID_ARRAY);
        return (PropertyTypeNode) propertyNode.orElse(null);
    }

    @Override
    public String[] getLocaleIdArray() {
        Optional<String[]> propertyValue = getProperty(ServerCapabilitiesType.LOCALE_ID_ARRAY);
        return propertyValue.orElse(null);
    }

    @Override
    public void setLocaleIdArray(String[] value) {
        setProperty(ServerCapabilitiesType.LOCALE_ID_ARRAY, value);
    }

    @Override
    public PropertyTypeNode getMinSupportedSampleRateNode() {
        Optional<VariableNode> propertyNode = getPropertyNode(ServerCapabilitiesType.MIN_SUPPORTED_SAMPLE_RATE);
        return (PropertyTypeNode) propertyNode.orElse(null);
    }

    @Override
    public Double getMinSupportedSampleRate() {
        Optional<Double> propertyValue = getProperty(ServerCapabilitiesType.MIN_SUPPORTED_SAMPLE_RATE);
        return propertyValue.orElse(null);
    }

    @Override
    public void setMinSupportedSampleRate(Double value) {
        setProperty(ServerCapabilitiesType.MIN_SUPPORTED_SAMPLE_RATE, value);
    }

    @Override
    public PropertyTypeNode getMaxBrowseContinuationPointsNode() {
        Optional<VariableNode> propertyNode = getPropertyNode(ServerCapabilitiesType.MAX_BROWSE_CONTINUATION_POINTS);
        return (PropertyTypeNode) propertyNode.orElse(null);
    }

    @Override
    public UShort getMaxBrowseContinuationPoints() {
        Optional<UShort> propertyValue = getProperty(ServerCapabilitiesType.MAX_BROWSE_CONTINUATION_POINTS);
        return propertyValue.orElse(null);
    }

    @Override
    public void setMaxBrowseContinuationPoints(UShort value) {
        setProperty(ServerCapabilitiesType.MAX_BROWSE_CONTINUATION_POINTS, value);
    }

    @Override
    public PropertyTypeNode getMaxQueryContinuationPointsNode() {
        Optional<VariableNode> propertyNode = getPropertyNode(ServerCapabilitiesType.MAX_QUERY_CONTINUATION_POINTS);
        return (PropertyTypeNode) propertyNode.orElse(null);
    }

    @Override
    public UShort getMaxQueryContinuationPoints() {
        Optional<UShort> propertyValue = getProperty(ServerCapabilitiesType.MAX_QUERY_CONTINUATION_POINTS);
        return propertyValue.orElse(null);
    }

    @Override
    public void setMaxQueryContinuationPoints(UShort value) {
        setProperty(ServerCapabilitiesType.MAX_QUERY_CONTINUATION_POINTS, value);
    }

    @Override
    public PropertyTypeNode getMaxHistoryContinuationPointsNode() {
        Optional<VariableNode> propertyNode = getPropertyNode(ServerCapabilitiesType.MAX_HISTORY_CONTINUATION_POINTS);
        return (PropertyTypeNode) propertyNode.orElse(null);
    }

    @Override
    public UShort getMaxHistoryContinuationPoints() {
        Optional<UShort> propertyValue = getProperty(ServerCapabilitiesType.MAX_HISTORY_CONTINUATION_POINTS);
        return propertyValue.orElse(null);
    }

    @Override
    public void setMaxHistoryContinuationPoints(UShort value) {
        setProperty(ServerCapabilitiesType.MAX_HISTORY_CONTINUATION_POINTS, value);
    }

    @Override
    public PropertyTypeNode getSoftwareCertificatesNode() {
        Optional<VariableNode> propertyNode = getPropertyNode(ServerCapabilitiesType.SOFTWARE_CERTIFICATES);
        return (PropertyTypeNode) propertyNode.orElse(null);
    }

    @Override
    public SignedSoftwareCertificate[] getSoftwareCertificates() {
        Optional<SignedSoftwareCertificate[]> propertyValue = getProperty(ServerCapabilitiesType.SOFTWARE_CERTIFICATES);
        return propertyValue.orElse(null);
    }

    @Override
    public void setSoftwareCertificates(SignedSoftwareCertificate[] value) {
        setProperty(ServerCapabilitiesType.SOFTWARE_CERTIFICATES, value);
    }

    @Override
    public PropertyTypeNode getMaxArrayLengthNode() {
        Optional<VariableNode> propertyNode = getPropertyNode(ServerCapabilitiesType.MAX_ARRAY_LENGTH);
        return (PropertyTypeNode) propertyNode.orElse(null);
    }

    @Override
    public UInteger getMaxArrayLength() {
        Optional<UInteger> propertyValue = getProperty(ServerCapabilitiesType.MAX_ARRAY_LENGTH);
        return propertyValue.orElse(null);
    }

    @Override
    public void setMaxArrayLength(UInteger value) {
        setProperty(ServerCapabilitiesType.MAX_ARRAY_LENGTH, value);
    }

    @Override
    public PropertyTypeNode getMaxStringLengthNode() {
        Optional<VariableNode> propertyNode = getPropertyNode(ServerCapabilitiesType.MAX_STRING_LENGTH);
        return (PropertyTypeNode) propertyNode.orElse(null);
    }

    @Override
    public UInteger getMaxStringLength() {
        Optional<UInteger> propertyValue = getProperty(ServerCapabilitiesType.MAX_STRING_LENGTH);
        return propertyValue.orElse(null);
    }

    @Override
    public void setMaxStringLength(UInteger value) {
        setProperty(ServerCapabilitiesType.MAX_STRING_LENGTH, value);
    }

    @Override
    public PropertyTypeNode getMaxByteStringLengthNode() {
        Optional<VariableNode> propertyNode = getPropertyNode(ServerCapabilitiesType.MAX_BYTE_STRING_LENGTH);
        return (PropertyTypeNode) propertyNode.orElse(null);
    }

    @Override
    public UInteger getMaxByteStringLength() {
        Optional<UInteger> propertyValue = getProperty(ServerCapabilitiesType.MAX_BYTE_STRING_LENGTH);
        return propertyValue.orElse(null);
    }

    @Override
    public void setMaxByteStringLength(UInteger value) {
        setProperty(ServerCapabilitiesType.MAX_BYTE_STRING_LENGTH, value);
    }

    @Override
    public OperationLimitsTypeNode getOperationLimitsNode() {
        Optional<ObjectNode> component = getObjectComponent("http://opcfoundation.org/UA/", "OperationLimits");
        return (OperationLimitsTypeNode) component.orElse(null);
    }

    @Override
    public FolderTypeNode getModellingRulesNode() {
        Optional<ObjectNode> component = getObjectComponent("http://opcfoundation.org/UA/", "ModellingRules");
        return (FolderTypeNode) component.orElse(null);
    }

    @Override
    public FolderTypeNode getAggregateFunctionsNode() {
        Optional<ObjectNode> component = getObjectComponent("http://opcfoundation.org/UA/", "AggregateFunctions");
        return (FolderTypeNode) component.orElse(null);
    }
}
