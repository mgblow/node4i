package org.eclipse.milo.opcua.sdk.server.nodes.serializable;

import com.google.gson.Gson;

import java.io.Serializable;

public class SerializableComponent implements Serializable {
    private String browseName;
    private String displayName;
    private String description;

    public String toJson() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }

    public String getBrowseName() {
        return browseName;
    }

    public SerializableComponent setBrowseName(String browseName) {
        this.browseName = browseName;
        return this;
    }

    public String getDisplayName() {
        return displayName;
    }

    public SerializableComponent setDisplayName(String displayName) {
        this.displayName = displayName;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public SerializableComponent setDescription(String description) {
        this.description = description;
        return this;
    }
}
