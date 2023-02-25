/*
 * Copyright (c) 2021 the Eclipse Milo Authors
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.milo.opcua.sdk.server.nodes;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Predicate;

import com.google.common.collect.ImmutableList;
import org.eclipse.milo.opcua.sdk.core.QualifiedProperty;
import org.eclipse.milo.opcua.sdk.core.Reference;
import org.eclipse.milo.opcua.sdk.core.nodes.Node;
import org.eclipse.milo.opcua.sdk.core.nodes.ObjectNode;
import org.eclipse.milo.opcua.sdk.core.nodes.VariableNode;
import org.eclipse.milo.opcua.sdk.server.api.NodeManager;
import org.eclipse.milo.opcua.sdk.server.model.nodes.variables.PropertyTypeNode;
import org.eclipse.milo.opcua.sdk.server.nodes.delegates.AttributeDelegate;
import org.eclipse.milo.opcua.sdk.server.nodes.filters.AttributeFilterChain;
import org.eclipse.milo.opcua.stack.core.AttributeId;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.StatusCodes;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.UaRuntimeException;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.ExpandedNodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UShort;
import org.eclipse.milo.opcua.stack.core.types.enumerated.NodeClass;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.milo.opcua.sdk.core.util.StreamUtil.opt2stream;
import static org.eclipse.milo.opcua.sdk.server.util.AttributeUtil.dv;
import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;

public abstract class UaNode implements UaServerNode {

    private AttributeDelegate attributeDelegate;
    private List<AttributeObserver> observers;

    final AttributeFilterChain filterChain = new AttributeFilterChain();
    ExecutorService executorService;
    private final UaNodeContext context;

    private NodeId nodeId;
    private NodeClass nodeClass;
    private QualifiedName browseName;
    private LocalizedText displayName;
    private LocalizedText description;
    private UInteger writeMask;
    private UInteger userWriteMask;


    protected UaNode(
        UaNodeContext context,
        NodeId nodeId,
        NodeClass nodeClass,
        QualifiedName browseName,
        LocalizedText displayName) {

        this(context, nodeId, nodeClass, browseName,
            displayName, LocalizedText.NULL_VALUE, UInteger.MIN, UInteger.MIN);
    }

    protected UaNode(
        UaNodeContext context,
        NodeId nodeId,
        NodeClass nodeClass,
        QualifiedName browseName,
        LocalizedText displayName,
        LocalizedText description,
        UInteger writeMask,
        UInteger userWriteMask) {

        this.context = context;

        this.nodeId = nodeId;
        this.nodeClass = nodeClass;
        this.browseName = browseName;
        this.displayName = displayName;
        this.description = description;
        this.writeMask = writeMask;
        this.userWriteMask = userWriteMask;
         executorService= Executors.newFixedThreadPool(this.context.getServer().getConfig().getComponentsThreadPoolCount());


    }

    @Override
    public NodeId getNodeId() {
        return (NodeId) filterChain.getAttribute(this, AttributeId.NodeId);
    }

    @Override
    public NodeClass getNodeClass() {
        return (NodeClass) filterChain.getAttribute(this, AttributeId.NodeClass);
    }

    @Override
    public QualifiedName getBrowseName() {
        return (QualifiedName) filterChain.getAttribute(this, AttributeId.BrowseName);
    }

    @Override
    public LocalizedText getDisplayName() {
        return (LocalizedText) filterChain.getAttribute(this, AttributeId.DisplayName);
    }

    @Override
    public LocalizedText getDescription() {
        return (LocalizedText) filterChain.getAttribute(this, AttributeId.Description);
    }

    @Override
    public UInteger getWriteMask() {
        return (UInteger) filterChain.getAttribute(this, AttributeId.WriteMask);
    }

    @Override
    public UInteger getUserWriteMask() {
        return (UInteger) filterChain.getAttribute(this, AttributeId.UserWriteMask);
    }

    @Override
    public void setNodeId(NodeId nodeId) {
        filterChain.setAttribute(this, AttributeId.NodeId, nodeId);
    }

    @Override
    public void setNodeClass(NodeClass nodeClass) {
        filterChain.setAttribute(this, AttributeId.NodeClass, nodeClass);
    }

    @Override
    public void setBrowseName(QualifiedName browseName) {
        filterChain.setAttribute(this, AttributeId.BrowseName, browseName);
    }

    @Override
    public void setDisplayName(LocalizedText displayName) {
        filterChain.setAttribute(this, AttributeId.DisplayName, displayName);
    }

    @Override
    public void setDescription(LocalizedText description) {
        filterChain.setAttribute(this, AttributeId.Description, description);
    }

    @Override
    public void setWriteMask(UInteger writeMask) {
        filterChain.setAttribute(this, AttributeId.WriteMask, writeMask);
    }

    @Override
    public void setUserWriteMask(UInteger userWriteMask) {
        filterChain.setAttribute(this, AttributeId.UserWriteMask, userWriteMask);
    }

    /**
     * Direct read access to the field for {@code attributeId}, bypassing the {@link AttributeFilterChain}.
     *
     * @param attributeId the {@link AttributeId} to get the value for.
     * @return the value for {@code attributeId}.
     */
    public synchronized Object getAttribute(AttributeId attributeId) {
        switch (attributeId) {
            case NodeId:
                return nodeId;

            case NodeClass:
                return nodeClass;

            case BrowseName:
                return browseName;

            case DisplayName:
                return displayName;

            case Description:
                return description;

            case WriteMask:
                return writeMask;

            case UserWriteMask:
                return userWriteMask;

            default:
                throw new UaRuntimeException(
                    StatusCodes.Bad_AttributeIdInvalid,
                    "AttributeId: " + attributeId
                );
        }
    }

    /**
     * Direct write access to the field for {@code attributeId}, bypassing the {@link AttributeFilterChain}.
     * <p>
     * Setting an attribute value invokes {@link #fireAttributeChanged(AttributeId, Object)}, notifying any registered
     * {@link AttributeObserver}s of the change.
     *
     * @param attributeId the {@link AttributeId} to set the value for.
     * @param value       the value to set.
     */
    public synchronized void setAttribute(AttributeId attributeId, Object value) {
        switch (attributeId) {
            case NodeId:
                nodeId = (NodeId) value;
                break;

            case NodeClass:
                nodeClass = (NodeClass) value;
                break;

            case BrowseName:
                browseName = (QualifiedName) value;
                break;

            case DisplayName:
                displayName = (LocalizedText) value;
                break;

            case Description:
                description = (LocalizedText) value;
                break;

            case WriteMask:
                writeMask = (UInteger) value;
                break;

            case UserWriteMask:
                userWriteMask = (UInteger) value;
                break;

            default:
                throw new UaRuntimeException(
                    StatusCodes.Bad_AttributeIdInvalid,
                    "AttributeId: " + attributeId
                );
        }

        fireAttributeChanged(attributeId, value);
    }

    /**
     * Delete this Node, its References, and all its child Nodes and their References, recursively.
     */
    public final void delete() {
        NodeManager<UaNode> nodeManager = context.getNodeManager();

        nodeManager.removeNode(getNodeId());

        for (Reference reference : nodeManager.getReferences(getNodeId())) {
            if (reference.isForward() && reference.subtypeOf(Identifiers.HasChild)) {
                Optional<UaNode> targetNode = nodeManager.getNode(
                    reference.getTargetNodeId(),
                    getNodeContext().getServer().getNamespaceTable()
                );

                targetNode.ifPresent(UaNode::delete);
            }

            nodeManager.removeReferences(reference, context.getNamespaceTable());
        }
    }

    @Override
    public final UaNodeContext getNodeContext() {
        return context;
    }

    public final NodeManager<UaNode> getNodeManager() {
        return context.getNodeManager();
    }

    protected Optional<UaNode> getManagedNode(NodeId nodeId) {
        //return getNodeManager(nodeId).flatMap(n -> n.getNode(nodeId));
        return context.getServer().getAddressSpaceManager().getManagedNode(nodeId);
    }

    protected Optional<UaNode> getManagedNode(ExpandedNodeId nodeId) {
        //return getNodeManager(nodeId).flatMap(n -> n.getNode(nodeId));
        return context.getServer().getAddressSpaceManager().getManagedNode(nodeId);
    }

    @Override
    public ImmutableList<Reference> getReferences() {
        return ImmutableList.copyOf(
            context.getServer()
                .getAddressSpaceManager()
                .getManagedReferences(getNodeId())
        );
    }

    /**
     * Add {@code reference} and its inverse.
     *
     * @param reference the {@link Reference} to add.
     */
    @Override
    public void addReference(Reference reference) {
        context.getNodeManager().addReferences(reference, context.getNamespaceTable());
    }

    /**
     * Remove {@code reference} and its inverse.
     *
     * @param reference the {@link Reference} to remove.
     */
    @Override
    public void removeReference(Reference reference) {
        context.getNodeManager().removeReferences(reference, context.getNamespaceTable());
    }

    public <T> Optional<T> getProperty(QualifiedProperty<T> property) {
        String namespaceUri = property.getNamespaceUri();

        UShort namespaceIndex = context.getServer().getNamespaceTable().getIndex(namespaceUri);

        if (namespaceIndex != null) {
            QualifiedName browseName = new QualifiedName(
                namespaceIndex,
                property.getBrowseName()
            );

            try {
                return getProperty(browseName)
                    .map(property.getJavaType()::cast);
            } catch (Throwable t) {
                return Optional.empty();
            }
        } else {
            return Optional.empty();
        }
    }

    public Optional<Object> getProperty(QualifiedName browseName) {
        return getPropertyNode(browseName)
            .map(node -> node.getValue().getValue().getValue());
    }

    public <T> void setProperty(QualifiedProperty<T> property, T value) {
        UShort namespaceIndex = context.getServer()
            .getNamespaceTable().getIndex(property.getNamespaceUri());

        if (namespaceIndex == null) {
            throw new IllegalArgumentException(
                "property belongs to unregistered " +
                    "namespace: " + property.getNamespaceUri());
        }

        VariableNode node = getPropertyNode(property).orElseGet(() -> {
            String browseName = property.getBrowseName();

            NodeId propertyNodeId = new NodeId(
                getNodeId().getNamespaceIndex(),
                String.format("%s.%s", getNodeId().getIdentifier().toString(), browseName)
            );

            PropertyTypeNode propertyNode = new PropertyTypeNode(
                context,
                propertyNodeId,
                new QualifiedName(namespaceIndex, browseName),
                LocalizedText.english(browseName),
                LocalizedText.NULL_VALUE,
                uint(0),
                uint(0)
            );

            NodeId dataType = property.getDataType()
                .toNodeId(context.getNamespaceTable())
                .orElse(NodeId.NULL_VALUE);

            propertyNode.setDataType(dataType);
            propertyNode.setValueRank(property.getValueRank());
            propertyNode.setArrayDimensions(property.getArrayDimensions());

            propertyNode.addReference(new Reference(
                propertyNode.getNodeId(),
                Identifiers.HasTypeDefinition,
                Identifiers.PropertyType.expanded(),
                true
            ));

            addProperty(propertyNode);

            context.getNodeManager().addNode(propertyNode);

            return propertyNode;
        });

        node.setValue(new DataValue(new Variant(value)));
    }

    public Optional<VariableNode> getPropertyNode(QualifiedProperty<?> property) {
        Optional<QualifiedName> qualifiedName = property
            .getQualifiedName(context.getServer().getNamespaceTable());

        return qualifiedName.map(this::getPropertyNode)
            .orElseGet(() -> getPropertyNode(property.getBrowseName()));
    }

    public Optional<VariableNode> getPropertyNode(String browseName) {
        return getPropertyNode(new QualifiedName(getNodeId().getNamespaceIndex(), browseName));
    }

    public Optional<VariableNode> getPropertyNode(QualifiedName browseName) {
        Node node = getReferences()
            .stream()
            .filter(Reference.HAS_PROPERTY_PREDICATE)
            .flatMap(r -> opt2stream(getManagedNode(r.getTargetNodeId())))
            .filter(n -> n.getBrowseName().equals(browseName))
            .findFirst().orElse(null);

        try {
            return Optional.ofNullable((VariableNode) node);
        } catch (Throwable t) {
            return Optional.empty();
        }
    }

    void addProperty(UaVariableNode node) {
        addReference(new Reference(
            getNodeId(),
            Identifiers.HasProperty,
            node.getNodeId().expanded(),
            true
        ));

        node.addReference(new Reference(
            node.getNodeId(),
            Identifiers.HasProperty,
            getNodeId().expanded(),
            false
        ));
    }

    void removeProperty(UaVariableNode node) {
        removeReference(new Reference(
            getNodeId(),
            Identifiers.HasProperty,
            node.getNodeId().expanded(),
            true
        ));

        node.removeReference(new Reference(
            node.getNodeId(),
            Identifiers.HasProperty,
            getNodeId().expanded(),
            false
        ));
    }

    /**
     * Find a {@link UaNode} with the specified {@code browseName} referenced by this node.
     *
     * @param browseName the Browse Name of the target node.
     * @return the target node, if one was found.
     */
    public Optional<UaNode> findNode(QualifiedName browseName) {
        return findNode(browseName, uaNode -> true, reference -> true);
    }

    /**
     * Find a {@link UaNode} with the specified {@code browseName} referenced by this node.
     *
     * @param browseName the Browse Name of the target node.
     * @param references a {@link Predicate} used to include/exclude references to follow.
     * @return the target node, if one was found.
     */
    public Optional<UaNode> findNode(QualifiedName browseName, Predicate<Reference> references) {
        return findNode(browseName, uaNode -> true, references);
    }

    /**
     * Find a {@link UaNode} with the specified {@code browseName} referenced by this node.
     *
     * @param browseName         the Browse Name of the target node.
     * @param nodePredicate      a {@link Predicate} used to include/exclude target Nodes.
     * @param referencePredicate a {@link Predicate} used to include/exclude references to follow.
     * @return the target node, if one was found.
     */
    public Optional<UaNode> findNode(
        QualifiedName browseName,
        Predicate<UaNode> nodePredicate,
        Predicate<Reference> referencePredicate
    ) {

        return getReferences()
            .stream()
            .filter(referencePredicate)
            .flatMap(r -> opt2stream(getManagedNode(r.getTargetNodeId())))
            .filter(nodePredicate)
            .filter(n -> n.getBrowseName().equals(browseName))
            .findFirst();
    }

    /**
     * Find a {@link UaNode} with a Browse Name of {@code browseName}, in {@code namespaceUri}, referenced by this node.
     *
     * @param namespaceUri       the namespace URI of {@code browseName}.
     * @param browseName         the Browse Name of the target node.
     * @param nodePredicate      a {@link Predicate} used to include/exclude target Nodes.
     * @param referencePredicate a {@link Predicate} used to include/exclude references to follow.
     * @return the target node, if one was found.
     */
    public Optional<UaNode> findNode(
        String namespaceUri,
        String browseName,
        Predicate<UaNode> nodePredicate,
        Predicate<Reference> referencePredicate) {

        return getReferences()
            .stream()
            .filter(referencePredicate)
            .flatMap(r -> opt2stream(getManagedNode(r.getTargetNodeId())))
            .filter(nodePredicate)
            .filter(n -> {
                String nodeBrowseName = n.getBrowseName().getName();

                UShort index = n.getBrowseName().getNamespaceIndex();
                String nodeBrowseNameUri = context.getServer().getNamespaceTable().getUri(index);

                return Objects.equals(browseName, nodeBrowseName) && Objects.equals(namespaceUri, nodeBrowseNameUri);
            })
            .findFirst();
    }

    protected Optional<ObjectNode> getObjectComponent(String namespaceUri, String name) {
        UShort namespaceIndex = context.getServer().getNamespaceTable().getIndex(namespaceUri);

        if (namespaceIndex != null) {
            return getObjectComponent(new QualifiedName(namespaceIndex, name));
        } else {
            return Optional.empty();
        }
    }

    protected Optional<ObjectNode> getObjectComponent(String browseName) {
        return getObjectComponent(new QualifiedName(getNodeId().getNamespaceIndex(), browseName));
    }

    protected Optional<ObjectNode> getObjectComponent(QualifiedName browseName) {
        ObjectNode node = (ObjectNode) getReferences()
            .stream()
            .filter(Reference.HAS_COMPONENT_PREDICATE)
            .flatMap(r -> opt2stream(getManagedNode(r.getTargetNodeId())))
            .filter(n -> n.getNodeClass() == NodeClass.Object && n.getBrowseName().equals(browseName))
            .findFirst().orElse(null);

        return Optional.ofNullable(node);
    }

    protected Optional<VariableNode> getVariableComponent(String namespaceUri, String name) {
        UShort namespaceIndex = context.getServer().getNamespaceTable().getIndex(namespaceUri);

        if (namespaceIndex != null) {
            return getVariableComponent(new QualifiedName(namespaceIndex, name));
        } else {
            return Optional.empty();
        }
    }

    protected Optional<VariableNode> getVariableComponent(String browseName) {
        return getVariableComponent(new QualifiedName(getNodeId().getNamespaceIndex(), browseName));
    }

    protected Optional<VariableNode> getVariableComponent(QualifiedName browseName) {
        VariableNode node = (VariableNode) getReferences()
            .stream()
            .filter(Reference.HAS_COMPONENT_PREDICATE)
            .flatMap(r -> opt2stream(getManagedNode(r.getTargetNodeId())))
            .filter(n -> n.getNodeClass() == NodeClass.Variable && n.getBrowseName().equals(browseName))
            .findFirst().orElse(null);

        return Optional.ofNullable(node);
    }

    public synchronized void addAttributeObserver(AttributeObserver observer) {
        if (observers == null) {
            observers = Collections.synchronizedList(new LinkedList<>());
        }
        observers.add(observer);
    }

    public synchronized void removeAttributeObserver(AttributeObserver observer) {
        if (observers == null) return;

        observers.remove(observer);

        if (observers.isEmpty()) observers = null;
    }

    public synchronized void fireAttributeChanged(AttributeId attributeId, Object attributeValue) {
        if (observers == null) return;
        List<AttributeObserver> toNotify = new ArrayList<>(observers);
        for (AttributeObserver attributeObserver : toNotify) {
            executorService.submit(() -> {
                attributeObserver.attributeChanged(this, attributeId, attributeValue);
            });
        }
    }

    /**
     * Get the {@link AttributeFilterChain} for this Node.
     *
     * @return the {@link AttributeFilterChain} for this Node.
     */
    public AttributeFilterChain getFilterChain() {
        return filterChain;
    }

    /**
     * Set the {@link AttributeDelegate} for this node.
     *
     * @param attributeDelegate the {@link AttributeDelegate}.
     */
    public synchronized void setAttributeDelegate(AttributeDelegate attributeDelegate) {
        this.attributeDelegate = attributeDelegate;
    }

    /**
     * Get the current {@link AttributeDelegate} for this node.
     *
     * @return the current {@link AttributeDelegate} for this node.
     */
    @Nullable
    public synchronized AttributeDelegate getAttributeDelegate() {
        return attributeDelegate;
    }

    @Override
    public DataValue getAttribute(AttributeContext context, AttributeId attributeId) {
        AttributeDelegate delegate = getAttributeDelegate();

        if (delegate == null) {
            try {
                Object attributeValue = getFilterChain().getAttribute(
                    context.getSession().orElse(null),
                    this,
                    attributeId
                );

                if (attributeId == AttributeId.Value) {
                    return (DataValue) attributeValue;
                } else {
                    return dv(attributeValue);
                }
            } catch (Throwable t) {
                StatusCode statusCode = UaException.extractStatusCode(t)
                    .orElse(new StatusCode(StatusCodes.Bad_InternalError));

                return new DataValue(statusCode);
            }
        } else {
            return delegate.getAttribute(context, this, attributeId);
        }
    }

    @Override
    public void setAttribute(
        AttributeContext context,
        AttributeId attributeId,
        DataValue value
    ) throws UaException {

        AttributeDelegate delegate = getAttributeDelegate();

        if (delegate == null) {
            try {
                getFilterChain().setAttribute(
                    context.getSession().orElse(null),
                    this,
                    attributeId,
                    attributeId == AttributeId.Value ? value : value.getValue().getValue()
                );
            } catch (Throwable t) {
                long statusCode = UaException.extractStatusCode(t)
                    .map(StatusCode::getValue)
                    .orElse(StatusCodes.Bad_InternalError);

                throw new UaException(statusCode, t);
            }
        } else {
            delegate.setAttribute(context, this, attributeId, value);
        }
    }

}
