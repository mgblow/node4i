/*
 * Copyright (c) 2019 the Eclipse Milo Authors
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.milo.platform.gateway.methods;

import org.eclipse.milo.opcua.sdk.core.ValueRanks;
import org.eclipse.milo.opcua.sdk.server.api.methods.AbstractMethodInvocationHandler;
import org.eclipse.milo.opcua.sdk.server.api.methods.InvalidArgumentException;
import org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaObjectNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaVariableNode;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.structured.Argument;
import org.eclipse.milo.platform.gateway.Gateway;
import org.eclipse.milo.platform.gateway.GatewayIdentifier;
import org.eclipse.milo.platform.validators.MethodInputValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChangeDeviceState extends AbstractMethodInvocationHandler {

    public static final Argument Name = new Argument("name", Identifiers.String, ValueRanks.Any, null, new LocalizedText("Device Name"));


    public static final Argument Result = new Argument("result", Identifiers.String, ValueRanks.Any, null, new LocalizedText("result"));

    public static String APP_NAME = null;
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private UaMethodNode uaMethodNode;
    private Gateway gateway;

    public ChangeDeviceState(UaMethodNode uaMethodNode, Gateway gateway) {
        super(uaMethodNode);
        APP_NAME = uaMethodNode.getNodeContext().getServer().getConfig().getApplicationName().getText();
        this.uaMethodNode = uaMethodNode;
        this.gateway = gateway;
    }

    @Override
    public Argument[] getInputArguments() {
        return new Argument[]{Name};
    }

    @Override
    public Argument[] getOutputArguments() {
        return new Argument[]{Result};
    }

    @Override
    protected Variant[] invoke(InvocationContext invocationContext, Variant[] inputValues) {
        logger.debug("Invoking ChangeDeviceState() method of objectId={}", invocationContext.getObjectId());
        String name = (String) inputValues[0].getValue();
        String DEVICE_NODE_ID  = GatewayIdentifier.IO_IDENTIFIER.getIdentifier().replace("{app-name}",APP_NAME).replace("{io-name}",name);
        UaObjectNode deviceNode = (UaObjectNode) uaMethodNode.getNodeManager().get(new NodeId(2, DEVICE_NODE_ID));
        if (deviceNode != null) {
            String currentState = deviceNode.getPropertyNode("Property/STATE").get().getValue().getValue().getValue().toString();
            if (currentState.equals("OFF")) {
                deviceNode.getPropertyNode("Property/STATE").get().setValue(new DataValue(new Variant("ON")));
            } else {
                deviceNode.getPropertyNode("Property/STATE").get().setValue(new DataValue(new Variant("OFF")));
            }
            final String protocolIdentifier = GatewayIdentifier.PROTOCOL_PROPERTY_IDENTIFIER.getIdentifier().replace("{io-identifier}", deviceNode.getNodeId().getIdentifier().toString());
            UaVariableNode protocolNode = (UaVariableNode) uaMethodNode.getNodeManager().get(new NodeId(2, protocolIdentifier));
            switch (protocolNode.getValue().getValue().getValue().toString().toUpperCase()) {
                case "S7":
                    if (currentState.equals("ON")) {
                        this.gateway.getS7Device().turnOffDevice(name);
                    } else {
                        this.gateway.getS7Device().turnOnDevice(name);
                    }
                    break;
                case "MQTT":
                    if (currentState.equals("ON")) {
                        this.gateway.getMqttDevice().turnOffDevice(name);
                    } else {
                        this.gateway.getMqttDevice().turnOnDevice(name);
                    }
                    break;
                case "OPCUA":
                    if (currentState.equals("ON")) {
                        this.gateway.getOpcUaDevice().turnOffDevice(name);
                    } else {
                        this.gateway.getOpcUaDevice().turnOnDevice(name);
                    }
                    break;
                case "AMQP_RMQ":
                    if (currentState.equals("ON")) {
                        this.gateway.getRabbitMQ().turnOffDevice(name);
                    } else {
                        this.gateway.getRabbitMQ().turnOnDevice(name);
                    }
                    break;
                case "AMQP_AMQ":
                    if (currentState.equals("ON")) {
                        this.gateway.getActiveMQ().turnOffDevice(name);
                    } else {
                        this.gateway.getActiveMQ().turnOnDevice(name);
                    }
                    break;
                case "KAFKA":
                    if (currentState.equals("ON")) {
                        this.gateway.getKafkaDevice().turnOffDevice(name);
                    } else {
                        this.gateway.getKafkaDevice().turnOnDevice(name);
                    }
                    break;
                case "REST":
                    if (currentState.equals("ON")) {
                        this.gateway.getRestDevice().turnOffDevice(name);
                    } else {
                        this.gateway.getRestDevice().turnOnDevice(name);
                    }
                    break;
                case "MODBUS":
                    if (currentState.equals("ON")) {
                        this.gateway.getModbusDevice().turnOffDevice(name);
                    } else {
                        this.gateway.getModbusDevice().turnOnDevice(name);
                    }
            }
        }

        return new Variant[]{new Variant(Boolean.TRUE)};
    }
    protected void validateInputArgumentValues(Variant[] inputArgumentValues) throws InvalidArgumentException {
        MethodInputValidator.forbiddenCharacter(this,Name);
        MethodInputValidator.NotNull(this, Name);
        MethodInputValidator.Exists(this,Name,APP_NAME+"/IO/");
        if (this.inputErrorMessages.size() != 0) {
            throw new InvalidArgumentException(null);
        }
    }

}
