/*
 * Copyright (c) 2019 the Eclipse Milo Authors
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.milo.opcua.sdk.server.model.methods;

import org.eclipse.milo.opcua.sdk.core.ValueRanks;
import org.eclipse.milo.opcua.sdk.server.api.methods.AbstractMethodInvocationHandler;
import org.eclipse.milo.opcua.sdk.server.api.methods.Out;
import org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.structured.Argument;

public abstract class CloseAndUpdateMethod extends AbstractMethodInvocationHandler {
    public static final Argument FILE_HANDLE = new Argument(
        "FileHandle",
        NodeId.parse("ns=0;i=7"),
        ValueRanks.Scalar,
        null,
        new LocalizedText("", "")
    );

    public static final Argument APPLY_CHANGES_REQUIRED = new Argument(
        "ApplyChangesRequired",
        NodeId.parse("ns=0;i=1"),
        ValueRanks.Scalar,
        null,
        new LocalizedText("", "")
    );

    public CloseAndUpdateMethod(UaMethodNode node) {
        super(node);
    }

    @Override
    public Argument[] getInputArguments() {
        return new Argument[]{FILE_HANDLE};
    }

    @Override
    public Argument[] getOutputArguments() {
        return new Argument[]{APPLY_CHANGES_REQUIRED};
    }

    @Override
    protected Variant[] invoke(AbstractMethodInvocationHandler.InvocationContext context,
                               Variant[] inputValues) throws UaException {
        UInteger fileHandle = (UInteger) inputValues[0].getValue();
        Out<Boolean> applyChangesRequired = new Out<Boolean>();
        invoke(context, fileHandle, applyChangesRequired);
        return new Variant[]{new Variant(applyChangesRequired.get())};
    }

    protected abstract void invoke(AbstractMethodInvocationHandler.InvocationContext context,
                                   UInteger fileHandle, Out<Boolean> applyChangesRequired) throws UaException;
}
