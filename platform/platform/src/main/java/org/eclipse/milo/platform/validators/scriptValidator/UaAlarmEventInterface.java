package org.eclipse.milo.platform.validators.scriptValidator;

import org.eclipse.milo.opcua.sdk.client.model.types.objects.AlarmConditionType;

import java.util.Collections;
import java.util.Map;

public class UaAlarmEventInterface {


    public UaAlarmEventInterface() {
    }

    public void fireEvent(String[] self, String message, String severity) throws Exception {
    }

    public void alert(String identifier, boolean active) throws Exception {
    }

    private void sendEventOverMqtt(AlarmConditionType event, String LOCATION) {
    }

    public String getNodeValue(String identifier) {
        return null;
    }

    public Map<String, String> getNodeValues(String[] identifiers) {
        return Collections.emptyMap();
    }

    public void saveNode(String location, String name, String value) throws Exception {
    }

}
