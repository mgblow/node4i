package org.eclipse.milo.opcua.sdk.server.api.persistence.caches;

import io.netty.util.internal.StringUtil;
import org.eclipse.milo.opcua.sdk.core.Reference;
import org.eclipse.milo.opcua.sdk.core.nodes.Node;
import org.eclipse.milo.opcua.sdk.server.api.persistence.RedisCommunication;
import org.eclipse.milo.opcua.sdk.server.api.persistence.RedisConstant;
import org.eclipse.milo.opcua.sdk.server.api.persistence.mapper.CacheMapper;
import org.eclipse.milo.opcua.sdk.server.nodes.serializable.*;
import org.eclipse.milo.opcua.sdk.server.util.Props;
import org.eclipse.milo.opcua.sdk.server.util.StringUtils;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.enumerated.NodeClass;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class NodeFactoryCache {
    private RedisCommunication redisCommunication;
    private CacheMapper cacheMapper;
    public static final int APP_NAMESPACE_INDEX = Integer.parseInt(Props.getProperty("app-namespace-index").toString());
    public static final String APP_NAME = Props.getProperty("app-name").toString();

    public NodeFactoryCache(RedisCommunication redisCommunication) {
        try {
            this.redisCommunication = redisCommunication;
            this.cacheMapper = new CacheMapper();
        } catch (Exception e) {
            LoggerFactory.getLogger(getClass()).error("Error creating AddressSpaceCache.");
        }
    }

    public void addNode(Node node) {
        try {
            SerializableNodeId serializableNodeId = new SerializableNodeId(node.getNodeId());
            switch (node.getNodeClass()) {
                case Variable:
                    SerializableUaVariableNode serializableUaVariableNode = new SerializableUaVariableNode(node);
                    if (serializableUaVariableNode.getDataType().equals(Identifiers.Argument)) return;
                    this.redisCommunication.getClient().jsonSet("UaNode:" + serializableNodeId.getIdentifier(), serializableUaVariableNode.toJson());
                    break;
                case Object:
                    this.redisCommunication.getClient().jsonSet("UaNode:" + serializableNodeId.getIdentifier(), new SerializableUaObjectNode(node).toJson());
                    break;
                default:
                    this.redisCommunication.getClient().jsonSet("UaNode:" + serializableNodeId.getIdentifier(), new SerializableUaNode(node).toJson());
            }
        } catch (Exception e) {
            LoggerFactory.getLogger(getClass()).error("error on persisting the node : " + node.getNodeId());
        }
    }

    public void addNode(String identifier, String value) {
        try {

            this.redisCommunication.getClient().jsonSet("UaNode:" + identifier, value);

        } catch (Exception e) {
            LoggerFactory.getLogger(getClass()).error("error on persisting the node : " + identifier);
        }
    }

    public void removeNode(NodeId nodeId) {
        try {
            SerializableNodeId serializableNodeId = new SerializableNodeId(nodeId);
            this.redisCommunication.getClient().del("UaNode:" + serializableNodeId.getIdentifier());
        } catch (Exception e) {
            LoggerFactory.getLogger(getClass()).error("error on removing the node : " + nodeId);
        }

    }

    public void addReference(Reference reference) {
        try {
            SerializableReference serializableReference = new SerializableReference(reference);
            this.redisCommunication.getClient().jsonSet("UaReference:" + serializableReference.getUniqueIdentifier(), serializableReference.toJson());
        } catch (Exception e) {
            LoggerFactory.getLogger(getClass()).error("error on persisting the reference for : " + reference.getSourceNodeId());
        }
    }

    public void addReference(String key, String value) {
        try {
            this.redisCommunication.getClient().jsonSet(key, value);
        } catch (Exception e) {
            LoggerFactory.getLogger(getClass()).error("error on persisting the reference for : " + key);
        }
    }

    public void removeReference(Reference reference) {
        try {
            SerializableReference serializableReference = new SerializableReference(reference);
            this.redisCommunication.getClient().del("UaReference:" + serializableReference.getUniqueIdentifier());
        } catch (Exception e) {
            LoggerFactory.getLogger(getClass()).error("error on persisting the reference for : " + reference.getSourceNodeId());
        }
    }

    public SerializableUaNode getNode(NodeId nodeId) {
        try {
            return new CacheMapper().FTSearchUaNodeMapper(this.redisCommunication.exec(new String[]{
                    RedisConstant.SEARCH_COMMAND,
                    RedisConstant.UA_NODE_INDEX,
                    "@nodeIdIdentifier:{" + StringUtils.redisCliParameter(nodeId.getIdentifier().toString()) + "}" +
                            "@nodeIdNamespaceIndex:[" + nodeId.getNamespaceIndex().intValue() + " " + nodeId.getNamespaceIndex().intValue() + "]"
            })).get(0);
        } catch (Exception e) {
            LoggerFactory.getLogger(getClass()).error("can not get node with nodeId : " + nodeId);
            return null;
        }
    }

    public Map<SerializableNodeId, SerializableNode> findNodesByNamespaceIndex(int namespaceIndex) {
        Map<SerializableNodeId, SerializableNode> uaNodes = new HashMap<>();
        int limit = 10000;
        int offset = 0;
        while (true) {
            Map<SerializableNodeId, SerializableNode> subUaNodes = findNodesByNamespaceIndex(namespaceIndex, offset, limit);
            if (subUaNodes != null && subUaNodes.size() > 0) {
                uaNodes.putAll(subUaNodes);
                offset = offset + limit;
            } else {
                break;
            }
        }
        return uaNodes;
    }

    public Map<SerializableNodeId, SerializableNode> findNodesByNamespaceIndex(int namespaceIndex, int offset, int limit) {
        try {
            return this.cacheMapper.nodesMapper(this.redisCommunication.exec(new String[]{
                    RedisConstant.SEARCH_COMMAND,
                    RedisConstant.UA_NODE_INDEX,
                    "@nodeIdNamespaceIndex:[" + namespaceIndex + " " + namespaceIndex + "]",
                    "LIMIT",
                    String.valueOf(offset),
                    String.valueOf(limit)

            }));
        } catch (Exception e) {
            LoggerFactory.getLogger(getClass()).error("can not findNodes() with namespaceIndex : " + namespaceIndex);
            return null;
        }
    }

    public List<SerializableHistorian> findSimpleArchive(List<String> identifiers, Long startTime, Long endTime, Long offset, Long limit) {
        final List<SerializableHistorian> historians =
                this.cacheMapper.simpleArchiveMapper
                        (
                                this.redisCommunication.exec
                                        (new String[]{RedisConstant.SEARCH_COMMAND, RedisConstant.SIMPLE_ARCHIVE_INDEX,
                                                "@identifier:{" + StringUtils.redisCliParameter(String.join("|", identifiers)) + "} @time:[" + startTime + " " + endTime + "]",
                                                "LIMIT",
                                                String.valueOf(offset),
                                                String.valueOf(limit)}));
        return historians.stream().sorted(Comparator.comparing(SerializableHistorian::getTime)).collect(Collectors.toList());
    }


    public List<SerializableUaNode> findGeneralComponents(Long offset, Long limit) {
        String conditionString = "@nodeIdIdentifier:{" + StringUtils.redisCliParameter("*/Runtime/General*") + "}" +
                "@nodeClass:[" + NodeClass.Object.getValue() + " " + NodeClass.Object.getValue() + "] ";
        return find(conditionString, offset, limit);

    }

    public List<SerializableUaNode> findWindows(Long offset, Long limit) {
        String conditionString = "@nodeIdIdentifier:{" + StringUtils.redisCliParameter("*/Runtime/Windowing*") + "}" +
                "@nodeClass:[" + NodeClass.Object.getValue() + " " + NodeClass.Object.getValue() + "] ";
        return find(conditionString, offset, limit);
    }

    public List<SerializableUaNode> findEvents(Long offset, Long limit) {
        String conditionString = "@nodeIdIdentifier:{" + StringUtils.redisCliParameter("*/Alarm&Events/*") + "}" +
                "@nodeClass:[" + NodeClass.Object.getValue() + " " + NodeClass.Object.getValue() + "] " +
                "-@nodeIdIdentifier:{*" + StringUtils.redisCliParameter("/Alarm&Events/Groups") + "*} " +
                "-@nodeIdIdentifier:{*" + StringUtils.redisCliParameter("/Alarm&Events/ConditionClasses") + "*}" +
                "-@displayName:{Interfaces} ";
        return find(conditionString, offset, limit);
    }

    public List<SerializableUaNode> findAll(String category) {
        List<SerializableUaNode> all = new ArrayList<>();
        Long limit = 10000l;
        Long offset = 0l;
        while (true) {
            List<SerializableUaNode> sub = null;
            if (category.equals("Alarm"))
                sub = findEvents(offset, limit);
            else if (category.equals("Windowing"))
                sub = findWindows(offset, limit);
            else if (category.equals("Component"))
                sub = findGeneralComponents(offset, limit);
            if (sub != null && sub.size() > 0) {
                all.addAll(sub);
                offset = offset + limit;
            } else {
                break;
            }
        }
        return all;
    }

    public List<SerializableComponent> findComponents(Long offset, Long limit) {
        return this.cacheMapper.componentMapper
                (
                        this.redisCommunication.exec
                                (new String[]{RedisConstant.SEARCH_COMMAND, RedisConstant.UA_NODE_INDEX,
                                        "@nodeIdIdentifier:{" + StringUtils.redisCliParameter("*/Runtime/General*") + "}"
                                                +
                                                "@nodeClass:[" + NodeClass.Object.getValue() + " " + NodeClass.Object.getValue() + "] ",
                                        "LIMIT",
                                        String.valueOf(offset),
                                        String.valueOf(limit)}));
    }

    public List<SerializableUaNode> find(String conditionString, Long offset, Long limit) {
        return this.cacheMapper.FTSearchUaNodeMapper(
                this.redisCommunication.exec
                        (new String[]{RedisConstant.SEARCH_COMMAND, RedisConstant.UA_NODE_INDEX,
                                conditionString,
                                "LIMIT",
                                String.valueOf(offset),
                                String.valueOf(limit)}));
    }

    public Object searchUaNode(SerializableUaNode node, int offset, int limit) {
        try {
            String filter = "";
            if (!String.valueOf(node.getNodeId().getNamespaceIndex()).equals(null)) {
                filter += "@nodeIdNamespaceIndex:[" + +node.getNodeId().getNamespaceIndex() + " " + node.getNodeId().getNamespaceIndex() + "] ";
            }
            if (!node.getNodeId().getIdentifier().equals(null)) {
                filter += "@nodeIdNamespaceIndex:[" + +node.getNodeId().getNamespaceIndex() + " " + node.getNodeId().getNamespaceIndex() + "] ";
            }
            if (!String.valueOf(node.getNodeClass()).equals(null)) {
                filter += "@nodeCLass:[" + +node.getNodeClass() + " " + node.getNodeClass() + "] ";
            }
            if (!node.getDisplayName().equals(null)) {
                filter += "@displayName:(" + node.getDisplayName() + ") ";
            }
            if (!node.getBrowseName().equals(null)) {
                filter += "@browseName:(" + node.getBrowseName() + ") ";
            }
            if (!node.getDescription().equals(null)) {
                filter += "@discription:(" + node.getDescription() + ") ";
            }
            String[] query = new String[]{
                    RedisConstant.SEARCH_COMMAND,
                    RedisConstant.UA_NODE_INDEX,
                    filter,
                    "LIMIT",
                    String.valueOf(offset),
                    String.valueOf(limit)

            };
            return this.redisCommunication.exec(query);
        } catch (Exception e) {
            LoggerFactory.getLogger(getClass()).error("can not get node with nodeId : ");
            return null;
        }
    }


    public Map<String, SerializableReference> findReferencesByNamespaceIndex(int namespaceIndex) {
        Map<String, SerializableReference> references = new HashMap<>();
        int limit = 10000;
        int offset = 0;
        while (true) {
            Map<String, SerializableReference> subReferences = findReferencesByNamespaceIndex(namespaceIndex, offset, limit);
            if (subReferences != null && subReferences.size() > 0) {
                references.putAll(subReferences);
                offset = offset + limit;
            } else {
                break;
            }
        }
        return references;
    }

    public Map<String, SerializableReference> findReferencesByNamespaceIndex(int namespaceIndex, int offset, int limit) {
        try {
            return this.cacheMapper.referencesMapper(this.redisCommunication.exec(new String[]{
                    RedisConstant.SEARCH_COMMAND,
                    RedisConstant.UA_REFERENCE_INDEX,
                    "@sourceNodeIdNamespaceIndex:[" + namespaceIndex + " " + namespaceIndex + "] " + "@targetNodeIdNamespaceIndex:[" + namespaceIndex + " " + namespaceIndex + "]",
                    "LIMIT",
                    String.valueOf(offset),
                    String.valueOf(limit)

            }));
        } catch (Exception e) {
            LoggerFactory.getLogger(getClass()).error("can not findNodes() with namespaceIndex : " + namespaceIndex);
            return null;
        }
    }

    public List<TreeNode> findSearchResults(String query, Long offset, Long limit) {
        query = StringUtils.redisCliParameter(query);
        List<SerializableUaNode> searchResults = new CacheMapper().FTSearchUaNodeMapper(this.redisCommunication.exec(new String[]{
                RedisConstant.SEARCH_COMMAND,
                RedisConstant.UA_NODE_INDEX,
                "(" +
                        "(@nodeIdIdentifier:{*" + query + "*})" +
                        "|(@description:{*" + query + "*}) " +
                        "|(@displayName:{*" + query + "*})" +
                        ") " +
                        "@nodeIdNamespaceIndex:[" + APP_NAMESPACE_INDEX + " " + APP_NAMESPACE_INDEX + "] ",
                "LIMIT",
                String.valueOf(offset),
                String.valueOf(limit)
        }));
        List<TreeNode> searchTreeNodes = new ArrayList<>();
        searchResults.forEach(i -> searchTreeNodes.add(new TreeNode(i)));
        return searchTreeNodes;
    }

    public Map<String, SerializableReference> findAllForwardReferencesBySourceNodeIdentifier(String identifiers, Long offset, Long limit) {
        return this.cacheMapper.referencesMapper(this.redisCommunication.exec(new String[]{
                RedisConstant.SEARCH_COMMAND,
                RedisConstant.UA_REFERENCE_INDEX,
                "@sourceNodeIdIdIdentifier:{" + StringUtils.redisCliParameter(identifiers) + "} " +
                        "@targetNodeIdNamespaceIndex:[" + APP_NAMESPACE_INDEX + " " + APP_NAMESPACE_INDEX + "]" +
                        "@direction:{FORWARD}",
                "LIMIT",
                String.valueOf(offset),
                String.valueOf(limit)
        }));
    }

    public Map<String, SerializableReference> searchInForwardReferencesBySourceNodeIdentifier(String identifiers, String query, Long offset, Long limit) {
        return this.cacheMapper.referencesMapper(this.redisCommunication.exec(new String[]{
                RedisConstant.SEARCH_COMMAND,
                RedisConstant.UA_REFERENCE_INDEX,
                "@sourceNodeIdIdIdentifier:{" + StringUtils.redisCliParameter(identifiers) + "} " +
                        "@targetNodeIdIdIdentifier:{*" + StringUtils.redisCliParameter(query) + "*} " +
                        "@targetNodeIdNamespaceIndex:[" + APP_NAMESPACE_INDEX + " " + APP_NAMESPACE_INDEX + "]" +
                        "@direction:{FORWARD}",
                "LIMIT",
                String.valueOf(offset),
                String.valueOf(limit)
        }));
    }

    public List<SerializableReference> findInverseReference(String identifier) {
        try {
            Map<String, SerializableReference> map = this.cacheMapper.referencesMapper(this.redisCommunication.exec(new String[]{
                    RedisConstant.SEARCH_COMMAND,
                    RedisConstant.UA_REFERENCE_INDEX,
                    "@sourceNodeIdIdIdentifier:{" + StringUtils.redisCliParameter(identifier) + "} " +
                            "@direction:{INVERSE}"
            }));
            return new ArrayList<SerializableReference>(map.values());
        } catch (Exception e) {
            LoggerFactory.getLogger(getClass()).error("error in find inverse reference for identifier: " + identifier);
            return null;
        }
    }

    public Map<String, SerializableUaNode> findSerializeUaNodes(List<String> identifiers, int namespaceIndex) {
        Map<String, SerializableUaNode> uaNodes = new HashMap<>();
        Long limit = 10000l;
        Long offset = 0l;
        while (true) {
            Map<String, SerializableUaNode> subUaNodes = findSerializeUaNodes(identifiers, namespaceIndex, offset, limit, true);
            if (subUaNodes != null && subUaNodes.size() > 0) {
                uaNodes.putAll(subUaNodes);
                offset = offset + limit;
            } else {
                break;
            }
        }
        return uaNodes;
    }

    public List<SerializableUaNode> findSerializeUaNodes(List<String> identifiers, int namespaceIndex, Long offset, Long limit) {
        return new CacheMapper().FTSearchUaNodeMapper(executeFindNodeQuery(identifiers, namespaceIndex, offset, limit));
    }

    public Map<String, SerializableUaNode> findSerializeUaNodes(List<String> identifiers, int namespaceIndex, Long offset, Long limit, boolean flag) {
        return new CacheMapper().FTSearchUaNodeMapper(executeFindNodeQuery(identifiers, namespaceIndex, offset, limit), true);
    }

    public Object executeFindNodeQuery(List<String> identifiers, int namespaceIndex, Long offset, Long limit) {
        String[] arrayString = new String[6];
        try {
            arrayString = new String[]{
                    RedisConstant.SEARCH_COMMAND,
                    RedisConstant.UA_NODE_INDEX,
                    "@nodeIdIdentifier:{" + StringUtils.redisCliParameter(String.join("|", identifiers)) + "}" +
                            "@nodeIdNamespaceIndex:[" + namespaceIndex + " " + namespaceIndex + "]",
                    "LIMIT",
                    String.valueOf(offset),
                    String.valueOf(limit)
            };
            return this.redisCommunication.exec(arrayString);
        } catch (Exception e) {
            LoggerFactory.getLogger(getClass()).error("can not get nodes: " + String.join("", identifiers));
            LoggerFactory.getLogger(getClass()).error("fail search query : " + Arrays.toString(arrayString));
            return null;
        }
    }


    public List<TreeNode> filterTreeNode(String query, List<String> list, Long offset, Long limit, boolean removeUnWantedConditionClass, boolean removeUnWantedGroup) {

        String filterQuery = "";
        for (String element : list) {
            filterQuery += String.format("*%s*|", StringUtils.redisCliParameter(element));
        }
        String removeConditionClass = removeUnWantedConditionClass ? "-@nodeIdIdentifier:{*" + StringUtils.redisCliParameter("/ConditionClasses/") + "*}, " : "";
        String removeGroups = removeUnWantedGroup ? "-@nodeIdIdentifier:{*" + StringUtils.redisCliParameter("/Groups/") + "*}, " : "";
        String removeProperties = "-@nodeIdIdentifier:{*" + StringUtils.redisCliParameter("/Property/") + "*}, ";
        String queryString = "";
        if (!StringUtil.isNullOrEmpty(query)) {
            query = StringUtils.redisCliParameter(query);
            queryString = "(" +
                    "(@nodeIdIdentifier:{*" + query + "*})" +
                    "|(@description:{*" + query + "*}) " +
                    "|(@displayName:{*" + query + "*})" +
                    ")";
        }

        filterQuery = filterQuery.substring(0, filterQuery.length() - 1);
        List<SerializableUaNode> searchResults = new CacheMapper().FTSearchUaNodeMapper(this.redisCommunication.exec(new String[]{
                RedisConstant.SEARCH_COMMAND,
                RedisConstant.UA_NODE_INDEX,
                queryString +
                        "@nodeIdNamespaceIndex:[" + APP_NAMESPACE_INDEX + " " + APP_NAMESPACE_INDEX + "] " +
                        "@nodeIdIdentifier:{" + filterQuery + "} " +
                        "@nodeClass:[1 2] " +
                        removeConditionClass +
                        removeGroups +
                        removeProperties +
                        "-@displayName:{Config|Attributes|Devices|NoDevice|Alarm&Events|ConditionClasses|Runtime|Interfaces|Groups|InputArguments|OutputArguments} ",
                "LIMIT",
                String.valueOf(offset),
                String.valueOf(limit)
        }));
        List<TreeNode> searchTreeNodes = new ArrayList<>();
        searchResults.forEach(i -> searchTreeNodes.add(new TreeNode(i)));
        return searchTreeNodes;
    }

//    public List<TreeNode> filterNoDeviceVariableNode(String query, Long offset, Long limit) {
//        String browsQuery = "";
//        browsQuery += String.format("*%s*", StringUtils.redisCliParameter("/NoDevice/TAG/"));
//        String queryString = "";
//        if (!StringUtil.isNullOrEmpty(query)) {
//            query = StringUtils.redisCliParameter(query);
//            queryString = "(" +
//                    "(@nodeIdIdentifier:{*" + query + "*})" +
//                    "|(@description:{*" + query + "*}) " +
//                    "|(@displayName:{*" + query + "*})" +
//                    ")";
//        }
//        List<SerializableUaNode> searchResults = new CacheMapper().FTSearchUaNodeMapper(this.redisCommunication.exec(new String[]{
//                RedisConstant.SEARCH_COMMAND,
//                RedisConstant.UA_NODE_INDEX,
//                queryString +
//                        "@nodeIdNamespaceIndex:[" + APP_NAMESPACE_INDEX + " " + APP_NAMESPACE_INDEX + "] " +
//                        "@nodeIdIdentifier:{" + browsQuery + "} " +
//                        "@nodeClass:[2 2] ",
//                "LIMIT",
//                String.valueOf(offset),
//                String.valueOf(limit)
//        }));
//        List<TreeNode> searchTreeNodes = new ArrayList<>();
//        searchResults.forEach(i -> searchTreeNodes.add(new TreeNode(i)));
//        return searchTreeNodes;
//    }

}
