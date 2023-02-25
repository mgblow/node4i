package org.eclipse.milo.platform.boot;

import org.eclipse.milo.opcua.sdk.server.AbstractLifecycle;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNodeContext;
import org.eclipse.milo.platform.alamEvent.AlarmEvent;
import org.eclipse.milo.platform.boot.loaders.EventLoader;
import org.eclipse.milo.platform.boot.loaders.NodeLoader;
import org.eclipse.milo.platform.boot.loaders.ReferenceLoader;
import org.eclipse.milo.platform.boot.loaders.RuntimeLoader;
import org.eclipse.milo.platform.runtime.RunTime;
import org.slf4j.LoggerFactory;

public class BootLoader extends AbstractLifecycle {
    private final UaNodeContext uaNodeContext;

    public BootLoader(UaNodeContext uaNodeContext) {
        this.uaNodeContext = uaNodeContext;
    }

    @Override
    protected void onStartup() {
        try {
            new NodeLoader(uaNodeContext).load();
            new ReferenceLoader(uaNodeContext).load();
        } catch (Exception e) {
            LoggerFactory.getLogger(getClass()).error("[BootLoader] service, went through errors : {}", e.getMessage());
        }
    }


    @Override
    protected void onShutdown() {

    }
}
