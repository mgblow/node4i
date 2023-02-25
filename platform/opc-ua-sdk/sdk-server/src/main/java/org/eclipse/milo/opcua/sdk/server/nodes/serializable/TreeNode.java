package org.eclipse.milo.opcua.sdk.server.nodes.serializable;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

public class TreeNode {
    private SerializableUaNode node;
    private String location;
    private List<TreeNode> children = new ArrayList<>();
    public TreeNode(SerializableUaNode node) {
        this.node = node;
    }
    public TreeNode(SerializableUaNode node , String location) {
        this.node = node;
        this.location=location;
    }

    public TreeNode() {
    }

    public List<TreeNode> getChildren() {
        return children;
    }

    public void setChildren(List<TreeNode> children) {
        this.children = children;
    }

    public void addChildren(TreeNode child) {
        this.children.add(child);
    }

    public SerializableUaNode getNode() {
        return node;
    }

    public void setNode(SerializableUaNode node) {
        this.node = node;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String toJson() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }

}
