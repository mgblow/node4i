/*
 * Copyright (c) 2021 the Eclipse Milo Authors
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.milo.opcua.sdk.client.methods;

import java.util.Arrays;

import org.eclipse.milo.opcua.sdk.client.AddressSpace;
import org.eclipse.milo.opcua.sdk.client.nodes.UaObjectNode;
import org.eclipse.milo.opcua.sdk.client.subscriptions.ManagedDataItem;
import org.eclipse.milo.opcua.sdk.client.subscriptions.ManagedSubscription;
import org.eclipse.milo.opcua.sdk.test.AbstractClientServerTest;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.structured.Argument;
import org.junit.jupiter.api.Test;

import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class UaMethodTest extends AbstractClientServerTest {

    @Test
    public void findMethod() throws UaException {
        ManagedSubscription subscription = ManagedSubscription.create(client);
        ManagedDataItem dataItem = subscription.createDataItem(Identifiers.Server_ServerStatus_CurrentTime);

        AddressSpace addressSpace = client.getAddressSpace();

        UaObjectNode serverNode = addressSpace.getObjectNode(Identifiers.Server);

        UaMethod getMonitoredItems = serverNode.getMethod("GetMonitoredItems");

        assertNotNull(getMonitoredItems);

        Argument[] inputArguments = getMonitoredItems.getInputArguments();
        Argument[] outputArguments = getMonitoredItems.getOutputArguments();

        assertEquals(1, inputArguments.length);
        assertEquals("SubscriptionId", inputArguments[0].getName());
        assertEquals(2, outputArguments.length);
        assertEquals("ServerHandles", outputArguments[0].getName());
        assertEquals("ClientHandles", outputArguments[1].getName());

        Variant[] outputs = getMonitoredItems.call(
            new Variant[]{
                new Variant(subscription.getSubscription().getSubscriptionId())
            }
        );

        UInteger[] expected0 = {dataItem.getMonitoredItem().getMonitoredItemId()};
        UInteger[] expected1 = {dataItem.getMonitoredItem().getClientHandle()};
        assertArrayEquals(expected0, (UInteger[]) outputs[0].getValue());
        assertArrayEquals(expected1, (UInteger[]) outputs[1].getValue());
    }

    @Test
    public void callMethod() throws UaException {
        ManagedSubscription subscription = ManagedSubscription.create(client);
        ManagedDataItem dataItem = subscription.createDataItem(Identifiers.Server_ServerStatus_CurrentTime);

        AddressSpace addressSpace = client.getAddressSpace();

        UaObjectNode serverNode = addressSpace.getObjectNode(Identifiers.Server);

        Variant[] outputs = serverNode.callMethod(
            "GetMonitoredItems",
            new Variant[]{
                new Variant(subscription.getSubscription().getSubscriptionId())
            }
        );

        UInteger[] expected0 = {dataItem.getMonitoredItem().getMonitoredItemId()};
        UInteger[] expected1 = {dataItem.getMonitoredItem().getClientHandle()};
        assertArrayEquals(expected0, (UInteger[]) outputs[0].getValue());
        assertArrayEquals(expected1, (UInteger[]) outputs[1].getValue());
    }

    @Test
    public void callMethodException() throws UaException {
        AddressSpace addressSpace = client.getAddressSpace();

        UaObjectNode serverNode = addressSpace.getObjectNode(Identifiers.Server);

        assertThrows(
            UaMethodException.class,
            () -> serverNode.callMethod(
                "GetMonitoredItems",
                new Variant[]{
                    new Variant(uint(0))
                }
            )
        );
    }

    @Test
    public void findMethodNotFound() throws UaException {
        AddressSpace addressSpace = client.getAddressSpace();

        UaObjectNode serverNode = addressSpace.getObjectNode(Identifiers.Server);

        assertThrows(UaException.class, () -> serverNode.getMethod("foo"));
    }


    @Test
    public void callMethodWithHasComponentReference() throws UaException {
        AddressSpace addressSpace = client.getAddressSpace();

        UaObjectNode objectsNode = addressSpace.getObjectNode(Identifiers.ObjectsFolder);

        Variant[] outputs = objectsNode.callMethod(
            new QualifiedName(2, "sqrt(x)"),
            new Variant[]{new Variant(16.0)}
        );

        assertEquals(4.0, outputs[0].getValue());
    }

    @Test
    public void callMethodWithHasOrderedComponentReference() throws UaException {
        AddressSpace addressSpace = client.getAddressSpace();

        UaObjectNode objectsNode = addressSpace.getObjectNode(Identifiers.ObjectsFolder);

        Variant[] outputs = objectsNode.callMethod(
            new QualifiedName(2, "sqrt2(x)"),
            new Variant[]{new Variant(16.0)}
        );

        assertEquals(4.0, outputs[0].getValue());
    }

    @Test
    public void callMethodWithNoInputsOrOutputs() throws UaException {
        AddressSpace addressSpace = client.getAddressSpace();

        UaObjectNode objectsNode = addressSpace.getObjectNode(Identifiers.ObjectsFolder);

        Variant[] outputs = objectsNode.callMethod(
            new QualifiedName(2, "hasNoInputsOrOutputs()"),
            new Variant[0]
        );

        assertEquals(0, outputs.length);
    }

    @Test
    public void throwsUaMethodException() throws UaException {
        AddressSpace addressSpace = client.getAddressSpace();

        UaObjectNode objectsNode = addressSpace.getObjectNode(Identifiers.ObjectsFolder);

        assertThrows(UaMethodException.class, () -> {
            try {
                objectsNode.callMethod(
                    new QualifiedName(2, "onlyAcceptsPositiveInputs()"),
                    new Variant[]{new Variant(-1)}
                );
            } catch (UaMethodException e) {
                System.out.println("result: " + e.getStatusCode());
                System.out.println("inputArgumentResults: " + Arrays.toString(e.getInputArgumentResults()));

                throw e;
            }
        });
    }

}
