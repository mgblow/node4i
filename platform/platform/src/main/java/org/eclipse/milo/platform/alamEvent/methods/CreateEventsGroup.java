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

import org.eclipse.milo.opcua.sdk.core.AccessLevel;
import org.eclipse.milo.opcua.sdk.core.Reference;
import org.eclipse.milo.opcua.sdk.core.ValueRanks;
import org.eclipse.milo.opcua.sdk.server.api.methods.AbstractMethodInvocationHandler;
import org.eclipse.milo.opcua.sdk.server.api.methods.InvalidArgumentException;
import org.eclipse.milo.opcua.sdk.server.nodes.*;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.types.builtin.*;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.structured.Argument;
import org.eclipse.milo.platform.validators.MethodInputValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
public class CreateEventsGroup extends AbstractMethodInvocationHandler {

    public static final Argument name = new Argument("name", Identifiers.String, ValueRanks.Any, null, new LocalizedText("NAME"));

    public static final Argument displayName = new Argument("displayName", Identifiers.String, ValueRanks.Any, null, new LocalizedText("DISPLAY_NAME"));

    public static final Argument events = new Argument("events", Identifiers.String, ValueRanks.OneDimension, new UInteger[10], new LocalizedText("Events"));

    public static final Argument Result = new Argument("result", Identifiers.String, ValueRanks.Any, null, new LocalizedText("result"));

    public static String APP_NAME = null;
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private UaMethodNode uaMethodNode;

    public CreateEventsGroup(UaMethodNode uaMethodNode) {
        super(uaMethodNode);
        APP_NAME = uaMethodNode.getNodeContext().getServer().getConfig().getApplicationName().getText();
        this.uaMethodNode = uaMethodNode;
    }

    @Override
    public Argument[] getInputArguments() {
        return new Argument[]{name, displayName, events};
    }

    @Override
    public Argument[] getOutputArguments() {
        return new Argument[]{Result};
    }

    @Override
    protected Variant[] invoke(InvocationContext invocationContext, Variant[] inputValues) {
        try {
            logger.debug("Invoking CreateEventsGroup() method of objectId={}", invocationContext.getObjectId());
            String name = (String) inputValues[0].getValue();
            String displayName = (String) inputValues[1].getValue();
            String[] events = (String[]) inputValues[2].getValue();
            String groupNodeId = APP_NAME + "/Alarm&Events/Groups/" + name;
            UaObjectNode groupNode = (UaObjectNode) uaMethodNode.getNodeManager().get(new NodeId(2, groupNodeId));

            if (groupNode != null && events.length!=0) {
                groupNode.setDisplayName(new LocalizedText(displayName));
                List<UaNode> componentNodes = groupNode.getComponentNodes();
                for (UaNode event: componentNodes) {
                    groupNode.removeComponent(event);
                }
            } else {
                groupNode = new UaObjectNode(uaMethodNode.getNodeContext(), new NodeId(2, groupNodeId), new QualifiedName(2, groupNodeId), new LocalizedText(displayName));
                uaMethodNode.getNodeManager().addNode(groupNode);
                UaVariableNode groupName = new UaVariableNode.UaVariableNodeBuilder(uaMethodNode.getNodeContext()).setNodeId(new NodeId(2, APP_NAME + "/Alarm&Events/Groups/" + name + "/name")).setAccessLevel(AccessLevel.READ_WRITE).setBrowseName(new QualifiedName(2, APP_NAME + "/Views/" + name + "/name")).setDisplayName(LocalizedText.english("name")).setDataType(Identifiers.String).setTypeDefinition(Identifiers.PropertyType).setValue(DataValue.valueOnly(new Variant(name))).build();
                this.uaMethodNode.getNodeManager().addNode(groupName);
                groupNode.addReference(new Reference(groupNode.getNodeId(), Identifiers.HasProperty, groupName.getNodeId().expanded(), true));
                UaFolderNode groupFolder = (UaFolderNode) groupNode.getNodeManager().get(new NodeId(2, APP_NAME + "/Alarm&Events/Groups"));
                groupFolder.addOrganizes(groupNode);
            }
            for (String event:events){
                UaObjectNode eventNode = (UaObjectNode) uaMethodNode.getNodeManager().get(new NodeId(2, APP_NAME + "/Alarm&Events/" + event));
                if (eventNode != null) {
                    groupNode.addComponent(eventNode);
                }
            }
            return new Variant[]{new Variant(Boolean.TRUE)};
        } catch (Exception e) {
            return new Variant[]{new Variant(Boolean.FALSE)};
        }
    }
    protected void validateInputArgumentValues(Variant[] inputArgumentValues) throws InvalidArgumentException {
        MethodInputValidator.forbiddenCharacter(this,name);
        MethodInputValidator.NotNull(this,name  );
        MethodInputValidator.Exists(this,events,APP_NAME+"/Alarm&Events/");
        if (this.inputErrorMessages.size() != 0) {
            throw new InvalidArgumentException(null);
        }
    }
}
