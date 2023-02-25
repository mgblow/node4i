package org.eclipse.milo.platform.alamEvent.observers;

import org.eclipse.milo.opcua.sdk.server.nodes.AttributeObserver;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNodeContext;
import org.eclipse.milo.opcua.stack.core.AttributeId;
import org.eclipse.milo.platform.alamEvent.interfaces.UaAlarmEventInterface;
import org.eclipse.milo.platform.util.LogUtil;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

public class AlarmConditionTypeObserver implements AttributeObserver {
    private Context runtime = Context.newBuilder("js").allowAllAccess(true).allowIO(true).build();
    String script;
    Logger logger = LoggerFactory.getLogger(getClass());
    UaNodeContext uaNodeContext;

    public AlarmConditionTypeObserver(UaNodeContext uaNodeContext, String script) {
        this.script = script;
        runtime.getBindings("js").putMember("uaAlarmEventInterface", UaAlarmEventInterface.getInstance(uaNodeContext));
        this.uaNodeContext = uaNodeContext;
    }

    @Override
    public void attributeChanged(UaNode node, AttributeId attributeId, Object value) {
        if (attributeId.equals(AttributeId.Value)) {
            // execute simple javascript
            try {
                CompletableFuture<Void> runAsync = CompletableFuture.runAsync(() -> {
                    runtime.eval(Source.create("js", this.script));
                });
                runAsync.join();
            } catch (Exception e) {
                 String message = String.format("error %s ## running event script on node : %s ## with script: %s", e.getMessage(), node.getNodeId(), this.script);
                LogUtil.getInstance().logAndFireEvent(getClass(),message,"ALARM");
            }
        }
    }
}
