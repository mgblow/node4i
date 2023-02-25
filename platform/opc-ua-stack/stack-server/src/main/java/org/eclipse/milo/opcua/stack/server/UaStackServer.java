/*
 * Copyright (c) 2019 the Eclipse Milo Authors
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.milo.opcua.stack.server;

import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;

import com.google.common.collect.ForwardingTable;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;
import com.google.common.collect.Tables;
import io.netty.channel.Channel;
import org.eclipse.milo.opcua.stack.core.NamespaceTable;
import org.eclipse.milo.opcua.stack.core.StatusCodes;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.channel.EncodingLimits;
import org.eclipse.milo.opcua.stack.core.security.SecurityPolicy;
import org.eclipse.milo.opcua.stack.core.serialization.SerializationContext;
import org.eclipse.milo.opcua.stack.core.serialization.UaRequestMessage;
import org.eclipse.milo.opcua.stack.core.types.DataTypeManager;
import org.eclipse.milo.opcua.stack.core.types.DefaultDataTypeManager;
import org.eclipse.milo.opcua.stack.core.types.builtin.ByteString;
import org.eclipse.milo.opcua.stack.core.types.builtin.ExpandedNodeId;
import org.eclipse.milo.opcua.stack.core.types.enumerated.ApplicationType;
import org.eclipse.milo.opcua.stack.core.types.enumerated.MessageSecurityMode;
import org.eclipse.milo.opcua.stack.core.types.structured.ActivateSessionRequest;
import org.eclipse.milo.opcua.stack.core.types.structured.AddNodesRequest;
import org.eclipse.milo.opcua.stack.core.types.structured.AddReferencesRequest;
import org.eclipse.milo.opcua.stack.core.types.structured.ApplicationDescription;
import org.eclipse.milo.opcua.stack.core.types.structured.BrowseNextRequest;
import org.eclipse.milo.opcua.stack.core.types.structured.BrowseRequest;
import org.eclipse.milo.opcua.stack.core.types.structured.CallRequest;
import org.eclipse.milo.opcua.stack.core.types.structured.CancelRequest;
import org.eclipse.milo.opcua.stack.core.types.structured.CloseSessionRequest;
import org.eclipse.milo.opcua.stack.core.types.structured.CreateMonitoredItemsRequest;
import org.eclipse.milo.opcua.stack.core.types.structured.CreateSessionRequest;
import org.eclipse.milo.opcua.stack.core.types.structured.CreateSubscriptionRequest;
import org.eclipse.milo.opcua.stack.core.types.structured.DeleteMonitoredItemsRequest;
import org.eclipse.milo.opcua.stack.core.types.structured.DeleteNodesRequest;
import org.eclipse.milo.opcua.stack.core.types.structured.DeleteReferencesRequest;
import org.eclipse.milo.opcua.stack.core.types.structured.DeleteSubscriptionsRequest;
import org.eclipse.milo.opcua.stack.core.types.structured.EndpointDescription;
import org.eclipse.milo.opcua.stack.core.types.structured.FindServersOnNetworkRequest;
import org.eclipse.milo.opcua.stack.core.types.structured.FindServersRequest;
import org.eclipse.milo.opcua.stack.core.types.structured.FindServersResponse;
import org.eclipse.milo.opcua.stack.core.types.structured.GetEndpointsRequest;
import org.eclipse.milo.opcua.stack.core.types.structured.GetEndpointsResponse;
import org.eclipse.milo.opcua.stack.core.types.structured.HistoryReadRequest;
import org.eclipse.milo.opcua.stack.core.types.structured.HistoryUpdateRequest;
import org.eclipse.milo.opcua.stack.core.types.structured.ModifyMonitoredItemsRequest;
import org.eclipse.milo.opcua.stack.core.types.structured.ModifySubscriptionRequest;
import org.eclipse.milo.opcua.stack.core.types.structured.PublishRequest;
import org.eclipse.milo.opcua.stack.core.types.structured.QueryFirstRequest;
import org.eclipse.milo.opcua.stack.core.types.structured.QueryNextRequest;
import org.eclipse.milo.opcua.stack.core.types.structured.ReadRequest;
import org.eclipse.milo.opcua.stack.core.types.structured.RegisterNodesRequest;
import org.eclipse.milo.opcua.stack.core.types.structured.RegisterServer2Request;
import org.eclipse.milo.opcua.stack.core.types.structured.RegisterServerRequest;
import org.eclipse.milo.opcua.stack.core.types.structured.RepublishRequest;
import org.eclipse.milo.opcua.stack.core.types.structured.SetMonitoringModeRequest;
import org.eclipse.milo.opcua.stack.core.types.structured.SetPublishingModeRequest;
import org.eclipse.milo.opcua.stack.core.types.structured.SetTriggeringRequest;
import org.eclipse.milo.opcua.stack.core.types.structured.TransferSubscriptionsRequest;
import org.eclipse.milo.opcua.stack.core.types.structured.TranslateBrowsePathsToNodeIdsRequest;
import org.eclipse.milo.opcua.stack.core.types.structured.UnregisterNodesRequest;
import org.eclipse.milo.opcua.stack.core.types.structured.UserTokenPolicy;
import org.eclipse.milo.opcua.stack.core.types.structured.WriteRequest;
import org.eclipse.milo.opcua.stack.core.util.EndpointUtil;
import org.eclipse.milo.opcua.stack.core.util.FutureUtils;
import org.eclipse.milo.opcua.stack.core.util.Lazy;
import org.eclipse.milo.opcua.stack.core.util.Unit;
import org.eclipse.milo.opcua.stack.server.services.AttributeHistoryServiceSet;
import org.eclipse.milo.opcua.stack.server.services.AttributeServiceSet;
import org.eclipse.milo.opcua.stack.server.services.DiscoveryServiceSet;
import org.eclipse.milo.opcua.stack.server.services.MethodServiceSet;
import org.eclipse.milo.opcua.stack.server.services.MonitoredItemServiceSet;
import org.eclipse.milo.opcua.stack.server.services.NodeManagementServiceSet;
import org.eclipse.milo.opcua.stack.server.services.QueryServiceSet;
import org.eclipse.milo.opcua.stack.server.services.ServiceRequest;
import org.eclipse.milo.opcua.stack.server.services.ServiceRequestHandler;
import org.eclipse.milo.opcua.stack.server.services.SessionServiceSet;
import org.eclipse.milo.opcua.stack.server.services.SubscriptionServiceSet;
import org.eclipse.milo.opcua.stack.server.services.ViewServiceSet;
import org.eclipse.milo.opcua.stack.server.transport.ServerChannelManager;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Strings.nullToEmpty;
import static com.google.common.collect.Lists.newArrayList;
import static java.util.stream.Collectors.toList;
import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.ubyte;
import static org.eclipse.milo.opcua.stack.core.util.ConversionUtil.a;

public class UaStackServer {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final ServiceHandlerTable serviceHandlerTable = new ServiceHandlerTable();

    private final LongAdder rejectedRequestCount = new LongAdder();
    private final LongAdder securityRejectedRequestCount = new LongAdder();

    private final Lazy<ApplicationDescription> applicationDescription = new Lazy<>();

    private final NamespaceTable namespaceTable = new NamespaceTable();

    private final DataTypeManager dataTypeManager =
        DefaultDataTypeManager.createAndInitialize(namespaceTable);

    private final AtomicLong channelIds = new AtomicLong();
    private final AtomicLong tokenIds = new AtomicLong();

    private final List<Channel> channels = new CopyOnWriteArrayList<>();

    private final Set<EndpointConfiguration> boundEndpoints = Sets.newConcurrentHashSet();

    private final ServerChannelManager channelManager;
    private final SerializationContext serializationContext;

    private final UaStackServerConfig config;

    public UaStackServer(UaStackServerConfig config) {
        this.config = config;

        channelManager = new ServerChannelManager(this);

        serializationContext = new SerializationContext() {
            @Override
            public EncodingLimits getEncodingLimits() {
                return config.getEncodingLimits();
            }

            @Override
            public NamespaceTable getNamespaceTable() {
                return namespaceTable;
            }

            @Override
            public DataTypeManager getDataTypeManager() {
                return dataTypeManager;
            }
        };

        config.getEndpoints().forEach(endpoint -> {
            String path = EndpointUtil.getPath(endpoint.getEndpointUrl());

            addServiceSet(path, new DefaultDiscoveryServiceSet(UaStackServer.this));
        });
    }

    public UaStackServerConfig getConfig() {
        return config;
    }

    public CompletableFuture<UaStackServer> startup() {
        List<CompletableFuture<Unit>> futures = new ArrayList<>();

        config.getEndpoints()
            .stream()
            .sorted(Comparator.comparing(EndpointConfiguration::getTransportProfile))
            .forEach(endpoint -> {
                logger.info(
                    "Binding endpoint {} to {}:{} [{}/{}]",
                    endpoint.getEndpointUrl(),
                    endpoint.getBindAddress(),
                    endpoint.getBindPort(),
                    endpoint.getSecurityPolicy(),
                    endpoint.getSecurityMode());

                futures.add(
                    channelManager.bind(endpoint)
                        .whenComplete((u, ex) -> {
                            if (u != null) {
                                boundEndpoints.add(endpoint);
                            }
                        })
                        .exceptionally(ex -> {
                            logger.warn(
                                "Bind failed for endpoint {}",
                                endpoint.getEndpointUrl(), ex);

                            return Unit.VALUE;
                        })
                );
            });

        return FutureUtils.sequence(futures)
            .thenApply(u -> UaStackServer.this);
    }

    public CompletableFuture<UaStackServer> shutdown() {
        List<CompletableFuture<Unit>> futures = new ArrayList<>();

        config.getEndpoints().forEach(endpoint ->
            futures.add(
                channelManager.unbind(endpoint).exceptionally(ex -> {
                    logger.warn(
                        "Unbind failed for endpoint {}",
                        endpoint.getEndpointUrl(), ex);

                    return Unit.VALUE;
                })
            )
        );

        channels.forEach(channel -> {
            CompletableFuture<Unit> f = new CompletableFuture<>();

            channel.close().addListener(fv -> f.complete(Unit.VALUE));

            futures.add(f);
        });

        channels.clear();

        boundEndpoints.clear();

        return FutureUtils.sequence(futures)
            .thenApply(u -> UaStackServer.this);
    }

    public NamespaceTable getNamespaceTable() {
        return namespaceTable;
    }

    public DataTypeManager getDataTypeManager() {
        return dataTypeManager;
    }

    public SerializationContext getSerializationContext() {
        return serializationContext;
    }

    public void registerConnectedChannel(Channel channel) {
        channels.add(channel);
    }

    public void unregisterConnectedChannel(Channel channel) {
        channels.remove(channel);
    }

    public List<Channel> getConnectedChannels() {
        return channels;
    }

    /**
     * Get the {@link EndpointConfiguration}s that were successfully bound during startup.
     *
     * @return the {@link EndpointConfiguration}s that were successfully bound during startup.
     */
    public Set<EndpointConfiguration> getBoundEndpoints() {
        return boundEndpoints;
    }

    public void onServiceRequest(String path, ServiceRequest serviceRequest) {
        config.getExecutor().execute(() -> handleServiceRequest(path, serviceRequest));
    }

    private void handleServiceRequest(String path, ServiceRequest serviceRequest) {
        UaRequestMessage request = serviceRequest.getRequest();

        if (logger.isTraceEnabled()) {
            logger.trace(
                "ServiceRequest received path={}, requestHandle={} request={}",
                path,
                request.getRequestHeader().getRequestHandle(),
                request.getClass().getSimpleName()
            );

            serviceRequest.getFuture().whenComplete((response, ex) -> {
                if (response != null) {
                    logger.trace(
                        "ServiceRequest completed path={}, requestHandle={} response={}",
                        path,
                        response.getResponseHeader().getRequestHandle(),
                        response.getClass().getSimpleName()
                    );
                } else {
                    logger.trace(
                        "ServiceRequest completed exceptionally path={}, requestHandle={}",
                        path,
                        request.getRequestHeader().getRequestHandle(),
                        ex
                    );
                }
            });
        }

        ServiceRequestHandler serviceHandler = getServiceHandler(path, request.getTypeId());

        try {
            if (serviceHandler != null) {
                serviceHandler.handle(serviceRequest);
            } else {
                serviceRequest.setServiceFault(StatusCodes.Bad_ServiceUnsupported);
            }
        } catch (UaException e) {
            serviceRequest.setServiceFault(e);
        } catch (Throwable t) {
            logger.error("Uncaught Throwable executing handler: {}", serviceHandler, t);
            serviceRequest.setServiceFault(StatusCodes.Bad_InternalError);
        }
    }

    public long getNextChannelId() {
        return channelIds.incrementAndGet();
    }

    public long getNextTokenId() {
        return tokenIds.incrementAndGet();
    }

    private ApplicationDescription getApplicationDescription() {
        return applicationDescription.getOrCompute(() -> {
            List<String> discoveryUrls = config.getEndpoints()
                .stream()
                .map(EndpointConfiguration::getEndpointUrl)
                .filter(url -> url.endsWith("/discovery"))
                .distinct()
                .collect(toList());

            if (discoveryUrls.isEmpty()) {
                discoveryUrls = config.getEndpoints()
                    .stream()
                    .map(EndpointConfiguration::getEndpointUrl)
                    .distinct()
                    .collect(toList());
            }

            return new ApplicationDescription(
                config.getApplicationUri(),
                config.getProductUri(),
                config.getApplicationName(),
                ApplicationType.Server,
                null,
                null,
                a(discoveryUrls, String.class)
            );
        });
    }

    public ImmutableList<EndpointDescription> getEndpointDescriptions() {
        return ImmutableList.<EndpointDescription>builder()
            .addAll(
                config.getEndpoints()
                    .stream()
                    .map(this::transformEndpoint)
                    .iterator()
            )
            .build();
    }

    private EndpointDescription transformEndpoint(EndpointConfiguration endpoint) {
        return new EndpointDescription(
            endpoint.getEndpointUrl(),
            getApplicationDescription(),
            certificateByteString(endpoint.getCertificate()),
            endpoint.getSecurityMode(),
            endpoint.getSecurityPolicy().getUri(),
            a(endpoint.getTokenPolicies(), UserTokenPolicy.class),
            endpoint.getTransportProfile().getUri(),
            ubyte(getSecurityLevel(endpoint.getSecurityPolicy(), endpoint.getSecurityMode()))
        );
    }

    private ByteString certificateByteString(@Nullable X509Certificate certificate) {
        if (certificate != null) {
            try {
                return ByteString.of(certificate.getEncoded());
            } catch (CertificateEncodingException e) {
                logger.error("Error decoding certificate.", e);
                return ByteString.NULL_VALUE;
            }
        } else {
            return ByteString.NULL_VALUE;
        }
    }

    private static short getSecurityLevel(SecurityPolicy securityPolicy, MessageSecurityMode securityMode) {
        short securityLevel = 0;

        switch (securityPolicy) {
            case Aes256_Sha256_RsaPss:
            case Basic256Sha256:
                securityLevel |= 0x08;
                break;
            case Aes128_Sha256_RsaOaep:
                securityLevel |= 0x04;
                break;
            case Basic256:
            case Basic128Rsa15:
                securityLevel |= 0x01;
                break;
            case None:
            default:
                break;
        }

        switch (securityMode) {
            case SignAndEncrypt:
                securityLevel |= 0x80;
                break;
            case Sign:
                securityLevel |= 0x40;
                break;
            default:
                securityLevel |= 0x20;
                break;
        }

        return securityLevel;
    }

    public LongAdder getRejectedRequestCount() {
        return rejectedRequestCount;
    }

    public LongAdder getSecurityRejectedRequestCount() {
        return securityRejectedRequestCount;
    }

    public <T extends UaRequestMessage> void addServiceHandler(
        String path,
        ExpandedNodeId dataTypeId,
        ServiceRequestHandler serviceHandler) {

        logger.debug("Adding ServiceHandler for {} at {}", dataTypeId, path);

        serviceHandlerTable.put(path, dataTypeId, serviceHandler);
    }

    public <T extends UaRequestMessage> void removeServiceHandler(String path, ExpandedNodeId dataTypeId) {
        logger.debug("Removing ServiceHandler for {} at {}", dataTypeId, path);

        serviceHandlerTable.remove(path, dataTypeId);
    }

    @Nullable
    public ServiceRequestHandler getServiceHandler(String path, ExpandedNodeId dataTypeId) {
        return serviceHandlerTable.get(path, dataTypeId);
    }

    public void addServiceSet(String path, AttributeServiceSet serviceSet) {
        addServiceHandler(path, ReadRequest.TYPE_ID, serviceSet::onRead);
        addServiceHandler(path, WriteRequest.TYPE_ID, serviceSet::onWrite);
    }

    public void addServiceSet(String path, AttributeHistoryServiceSet serviceSet) {
        addServiceHandler(path, HistoryReadRequest.TYPE_ID, serviceSet::onHistoryRead);
        addServiceHandler(path, HistoryUpdateRequest.TYPE_ID, serviceSet::onHistoryUpdate);
    }

    public void addServiceSet(String path, DiscoveryServiceSet serviceSet) {
        addServiceHandler(path, GetEndpointsRequest.TYPE_ID, serviceSet::onGetEndpoints);
        addServiceHandler(path, FindServersRequest.TYPE_ID, serviceSet::onFindServers);
        addServiceHandler(path, FindServersOnNetworkRequest.TYPE_ID, serviceSet::onFindServersOnNetwork);
        addServiceHandler(path, RegisterServerRequest.TYPE_ID, serviceSet::onRegisterServer);
        addServiceHandler(path, RegisterServer2Request.TYPE_ID, serviceSet::onRegisterServer2);
    }

    public void addServiceSet(String path, QueryServiceSet serviceSet) {
        addServiceHandler(path, QueryFirstRequest.TYPE_ID, serviceSet::onQueryFirst);
        addServiceHandler(path, QueryNextRequest.TYPE_ID, serviceSet::onQueryNext);
    }

    public void addServiceSet(String path, MethodServiceSet serviceSet) {
        addServiceHandler(path, CallRequest.TYPE_ID, serviceSet::onCall);
    }

    public void addServiceSet(String path, MonitoredItemServiceSet serviceSet) {
        addServiceHandler(path, CreateMonitoredItemsRequest.TYPE_ID, serviceSet::onCreateMonitoredItems);
        addServiceHandler(path, ModifyMonitoredItemsRequest.TYPE_ID, serviceSet::onModifyMonitoredItems);
        addServiceHandler(path, DeleteMonitoredItemsRequest.TYPE_ID, serviceSet::onDeleteMonitoredItems);
        addServiceHandler(path, SetMonitoringModeRequest.TYPE_ID, serviceSet::onSetMonitoringMode);
        addServiceHandler(path, SetTriggeringRequest.TYPE_ID, serviceSet::onSetTriggering);
    }

    public void addServiceSet(String path, NodeManagementServiceSet serviceSet) {
        addServiceHandler(path, AddNodesRequest.TYPE_ID, serviceSet::onAddNodes);
        addServiceHandler(path, DeleteNodesRequest.TYPE_ID, serviceSet::onDeleteNodes);
        addServiceHandler(path, AddReferencesRequest.TYPE_ID, serviceSet::onAddReferences);
        addServiceHandler(path, DeleteReferencesRequest.TYPE_ID, serviceSet::onDeleteReferences);
    }

    public void addServiceSet(String path, SessionServiceSet serviceSet) {
        addServiceHandler(path, CreateSessionRequest.TYPE_ID, serviceSet::onCreateSession);
        addServiceHandler(path, ActivateSessionRequest.TYPE_ID, serviceSet::onActivateSession);
        addServiceHandler(path, CloseSessionRequest.TYPE_ID, serviceSet::onCloseSession);
        addServiceHandler(path, CancelRequest.TYPE_ID, serviceSet::onCancel);
    }

    public void addServiceSet(String path, SubscriptionServiceSet serviceSet) {
        addServiceHandler(path, CreateSubscriptionRequest.TYPE_ID, serviceSet::onCreateSubscription);
        addServiceHandler(path, ModifySubscriptionRequest.TYPE_ID, serviceSet::onModifySubscription);
        addServiceHandler(path, DeleteSubscriptionsRequest.TYPE_ID, serviceSet::onDeleteSubscriptions);
        addServiceHandler(path, TransferSubscriptionsRequest.TYPE_ID, serviceSet::onTransferSubscriptions);
        addServiceHandler(path, SetPublishingModeRequest.TYPE_ID, serviceSet::onSetPublishingMode);
        addServiceHandler(path, PublishRequest.TYPE_ID, serviceSet::onPublish);
        addServiceHandler(path, RepublishRequest.TYPE_ID, serviceSet::onRepublish);
    }

    public void addServiceSet(String path, ViewServiceSet serviceSet) {
        addServiceHandler(path, BrowseRequest.TYPE_ID, serviceSet::onBrowse);
        addServiceHandler(path, BrowseNextRequest.TYPE_ID, serviceSet::onBrowseNext);
        addServiceHandler(path, TranslateBrowsePathsToNodeIdsRequest.TYPE_ID, serviceSet::onTranslateBrowsePaths);
        addServiceHandler(path, RegisterNodesRequest.TYPE_ID, serviceSet::onRegisterNodes);
        addServiceHandler(path, UnregisterNodesRequest.TYPE_ID, serviceSet::onUnregisterNodes);
    }

    private static class DefaultDiscoveryServiceSet implements DiscoveryServiceSet {

        private final Logger logger = LoggerFactory.getLogger(getClass());

        private final UaStackServerConfig config;

        private final UaStackServer stackServer;

        public DefaultDiscoveryServiceSet(UaStackServer stackServer) {
            this.stackServer = stackServer;

            this.config = stackServer.getConfig();
        }

        @Override
        public void onGetEndpoints(ServiceRequest serviceRequest) {
            GetEndpointsRequest request = (GetEndpointsRequest) serviceRequest.getRequest();

            List<String> profileUris = request.getProfileUris() != null ?
                newArrayList(request.getProfileUris()) :
                new ArrayList<>();

            List<EndpointDescription> allEndpoints = stackServer.getEndpointDescriptions()
                .stream()
                .filter(ed -> !ed.getEndpointUrl().endsWith("/discovery"))
                .filter(ed -> filterProfileUris(ed, profileUris))
                .distinct()
                .collect(Collectors.toList());

            ApplicationDescription filteredApplicationDescription =
                getFilteredApplicationDescription(request.getEndpointUrl());

            List<EndpointDescription> matchingEndpoints = allEndpoints.stream()
                .filter(endpoint -> filterEndpointUrls(endpoint, request.getEndpointUrl()))
                .map(endpoint ->
                    replaceApplicationDescription(
                        endpoint,
                        filteredApplicationDescription
                    )
                )
                .distinct()
                .collect(toList());

            GetEndpointsResponse response = new GetEndpointsResponse(
                serviceRequest.createResponseHeader(),
                matchingEndpoints.isEmpty() ?
                    allEndpoints.toArray(new EndpointDescription[0]) :
                    matchingEndpoints.toArray(new EndpointDescription[0])
            );

            serviceRequest.setResponse(response);
        }

        private boolean filterProfileUris(EndpointDescription endpoint, List<String> profileUris) {
            return profileUris.size() == 0 || profileUris.contains(endpoint.getTransportProfileUri());
        }

        private boolean filterEndpointUrls(EndpointDescription endpoint, String endpointUrl) {
            try {
                String requestedHost = EndpointUtil.getHost(endpointUrl);
                String endpointHost = EndpointUtil.getHost(endpoint.getEndpointUrl());

                return nullToEmpty(requestedHost).equalsIgnoreCase(endpointHost);
            } catch (Throwable e) {
                logger.debug("Unable to create URI.", e);
                return false;
            }
        }

        private EndpointDescription replaceApplicationDescription(
            EndpointDescription endpoint,
            ApplicationDescription applicationDescription) {

            return new EndpointDescription(
                endpoint.getEndpointUrl(),
                applicationDescription,
                endpoint.getServerCertificate(),
                endpoint.getSecurityMode(),
                endpoint.getSecurityPolicyUri(),
                endpoint.getUserIdentityTokens(),
                endpoint.getTransportProfileUri(),
                endpoint.getSecurityLevel()
            );
        }

        @Override
        public void onFindServers(ServiceRequest serviceRequest) {
            FindServersRequest request = (FindServersRequest) serviceRequest.getRequest();

            List<String> serverUris = request.getServerUris() != null ?
                newArrayList(request.getServerUris()) :
                new ArrayList<>();

            List<ApplicationDescription> applicationDescriptions =
                newArrayList(getFilteredApplicationDescription(request.getEndpointUrl()));

            applicationDescriptions = applicationDescriptions.stream()
                .filter(ad -> filterServerUris(ad, serverUris))
                .collect(toList());

            FindServersResponse response = new FindServersResponse(
                serviceRequest.createResponseHeader(),
                a(applicationDescriptions, ApplicationDescription.class)
            );

            serviceRequest.setResponse(response);
        }

        private ApplicationDescription getFilteredApplicationDescription(String endpointUrl) {
            List<String> allDiscoveryUrls = config.getEndpoints()
                .stream()
                .map(EndpointConfiguration::getEndpointUrl)
                .filter(url -> url.endsWith("/discovery"))
                .distinct()
                .collect(toList());

            if (allDiscoveryUrls.isEmpty()) {
                allDiscoveryUrls = config.getEndpoints()
                    .stream()
                    .map(EndpointConfiguration::getEndpointUrl)
                    .distinct()
                    .collect(toList());
            }

            List<String> matchingDiscoveryUrls = allDiscoveryUrls.stream()
                .filter(discoveryUrl -> {
                    try {

                        String requestedHost = EndpointUtil.getHost(endpointUrl);
                        String discoveryHost = EndpointUtil.getHost(discoveryUrl);

                        logger.debug("requestedHost={}, discoveryHost={}", requestedHost, discoveryHost);

                        return nullToEmpty(requestedHost).equalsIgnoreCase(discoveryHost);
                    } catch (Throwable e) {
                        logger.debug("Unable to create URI.", e);
                        return false;
                    }
                })
                .distinct()
                .collect(toList());


            logger.debug("Matching discovery URLs: {}", matchingDiscoveryUrls);

            return new ApplicationDescription(
                config.getApplicationUri(),
                config.getProductUri(),
                config.getApplicationName(),
                ApplicationType.Server,
                null,
                null,
                matchingDiscoveryUrls.isEmpty() ?
                    allDiscoveryUrls.toArray(new String[0]) :
                    matchingDiscoveryUrls.toArray(new String[0])
            );
        }

        private boolean filterServerUris(ApplicationDescription ad, List<String> serverUris) {
            return serverUris.size() == 0 || serverUris.contains(ad.getApplicationUri());
        }

    }

    private static class ServiceHandlerTable extends
        ForwardingTable<String, ExpandedNodeId, ServiceRequestHandler> {

        private final Table<String, ExpandedNodeId, ServiceRequestHandler> delegate =
            Tables.synchronizedTable(HashBasedTable.create());

        @Override
        protected Table<String, ExpandedNodeId, ServiceRequestHandler> delegate() {
            return delegate;
        }

    }

}
