package org.eclipse.milo.opcua.sdk.server.api.persistence.caches;

import com.google.gson.Gson;
import org.eclipse.milo.opcua.sdk.server.api.persistence.RedisCommunication;
import org.eclipse.milo.opcua.sdk.server.api.persistence.RedisConstant;
import org.eclipse.milo.opcua.sdk.server.api.persistence.mapper.CacheMapper;
import org.eclipse.milo.opcua.sdk.server.model.nodes.objects.AlarmConditionTypeNode;
import org.eclipse.milo.opcua.sdk.server.nodes.serializable.SerializableAlarmConditionTypeNode;
import org.eclipse.milo.opcua.sdk.server.util.StringUtils;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class EventFactoryCache {
    private RedisCommunication redisCommunication;


    public EventFactoryCache(RedisCommunication redisCommunication) {
        try {
            this.redisCommunication = redisCommunication;
        } catch (Exception e) {
            LoggerFactory.getLogger(getClass()).error("Error creating AddressSpaceCache.");
        }
    }

    public List<SerializableAlarmConditionTypeNode> find(Map<String, List<String>> orMap, Map<String, String> andMap, Long offset, Long limit) {
        Map<String, SerializableAlarmConditionTypeNode> map = new HashMap<>();
        StringBuilder filter = new StringBuilder();
        filter.append(String.format(RedisConstant.REDIS_NUMBER_PARAM, "time", andMap.get("from"), andMap.get("to")));
        for (Map.Entry<String, String> entry : andMap.entrySet()) {
            String key = entry.getKey();
            if (key.equals("severity"))
                filter.append(String.format(RedisConstant.REDIS_NUMBER_PARAM, entry.getKey(), entry.getValue(), entry.getValue()));
            else if (!key.equals("from") && !key.equals("to"))
                filter.append(String.format("@%s:{%s} ", entry.getKey(), StringUtils.redisCliParameter(entry.getValue())));
        }
        if (orMap.size() == 0) {
            map.putAll(new CacheMapper().FTSearchEventMapper(
                    this.redisCommunication.exec(new String[]{
                            RedisConstant.SEARCH_COMMAND,
                            RedisConstant.EVENT_NODE_INDEX,
                            filter.toString(),
                            "LIMIT",
                            String.valueOf(offset),
                            String.valueOf(limit)
                    })));
        } else {
            for (Map.Entry<String, List<String>> element : orMap.entrySet()) {
                filter.append(String.format("@%s:{%s} ", element.getKey(), StringUtils.redisCliParameter(String.join("|", element.getValue()))));
                map.putAll(new CacheMapper().FTSearchEventMapper(
                        this.redisCommunication.exec(new String[]{
                                RedisConstant.SEARCH_COMMAND,
                                RedisConstant.EVENT_NODE_INDEX,
                                filter.toString(),
                                "LIMIT",
                                String.valueOf(offset),
                                String.valueOf(limit)
                        })));
            }
        }
        return map.values().stream().sorted(Comparator.comparing(SerializableAlarmConditionTypeNode::getTime)).collect(Collectors.toList());
    }

    public List<SerializableAlarmConditionTypeNode> findByEventId(String eventId) {
        Map<String, SerializableAlarmConditionTypeNode> map = findMapByEventId(eventId);
        return new ArrayList<>(map.values());
    }

    public Map<String, SerializableAlarmConditionTypeNode> findMapByEventId(String eventId) {
        return new CacheMapper().FTSearchEventMapper(
                redisCommunication.exec(new String[]{
                        RedisConstant.SEARCH_COMMAND,
                        RedisConstant.EVENT_NODE_INDEX,
                        "@eventId:{" + StringUtils.redisCliParameter(eventId) + "}"
                }));
    }

    public List<SerializableAlarmConditionTypeNode> findBySourceNode(String sourceNode) {
        Map<String, SerializableAlarmConditionTypeNode> map = findMapByEventId(sourceNode);
        return new ArrayList<>(map.values());
    }

    public SerializableAlarmConditionTypeNode findNewestBySourceNode(String sourceNode) {
        Map<String, SerializableAlarmConditionTypeNode> mapValue = new CacheMapper().FTSearchEventMapper(
                this.redisCommunication.exec(new String[]{
                        RedisConstant.SEARCH_COMMAND,
                        RedisConstant.EVENT_NODE_INDEX,
                        "@sourceNode:{" + StringUtils.redisCliParameter(sourceNode) + "}",
                        "SORTBY", "time", "DESC",
                        "LIMIT",
                        "0",
                        "1"

                }), true);
        if (mapValue.size() == 0)
            return null;
        else
            return new ArrayList<>(mapValue.values()).get(0);
    }

    public Map<String, SerializableAlarmConditionTypeNode> findMapBySourceNode(String sourceNode) {
        return new CacheMapper().FTSearchEventMapper(
                redisCommunication.exec(new String[]{
                        RedisConstant.SEARCH_COMMAND,
                        RedisConstant.EVENT_NODE_INDEX,
                        "@sourceNode:{" + StringUtils.redisCliParameter(sourceNode) + "}"
                }));
    }

    public void save(AlarmConditionTypeNode event) {
        this.redisCommunication.getClient().jsonSet("AlarmConditionTypeNodeEvent:" + event.getSourceNode().getIdentifier() + ":" + UUID.randomUUID(), serializedAlarmConditionTypeNodeEvent(event));
    }

    public void remove(String eventId) {
        List<String> keylist = new ArrayList<>(findMapByEventId(eventId).keySet());
        String[] array = keylist.toArray(new String[0]);
        this.redisCommunication.getClient().del(array);

    }

    public void save(String key, SerializableAlarmConditionTypeNode event) {
        Gson gson = new Gson();
        this.redisCommunication.getClient().jsonSet(key, gson.toJson(event));
    }

    private String serializedAlarmConditionTypeNodeEvent(AlarmConditionTypeNode event) {
        SerializableAlarmConditionTypeNode serializableAlarmConditionTypeNode = new SerializableAlarmConditionTypeNode(event);
        Gson gson = new Gson();
        return gson.toJson(serializableAlarmConditionTypeNode);
    }

}
