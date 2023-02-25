/*
 * Copyright (c) 2019 the Eclipse Milo Authors
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.milo.opcua.sdk.server.model.types.variables;

import org.eclipse.milo.opcua.sdk.core.QualifiedProperty;
import org.eclipse.milo.opcua.sdk.core.ValueRanks;
import org.eclipse.milo.opcua.stack.core.types.builtin.ExpandedNodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;

public interface MultiStateDiscreteType extends DiscreteItemType {
    QualifiedProperty<LocalizedText[]> ENUM_STRINGS = new QualifiedProperty<>(
        "http://opcfoundation.org/UA/",
        "EnumStrings",
        ExpandedNodeId.parse("nsu=http://opcfoundation.org/UA/;i=21"),
        ValueRanks.OneDimension,
        LocalizedText[].class
    );

    PropertyType getEnumStringsNode();

    LocalizedText[] getEnumStrings();

    void setEnumStrings(LocalizedText[] value);
}
