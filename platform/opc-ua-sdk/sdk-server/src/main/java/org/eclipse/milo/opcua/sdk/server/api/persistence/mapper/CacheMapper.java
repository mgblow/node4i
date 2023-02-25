package org.eclipse.milo.opcua.sdk.server.api.persistence.mapper;

import com.google.gson.Gson;
import org.eclipse.milo.opcua.sdk.server.nodes.serializable.*;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CacheMapper {
    private Gson gson = new Gson();

    public Map<String, SerializableReference> referencesMapper(Object result) {
        try {
            Map<String, SerializableReference> references = new HashMap<>();
            ArrayList<Object> listOfResults = (ArrayList<Object>) result;
            for (int i = 2; i < listOfResults.size(); i += 2) {
                ArrayList<String> jsonNode = (ArrayList<String>) listOfResults.get(i);
                SerializableReference reference = gson.fromJson(jsonNode.get(1), SerializableReference.class);
                references.put(listOfResults.get(i - 1).toString(), reference);
            }
            return references;
        } catch (Exception e) {
            LoggerFactory.getLogger(getClass()).error("error mapping the result of search for " + result);
            return null;
        }
    }

    public Map<SerializableNodeId, SerializableNode> nodesMapper(Object result) {
        try {
            Map<SerializableNodeId, SerializableNode> nodes = new HashMap<>();
            ArrayList<Object> listOfResults = (ArrayList<Object>) result;
            for (int i = 2; i < listOfResults.size(); i += 2) {
                ArrayList<String> jsonNode = (ArrayList<String>) listOfResults.get(i);
                SerializableUaNode node = gson.fromJson(jsonNode.get(1), SerializableUaNode.class);
                switch (node.getNodeClass()) {
                    case 2:
                        nodes.put(node.getNodeId(), gson.fromJson(jsonNode.get(1), SerializableUaVariableNode.class));
                        break;
                    case 1:
                        nodes.put(node.getNodeId(), gson.fromJson(jsonNode.get(1), SerializableUaObjectNode.class));
                        break;
                    default:
                        nodes.put(node.getNodeId(), node);
                        break;
                }
            }
            return nodes;
        } catch (Exception e) {
            LoggerFactory.getLogger(getClass()).error("error mapping the result of search for " + result);
            return null;
        }
    }

    public List<SerializableHistorian> simpleArchiveMapper(Object result) {
        List<SerializableHistorian> historiansOutput = new ArrayList<>();
        try {
            List<Object> historians = (ArrayList<Object>) result;

            for (int i = 2; i < historians.size(); i += 2) {
                ArrayList<String> jsonNode = (ArrayList<String>) historians.get(i);
                SerializableHistorian node = gson.fromJson(jsonNode.get(1), SerializableHistorian.class);
                historiansOutput.add(node);
            }
            return historiansOutput;
        } catch (Exception e) {
            LoggerFactory.getLogger(getClass()).error("error mapping the result of search for " + result);
            return null;
        }
    }

    public List<SerializableComponent> componentMapper(Object result) {
        List<SerializableComponent> serializableComponents = new ArrayList<>();
        try {
            List<Object> components = (ArrayList<Object>) result;

            for (int i = 2; i < components.size(); i += 2) {
                ArrayList<String> jsonNode = (ArrayList<String>) components.get(i);
                SerializableComponent node = gson.fromJson(jsonNode.get(1), SerializableComponent.class);
                serializableComponents.add(node);
            }
            return serializableComponents;
        } catch (Exception e) {
            LoggerFactory.getLogger(getClass()).error("error mapping the result of get components for " + result);
            return null;
        }
    }

    public SerializableTriggerParent triggerParentMapper(Object result) {
        SerializableTriggerParent serializableTriggerParent = new SerializableTriggerParent();
        try {
            List<Object> triggerParents = (ArrayList<Object>) result;

            for (int i = 2; i < triggerParents.size(); i += 2) {
                ArrayList<String> jsonNode = (ArrayList<String>) triggerParents.get(i);
                serializableTriggerParent = gson.fromJson(jsonNode.get(1), SerializableTriggerParent.class);
            }
            return serializableTriggerParent;
        } catch (Exception e) {
            LoggerFactory.getLogger(getClass()).error("error mapping the result of search for " + result);
            return null;
        }
    }

    public SerializableParentTrigger parentTriggerMapper(Object result) {
        SerializableParentTrigger serializableParentTrigger = new SerializableParentTrigger();
        try {
            List<Object> triggerParents = (ArrayList<Object>) result;

            for (int i = 2; i < triggerParents.size(); i += 2) {
                ArrayList<String> jsonNode = (ArrayList<String>) triggerParents.get(i);
                serializableParentTrigger = gson.fromJson(jsonNode.get(1), SerializableParentTrigger.class);
            }
            return serializableParentTrigger;
        } catch (Exception e) {
            LoggerFactory.getLogger(getClass()).error("error mapping the result of search for " + result);
            return null;
        }
    }

    //    public List<SerializableUaNode> FTSearchUaNodeMapper(Object result) {
//        try {
//            Map<SerializableNodeId, SerializableUaNode> nodes = new HashMap<>();
//            ArrayList<Object> listOfResults = (ArrayList<Object>) result;
//            ArrayList<String> jsonNode = (ArrayList<String>) listOfResults.get(2);
//            SerializableUaNode node = gson.fromJson(jsonNode.get(1), SerializableUaNode.class);
//            switch (node.getNodeClass()) {
//                case 2:
//                    return gson.fromJson(jsonNode.get(1), SerializableUaVariableNode.class);
//                case 1:
//                    return gson.fromJson(jsonNode.get(1), SerializableUaObjectNode.class);
//                default:
//                    return node;
//            }
//        } catch (Exception e) {
//            LoggerFactory.getLogger(getClass()).error("error mapping the result of search for " + result);
//            return null;
//        }
//    }
    public List<SerializableUaNode> FTSearchUaNodeMapper(Object result) {
        return new ArrayList<SerializableUaNode>(FTSearchUaNodeMapper(result, true).values());
    }

    public Map<String, SerializableUaNode> FTSearchUaNodeMapper(Object result, boolean resultInMap) {
        try {
            ArrayList<Object> listOfResults = (ArrayList<Object>) result;
            Map<String, SerializableUaNode> nodes = new HashMap<>();
            for (int i = 2; i < listOfResults.size(); i++) {
                if (i % 2 == 0) {
                    ArrayList<String> jsonNode = (ArrayList<String>) listOfResults.get(i);
                    SerializableUaNode node = gson.fromJson(jsonNode.get(1), SerializableUaNode.class);
                    switch (node.getNodeClass()) {
                        case 1:
                            node = gson.fromJson(jsonNode.get(1), SerializableUaObjectNode.class);
                            break;
                        case 2:
                            node = gson.fromJson(jsonNode.get(1), SerializableUaVariableNode.class);
                            break;
                    }
                    String key = listOfResults.get(i - 1).toString();
                    nodes.put(key, node);
                }
            }
            return nodes;

        } catch (Exception e) {
            LoggerFactory.getLogger(getClass()).error("error mapping the result of search for " + result);
            return null;
        }
    }


    public Map<String, SerializableAlarmConditionTypeNode> FTSearchEventMapper(Object result) {
        try {
            ArrayList<Object> listOfResults = (ArrayList<Object>) result;
            Map<String, SerializableAlarmConditionTypeNode> events = new HashMap<>();
            for (int i = 2; i < listOfResults.size(); i++) {
                if (i % 2 == 0) {
                    ArrayList<String> jsonNode = (ArrayList<String>) listOfResults.get(i);
                    SerializableAlarmConditionTypeNode event = gson.fromJson(jsonNode.get(1), SerializableAlarmConditionTypeNode.class);
                    String key = listOfResults.get(i - 1).toString();
                    events.put(key, event);
                }
            }
            return events;
        } catch (Exception e) {
            LoggerFactory.getLogger(getClass()).error("error mapping the result of search for " + result);
            return null;
        }
    }
    public Map<String, SerializableAlarmConditionTypeNode> FTSearchEventMapper(Object result , Boolean flag) {
        try {
            ArrayList<Object> listOfResults = (ArrayList<Object>) result;
            Map<String, SerializableAlarmConditionTypeNode> events = new HashMap<>();
            for (int i = 2; i < listOfResults.size(); i++) {
                if (i % 2 == 0) {
                    ArrayList<String> jsonNode = (ArrayList<String>) listOfResults.get(i);
                    SerializableAlarmConditionTypeNode event = gson.fromJson(jsonNode.get(3), SerializableAlarmConditionTypeNode.class);
                    String key = listOfResults.get(i - 1).toString();
                    events.put(key, event);
                }
            }
            return events;
        } catch (Exception e) {
            LoggerFactory.getLogger(getClass()).error("error mapping the result of search for " + result);
            return null;
        }
    }
}
