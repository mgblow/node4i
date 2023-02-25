package org.eclipse.milo.opcua.sdk.server.nodes.serializable;

import com.google.gson.Gson;

import java.io.Serializable;
import java.util.List;

public class SerializableTriggerParent implements Serializable {
    private String identifier;
    private List<String> parents;

    public String toJson() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }

    public String getIdentifier() {
        return identifier;
    }

    public List<String> addParent(String parent) {
        parents.add(parent);
        return parents;
    }

    public SerializableTriggerParent setIdentifier(String identifier) {
        this.identifier = identifier;
        return this;
    }

    public List<String> getParents() {
        return parents;
    }

    public SerializableTriggerParent setParents(List<String> parents) {
        this.parents = parents;
        return this;
    }
}
