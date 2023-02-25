/*
 * Copyright (c) 2019 the Eclipse Milo Authors
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.milo.opcua.sdk.server.model.nodes.objects;

import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.eclipse.milo.opcua.sdk.core.Reference;
import org.eclipse.milo.opcua.sdk.core.nodes.VariableNode;
import org.eclipse.milo.opcua.sdk.server.model.nodes.variables.TwoStateVariableTypeNode;
import org.eclipse.milo.opcua.sdk.server.model.types.objects.AcknowledgeableConditionType;
import org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNodeContext;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UByte;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;

public class AcknowledgeableConditionTypeNode extends ConditionTypeNode implements AcknowledgeableConditionType {
    public AcknowledgeableConditionTypeNode(UaNodeContext context, NodeId nodeId,
                                            QualifiedName browseName, LocalizedText displayName, LocalizedText description,
                                            UInteger writeMask, UInteger userWriteMask) {
        super(context, nodeId, browseName, displayName, description, writeMask, userWriteMask);
    }

    public AcknowledgeableConditionTypeNode(UaNodeContext context, NodeId nodeId,
                                            QualifiedName browseName, LocalizedText displayName, LocalizedText description,
                                            UInteger writeMask, UInteger userWriteMask, UByte eventNotifier) {
        super(context, nodeId, browseName, displayName, description, writeMask, userWriteMask, eventNotifier);
    }

    @Override
    public TwoStateVariableTypeNode getEnabledStateNode() {
        Optional<VariableNode> component = getVariableComponent("http://opcfoundation.org/UA/", "EnabledState");
        return (TwoStateVariableTypeNode) component.orElse(null);
    }

    @Override
    public LocalizedText getEnabledState() {
        Optional<VariableNode> component = getVariableComponent("http://opcfoundation.org/UA/", "EnabledState");
        return component.map(node -> (LocalizedText) node.getValue().getValue().getValue()).orElse(null);
    }

    @Override
    public void setEnabledState(LocalizedText value) {
        getVariableComponent("http://opcfoundation.org/UA/", "EnabledState").ifPresent(n -> n.setValue(new DataValue(new Variant(value))));
    }

    @Override
    public TwoStateVariableTypeNode getAckedStateNode() {
        Optional<UaNode> component = this.getComponentNodes().stream().filter(new Predicate<UaNode>() {
            @Override
            public boolean test(UaNode uaNode) {
                return uaNode.getBrowseName().equals(new QualifiedName(2, "Property/AckedState"));
            }
        }).findFirst();
        if (!component.isEmpty()) {
            return (TwoStateVariableTypeNode) this.getNodeContext().getNodeManager().get(component.get().getNodeId());
        } else {
            return null;
        }
    }

    @Override
    public LocalizedText getAckedState() {
        Optional<VariableNode> component = getVariableComponent("http://opcfoundation.org/UA/", "AckedState");
        return component.map(node -> (LocalizedText) node.getValue().getValue().getValue()).orElse(null);
    }

    @Override
    public void setAckedState(LocalizedText value) {
        getVariableComponent("http://opcfoundation.org/UA/", "Property/AckedState").ifPresent(n -> n.setValue(new DataValue(new Variant(value))));
    }

    @Override
    public TwoStateVariableTypeNode getConfirmedStateNode() {
        Optional<VariableNode> component = getVariableComponent("http://opcfoundation.org/UA/", "ConfirmedState");
        return (TwoStateVariableTypeNode) component.orElse(null);
    }

    @Override
    public LocalizedText getConfirmedState() {
        Optional<VariableNode> component = getVariableComponent("http://opcfoundation.org/UA/", "ConfirmedState");
        return component.map(node -> (LocalizedText) node.getValue().getValue().getValue()).orElse(null);
    }

    @Override
    public void setConfirmedState(LocalizedText value) {
        getVariableComponent("http://opcfoundation.org/UA/", "Property/ConfirmedState").ifPresent(n -> n.setValue(new DataValue(new Variant(value))));
    }

    @Override
    public UaMethodNode getAcknowledgeMethodNode() {
        Optional<UaNode> methodNode = findNode("http://opcfoundation.org/UA/", "Acknowledge", node -> node instanceof UaMethodNode, Reference.HAS_COMPONENT_PREDICATE);
        return (UaMethodNode) methodNode.orElse(null);
    }

    @Override
    public UaMethodNode getConfirmMethodNode() {
        Optional<UaNode> methodNode = findNode("http://opcfoundation.org/UA/", "Confirm", node -> node instanceof UaMethodNode, Reference.HAS_COMPONENT_PREDICATE);
        return (UaMethodNode) methodNode.orElse(null);
    }

}
