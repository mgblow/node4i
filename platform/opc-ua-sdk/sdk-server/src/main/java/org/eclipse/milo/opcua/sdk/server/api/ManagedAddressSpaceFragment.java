/*
 * Copyright (c) 2019 the Eclipse Milo Authors
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.milo.opcua.sdk.server.api;

import org.eclipse.milo.opcua.sdk.server.OpcUaServer;
import org.eclipse.milo.opcua.sdk.server.UaNodeManager;

public abstract class ManagedAddressSpaceFragment extends ManagedAddressSpace implements AddressSpaceFragment {

    public ManagedAddressSpaceFragment(OpcUaServer server) {
        super(server);
    }

    public ManagedAddressSpaceFragment(OpcUaServer server, UaNodeManager nodeManager) {
        super(server, nodeManager);
    }

}
