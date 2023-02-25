package org.eclipse.milo.platform.structs.modbus;

import java.util.List;

public class ModbusConfiguration {

    Config config;
    List<ModbusTag> tags;

    public Config getConfig() {
        return config;
    }

    public void setConfig(Config config) {
        this.config = config;
    }

    public List<ModbusTag> getTags() {
        return tags;
    }

    public void setTags(List<ModbusTag> tags) {
        this.tags = tags;
    }

    public static class Config {
        ModbusController controller;
        int pullingInterval;

        public ModbusController getController() {
            return controller;
        }

        public void setController(ModbusController controller) {
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
