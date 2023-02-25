/*
 * Copyright (c) 2021 the Eclipse Milo Authors
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.milo.opcua.sdk.server.nodes;

import org.eclipse.milo.opcua.sdk.core.nodes.DataTypeNode;
import org.eclipse.milo.opcua.sdk.core.nodes.DataTypeNodeProperties;
import org.eclipse.milo.opcua.stack.core.AttributeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.enumerated.NodeClass;
import org.eclipse.milo.opcua.stack.core.types.structured.EnumValueType;
import org.jetbrains.annotations.Nullable;

public class UaDataTypeNode extends UaNode implements DataTypeNode {

    private volatile Boolean isAbstract;

    public UaDataTypeNode(
        UaNodeContext context,
        NodeId nodeId,
        QualifiedName browseName,
        LocalizedText displayName,
        LocalizedText description,
        UInteger writeMask,
        UInteger userWriteMask,
        boolean isAbstract) {

        super(context, nodeId, NodeClass.DataType,
            browseName, displayName, description, writeMask, userWriteMask);

        this.isAbstract = isAbstract;
    }

    @Override
    public Boolean getIsAbstract() {
        return (Boolean) filterChain.getAttribute(this, AttributeId.IsAbstract);
    }

    @Override
    public void setIsAbstract(Boolean isAbstract) {
        filterChain.setAttribute(this, AttributeId.IsAbstract, isAbstract);
    }

    @Override
    public synchronized Object getAttribute(AttributeId attributeId) {
        if (attributeId == AttributeId.IsAbstract) {
            return isAbstract;
        } else {
            return super.getAttribute(attributeId);
        }
    }

    @Override
    public synchronized void setAttribute(AttributeId attributeId, Object value) {
        if (attributeId == AttributeId.IsAbstract) {
            isAbstract = (Boolean) value;
            fireAttributeChanged(attributeId, value);
        } else {
            super.setAttribute(attributeId, value);
        }
    }

    /**
     * Get the value of the NodeVersion Property, if it exists.
     *
     * @return the value of the NodeVersion Property, if it exists.
     * @see DataTypeNodeProperties#NodeVersion
     */
    @Nullable
    public String getNodeVersion() {
        return getProperty(DataTypeNodeProperties.NodeVersion).orElse(null);
    }

    /**
     * Get the value of the EnumStrings Property, if it exists.
     *
     * @return the value of the EnumStrings Property, if it exists.
     * @see DataTypeNodeProperties#EnumStrings
     */
    @Nullable
    public LocalizedText[] getEnumStrings() {
        return getProperty(DataTypeNodeProperties.EnumStrings).orElse(null);
    }

    /**
     * Get the value of the EnumValues Property, if it exists.
     *
     * @return the value of the EnumValues Property, if it exists.
     * @see DataTypeNodeProperties#EnumValues
     */
    @Nullable
    public EnumValueType[] getEnumValues() {
        return getProperty(DataTypeNodeProperties.EnumValues).orElse(null);
    }

    /**
     * Get the value of the OptionSetValues Property, if it exists.
     *
     * @return the value of the OptionSetValues Property, if it exists.
     * @see DataTypeNodeProperties#OptionSetValues
     */
    @Nullable
    public LocalizedText[] getOptionSetValues() {
        return getProperty(DataTypeNodeProperties.OptionSetValues).orElse(null);
    }

    /**
     * Set the value of the NodeVersion Property.
     * <p>
     * A PropertyNode will be created if it does not already exist.
     *
     * @param nodeVersion the value to set.
     * @see DataTypeNodeProperties#NodeVersion
     */
    public void setNodeVersion(String nodeVersion) {
        setProperty(DataTypeNodeProperties.NodeVersion, nodeVersion);
    }

    /**
     * Set the value of the EnumStrings Property.
     * <p>
     * A PropertyNode will be created if it does not already exist.
     *
     * @param enumStrings the value to set.
     * @see DataTypeNodeProperties#EnumStrings
     */
    public void setEnumStrings(LocalizedText[] enumStrings) {
        setProperty(DataTypeNodeProperties.EnumStrings, enumStrings);
    }

    /**
     * Set the value of the EnumValues Property.
     * <p>
     * A PropertyNode will be created if it does not already exist.
     *
     * @param enumValues the value to set.
     * @see DataTypeNodeProperties#EnumValues
     */
    public void setEnumValues(EnumValueType[] enumValues) {
        setProperty(DataTypeNodeProperties.EnumValues, enumValues);
    }

    /**
     * Set the value of the OptionSetValues Property.
     * <p>
     * A PropertyNode will be created if it does not already exist.
     *
     * @param optionSetValues the value to set.
     * @see DataTypeNodeProperties#OptionSetValues
     */
    public void setOptionSetValues(LocalizedText[] optionSetValues) {
        setProperty(DataTypeNodeProperties.OptionSetValues, optionSetValues);
    }

}
