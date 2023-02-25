package org.eclipse.milo.platform.runtime.observers;

import com.hivemq.client.mqtt.datatypes.MqttQos;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ObjectWriter;
import org.eclipse.milo.opcua.sdk.server.nodes.AttributeObserver;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNodeContext;
import org.eclipse.milo.opcua.stack.core.AttributeId;
import org.eclipse.milo.platform.runtime.interfaces.UaInterface;
import org.eclipse.milo.platform.util.LogUtil;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;

import java.util.Date;
import java.util.concurrent.CompletableFuture;

public class JavascriptRuntimeObserver implements AttributeObserver {
    private Context runtime = Context.newBuilder("js").allowAllAccess(true).allowIO(true).build();
    String script;
    UaNodeContext uaNodeContext;

    public JavascriptRuntimeObserver(UaNodeContext uaNodeContext, String script) {
        this.script = script;
        runtime.getBindings("js").putMember("uaInterface", UaInterface.getNewInstance(uaNodeContext));
        this.uaNodeContext = uaNodeContext;
    }

    @Override
    public synchronized void attributeChanged(UaNode node, AttributeId attributeId, Object value) {
        if (attributeId.equals(AttributeId.Value)) {
            try {
                CompletableFuture<Void> runAsync = CompletableFuture.runAsync(() -> runtime.eval(Source.create("js", this.script)));
                runAsync.join();
            } catch (Exception e) {
                String message = String.format("error running script %s with error %s for node %s", this.script, e.getMessage(), node.getNodeId().getIdentifier().toString());
                LogUtil.getInstance().logAndFireEvent(getClass(), message, "RUNTIME");
            }
        }
    }
}
