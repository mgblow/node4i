package org.eclipse.milo.platform.structs.siemens;

import java.util.List;

public class S7Configuration {

    Config config;
    List<S7Tag> tags;

    Object[] values = null;

    public Config getConfig() {
        return config;
    }

    public void setConfig(Config config) {
        this.config = config;
    }

    public List<S7Tag> getTags() {
        return tags;
    }

    public void setTags(List<S7Tag> tags) {
        this.tags = tags;
    }

    public Object[] getValues() {
        return values;
    }

    public void setValues(Object[] values) {
        this.values = values;
    }

    public static class Config {
        S7Controller controller;
        int pullingInterval;

        public S7Controller getController() {
            return controller;
        }

        public void setController(S7Controller controller) {
            this.controller = controller;
        }

        public int getPullingInterval() {
            return pullingInterval;
        }

        public void setPullingInterval(int pullingInterval) {
            this.pullingInterval = pullingInterval;
        }
    }
}
