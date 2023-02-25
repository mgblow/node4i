package org.eclipse.milo.platform.runtime.interfaces;

import com.google.gson.Gson;
import org.eclipse.milo.opcua.sdk.server.nodes.serializable.SerializableWindowingNode;
import org.eclipse.milo.platform.gateway.interfaces.OpcUaDeviceClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Window implements OpcUaDeviceClient {
    private static Window window;
    Map<String, HashMap<String, List<SerializableWindowingNode>>> valuesMap = new HashMap<>();
    Gson gson = new Gson();

    public static Window getInstance() {
        if (window == null) {
            window = new Window();
        }
        return window;
    }

    public Map<String, HashMap<String, List<SerializableWindowingNode>>> getValuesMap() {
        return valuesMap;
    }

    public HashMap<String, List<SerializableWindowingNode>> getValuesMap(String key) {
        return valuesMap.get(key);
    }

    public String values(String key) {
        return gson.toJson(valuesMap.get(key));
    }

    public void putValuesMap(String key, HashMap<String, List<SerializableWindowingNode>> values) {
        valuesMap.put(key, values);
    }

    public String values() {
        return gson.toJson(valuesMap);
    }

    public void setValuesMap(Map<String, HashMap<String, List<SerializableWindowingNode>>> valuesMap) {
        this.valuesMap = valuesMap;
    }
}
