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
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.DateTime;
import org.eclipse.milo.opcua.stack.core.types.builtin.ExpandedNodeId;

public interface AuditHistoryRawModifyDeleteEventType extends AuditHistoryDeleteEventType {
    QualifiedProperty<Boolean> IS_DELETE_MODIFIED = new QualifiedProperty<>(
        "http://opcfoundation.org/UA/",
        "IsDeleteModified",
        ExpandedNodeId.parse("nsu=http://opcfoundation.org/UA/;i=1"),
        ValueRanks.Scalar,
        Boolean.class
    );

    QualifiedProperty<DateTime> START_TIME = new QualifiedProperty<>(
        "http://opcfoundation.org/UA/",
        "StartTime",
        ExpandedNodeId.parse("nsu=http://opcfoundation.org/UA/;i=294"),
        ValueRanks.Scalar,
        DateTime.class
    );

    QualifiedProperty<DateTime> END_TIME = new QualifiedProperty<>(
        "http://opcfoundation.org/UA/",
        "EndTime",
        ExpandedNodeId.parse("nsu=http://opcfoundation.org/UA/;i=294"),
        ValueRanks.Scalar,
        DateTime.class
    );

    QualifiedProperty<DataValue[]> OLD_VALUES = new QualifiedProperty<>(
        "http://opcfoundation.org/UA/",
        "OldValues",
        ExpandedNodeId.parse("nsu=http://opcfoundation.org/UA/;i=23"),
        ValueRanks.OneDimension,
        DataValue[].class
    );

    PropertyType getIsDeleteModifiedNode();

    Boolean getIsDeleteModified();

    void setIsDeleteModified(Boolean value);

    PropertyType getStartTimeNode();

    DateTime getStartTime();

    void setStartTime(DateTime value);

    PropertyType getEndTimeNode();

    DateTime getEndTime();

    void setEndTime(DateTime value);

    PropertyType getOldValuesNode();

    DataValue[] getOldValues();

    void setOldValues(DataValue[] value);
}
