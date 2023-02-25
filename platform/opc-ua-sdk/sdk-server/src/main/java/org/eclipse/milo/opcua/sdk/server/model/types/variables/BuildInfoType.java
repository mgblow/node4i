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

import org.eclipse.milo.opcua.stack.core.types.builtin.DateTime;

public interface BuildInfoType extends BaseDataVariableType {
    BaseDataVariableType getProductUriNode();

    String getProductUri();

    void setProductUri(String value);

    BaseDataVariableType getManufacturerNameNode();

    String getManufacturerName();

    void setManufacturerName(String value);

    BaseDataVariableType getProductNameNode();

    String getProductName();

    void setProductName(String value);

    BaseDataVariableType getSoftwareVersionNode();

    String getSoftwareVersion();

    void setSoftwareVersion(String value);

    BaseDataVariableType getBuildNumberNode();

    String getBuildNumber();

    void setBuildNumber(String value);

    BaseDataVariableType getBuildDateNode();

    DateTime getBuildDate();

    void setBuildDate(DateTime value);
}
