package org.eclipse.milo.platform.boot.loaders;

import org.eclipse.milo.opcua.sdk.server.nodes.UaNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNodeContext;
import org.eclipse.milo.opcua.sdk.server.nodes.UaObjectNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaVariableNode;
import org.eclipse.milo.opcua.sdk.server.nodes.serializable.SerializableUaNode;
import org.eclipse.milo.opcua.sdk.server.nodes.serializable.SerializableUaVariableNode;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.platform.runtime.RunTime;
import org.eclipse.milo.platform.runtime.RuntimeIdentifier;
import org.eclipse.milo.platform.util.Props;
import org.eclipse.milo.platform.util.StringUtils;
import org.eclipse.milo.platform.util.Utils;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RuntimeLoader {
    public static String ROOT_APP_NAME = null;
    public static final int APP_NAMESPACE_INDEX = Integer.parseInt(Props.getProperty("app-namespace-index").toString());
    private final UaNodeContext uaNodeContext;
    private final RunTime runtime;

    public RuntimeLoader(UaNodeContext uaNodeContext, RunTime runtime) {
        this.uaNodeContext = uaNodeContext;
        ROOT_APP_NAME = uaNodeContext.getServer().getConfig().getApplicationName().getText();
        this.runtime = runtime;
    }

    public void load() {
        List<SerializableUaNode> windows = this.uaNodeContext.getNodeManager().cacheNodeManager().getNodeFactoryCache().findAll("Windowing");
        List<SerializableUaNode> generalComponents = this.uaNodeContext.getNodeManager().cacheNodeManager().getNodeFactoryCache().findAll("Component");
        if (!generalComponents.isEmpty())
            loadGeneralComponents(generalComponents);
        if (!windows.isEmpty())
            loadWindowing(windows);
    }

    public void load(String... ids) {
        List<String> generalIds = Arrays.stream(ids).filter(i -> i.contains("/Runtime/General/")).collect(Collectors.toList());
        List<String> windowIds = Arrays.stream(ids).filter(i -> i.contains("/Runtime/Windowing")).collect(Collectors.toList());
        Map<String, SerializableUaNode> windowMap = this.uaNodeContext.getNodeManager().cacheNodeManager().getNodeFactoryCache().findSerializeUaNodes(windowIds, APP_NAMESPACE_INDEX);
        Map<String, SerializableUaNode> generalMap = this.uaNodeContext.getNodeManager().cacheNodeManager().getNodeFactoryCache().findSerializeUaNodes(generalIds, APP_NAMESPACE_INDEX);
        List<SerializableUaNode> generalComponentList = new ArrayList<>(generalMap.values());
        List<SerializableUaNode> windows = new ArrayList<>(windowMap.values());
        loadGeneralComponents(generalComponentList);
        loadWindowing(windows);
    }

    private void loadWindowing(List<SerializableUaNode> windows) {
        try {
            LoggerFactory.getLogger(getClass()).info("[RuntimeLoader] started loading windowing ...");
            List<String> nodesToWindowIds = windows.stream().map(i -> RuntimeIdentifier.RUNTIME_PROPERTY.getIdentifier().replace("{property-name}", "nodesToWindow").replace("{runtime-identifier}", i.getNodeId().getIdentifier()))
                    .collect(Collectors.toList());
            List<String> nameIds = windows.stream().map(i -> RuntimeIdentifier.RUNTIME_PROPERTY.getIdentifier().replace("{property-name}", "name").replace("{runtime-identifier}", i.getNodeId().getIdentifier()))
                    .collect(Collectors.toList());
            List<String> samplingIntervalIds = windows.stream().map(i -> RuntimeIdentifier.RUNTIME_PROPERTY.getIdentifier().replace("{property-name}", "samplingInterval").replace("{runtime-identifier}", i.getNodeId().getIdentifier()))
                    .collect(Collectors.toList());
            List<String> windowingLengthIds = windows.stream().map(i -> RuntimeIdentifier.RUNTIME_PROPERTY.getIdentifier().replace("{property-name}", "windowingLength").replace("{runtime-identifier}", i.getNodeId().getIdentifier()))
                    .collect(Collectors.toList());
            Map<String, SerializableUaNode> nodesToWindowMap = this.uaNodeContext.getNodeManager().cacheNodeManager().getNodeFactoryCache().findSerializeUaNodes(nodesToWindowIds, APP_NAMESPACE_INDEX);
            Map<String, SerializableUaNode> nameMap = this.uaNodeContext.getNodeManager().cacheNodeManager().getNodeFactoryCache().findSerializeUaNodes(nameIds, APP_NAMESPACE_INDEX);
            Map<String, SerializableUaNode> samplingIntervalMap = this.uaNodeContext.getNodeManager().cacheNodeManager().getNodeFactoryCache().findSerializeUaNodes(samplingIntervalIds, APP_NAMESPACE_INDEX);
            Map<String, SerializableUaNode> windowingLengthMap = this.uaNodeContext.getNodeManager().cacheNodeManager().getNodeFactoryCache().findSerializeUaNodes(windowingLengthIds, APP_NAMESPACE_INDEX);
            windows.forEach(w -> {
                try {
                    String nodesToWindowValue = getSerializeMapValue(nodesToWindowMap, RuntimeIdentifier.RUNTIME_PROPERTY.getIdentifier().replace("{property-name}", "nodesToWindow").replace("{runtime-identifier}", w.getNodeId().getIdentifier()));

                    String nameValue = getSerializeMapValue(nameMap, RuntimeIdentifier.RUNTIME_PROPERTY.getIdentifier().replace("{property-name}", "name").replace("{runtime-identifier}", w.getNodeId().getIdentifier()));


                    String samplingIntervalValue = getSerializeMapValue(samplingIntervalMap, RuntimeIdentifier.RUNTIME_PROPERTY.getIdentifier().replace("{property-name}", "samplingInterval").replace("{runtime-identifier}", w.getNodeId().getIdentifier()));


                    String windowingLengthValue = getSerializeMapValue(windowingLengthMap, RuntimeIdentifier.RUNTIME_PROPERTY.getIdentifier().replace("{property-name}", "windowingLength").replace("{runtime-identifier}", w.getNodeId().getIdentifier()));


                    UaObjectNode windowingComponentNode = (UaObjectNode) this.uaNodeContext.getNodeManager().get(new NodeId(APP_NAMESPACE_INDEX, w.getNodeId().getIdentifier()));

                    addNodeToViewNodes(windowingComponentNode, nameValue, samplingIntervalValue, windowingLengthValue, Utils.fromJsonToArrayOfString(nodesToWindowValue), Utils.newNodeId(RuntimeIdentifier.RUNTIME_PROPERTY.getIdentifier().replace("{property-name}", "output").replace("{runtime-identifier}", w.getNodeId().getIdentifier())));
                } catch (Exception e) {
                    LoggerFactory.getLogger(getClass()).error("Error in run window " + w.getNodeId().getIdentifier());
                }
            });
        } catch (Exception e) {
            LoggerFactory.getLogger(getClass()).error("[Error in ] Run Windows");
        }
    }


    private void addNodeToViewNodes(UaObjectNode componentNode, String mainNode, String minimumSamplingInterval, String windowingLength, String[] nodesToWindow, NodeId outPutNode) {
        UaVariableNode node = (UaVariableNode) uaNodeContext.getNodeManager().get(Utils.newNodeId("Scheduled"));
        runtime.windowingLoad(node, componentNode, mainNode, Long.parseLong(windowingLength), Long.valueOf(minimumSamplingInterval), nodesToWindow, outPutNode);
    }

    public void loadGeneralComponents(List<SerializableUaNode> components) {
        try {
            LoggerFactory.getLogger(getClass()).info("[RuntimeLoader] started loading runtime scrips ...");
            List<String> scriptIdentifiers = components.stream().map(
                            i -> RuntimeIdentifier.RUNTIME_PROPERTY.getIdentifier().replace("{property-name}", "script").replace("{runtime-identifier}", i.getNodeId().getIdentifier()))
                    .collect(Collectors.toList());
            Map<String, SerializableUaNode> scriptMap = this.uaNodeContext.getNodeManager().cacheNodeManager().getNodeFactoryCache().findSerializeUaNodes(scriptIdentifiers, APP_NAMESPACE_INDEX);
            List<String> triggerNodeIdentifiers = components.stream().map(
                            i -> RuntimeIdentifier.RUNTIME_PROPERTY.getIdentifier().replace("{property-name}", "triggerNodes").replace("{runtime-identifier}", i.getNodeId().getIdentifier()))
                    .collect(Collectors.toList());
            Map<String, SerializableUaNode> triggerMap = this.uaNodeContext.getNodeManager().cacheNodeManager().getNodeFactoryCache().findSerializeUaNodes(triggerNodeIdentifiers, APP_NAMESPACE_INDEX);
            components.forEach(c -> {
                try {
                    String scriptValue = getSerializeMapValue(scriptMap,
                            RuntimeIdentifier.RUNTIME_PROPERTY.getIdentifier().replace("{property-name}", "script").replace("{runtime-identifier}", c.getNodeId().getIdentifier()));

                    String triggerValue = getSerializeMapValue(triggerMap,
                            RuntimeIdentifier.RUNTIME_PROPERTY.getIdentifier().replace("{property-name}", "triggerNodes").replace("{runtime-identifier}", c.getNodeId().getIdentifier()));
                        UaNode program = uaNodeContext.getNodeManager().get(new NodeId(APP_NAMESPACE_INDEX, c.getNodeId().getIdentifier()));
                        addRuntimeTriggerNodes(Utils.fromJsonToArrayOfString(triggerValue), scriptValue, program);
                } catch (Exception e) {
                    LoggerFactory.getLogger(getClass()).error("Error in run component " + c.getNodeId().getIdentifier());
                }
            });
        } catch (Exception e) {
            LoggerFactory.getLogger(getClass()).error("Error in run Components ...");

        }
    }

    private void addRuntimeTriggerNodes(String[] triggerNodes, String script, UaNode componentNode) {
        if (triggerNodes.length == 0) {
            UaVariableNode node = (UaVariableNode) uaNodeContext.getNodeManager().get(Utils.newNodeId("TIME"));
            runtime.load(node, componentNode, script);
        }
        for (String identifiers : triggerNodes) {
            UaVariableNode node = (UaVariableNode) uaNodeContext.getNodeManager().get(new NodeId(APP_NAMESPACE_INDEX, identifiers));
            runtime.load(node, componentNode, script);
        }
    }

    private String getSerializeMapValue(Map<String, SerializableUaNode> map, String identifier) {
        SerializableUaNode script = map.get("UaNode:" + identifier);
        return ((SerializableUaVariableNode) script).getValue();
    }
}
