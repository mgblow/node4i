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
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;

public interface OperationLimitsType extends FolderType {
    QualifiedProperty<UInteger> MAX_NODES_PER_READ = new QualifiedProperty<>(
        "http://opcfoundation.org/UA/",
        "MaxNodesPerRead",
        ExpandedNodeId.parse("nsu=http://opcfoundation.org/UA/;i=7"),
        ValueRanks.Scalar,
        UInteger.class
    );

    QualifiedProperty<UInteger> MAX_NODES_PER_HISTORY_READ_DATA = new QualifiedProperty<>(
        "http://opcfoundation.org/UA/",
        "MaxNodesPerHistoryReadData",
        ExpandedNodeId.parse("nsu=http://opcfoundation.org/UA/;i=7"),
        ValueRanks.Scalar,
        UInteger.class
    );

    QualifiedProperty<UInteger> MAX_NODES_PER_HISTORY_READ_EVENTS = new QualifiedProperty<>(
        "http://opcfoundation.org/UA/",
        "MaxNodesPerHistoryReadEvents",
        ExpandedNodeId.parse("nsu=http://opcfoundation.org/UA/;i=7"),
        ValueRanks.Scalar,
        UInteger.class
    );

    QualifiedProperty<UInteger> MAX_NODES_PER_WRITE = new QualifiedProperty<>(
        "http://opcfoundation.org/UA/",
        "MaxNodesPerWrite",
        ExpandedNodeId.parse("nsu=http://opcfoundation.org/UA/;i=7"),
        ValueRanks.Scalar,
        UInteger.class
    );

    QualifiedProperty<UInteger> MAX_NODES_PER_HISTORY_UPDATE_DATA = new QualifiedProperty<>(
        "http://opcfoundation.org/UA/",
        "MaxNodesPerHistoryUpdateData",
        ExpandedNodeId.parse("nsu=http://opcfoundation.org/UA/;i=7"),
        ValueRanks.Scalar,
        UInteger.class
    );

    QualifiedProperty<UInteger> MAX_NODES_PER_HISTORY_UPDATE_EVENTS = new QualifiedProperty<>(
        "http://opcfoundation.org/UA/",
        "MaxNodesPerHistoryUpdateEvents",
        ExpandedNodeId.parse("nsu=http://opcfoundation.org/UA/;i=7"),
        ValueRanks.Scalar,
        UInteger.class
    );

    QualifiedProperty<UInteger> MAX_NODES_PER_METHOD_CALL = new QualifiedProperty<>(
        "http://opcfoundation.org/UA/",
        "MaxNodesPerMethodCall",
        ExpandedNodeId.parse("nsu=http://opcfoundation.org/UA/;i=7"),
        ValueRanks.Scalar,
        UInteger.class
    );

    QualifiedProperty<UInteger> MAX_NODES_PER_BROWSE = new QualifiedProperty<>(
        "http://opcfoundation.org/UA/",
        "MaxNodesPerBrowse",
        ExpandedNodeId.parse("nsu=http://opcfoundation.org/UA/;i=7"),
        ValueRanks.Scalar,
        UInteger.class
    );

    QualifiedProperty<UInteger> MAX_NODES_PER_REGISTER_NODES = new QualifiedProperty<>(
        "http://opcfoundation.org/UA/",
        "MaxNodesPerRegisterNodes",
        ExpandedNodeId.parse("nsu=http://opcfoundation.org/UA/;i=7"),
        ValueRanks.Scalar,
        UInteger.class
    );

    QualifiedProperty<UInteger> MAX_NODES_PER_TRANSLATE_BROWSE_PATHS_TO_NODE_IDS = new QualifiedProperty<>(
        "http://opcfoundation.org/UA/",
        "MaxNodesPerTranslateBrowsePathsToNodeIds",
        ExpandedNodeId.parse("nsu=http://opcfoundation.org/UA/;i=7"),
        ValueRanks.Scalar,
        UInteger.class
    );

    QualifiedProperty<UInteger> MAX_NODES_PER_NODE_MANAGEMENT = new QualifiedProperty<>(
        "http://opcfoundation.org/UA/",
        "MaxNodesPerNodeManagement",
        ExpandedNodeId.parse("nsu=http://opcfoundation.org/UA/;i=7"),
        ValueRanks.Scalar,
        UInteger.class
    );

    QualifiedProperty<UInteger> MAX_MONITORED_ITEMS_PER_CALL = new QualifiedProperty<>(
        "http://opcfoundation.org/UA/",
        "MaxMonitoredItemsPerCall",
        ExpandedNodeId.parse("nsu=http://opcfoundation.org/UA/;i=7"),
        ValueRanks.Scalar,
        UInteger.class
    );

    PropertyType getMaxNodesPerReadNode();

    UInteger getMaxNodesPerRead();

    void setMaxNodesPerRead(UInteger value);

    PropertyType getMaxNodesPerHistoryReadDataNode();

    UInteger getMaxNodesPerHistoryReadData();

    void setMaxNodesPerHistoryReadData(UInteger value);

    PropertyType getMaxNodesPerHistoryReadEventsNode();

    UInteger getMaxNodesPerHistoryReadEvents();

    void setMaxNodesPerHistoryReadEvents(UInteger value);

    PropertyType getMaxNodesPerWriteNode();

    UInteger getMaxNodesPerWrite();

    void setMaxNodesPerWrite(UInteger value);

    PropertyType getMaxNodesPerHistoryUpdateDataNode();

    UInteger getMaxNodesPerHistoryUpdateData();

    void setMaxNodesPerHistoryUpdateData(UInteger value);

    PropertyType getMaxNodesPerHistoryUpdateEventsNode();

    UInteger getMaxNodesPerHistoryUpdateEvents();

    void setMaxNodesPerHistoryUpdateEvents(UInteger value);

    PropertyType getMaxNodesPerMethodCallNode();

    UInteger getMaxNodesPerMethodCall();

    void setMaxNodesPerMethodCall(UInteger value);

    PropertyType getMaxNodesPerBrowseNode();

    UInteger getMaxNodesPerBrowse();

    void setMaxNodesPerBrowse(UInteger value);

    PropertyType getMaxNodesPerRegisterNodesNode();

    UInteger getMaxNodesPerRegisterNodes();

    void setMaxNodesPerRegisterNodes(UInteger value);

    PropertyType getMaxNodesPerTranslateBrowsePathsToNodeIdsNode();

    UInteger getMaxNodesPerTranslateBrowsePathsToNodeIds();

    void setMaxNodesPerTranslateBrowsePathsToNodeIds(UInteger value);

    PropertyType getMaxNodesPerNodeManagementNode();

    UInteger getMaxNodesPerNodeManagement();

    void setMaxNodesPerNodeManagement(UInteger value);

    PropertyType getMaxMonitoredItemsPerCallNode();

    UInteger getMaxMonitoredItemsPerCall();

    void setMaxMonitoredItemsPerCall(UInteger value);
}
