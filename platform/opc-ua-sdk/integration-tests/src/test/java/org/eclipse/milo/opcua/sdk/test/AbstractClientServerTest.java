/*
 * Copyright (c) 2019 the Eclipse Milo Authors
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.milo.opcua.sdk.test;

import java.util.concurrent.ExecutionException;

import org.eclipse.milo.opcua.binaryschema.GenericBsdParser;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.dtd.DataTypeDictionarySessionInitializer;
import org.eclipse.milo.opcua.sdk.server.OpcUaServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class AbstractClientServerTest {

    protected OpcUaClient client;
    protected OpcUaServer server;
    protected TestNamespace testNamespace;

    @BeforeAll
    public void startClientAndServer() throws Exception {
        server = TestServer.create();

        testNamespace = new TestNamespace(server);
        testNamespace.startup();

        server.startup().get();

        client = TestClient.create(server);
        client.addSessionInitializer(new DataTypeDictionarySessionInitializer(new GenericBsdParser()));

        client.connect().get();
    }

    @AfterAll
    public void stopClientAndServer() throws ExecutionException, InterruptedException {
        client.disconnect().get();

        testNamespace.shutdown();
        server.shutdown().get();
    }

}
