package org.eclipse.milo.opcua.sdk.server.api.persistence;

public class RedisConstant {
    public static final String REDIS_NUMBER_PARAM = "@%s:[%s %s] ";
    public static final String SEARCH_COMMAND = "FT.SEARCH";
    public static final String PARENT_TRIGGER_INDEX = "ParentTriggerIndex";
    public static final String TRIGGER_PARENT_INDEX = "TriggerParentIndex";
    public static final String SIMPLE_ARCHIVE_INDEX = "simpleArchiveIndex";
    public static final String EVENT_NODE_INDEX = "UaEventNodeIndex";
    public static final String UA_NODE_INDEX = "UaNodeIndex";
    public static final String UA_REFERENCE_INDEX = "UaReferenceIndex";

    private RedisConstant() {
    }
}
