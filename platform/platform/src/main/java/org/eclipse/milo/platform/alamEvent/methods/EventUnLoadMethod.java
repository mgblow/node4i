/*
 * Copyright (c) 2019 the Eclipse Milo Authors
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.milo.platform.alamEvent.methods;

import org.eclipse.milo.opcua.sdk.core.ValueRanks;
import org.eclipse.milo.opcua.sdk.server.api.methods.AbstractMethodInvocationHandler;
import org.eclipse.milo.opcua.sdk.server.api.methods.InvalidArgumentException;
import org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaObjectNode;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.structured.Argument;
import org.eclipse.milo.platform.alamEvent.AlarmEvent;
import org.eclipse.milo.platform.alamEvent.EventIdentifier;
import org.eclipse.milo.platform.util.Props;
import org.eclipse.milo.platform.validators.MethodInputValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventUnLoadMethod extends AbstractMethodInvocationHandler {

    public static final Argument name = new Argument("name", Identifiers.String, ValueRanks.Any, null, new LocalizedText("name"));
    public static final Argument Result = new Argument("result", Identifiers.String, ValueRanks.Any, null, new LocalizedText("result"));

    public static String APP_NAME = null;
    public static final int APP_NAMESPACE_INDEX = Integer.parseInt(Props.getProperty("app-namespace-index").toString());
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private UaMethodNode uaMethodNode;
    private AlarmEvent alarmEvent;

    public EventUnLoadMethod(UaMethodNode uaMethodNode, AlarmEvent alarmEvent) {
        super(uaMethodNode);
        APP_NAME = uaMethodNode.getNodeContext().getServer().getConfig().getApplicationName().getText();
        this.uaMethodNode = uaMethodNode;
        this.alarmEvent = alarmEvent;
    }

    @Override
    public Argument[] getInputArguments() {
        return new Argument[]{name};
    }

    @Override
    public Argument[] getOutputArguments() {
        return new Argument[]{Result};
    }

    @Override
    protected Variant[] invoke(InvocationContext invocationContext, Variant[] inputValues) throws UaException {
        logger.debug("Invoking program() method of objectId={}", invocationContext.getObjectId());
        String name = (String) inputValues[0].getValue();
        String ALARM_EVENT_NODE_ID = EventIdentifier.ALARM_IDENTIFIER.getIdentifier().replace("{app-name}", APP_NAME).replace("{alarm-name}", name);
        try {

            UaObjectNode event = (UaObjectNode) this.uaMethodNode.getNodeContext().getNodeManager().get(new NodeId(Integer.parseInt(Props.getProperty("app-namespace-index").toString()), ALARM_EVENT_NODE_ID));

            if (event == null) {
                LoggerFactory.getLogger(getClass()).error("error removing event with nodeId : {} does not exists.", ALARM_EVENT_NODE_ID);
                throw new UaException(StatusCode.BAD);
            }

            this.alarmEvent.unload(event);
            return new Variant[]{new Variant(true)};
        } catch (Exception e) {
            logger.error("Error invoking EventUnLoadMethod method of objectId={} with message {}", invocationContext.getObjectId(), e.getMessage());
            return new Variant[]{new Variant(false)};
        }
    }

    protected void validateInputArgumentValues(Variant[] inputArgumentValues) throws InvalidArgumentException {
        MethodInputValidator.forbiddenCharacter(this, name);
        MethodInputValidator.Exists(this, name, APP_NAME + "/Alarm&Events/");
        MethodInputValidator.NotNull(this, name);
        if (this.inputErrorMessages.size() != 0) {
            throw new InvalidArgumentException(null);
        }
    }
}
