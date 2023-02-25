package org.eclipse.milo.opcua.sdk.server.nodes.serializable;

import com.google.gson.Gson;

import java.io.Serializable;

public class SerializableHistorian implements Serializable {
    private String identifier;
    private Long time;
    private String value;
    private String displayName;
    private String browseName;

    public String toJson() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }

    public String getIdentifier() {
        return identifier;
    }

    public SerializableHistorian setIdentifier(String identifier) {
        this.identifier = identifier;
        return this;
    }

    public Long getTime() {
        return time;
    }

    public SerializableHistorian setTime(Long time) {
        this.time = time;
        return this;
    }

    public String getValue() {
        return value;
    }

    public SerializableHistorian setValue(String value) {
        this.value = value;
        return this;
    }

    public String getDisplayName() {
        return displayName;
    }

    public SerializableHistorian setDisplayName(String displayName) {
        this.displayName = displayName;
        return this;
    }

    public String getBrowseName() {
        return browseName;
    }

    public SerializableHistorian setBrowseName(String browseName) {
        this.browseName = browseName;
        return this;
    }
}
