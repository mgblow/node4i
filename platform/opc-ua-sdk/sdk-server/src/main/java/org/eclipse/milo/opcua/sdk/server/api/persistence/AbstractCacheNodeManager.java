package org.eclipse.milo.opcua.sdk.server.api.persistence;

import org.eclipse.milo.opcua.sdk.server.OpcUaServer;
import org.eclipse.milo.opcua.sdk.server.api.persistence.caches.EventFactoryCache;
import org.eclipse.milo.opcua.sdk.server.api.persistence.caches.NodeFactoryCache;
import org.eclipse.milo.opcua.sdk.server.api.persistence.caches.TriggerParentFactoryCache;
import org.slf4j.LoggerFactory;

public class AbstractCacheNodeManager {
    private RedisCommunication redisCommunication;
    NodeFactoryCache nodeFactoryCache;
    EventFactoryCache eventFactoryCache;
    TriggerParentFactoryCache triggerParentFactoryCache;

    public AbstractCacheNodeManager(OpcUaServer server) {
        try {
            this.redisCommunication = new RedisCommunication(server);
            new Schema(this.redisCommunication).build();
            this.nodeFactoryCache = new NodeFactoryCache(this.redisCommunication);
            this.eventFactoryCache = new EventFactoryCache(this.redisCommunication);
            this.triggerParentFactoryCache = new TriggerParentFactoryCache(this.redisCommunication);
        } catch (Exception e) {
            LoggerFactory.getLogger(getClass()).error("Error connection to redis at address : [" + server.getConfig().getRedisUri() + "], because of : " + e.getMessage());
            Runtime.getRuntime().exit(0);
        }
    }

    public NodeFactoryCache getNodeFactoryCache() {
        return this.nodeFactoryCache;
    }

    public TriggerParentFactoryCache getTriggerParentFactoryCache() {
        return triggerParentFactoryCache;
    }
}
