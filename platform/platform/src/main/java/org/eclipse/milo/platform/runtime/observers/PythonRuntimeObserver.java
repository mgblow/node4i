package org.eclipse.milo.platform.runtime.observers;

import org.eclipse.milo.opcua.sdk.server.nodes.AttributeObserver;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNode;
import org.eclipse.milo.opcua.stack.core.AttributeId;
import org.python.util.PythonInterpreter;

public class PythonRuntimeObserver implements AttributeObserver {
    PythonInterpreter runtime;
    String script;

    @Override
    public void attributeChanged(UaNode node, AttributeId attributeId, Object value) {
        if (attributeId.equals(AttributeId.Value)) {
            // ad node that just executes a program
            runtime.exec(script);
        }
    }
}
