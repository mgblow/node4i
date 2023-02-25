/*
 * Copyright (c) 2019 the Eclipse Milo Authors
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.milo.opcua.sdk.server.api.services;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.eclipse.milo.opcua.sdk.core.Reference;
import org.eclipse.milo.opcua.sdk.server.DiagnosticsContext;
import org.eclipse.milo.opcua.sdk.server.OpcUaServer;
import org.eclipse.milo.opcua.sdk.server.Session;
import org.eclipse.milo.opcua.sdk.server.api.AccessContext;
import org.eclipse.milo.opcua.sdk.server.api.AsyncOperationContext;
import org.eclipse.milo.opcua.sdk.server.api.ServiceOperationContext;
import org.eclipse.milo.opcua.stack.core.StatusCodes;
import org.eclipse.milo.opcua.stack.core.types.builtin.DateTime;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.structured.ViewDescription;
import org.eclipse.milo.opcua.stack.core.util.Unit;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;

public interface ViewServices {

    /**
     * Like {@link #browse(BrowseContext, ViewDescription, NodeId)} but with a null/empty {@link ViewDescription}.
     *
     * @param context the {@link BrowseContext}.
     * @param nodeId  the {@link NodeId} to browse.
     */
    default void browse(BrowseContext context, NodeId nodeId) {
        ViewDescription view = new ViewDescription(
            NodeId.NULL_VALUE,
            DateTime.NULL_VALUE,
            uint(0)
        );

        browse(context, view, nodeId);
    }

    /**
     * Get all References for which {@code nodeId} is the source.
     * <p>
     * If a Node instance for {@code nodeId} does not exist then {@link BrowseContext#failure(StatusCode)} should be
     * invoked with {@link StatusCodes#Bad_NodeIdUnknown}.
     *
     * @param context the {@link BrowseContext}.
     * @param view    the {@link ViewDescription}.
     * @param nodeId  the {@link NodeId} to browse.
     */
    void browse(BrowseContext context, ViewDescription view, NodeId nodeId);

    /**
     * References for which {@code nodeId} is the source are being collected from all AddressSpace instances.
     * Return any References where {@code nodeId} is the source this AddressSpace may have to contribute.
     * <p>
     * The Node identified by {@code nodeId} may be managed by another AddressSpace.
     *
     * @param context the {@link BrowseContext}.
     * @param view    the {@link ViewDescription}.
     * @param nodeId  the {@link NodeId} to get references fo.
     */
    void getReferences(BrowseContext context, ViewDescription view, NodeId nodeId);

    /**
     * Register one or more {@link NodeId}s.
     *
     * @param context the {@link RegisterNodesContext}.
     * @param nodeIds the {@link NodeId}s to register.
     */
    default void registerNodes(RegisterNodesContext context, List<NodeId> nodeIds) {
        context.success(nodeIds);
    }

    /**
     * Unregister one or more previously registered {@link NodeId}s.
     *
     * @param context the {@link UnregisterNodesContext}.
     * @param nodeIds the {@link NodeId}s to unregister.
     */
    default void unregisterNodes(UnregisterNodesContext context, List<NodeId> nodeIds) {
        context.success(Collections.nCopies(nodeIds.size(), Unit.VALUE));
    }

    /**
     * Get the number of views, if any, managed by this {@link ViewServices} implementation.
     *
     * @return the number of views, if any, managed by this {@link ViewServices} implementation.
     */
    default UInteger getViewCount() {
        return uint(0);
    }


    final class BrowseContext extends AsyncOperationContext<List<Reference>> implements AccessContext {

        private final Session session;

        public BrowseContext(OpcUaServer server, @Nullable Session session) {
            super(server);

            this.session = session;
        }

        @Override
        public Optional<Session> getSession() {
            return Optional.ofNullable(session);
        }

    }

    final class RegisterNodesContext extends ServiceOperationContext<NodeId, NodeId> implements AccessContext {

        public RegisterNodesContext(OpcUaServer server, @Nullable Session session) {
            super(server, session);
        }

        public RegisterNodesContext(
            OpcUaServer server,
            @Nullable Session session,
            DiagnosticsContext<NodeId> diagnosticsContext
        ) {

            super(server, session, diagnosticsContext);
        }

    }

    final class UnregisterNodesContext extends ServiceOperationContext<NodeId, Unit> implements AccessContext {

        public UnregisterNodesContext(OpcUaServer server, @Nullable Session session) {
            super(server, session);
        }

        public UnregisterNodesContext(
            OpcUaServer server,
            @Nullable Session session,
            DiagnosticsContext<NodeId> diagnosticsContext
        ) {

            super(server, session, diagnosticsContext);
        }

    }

}
