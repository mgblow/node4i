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
import org.eclipse.milo.opcua.stack.core.types.builtin.DateTime;
import org.eclipse.milo.opcua.stack.core.types.builtin.ExpandedNodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;

public interface TransitionVariableType extends BaseDataVariableType {
    QualifiedProperty<Object> ID = new QualifiedProperty<>(
        "http://opcfoundation.org/UA/",
        "Id",
        ExpandedNodeId.parse("nsu=http://opcfoundation.org/UA/;i=24"),
        ValueRanks.Scalar,
        Object.class
    );

    QualifiedProperty<QualifiedName> NAME = new QualifiedProperty<>(
        "http://opcfoundation.org/UA/",
        "Name",
        ExpandedNodeId.parse("nsu=http://opcfoundation.org/UA/;i=20"),
        ValueRanks.Scalar,
        QualifiedName.class
    );

    QualifiedProperty<UInteger> NUMBER = new QualifiedProperty<>(
        "http://opcfoundation.org/UA/",
        "Number",
        ExpandedNodeId.parse("nsu=http://opcfoundation.org/UA/;i=7"),
        ValueRanks.Scalar,
        UInteger.class
    );

    QualifiedProperty<DateTime> TRANSITION_TIME = new QualifiedProperty<>(
        "http://opcfoundation.org/UA/",
        "TransitionTime",
        ExpandedNodeId.parse("nsu=http://opcfoundation.org/UA/;i=294"),
        ValueRanks.Scalar,
        DateTime.class
    );

    QualifiedProperty<DateTime> EFFECTIVE_TRANSITION_TIME = new QualifiedProperty<>(
        "http://opcfoundation.org/UA/",
        "EffectiveTransitionTime",
        ExpandedNodeId.parse("nsu=http://opcfoundation.org/UA/;i=294"),
        ValueRanks.Scalar,
        DateTime.class
    );

    PropertyType getIdNode();

    Object getId();

    void setId(Object value);

    PropertyType getNameNode();

    QualifiedName getName();

    void setName(QualifiedName value);

    PropertyType getNumberNode();

    UInteger getNumber();

    void setNumber(UInteger value);

    PropertyType getTransitionTimeNode();

    DateTime getTransitionTime();

    void setTransitionTime(DateTime value);

    PropertyType getEffectiveTransitionTimeNode();

    DateTime getEffectiveTransitionTime();

    void setEffectiveTransitionTime(DateTime value);
}
