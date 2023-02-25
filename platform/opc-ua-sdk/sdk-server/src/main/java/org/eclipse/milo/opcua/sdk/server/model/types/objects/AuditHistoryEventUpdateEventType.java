/*
 * Copyright (c) 2019 the Eclipse Milo Authors
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.milo.opcua.sdk.server.model.types.objects;

import org.eclipse.milo.opcua.sdk.core.QualifiedProperty;
import org.eclipse.milo.opcua.sdk.core.ValueRanks;
import org.eclipse.milo.opcua.sdk.server.model.types.variables.PropertyType;
import org.eclipse.milo.opcua.stack.core.types.builtin.ExpandedNodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.enumerated.PerformUpdateType;
import org.eclipse.milo.opcua.stack.core.types.structured.EventFilter;
import org.eclipse.milo.opcua.stack.core.types.structured.HistoryEventFieldList;

public interface AuditHistoryEventUpdateEventType extends AuditHistoryUpdateEventType {
    QualifiedProperty<NodeId> UPDATED_NODE = new QualifiedProperty<>(
        "http://opcfoundation.org/UA/",
        "UpdatedNode",
        ExpandedNodeId.parse("nsu=http://opcfoundation.org/UA/;i=17"),
        ValueRanks.Scalar,
        NodeId.class
    );

    QualifiedProperty<PerformUpdateType> PERFORM_INSERT_REPLACE = new QualifiedProperty<>(
        "http://opcfoundation.org/UA/",
        "PerformInsertReplace",
        ExpandedNodeId.parse("nsu=http://opcfoundation.org/UA/;i=11293"),
        ValueRanks.Scalar,
        PerformUpdateType.class
    );

    QualifiedProperty<EventFilter> FILTER = new QualifiedProperty<>(
        "http://opcfoundation.org/UA/",
        "Filter",
        ExpandedNodeId.parse("nsu=http://opcfoundation.org/UA/;i=725"),
        ValueRanks.Scalar,
        EventFilter.class
    );

    QualifiedProperty<HistoryEventFieldList[]> NEW_VALUES = new QualifiedProperty<>(
        "http://opcfoundation.org/UA/",
        "NewValues",
        ExpandedNodeId.parse("nsu=http://opcfoundation.org/UA/;i=920"),
        ValueRanks.OneDimension,
        HistoryEventFieldList[].class
    );

    QualifiedProperty<HistoryEventFieldList[]> OLD_VALUES = new QualifiedProperty<>(
        "http://opcfoundation.org/UA/",
        "OldValues",
        ExpandedNodeId.parse("nsu=http://opcfoundation.org/UA/;i=920"),
        ValueRanks.OneDimension,
        HistoryEventFieldList[].class
    );

    PropertyType getUpdatedNodeNode();

    NodeId getUpdatedNode();

    void setUpdatedNode(NodeId value);

    PropertyType getPerformInsertReplaceNode();

    PerformUpdateType getPerformInsertReplace();

    void setPerformInsertReplace(PerformUpdateType value);

    PropertyType getFilterNode();

    EventFilter getFilter();

    void setFilter(EventFilter value);

    PropertyType getNewValuesNode();

    HistoryEventFieldList[] getNewValues();

    void setNewValues(HistoryEventFieldList[] value);

    PropertyType getOldValuesNode();

    HistoryEventFieldList[] getOldValues();

    void setOldValues(HistoryEventFieldList[] value);
}
