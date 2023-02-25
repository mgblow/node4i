package org.eclipse.milo.platform.runtime.observers;

import org.eclipse.milo.opcua.sdk.server.nodes.AttributeObserver;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNodeContext;
import org.eclipse.milo.opcua.sdk.server.nodes.UaVariableNode;
import org.eclipse.milo.opcua.sdk.server.nodes.serializable.SerializableWindowingNode;
import org.eclipse.milo.opcua.stack.core.AttributeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.platform.runtime.interfaces.Window;
import org.eclipse.milo.platform.util.Utils;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class JavascriptWindowingRuntimeObserver implements AttributeObserver {
    private final Context runtime = Context.newBuilder("js").allowAllAccess(true).allowIO(true).build();
    String mainNode;
    Value jsBinding;
    long startTime;
    long windowingLength;
    Window window;
    Long counter = 1L;
    Long sampleInterval;
    String[] nodesToWindow;
    UaNodeContext uaNodeContext;
    NodeId outPutNodeId;

    public JavascriptWindowingRuntimeObserver(UaNodeContext uaNodeContext, String mainNode, Long windowingLength, Long sampleInterval, String[] nodesToWindow, NodeId outPutNodeId) {
        this.mainNode = mainNode;
        jsBinding = runtime.getBindings("js");
        jsBinding.putMember("window", this.window);
        this.window = Window.getInstance();
        startTime = System.currentTimeMillis();
        this.windowingLength = windowingLength;
        this.sampleInterval = sampleInterval;
        this.nodesToWindow = nodesToWindow;
        this.uaNodeContext = uaNodeContext;
        this.outPutNodeId = outPutNodeId;
    }

    @Override
    public void attributeChanged(UaNode node, AttributeId attributeId, Object value) {
        if (counter == (sampleInterval / 100)) {
            Arrays.stream(nodesToWindow).forEach(item -> {
                        final UaVariableNode uaVariableNode = (UaVariableNode) uaNodeContext.getNodeManager().get(Utils.newNodeId(item));
                        HashMap<String, List<SerializableWindowingNode>> valuesMap = this.window.getValuesMap(mainNode);
                        if (valuesMap == null) {
                            valuesMap = new HashMap<>();
                        }
                        if (valuesMap.get(item) == null) {
                            valuesMap.put(item, new ArrayList<>());
                        }
                        List<SerializableWindowingNode> strings = valuesMap.get(item);
                        strings.add(new SerializableWindowingNode(uaVariableNode.getValue().getValue().getValue().toString(), System.currentTimeMillis()));

                        valuesMap.put(item, strings);
                        this.window.putValuesMap(mainNode, valuesMap);
                    }

            );
            counter = 1L;
        } else {
            counter++;
        }
        if (attributeId.equals(AttributeId.Value)) {
            if (System.currentTimeMillis() - startTime >= windowingLength) {
                // execute simple javascript
                try {
                    UaVariableNode uaVariableNode = (UaVariableNode) uaNodeContext.getNodeManager().get(outPutNodeId);
                    DataValue nodeValue = new DataValue(new Variant(window.values(mainNode)), StatusCode.GOOD);
                    uaVariableNode.setValue(nodeValue);
                } catch (Exception e) {
                    LoggerFactory.getLogger(getClass()).error("error running windowing {} with error {} ", mainNode, e.getMessage());
                } finally {
                    this.window.getValuesMap().remove(mainNode);
                    startTime = System.currentTimeMillis();
                }
            }
        }
    }
}
