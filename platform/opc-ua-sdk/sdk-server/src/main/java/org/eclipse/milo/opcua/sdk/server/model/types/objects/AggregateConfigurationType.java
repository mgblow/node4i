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
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UByte;

public interface AggregateConfigurationType extends BaseObjectType {
    QualifiedProperty<Boolean> TREAT_UNCERTAIN_AS_BAD = new QualifiedProperty<>(
        "http://opcfoundation.org/UA/",
        "TreatUncertainAsBad",
        ExpandedNodeId.parse("nsu=http://opcfoundation.org/UA/;i=1"),
        ValueRanks.Scalar,
        Boolean.class
    );

    QualifiedProperty<UByte> PERCENT_DATA_BAD = new QualifiedProperty<>(
        "http://opcfoundation.org/UA/",
        "PercentDataBad",
        ExpandedNodeId.parse("nsu=http://opcfoundation.org/UA/;i=3"),
        ValueRanks.Scalar,
        UByte.class
    );

    QualifiedProperty<UByte> PERCENT_DATA_GOOD = new QualifiedProperty<>(
        "http://opcfoundation.org/UA/",
        "PercentDataGood",
        ExpandedNodeId.parse("nsu=http://opcfoundation.org/UA/;i=3"),
        ValueRanks.Scalar,
        UByte.class
    );

    QualifiedProperty<Boolean> USE_SLOPED_EXTRAPOLATION = new QualifiedProperty<>(
        "http://opcfoundation.org/UA/",
        "UseSlopedExtrapolation",
        ExpandedNodeId.parse("nsu=http://opcfoundation.org/UA/;i=1"),
        ValueRanks.Scalar,
        Boolean.class
    );

    PropertyType getTreatUncertainAsBadNode();

    Boolean getTreatUncertainAsBad();

    void setTreatUncertainAsBad(Boolean value);

    PropertyType getPercentDataBadNode();

    UByte getPercentDataBad();

    void setPercentDataBad(UByte value);

    PropertyType getPercentDataGoodNode();

    UByte getPercentDataGood();

    void setPercentDataGood(UByte value);

    PropertyType getUseSlopedExtrapolationNode();

    Boolean getUseSlopedExtrapolation();

    void setUseSlopedExtrapolation(Boolean value);
}
