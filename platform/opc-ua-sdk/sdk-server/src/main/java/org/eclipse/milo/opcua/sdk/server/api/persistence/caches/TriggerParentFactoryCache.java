package org.eclipse.milo.opcua.sdk.server.api.persistence.caches;

import org.eclipse.milo.opcua.sdk.server.api.persistence.RedisCommunication;
import org.eclipse.milo.opcua.sdk.server.api.persistence.RedisConstant;
import org.eclipse.milo.opcua.sdk.server.api.persistence.mapper.CacheMapper;
import org.eclipse.milo.opcua.sdk.server.nodes.serializable.SerializableParentTrigger;
import org.eclipse.milo.opcua.sdk.server.nodes.serializable.SerializableTriggerParent;
import org.eclipse.milo.opcua.sdk.server.util.StringUtils;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

public class TriggerParentFactoryCache {
    private RedisCommunication redisCommunication;
    private CacheMapper cacheMapper;

    public TriggerParentFactoryCache(RedisCommunication redisCommunication) {
        try {
            this.redisCommunication = redisCommunication;
            this.cacheMapper = new CacheMapper();
        } catch (Exception e) {
            LoggerFactory.getLogger(getClass()).error("Error creating AddressSpaceCache.");
        }
    }

    public void addTriggerParents(SerializableTriggerParent serializableTriggerParent) {
        try {
            final SerializableTriggerParent triggerParent = findTriggerParentsByTriggerIdentifier(serializableTriggerParent.getIdentifier());
            triggerParent.setIdentifier(serializableTriggerParent.getIdentifier());
            if (triggerParent.getParents() != null) {
                triggerParent.addParent(serializableTriggerParent.getParents().get(0));
            } else {
                triggerParent.setParents(Arrays.asList(serializableTriggerParent.getParents().get(0)));
            }
            this.redisCommunication.getClient().jsonSet("TriggerParent:" + serializableTriggerParent.getIdentifier(), triggerParent.toJson());
        } catch (Exception e) {
            LoggerFactory.getLogger(getClass()).error("error on persisting the reference for : " + serializableTriggerParent.getIdentifier());
        }
    }

    public void deleteParentFromTriggerParents(String triggerIdentifier, String parentIdentifier) {
        try {
            final SerializableTriggerParent triggerParent = findTriggerParentsByTriggerIdentifier(triggerIdentifier);
            triggerParent.setIdentifier(triggerIdentifier);
            final List<String> parents = triggerParent.getParents();
            parents.remove(parentIdentifier);
            if (!parents.isEmpty()) {
                triggerParent.setParents(parents);
                this.redisCommunication.getClient().jsonSet("TriggerParent:" + triggerIdentifier, triggerParent.toJson());
            } else {
                this.redisCommunication.getClient().del("TriggerParent:" + triggerIdentifier);
            }
        } catch (Exception e) {
            LoggerFactory.getLogger(getClass()).error("error on persisting the reference for : " + triggerIdentifier);
        }
    }

    public void deleteParent(String parentIdentifier) {
        try {
            SerializableParentTrigger parentTriggersByParentIdentifier = findParentTriggersByParentIdentifier(parentIdentifier);
            for (String trigger : parentTriggersByParentIdentifier.getTriggers()) {
                this.deleteParentFromTriggerParents(trigger, parentIdentifier);
            }
            this.redisCommunication.getClient().del("ParentTrigger:" + parentIdentifier);
        } catch (Exception e) {
            LoggerFactory.getLogger(getClass()).error("error on persisting the reference for : " + parentIdentifier);
        }
    }

    public void addParentTrigger(SerializableParentTrigger serializableParentTrigger) {
        try {
            this.redisCommunication.getClient().jsonSet("ParentTrigger:" + serializableParentTrigger.getIdentifier(), serializableParentTrigger.toJson());
        } catch (Exception e) {
            LoggerFactory.getLogger(getClass()).error("error on persisting the reference for : " + serializableParentTrigger.getIdentifier());
        }
    }

    public SerializableTriggerParent findTriggerParentsByTriggerIdentifier(String identifier) {
        try {
            SerializableTriggerParent serializableTriggerParent = this.cacheMapper.triggerParentMapper(this.redisCommunication.exec(new String[]{RedisConstant.SEARCH_COMMAND, RedisConstant.TRIGGER_PARENT_INDEX, "@identifier:{" + StringUtils.redisCliParameter(identifier) + "}"}));
            return serializableTriggerParent;
        } catch (Exception e) {
            LoggerFactory.getLogger(getClass()).error("can not findNodes() in trigger");
            return null;
        }
    }

    public SerializableParentTrigger findParentTriggersByParentIdentifier(String identifier) {
        try {
            SerializableParentTrigger serializableParentTrigger = this.cacheMapper.parentTriggerMapper(this.redisCommunication.exec(new String[]{RedisConstant.SEARCH_COMMAND, RedisConstant.PARENT_TRIGGER_INDEX, "@identifier:{" + StringUtils.redisCliParameter(identifier) + "}"}));
            return serializableParentTrigger;
        } catch (Exception e) {
            LoggerFactory.getLogger(getClass()).error("can not findNodes() in trigger");
            return null;
        }
    }
}
