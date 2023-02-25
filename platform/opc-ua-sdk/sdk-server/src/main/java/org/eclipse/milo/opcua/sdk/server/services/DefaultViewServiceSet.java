/*
 * Copyright (c) 2019 the Eclipse Milo Authors
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.milo.opcua.sdk.server.services;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.stream.Stream;

import org.eclipse.milo.opcua.sdk.server.DiagnosticsContext;
import org.eclipse.milo.opcua.sdk.server.OpcUaServer;
import org.eclipse.milo.opcua.sdk.server.Session;
import org.eclipse.milo.opcua.sdk.server.api.services.ViewServices.RegisterNodesContext;
import org.eclipse.milo.opcua.sdk.server.api.services.ViewServices.UnregisterNodesContext;
import org.eclipse.milo.opcua.sdk.server.services.helpers.BrowseHelper;
import org.eclipse.milo.opcua.sdk.server.services.helpers.BrowsePathsHelper;
import org.eclipse.milo.opcua.stack.core.StatusCodes;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.DiagnosticInfo;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.structured.BrowseDescription;
import org.eclipse.milo.opcua.stack.core.types.structured.BrowseRequest;
import org.eclipse.milo.opcua.stack.core.types.structured.BrowseResponse;
import org.eclipse.milo.opcua.stack.core.types.structured.BrowseResult;
import org.eclipse.milo.opcua.stack.core.types.structured.RegisterNodesRequest;
import org.eclipse.milo.opcua.stack.core.types.structured.RegisterNodesResponse;
import org.eclipse.milo.opcua.stack.core.types.structured.ResponseHeader;
import org.eclipse.milo.opcua.stack.core.types.structured.UnregisterNodesRequest;
import org.eclipse.milo.opcua.stack.core.types.structured.UnregisterNodesResponse;
import org.eclipse.milo.opcua.stack.core.util.FutureUtils;
import org.eclipse.milo.opcua.stack.server.services.ServiceRequest;
import org.eclipse.milo.opcua.stack.server.services.ViewServiceSet;

import static org.eclipse.milo.opcua.stack.core.util.ConversionUtil.a;
import static org.eclipse.milo.opcua.stack.core.util.ConversionUtil.l;

public class DefaultViewServiceSet implements ViewServiceSet {

    private final ServiceCounter browseCounter = new ServiceCounter();
    private final ServiceCounter browseNextCounter = new ServiceCounter();
    private final ServiceCounter translateBrowsePathsCounter = new ServiceCounter();

    private final BrowseHelper browseHelper;

    public DefaultViewServiceSet(ExecutorService executor) {
        browseHelper = new BrowseHelper(executor);
    }

    @Override
    public void onBrowse(ServiceRequest service) {
        browseCounter.record(service);

        BrowseRequest request = (BrowseRequest) service.getRequest();

        DiagnosticsContext<BrowseDescription> diagnosticsContext = new DiagnosticsContext<>();

        OpcUaServer server = service.attr(ServiceAttributes.SERVER_KEY).get();
        Session session = service.attr(ServiceAttributes.SESSION_KEY).get();

        List<BrowseDescription> nodesToBrowse = l(request.getNodesToBrowse());

        if (nodesToBrowse.isEmpty()) {
            service.setServiceFault(StatusCodes.Bad_NothingToDo);
            return;
        }

        if (nodesToBrowse.size() > server.getConfig().getLimits().getMaxNodesPerBrowse().intValue()) {
            service.setServiceFault(StatusCodes.Bad_TooManyOperations);
            return;
        }

        Stream<CompletableFuture<BrowseResult>> futures = nodesToBrowse.stream().map(
            browseDescription ->
                browseHelper.browse(
                    () -> Optional.of(session),
                    server,
                    request.getView(),
                    request.getRequestedMaxReferencesPerNode(),
                    browseDescription
                )
        );

        FutureUtils.sequence(futures).thenAccept(results -> {
            ResponseHeader header = service.createResponseHeader();

            DiagnosticInfo[] diagnosticInfos =
                diagnosticsContext.getDiagnosticInfos(nodesToBrowse);

            BrowseResponse response = new BrowseResponse(
                header,
                a(results, BrowseResult.class),
                diagnosticInfos
            );

            service.setResponse(response);
        });
    }

    @Override
    public void onBrowseNext(ServiceRequest service) {
        browseNextCounter.record(service);

        browseHelper.browseNext(service);
    }

    @Override
    public void onTranslateBrowsePaths(ServiceRequest service) {
        translateBrowsePathsCounter.record(service);

        OpcUaServer server = service.attr(ServiceAttributes.SERVER_KEY).get();
        Session session = service.attr(ServiceAttributes.SESSION_KEY).get();

        BrowsePathsHelper browsePathsHelper = new BrowsePathsHelper(
            () -> Optional.ofNullable(session),
            server
        );

        browsePathsHelper.onTranslateBrowsePaths(service);
    }

    @Override
    public void onRegisterNodes(ServiceRequest service) throws UaException {
        OpcUaServer server = service.attr(ServiceAttributes.SERVER_KEY).get();
        Session session = service.attr(ServiceAttributes.SESSION_KEY).get();

        RegisterNodesRequest request = (RegisterNodesRequest) service.getRequest();

        List<NodeId> nodeIds = l(request.getNodesToRegister());

        if (nodeIds.isEmpty()) {
            throw new UaException(StatusCodes.Bad_NothingToDo);
        }

        if (nodeIds.size() > server.getConfig().getLimits().getMaxNodesPerRegisterNodes().intValue()) {
            throw new UaException(StatusCodes.Bad_TooManyOperations);
        }

        RegisterNodesContext context = new RegisterNodesContext(server, session);

        server.getAddressSpaceManager().registerNodes(context, nodeIds);

        context.getFuture().thenAccept(registeredNodeIds -> {
            ResponseHeader header = service.createResponseHeader();

            RegisterNodesResponse response = new RegisterNodesResponse(
                header,
                registeredNodeIds.toArray(new NodeId[0])
            );

            service.setResponse(response);
        });
    }

    @Override
    public void onUnregisterNodes(ServiceRequest service) throws UaException {
        OpcUaServer server = service.attr(ServiceAttributes.SERVER_KEY).get();
        Session session = service.attr(ServiceAttributes.SESSION_KEY).get();

        UnregisterNodesRequest request = (UnregisterNodesRequest) service.getRequest();

        List<NodeId> nodeIds = l(request.getNodesToUnregister());

        if (nodeIds.isEmpty()) {
            throw new UaException(StatusCodes.Bad_NothingToDo);
        }

        if (nodeIds.size() > server.getConfig().getLimits().getMaxNodesPerRegisterNodes().intValue()) {
            throw new UaException(StatusCodes.Bad_TooManyOperations);
        }

        UnregisterNodesContext context = new UnregisterNodesContext(server, session);

        server.getAddressSpaceManager().unregisterNodes(context, nodeIds);

        context.getFuture().thenAccept(registeredNodeIds -> {
            ResponseHeader header = service.createResponseHeader();

            UnregisterNodesResponse response = new UnregisterNodesResponse(header);

            service.setResponse(response);
        });
    }

}
