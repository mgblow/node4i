/*
 * Copyright (c) 2019 the Eclipse Milo Authors
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.milo.opcua.sdk.server.namespaces;

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import com.google.common.collect.Lists;
import org.eclipse.milo.opcua.sdk.server.OpcUaServer;
import org.eclipse.milo.opcua.sdk.server.Session;
import org.eclipse.milo.opcua.sdk.server.api.DataItem;
import org.eclipse.milo.opcua.sdk.server.api.EventItem;
import org.eclipse.milo.opcua.sdk.server.api.ManagedNamespaceWithLifecycle;
import org.eclipse.milo.opcua.sdk.server.api.MonitoredItem;
import org.eclipse.milo.opcua.sdk.server.api.config.OpcUaServerConfigLimits;
import org.eclipse.milo.opcua.sdk.server.api.methods.AbstractMethodInvocationHandler;
import org.eclipse.milo.opcua.sdk.server.api.methods.Out;
import org.eclipse.milo.opcua.sdk.server.items.BaseMonitoredItem;
import org.eclipse.milo.opcua.sdk.server.items.MonitoredDataItem;
import org.eclipse.milo.opcua.sdk.server.model.methods.ConditionRefreshMethod;
import org.eclipse.milo.opcua.sdk.server.model.methods.GetMonitoredItemsMethod;
import org.eclipse.milo.opcua.sdk.server.model.methods.ResendDataMethod;
import org.eclipse.milo.opcua.sdk.server.model.nodes.objects.BaseEventTypeNode;
import org.eclipse.milo.opcua.sdk.server.model.nodes.objects.OperationLimitsTypeNode;
import org.eclipse.milo.opcua.sdk.server.model.nodes.objects.ServerCapabilitiesTypeNode;
import org.eclipse.milo.opcua.sdk.server.model.nodes.objects.ServerTypeNode;
import org.eclipse.milo.opcua.sdk.server.model.nodes.variables.ServerStatusTypeNode;
import org.eclipse.milo.opcua.sdk.server.namespaces.loader.NodeLoader;
import org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaVariableNode;
import org.eclipse.milo.opcua.sdk.server.nodes.filters.AttributeFilters;
import org.eclipse.milo.opcua.sdk.server.subscriptions.Subscription;
import org.eclipse.milo.opcua.sdk.server.util.SubscriptionModel;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.StatusCodes;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.DateTime;
import org.eclipse.milo.opcua.stack.core.types.builtin.ExtensionObject;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.enumerated.RedundancySupport;
import org.eclipse.milo.opcua.stack.core.types.enumerated.ServerState;
import org.eclipse.milo.opcua.stack.core.types.structured.BuildInfo;
import org.eclipse.milo.opcua.stack.core.types.structured.ServerStatusDataType;
import org.eclipse.milo.opcua.stack.core.util.Namespaces;
import org.eclipse.milo.opcua.stack.core.util.NonceUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.ubyte;
import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;
import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.ushort;

public class OpcUaNamespace extends ManagedNamespaceWithLifecycle {

    private static final double MIN_SAMPLING_INTERVAL = 100.0;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final SubscriptionModel subscriptionModel;

    private final OpcUaServer server;

    public OpcUaNamespace(OpcUaServer server) {
        super(server, Namespaces.OPC_UA);

        this.server = server;

        subscriptionModel = new SubscriptionModel(server, this);

        getLifecycleManager().addStartupTask(() -> {
            loadNodes();
            configureServerObject();
            configureConditionRefresh();

            // Set a reasonable value for the MinimumSamplingInterval
            // attribute on all VariableNodes, otherwise it defaults to 0.
            getNodeManager().getNodes()
                .stream()
                .filter(node -> node instanceof UaVariableNode)
                .map(UaVariableNode.class::cast)
                .forEach(n -> n.setMinimumSamplingInterval(MIN_SAMPLING_INTERVAL));
        });

        getLifecycleManager().addLifecycle(subscriptionModel);
    }

    @Override
    public void onDataItemsCreated(List<DataItem> dataItems) {
        subscriptionModel.onDataItemsCreated(dataItems);
    }

    @Override
    public void onDataItemsModified(List<DataItem> dataItems) {
        subscriptionModel.onDataItemsModified(dataItems);
    }

    @Override
    public void onDataItemsDeleted(List<DataItem> dataItems) {
        subscriptionModel.onDataItemsDeleted(dataItems);
    }

    @Override
    public void onMonitoringModeChanged(List<MonitoredItem> monitoredItems) {
        subscriptionModel.onMonitoringModeChanged(monitoredItems);
    }

    @Override
    public void onEventItemsCreated(List<EventItem> eventItems) {
        eventItems.stream()
            .filter(MonitoredItem::isSamplingEnabled)
            .forEach(item -> server.getEventBus().register(item));
    }

    @Override
    public void onEventItemsModified(List<EventItem> eventItems) {
        for (EventItem item : eventItems) {
            if (item.isSamplingEnabled()) {
                server.getEventBus().register(item);
            } else {
                server.getEventBus().unregister(item);
            }
        }
    }

    @Override
    public void onEventItemsDeleted(List<EventItem> eventItems) {
        eventItems.forEach(item -> server.getEventBus().unregister(item));
    }

    private void loadNodes() {
        try {
            long startTime = System.nanoTime();
            long startCount = getNodeManager().getNodes().size();

            new NodeLoader(getNodeContext(), getNodeManager()).loadNodes();

            long deltaMs = TimeUnit.MILLISECONDS.convert(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
            long deltaCount = getNodeManager().getNodes().size() - startCount;

            logger.info("Loaded {} nodes in {}ms.", deltaCount, deltaMs);
        } catch (Exception e) {
            logger.error("Error loading nodes.", e);
        }
    }

    private void configureServerObject() {
        ServerTypeNode serverTypeNode = (ServerTypeNode) getNodeManager().get(Identifiers.Server);

        assert serverTypeNode != null;

        // This Node is optional and we don't support it, so delete it entirely.
        serverTypeNode.getNamespacesNode().delete();

        serverTypeNode.getNamespaceArrayNode().getFilterChain().addLast(
            AttributeFilters.getValue(
                ctx ->
                    new DataValue(new Variant(server.getNamespaceTable().toArray()))
            )
        );

        serverTypeNode.getServerArrayNode().getFilterChain().addLast(
            AttributeFilters.getValue(
                ctx ->
                    new DataValue(new Variant(server.getServerTable().toArray()))
            )
        );

        serverTypeNode.setAuditing(false);
        serverTypeNode.getServerDiagnosticsNode().setEnabledFlag(false);
        serverTypeNode.setServiceLevel(ubyte(255));

        ServerStatusTypeNode serverStatus = serverTypeNode.getServerStatusNode();

        BuildInfo buildInfo = server.getConfig().getBuildInfo();
        serverStatus.setBuildInfo(buildInfo);
        serverStatus.getBuildInfoNode().setBuildDate(buildInfo.getBuildDate());
        serverStatus.getBuildInfoNode().setBuildNumber(buildInfo.getBuildNumber());
        serverStatus.getBuildInfoNode().setManufacturerName(buildInfo.getManufacturerName());
        serverStatus.getBuildInfoNode().setProductName(buildInfo.getProductName());
        serverStatus.getBuildInfoNode().setProductUri(buildInfo.getProductUri());
        serverStatus.getBuildInfoNode().setSoftwareVersion(buildInfo.getSoftwareVersion());

        serverStatus.setCurrentTime(DateTime.now());
        serverStatus.setSecondsTillShutdown(uint(0));
        serverStatus.setShutdownReason(LocalizedText.NULL_VALUE);
        serverStatus.setState(ServerState.Running);
        serverStatus.setStartTime(DateTime.now());

        serverStatus.getCurrentTimeNode().getFilterChain().addLast(
            AttributeFilters.getValue(
                ctx ->
                    new DataValue(new Variant(DateTime.now()))
            )
        );

        serverStatus.getFilterChain().addLast(
            AttributeFilters.getValue(ctx -> {
                ServerStatusTypeNode serverStatusNode = (ServerStatusTypeNode) ctx.getNode();

                ExtensionObject xo = ExtensionObject.encode(
                    server.getSerializationContext(),
                    new ServerStatusDataType(
                        serverStatusNode.getStartTime(),
                        DateTime.now(),
                        serverStatusNode.getState(),
                        serverStatusNode.getBuildInfo(),
                        serverStatusNode.getSecondsTillShutdown(),
                        serverStatusNode.getShutdownReason()
                    )
                );

                return new DataValue(new Variant(xo));
            })
        );

        final OpcUaServerConfigLimits limits = server.getConfig().getLimits();
        ServerCapabilitiesTypeNode serverCapabilities = serverTypeNode.getServerCapabilitiesNode();
        serverCapabilities.setServerProfileArray(new String[]{"http://opcfoundation.org/UA-Profile/Server/StandardUA"});
        serverCapabilities.setLocaleIdArray(new String[]{Locale.ENGLISH.getLanguage()});
        serverCapabilities.setMaxArrayLength(limits.getMaxArrayLength());
        serverCapabilities.setMaxStringLength(limits.getMaxStringLength());
        serverCapabilities.setMaxByteStringLength(limits.getMaxByteStringLength());
        serverCapabilities.setMaxBrowseContinuationPoints(limits.getMaxBrowseContinuationPoints());
        serverCapabilities.setMaxHistoryContinuationPoints(limits.getMaxHistoryContinuationPoints());
        serverCapabilities.setMaxQueryContinuationPoints(limits.getMaxQueryContinuationPoints());
        serverCapabilities.setMinSupportedSampleRate(limits.getMinSupportedSampleRate());

        OperationLimitsTypeNode limitsNode = serverCapabilities.getOperationLimitsNode();
        limitsNode.setMaxMonitoredItemsPerCall(limits.getMaxMonitoredItemsPerCall());
        limitsNode.setMaxNodesPerBrowse(limits.getMaxNodesPerBrowse());
        limitsNode.setMaxNodesPerHistoryReadData(limits.getMaxNodesPerHistoryReadData());
        limitsNode.setMaxNodesPerHistoryReadEvents(limits.getMaxNodesPerHistoryReadEvents());
        limitsNode.setMaxNodesPerHistoryUpdateData(limits.getMaxNodesPerHistoryUpdateData());
        limitsNode.setMaxNodesPerHistoryUpdateEvents(limits.getMaxNodesPerHistoryUpdateEvents());
        limitsNode.setMaxNodesPerMethodCall(limits.getMaxNodesPerMethodCall());
        limitsNode.setMaxNodesPerNodeManagement(limits.getMaxNodesPerNodeManagement());
        limitsNode.setMaxNodesPerRead(limits.getMaxNodesPerRead());
        limitsNode.setMaxNodesPerRegisterNodes(limits.getMaxNodesPerRegisterNodes());
        limitsNode.setMaxNodesPerTranslateBrowsePathsToNodeIds(limits.getMaxNodesPerTranslateBrowsePathsToNodeIds());
        limitsNode.setMaxNodesPerWrite(limits.getMaxNodesPerWrite());

        serverTypeNode.getServerRedundancyNode().setRedundancySupport(RedundancySupport.None);

        configureGetMonitoredItems();
        configureResendData();
    }

    private void configureGetMonitoredItems() {
        UaNode node = getNodeManager().get(Identifiers.Server_GetMonitoredItems);

        if (node instanceof UaMethodNode) {
            UaMethodNode methodNode = (UaMethodNode) node;

            configureMethodNode(methodNode, GetMonitoredItemsMethodImpl::new);
        } else {
            logger.warn("GetMonitoredItems UaMethodNode not found.");
        }
    }

    private void configureResendData() {
        UaNode node = getNodeManager().get(Identifiers.Server_ResendData);

        if (node instanceof UaMethodNode) {
            UaMethodNode resendDataNode = (UaMethodNode) node;

            configureMethodNode(resendDataNode, ResendDataMethodImpl::new);
        } else {
            logger.warn("ResendData UaMethodNode not found.");
        }
    }

    private void configureConditionRefresh() {
        UaNode node = getNodeManager().get(Identifiers.ConditionType_ConditionRefresh);

        if (node instanceof UaMethodNode) {
            UaMethodNode conditionRefreshNode = (UaMethodNode) node;

            configureMethodNode(conditionRefreshNode, ConditionRefreshMethodImpl::new);
        } else {
            logger.warn("ConditionRefresh UaMethodNode not found.");
        }
    }

    private static <T extends AbstractMethodInvocationHandler> void configureMethodNode(
        UaMethodNode methodNode,
        Function<UaMethodNode, T> f
    ) {

        T invocationHandler = f.apply(methodNode);

        methodNode.setInvocationHandler(invocationHandler);
        methodNode.setInputArguments(invocationHandler.getInputArguments());
        methodNode.setOutputArguments(invocationHandler.getOutputArguments());
    }

    private static class ConditionRefreshMethodImpl extends ConditionRefreshMethod {

        private final OpcUaServer server;

        ConditionRefreshMethodImpl(UaMethodNode node) {
            super(node);

            server = node.getNodeContext().getServer();
        }

        @Override
        protected void invoke(InvocationContext context, UInteger subscriptionId) throws UaException {
            Session session = context.getSession().orElse(null);

            if (session != null) {
                Subscription subscription = session.getSubscriptionManager().getSubscription(subscriptionId);

                if (subscription != null) {
                    BaseEventTypeNode refreshStart = server.getEventFactory().createEvent(
                        new NodeId(1, UUID.randomUUID()),
                        Identifiers.RefreshStartEventType
                    );

                    refreshStart.setBrowseName(new QualifiedName(1, "RefreshStart"));
                    refreshStart.setDisplayName(LocalizedText.english("RefreshStart"));
                    refreshStart.setEventId(NonceUtil.generateNonce(16));
                    refreshStart.setEventType(Identifiers.RefreshStartEventType);
                    refreshStart.setSourceNode(Identifiers.Server);
                    refreshStart.setSourceName("Server");
                    refreshStart.setTime(DateTime.now());
                    refreshStart.setReceiveTime(DateTime.NULL_VALUE);
                    refreshStart.setMessage(LocalizedText.english("RefreshStart"));
                    refreshStart.setSeverity(ushort(0));

                    BaseEventTypeNode refreshEnd = server.getEventFactory().createEvent(
                        new NodeId(1, UUID.randomUUID()),
                        Identifiers.RefreshEndEventType
                    );

                    refreshEnd.setBrowseName(new QualifiedName(1, "RefreshEnd"));
                    refreshEnd.setDisplayName(LocalizedText.english("RefreshEnd"));
                    refreshEnd.setEventId(NonceUtil.generateNonce(16));
                    refreshEnd.setEventType(Identifiers.RefreshEndEventType);
                    refreshEnd.setSourceNode(Identifiers.Server);
                    refreshEnd.setSourceName("Server");
                    refreshEnd.setTime(DateTime.now());
                    refreshEnd.setReceiveTime(DateTime.NULL_VALUE);
                    refreshEnd.setMessage(LocalizedText.english("RefreshEnd"));
                    refreshEnd.setSeverity(ushort(0));

                    server.getEventBus().post(refreshStart);
                    server.getEventBus().post(refreshEnd);

                    refreshStart.delete();
                    refreshEnd.delete();
                } else {
                    throw new UaException(StatusCodes.Bad_SubscriptionIdInvalid);
                }
            } else {
                throw new UaException(StatusCodes.Bad_UserAccessDenied);
            }
        }

    }

    private static class GetMonitoredItemsMethodImpl extends GetMonitoredItemsMethod {

        private final OpcUaServer server;

        GetMonitoredItemsMethodImpl(UaMethodNode node) {
            super(node);

            server = node.getNodeContext().getServer();
        }

        @Override
        protected void invoke(
            InvocationContext context,
            UInteger subscriptionId,
            Out<UInteger[]> serverHandles,
            Out<UInteger[]> clientHandles
        ) throws UaException {

            Session session = context.getSession().orElseThrow(
                () ->
                    new UaException(StatusCodes.Bad_SessionIdInvalid)
            );

            Subscription subscription = server.getSubscriptions().get(subscriptionId);

            if (subscription == null) {
                throw new UaException(StatusCodes.Bad_SubscriptionIdInvalid);
            }

            if (!session.getSessionId().equals(subscription.getSession().getSessionId())) {
                throw new UaException(StatusCodes.Bad_UserAccessDenied);
            }

            List<UInteger> serverHandleList = Lists.newArrayList();
            List<UInteger> clientHandleList = Lists.newArrayList();

            for (BaseMonitoredItem<?> item : subscription.getMonitoredItems().values()) {
                serverHandleList.add(item.getId());
                clientHandleList.add(uint(item.getClientHandle()));
            }

            serverHandles.set(serverHandleList.toArray(new UInteger[0]));
            clientHandles.set(clientHandleList.toArray(new UInteger[0]));
        }

    }

    private static class ResendDataMethodImpl extends ResendDataMethod {

        ResendDataMethodImpl(UaMethodNode node) {
            super(node);
        }

        @Override
        protected void invoke(InvocationContext context, UInteger subscriptionId) throws UaException {
            Session session = context.getSession().orElse(null);

            if (session != null) {
                Subscription subscription = session.getSubscriptionManager().getSubscription(subscriptionId);

                if (subscription == null) {
                    throw new UaException(StatusCodes.Bad_SubscriptionIdInvalid);
                } else {
                    subscription.getMonitoredItems().values().stream()
                        .filter(item -> item instanceof MonitoredDataItem)
                        .map(item -> (MonitoredDataItem) item)
                        .forEach(MonitoredDataItem::maybeSendLastValue);
                }
            } else {
                throw new UaException(StatusCodes.Bad_UserAccessDenied);
            }
        }

    }

}
