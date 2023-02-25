/*
 * Copyright (c) 2019 the Eclipse Milo Authors
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.milo.platform.methods;

import org.eclipse.milo.opcua.sdk.core.ValueRanks;
import org.eclipse.milo.opcua.sdk.server.UaNodeManager;
import org.eclipse.milo.opcua.sdk.server.api.methods.AbstractMethodInvocationHandler;
import org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNode;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.structured.Argument;
import org.eclipse.milo.platform.nodes.ExportList;
import org.eclipse.milo.platform.util.Props;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ExportServer extends AbstractMethodInvocationHandler {

    public static final Argument Result = new Argument("result", Identifiers.String, ValueRanks.Any, null, new LocalizedText("result"));

    public static final String APP_NAME = (String) Props.getProperty("app-name");
    public static final int APP_NAMESPACE_INDEX = Integer.parseInt((String) Props.getProperty("app-namespace-index"));
    public static final String REDIS_CONNECTION_URL = (String) Props.getProperty("redis-connection-url");
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private UaMethodNode uaMethodNode;
    private UaNodeManager uaNodeManager;

    private ExportList exportList;

    public ExportServer(UaMethodNode node, UaNodeManager nodeManager) {
        super(node);
        this.uaMethodNode = node;
        this.exportList = new ExportList(uaMethodNode.getNodeContext());
        this.uaNodeManager = nodeManager;
    }

    @Override
    public Argument[] getInputArguments() {
        return new Argument[]{};
    }

    @Override
    public Argument[] getOutputArguments() {
        return new Argument[]{Result};
    }

    @Override
    protected Variant[] invoke(InvocationContext invocationContext, Variant[] inputValues) {
        logger.debug("Invoking ChangeDeviceState() method of objectId={}", invocationContext.getObjectId());
        List<UaNode> nodes = uaNodeManager.getNodes();
//        Document t = XmlUtil.beanToXml(nodes);
        return new Variant[]{new Variant(Boolean.TRUE)};
    }


}
