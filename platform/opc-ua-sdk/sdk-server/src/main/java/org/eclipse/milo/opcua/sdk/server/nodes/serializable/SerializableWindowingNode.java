package org.eclipse.milo.opcua.sdk.server.nodes.serializable;

import com.google.gson.Gson;

import java.io.Serializable;

public class SerializableWindowingNode implements Serializable {
    private String variable;
    private Long time;

    public SerializableWindowingNode(String variable, Long time) {
        this.variable = variable;
        this.time = time;
    }

    public String getVariable() {
        return variable;
    }

    public SerializableWindowingNode setVariable(String variable) {
        this.variable = variable;
        return this;
    }

    public Long getTime() {
        return time;
    }

    public SerializableWindowingNode setTime(Long time) {
        this.time = time;
        return this;
    }

    public String toJson() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }

}
