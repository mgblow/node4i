/*
 * Copyright (c) 2019 the Eclipse Milo Authors
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.milo.opcua.sdk.server.nodes.delegates;

import org.eclipse.milo.opcua.sdk.core.nodes.ObjectTypeNode;
import org.eclipse.milo.opcua.sdk.server.nodes.AttributeContext;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaObjectTypeNode;
import org.eclipse.milo.opcua.sdk.server.util.AttributeUtil;
import org.eclipse.milo.opcua.stack.core.AttributeId;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;

import static org.eclipse.milo.opcua.sdk.server.util.AttributeUtil.dv;

public interface GetSetObjectTypeNode extends GetSetBase {

    default DataValue getObjectTypeAttribute(
        AttributeContext context,
        UaObjectTypeNode node,
        AttributeId attributeId
    ) throws UaException {

        switch (attributeId) {
            case IsAbstract:
                return dv(getIsAbstract(context, node));

            default:
                return getBaseAttribute(context, node, attributeId);
        }
    }

    default void setObjectTypeAttribute(
        AttributeContext context,
        UaObjectTypeNode node,
        AttributeId attributeId,
        DataValue value
    ) throws UaException {

        switch (attributeId) {
            case IsAbstract:
                setIsAbstract(context, node, AttributeUtil.extract(value));
                break;

            default:
                setBaseAttribute(context, node, attributeId, value);
        }
    }

    default Boolean getIsAbstract(AttributeContext context, ObjectTypeNode node) throws UaException {
        return (Boolean) ((UaNode) node).getFilterChain().getAttribute(
            context.getSession().orElse(null),
            (UaNode) node,
            AttributeId.IsAbstract
        );
    }

    default void setIsAbstract(AttributeContext context, ObjectTypeNode node, Boolean isAbstract) throws UaException {
        ((UaNode) node).getFilterChain().setAttribute(
            context.getSession().orElse(null),
            (UaNode) node,
            AttributeId.IsAbstract,
            isAbstract
        );
    }

}
