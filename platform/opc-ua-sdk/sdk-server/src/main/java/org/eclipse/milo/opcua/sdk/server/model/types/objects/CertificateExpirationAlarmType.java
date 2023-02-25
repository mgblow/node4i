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
import org.eclipse.milo.opcua.stack.core.types.builtin.ByteString;
import org.eclipse.milo.opcua.stack.core.types.builtin.DateTime;
import org.eclipse.milo.opcua.stack.core.types.builtin.ExpandedNodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;

public interface CertificateExpirationAlarmType extends SystemOffNormalAlarmType {
    QualifiedProperty<DateTime> EXPIRATION_DATE = new QualifiedProperty<>(
        "http://opcfoundation.org/UA/",
        "ExpirationDate",
        ExpandedNodeId.parse("nsu=http://opcfoundation.org/UA/;i=13"),
        ValueRanks.Scalar,
        DateTime.class
    );

    QualifiedProperty<Double> EXPIRATION_LIMIT = new QualifiedProperty<>(
        "http://opcfoundation.org/UA/",
        "ExpirationLimit",
        ExpandedNodeId.parse("nsu=http://opcfoundation.org/UA/;i=290"),
        ValueRanks.Scalar,
        Double.class
    );

    QualifiedProperty<NodeId> CERTIFICATE_TYPE = new QualifiedProperty<>(
        "http://opcfoundation.org/UA/",
        "CertificateType",
        ExpandedNodeId.parse("nsu=http://opcfoundation.org/UA/;i=17"),
        ValueRanks.Scalar,
        NodeId.class
    );

    QualifiedProperty<ByteString> CERTIFICATE = new QualifiedProperty<>(
        "http://opcfoundation.org/UA/",
        "Certificate",
        ExpandedNodeId.parse("nsu=http://opcfoundation.org/UA/;i=15"),
        ValueRanks.Scalar,
        ByteString.class
    );

    PropertyType getExpirationDateNode();

    DateTime getExpirationDate();

    void setExpirationDate(DateTime value);

    PropertyType getExpirationLimitNode();

    Double getExpirationLimit();

    void setExpirationLimit(Double value);

    PropertyType getCertificateTypeNode();

    NodeId getCertificateType();

    void setCertificateType(NodeId value);

    PropertyType getCertificateNode();

    ByteString getCertificate();

    void setCertificate(ByteString value);
}
