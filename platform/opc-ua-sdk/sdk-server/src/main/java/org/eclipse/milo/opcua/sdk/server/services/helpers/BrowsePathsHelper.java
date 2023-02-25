/*
 * Copyright (c) 2019 the Eclipse Milo Authors
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.milo.opcua.sdk.server.services.helpers;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.eclipse.milo.opcua.sdk.core.Reference;
import org.eclipse.milo.opcua.sdk.server.OpcUaServer;
import org.eclipse.milo.opcua.sdk.server.api.AccessContext;
import org.eclipse.milo.opcua.sdk.server.api.services.AttributeServices.ReadContext;
import org.eclipse.milo.opcua.sdk.server.api.services.ViewServices.BrowseContext;
import org.eclipse.milo.opcua.sdk.server.services.ServiceAttributes;
import org.eclipse.milo.opcua.stack.core.AttributeId;
import org.eclipse.milo.opcua.stack.core.StatusCodes;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.DiagnosticInfo;
import org.eclipse.milo.opcua.stack.core.types.builtin.ExpandedNodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.enumerated.TimestampsToReturn;
import org.eclipse.milo.opcua.stack.core.types.structured.BrowsePath;
import org.eclipse.milo.opcua.stack.core.types.structured.BrowsePathResult;
import org.eclipse.milo.opcua.stack.core.types.structured.BrowsePathTarget;
import org.eclipse.milo.opcua.stack.core.types.structured.ReadValueId;
import org.eclipse.milo.opcua.stack.core.types.structured.RelativePath;
import org.eclipse.milo.opcua.stack.core.types.structured.RelativePathElement;
import org.eclipse.milo.opcua.stack.core.types.structured.ResponseHeader;
import org.eclipse.milo.opcua.stack.core.types.structured.TranslateBrowsePathsToNodeIdsRequest;
import org.eclipse.milo.opcua.stack.core.types.structured.TranslateBrowsePathsToNodeIdsResponse;
import org.eclipse.milo.opcua.stack.server.services.ServiceRequest;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.newArrayListWithCapacity;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.stream.Collectors.toList;
import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;
import static org.eclipse.milo.opcua.stack.core.util.ConversionUtil.a;
import static org.eclipse.milo.opcua.stack.core.util.ConversionUtil.l;
import static org.eclipse.milo.opcua.stack.core.util.FutureUtils.failedUaFuture;
import static org.eclipse.milo.opcua.stack.core.util.FutureUtils.sequence;

public class BrowsePathsHelper {

    private final AccessContext context;
    private final OpcUaServer server;

    public BrowsePathsHelper(AccessContext context, OpcUaServer server) {
        this.context = context;
        this.server = server;
    }

    public void onTranslateBrowsePaths(ServiceRequest service) {
        TranslateBrowsePathsToNodeIdsRequest request = (TranslateBrowsePathsToNodeIdsRequest) service.getRequest();

        OpcUaServer server = service.attr(ServiceAttributes.SERVER_KEY).get();

        List<BrowsePath> browsePaths = l(request.getBrowsePaths());

        if (browsePaths.isEmpty()) {
            service.setServiceFault(StatusCodes.Bad_NothingToDo);
            return;
        }

        if (browsePaths.size() >
            server.getConfig().getLimits().getMaxNodesPerTranslateBrowsePathsToNodeIds().intValue()) {

            service.setServiceFault(StatusCodes.Bad_TooManyOperations);
            return;
        }

        List<CompletableFuture<BrowsePathResult>> futures = newArrayListWithCapacity(browsePaths.size());

        for (BrowsePath browsePath : browsePaths) {
            futures.add(translate(browsePath));
        }

        sequence(futures).thenAcceptAsync(results -> {
            ResponseHeader header = service.createResponseHeader();

            TranslateBrowsePathsToNodeIdsResponse response = new TranslateBrowsePathsToNodeIdsResponse(
                header,
                a(results, BrowsePathResult.class),
                new DiagnosticInfo[0]
            );

            service.setResponse(response);
        }, server.getExecutorService());
    }

    private CompletableFuture<BrowsePathResult> translate(BrowsePath browsePath) {
        CompletableFuture<BrowsePathResult> future = new CompletableFuture<>();

        NodeId startingNode = browsePath.getStartingNode();
        RelativePath relativePath = browsePath.getRelativePath();

        if (startingNode.isNull()) {
            future.complete(new BrowsePathResult(
                new StatusCode(StatusCodes.Bad_NodeIdInvalid),
                new BrowsePathTarget[0]
            ));

            return future;
        }

        List<RelativePathElement> relativePathElements = l(relativePath.getElements());

        if (relativePathElements.isEmpty()) {
            future.complete(new BrowsePathResult(
                new StatusCode(StatusCodes.Bad_NothingToDo),
                new BrowsePathTarget[0]
            ));

            return future;
        }

        follow(startingNode, relativePathElements).whenComplete((targets, ex) -> {
            if (targets != null) {
                BrowsePathResult result;

                if (!targets.isEmpty()) {
                    result = new BrowsePathResult(
                        StatusCode.GOOD,
                        a(targets, BrowsePathTarget.class)
                    );
                } else {
                    result = new BrowsePathResult(
                        new StatusCode(StatusCodes.Bad_NoMatch),
                        new BrowsePathTarget[0]
                    );
                }

                future.complete(result);
            } else {
                StatusCode statusCode = UaException.extractStatusCode(ex)
                    .orElse(new StatusCode(StatusCodes.Bad_NoMatch));

                BrowsePathResult result = new BrowsePathResult(
                    statusCode,
                    new BrowsePathTarget[0]
                );

                future.complete(result);
            }
        });

        return future;
    }

    private CompletableFuture<List<BrowsePathTarget>> follow(
        NodeId nodeId,
        List<RelativePathElement> elements
    ) {

        if (elements.isEmpty()) {
            return completedFuture(Collections.emptyList());
        } else if (elements.size() == 1) {
            return target(nodeId, elements.get(0)).thenApply(targets ->
                targets.stream()
                    .map(n -> new BrowsePathTarget(n, UInteger.MAX))
                    .collect(toList())
            );
        } else {
            RelativePathElement e = elements.get(0);

            return next(nodeId, e).thenCompose(nextExId -> {
                if (nextExId.isNull()) {
                    // There was no match for the target name
                    return failedUaFuture(StatusCodes.Bad_NoMatch);
                }

                List<RelativePathElement> nextElements = elements.subList(1, elements.size());

                Optional<NodeId> nextId = nextExId.toNodeId(server.getNamespaceTable());

                if (nextId.isPresent()) {
                    return follow(nextId.get(), nextElements);
                } else {
                    UInteger remaining = nextElements.isEmpty() ?
                        UInteger.MAX : uint(nextElements.size());

                    List<BrowsePathTarget> targets = newArrayList(
                        new BrowsePathTarget(nextExId, remaining)
                    );

                    return completedFuture(targets);
                }
            });
        }
    }

    private CompletableFuture<ExpandedNodeId> next(NodeId nodeId, RelativePathElement element) {
        NodeId referenceTypeId = element.getReferenceTypeId();
        boolean includeSubtypes = element.getIncludeSubtypes();
        QualifiedName targetName = element.getTargetName();

        if (targetName.isNull()) {
            return failedUaFuture(StatusCodes.Bad_BrowseNameInvalid);
        }

        BrowseContext browseContext = new BrowseContext(
            server,
            context.getSession().orElse(null)
        );

        server.getAddressSpaceManager().browse(browseContext, nodeId);

        CompletableFuture<List<Reference>> future = browseContext.getFuture();

        return future.thenCompose(references -> {
            List<ExpandedNodeId> targetNodeIds = references.stream()
                /* Filter for references of the requested type or its subtype, if allowed... */
                .filter(r -> referenceTypeId.isNull() ||
                    r.getReferenceTypeId().equals(referenceTypeId) ||
                    (includeSubtypes && r.subtypeOf(referenceTypeId, server.getReferenceTypes())))

                /* Filter for reference direction... */
                .filter(r -> r.isInverse() == element.getIsInverse())

                /* Map to target ExpandedNodeId... */
                .map(Reference::getTargetNodeId)
                .collect(toList());

            if (targetNodeIds.isEmpty()) {
                return failedUaFuture(StatusCodes.Bad_NoMatch);
            } else {
                return readTargetBrowseNames(targetNodeIds).thenApply(browseNames -> {
                    for (int i = 0; i < targetNodeIds.size(); i++) {
                        ExpandedNodeId targetNodeId = targetNodeIds.get(i);
                        QualifiedName browseName = browseNames.get(i);
                        if (browseName.equals(targetName)) {
                            return targetNodeId;
                        }
                    }

                    return ExpandedNodeId.NULL_VALUE;
                });
            }
        });
    }

    private CompletableFuture<List<ExpandedNodeId>> target(NodeId nodeId, RelativePathElement element) {
        NodeId referenceTypeId = element.getReferenceTypeId();
        boolean includeSubtypes = element.getIncludeSubtypes();
        QualifiedName targetName = element.getTargetName();

        if (targetName.isNull()) {
            return failedUaFuture(StatusCodes.Bad_BrowseNameInvalid);
        }

        BrowseContext browseContext = new BrowseContext(
            server,
            context.getSession().orElse(null)
        );

        server.getAddressSpaceManager().browse(browseContext, nodeId);

        CompletableFuture<List<Reference>> future = browseContext.getFuture();

        return future.thenCompose(references -> {
            List<ExpandedNodeId> targetNodeIds = references.stream()
                /* Filter for references of the requested type or its subtype, if allowed... */
                .filter(r -> referenceTypeId.isNull() ||
                    r.getReferenceTypeId().equals(referenceTypeId) ||
                    (includeSubtypes && r.subtypeOf(referenceTypeId, server.getReferenceTypes())))

                /* Filter for reference direction... */
                .filter(r -> r.isInverse() == element.getIsInverse())

                /* Map to target ExpandedNodeId... */
                .map(Reference::getTargetNodeId)
                .collect(toList());

            if (targetNodeIds.isEmpty()) {
                return failedUaFuture(StatusCodes.Bad_NoMatch);
            } else {
                return readTargetBrowseNames(targetNodeIds).thenApply(browseNames -> {
                    List<ExpandedNodeId> targets = newArrayList();

                    for (int i = 0; i < targetNodeIds.size(); i++) {
                        ExpandedNodeId targetNodeId = targetNodeIds.get(i);
                        QualifiedName browseName = browseNames.get(i);
                        if (matchesTarget(browseName, targetName)) {
                            targets.add(targetNodeId);
                        }
                    }

                    return targets;
                });
            }
        });
    }

    private CompletableFuture<List<QualifiedName>> readTargetBrowseNames(List<ExpandedNodeId> targetNodeIds) {
        List<CompletableFuture<List<DataValue>>> futures = newArrayListWithCapacity(targetNodeIds.size());

        for (ExpandedNodeId xni : targetNodeIds) {
            CompletableFuture<List<DataValue>> future = xni.toNodeId(server.getNamespaceTable())
                .map(nodeId -> {
                    ReadValueId readValueId = new ReadValueId(
                        nodeId,
                        AttributeId.BrowseName.uid(),
                        null,
                        QualifiedName.NULL_VALUE
                    );

                    ReadContext context = new ReadContext(server, null);

                    server.getAddressSpaceManager().read(
                        context,
                        0.0,
                        TimestampsToReturn.Neither,
                        newArrayList(readValueId)
                    );

                    return context.getFuture();
                })
                .orElse(completedFuture(newArrayList(new DataValue(StatusCodes.Bad_NodeIdUnknown))));

            futures.add(future);
        }

        return sequence(futures).thenApply(values ->
            values.stream().map(l -> {
                DataValue v = l.get(0);
                return (QualifiedName) v.getValue().getValue();
            }).collect(toList()));
    }

    private boolean matchesTarget(QualifiedName browseName, QualifiedName targetName) {
        return targetName == null ||
            targetName.equals(QualifiedName.NULL_VALUE) ||
            targetName.equals(browseName);
    }

}
