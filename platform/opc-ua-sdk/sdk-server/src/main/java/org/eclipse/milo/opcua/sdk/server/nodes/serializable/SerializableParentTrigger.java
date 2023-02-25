package org.eclipse.milo.opcua.sdk.server.nodes.serializable;

import com.google.gson.Gson;

import java.io.Serializable;
import java.util.List;

public class SerializableParentTrigger implements Serializable {
    private String identifier;
    private List<String> triggers;

    public String toJson() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }

    public String getIdentifier() {
        return identifier;
    }

    public SerializableParentTrigger setIdentifier(String identifier) {
        this.identifier = identifier;
        return this;
    }

    public List<String> getTriggers() {
        return triggers;
    }

    public SerializableParentTrigger setTriggers(List<String> triggers) {
        this.triggers = triggers;
        return this;
    }
}
