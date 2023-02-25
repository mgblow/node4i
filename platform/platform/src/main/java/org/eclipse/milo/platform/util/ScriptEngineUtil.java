package org.eclipse.milo.platform.util;

import org.eclipse.milo.opcua.sdk.server.nodes.UaNode;
import org.eclipse.milo.platform.alamEvent.EventIdentifier;

public class ScriptEngineUtil {
    private static Engine type;

    public ScriptEngineUtil(Engine engine) {
        type = engine;
    }

    public String constructAlarmEventsScript(UaNode event) {
        String functionName = Utils.generateRandomString();
        String condition = "";
        String script = Utils.getPropertyValue(event, EventIdentifier.PROPERTY_BROWSE_NAME.getIdentifier().replace("{property-name}","condition"));
        switch (type) {
            case JavaScript:
                condition += "function " + functionName + "(){";
                condition += script;
                condition += "return false;}";
                condition += "if(" + functionName + "() ){";
                condition += "uaAlarmEventInterface.alert('" + event.getNodeId().getIdentifier().toString() + "',true);";
                condition += "}else{";
                condition += "uaAlarmEventInterface.alert('" + event.getNodeId().getIdentifier().toString() + "',false);";
                condition += "}";
                break;
            case Jython:
                // Not implemented
                break;
        }
        return condition;
    }

    public String constructRuntimeScript(UaNode event) {
        String functionName = Utils.generateRandomString();
        String condition = "";
        String script = Utils.getPropertyValue(event,  EventIdentifier.PROPERTY_BROWSE_NAME.getIdentifier().replace("{property-name}","script"));
        switch (type) {
            case JavaScript:
                // not implemented
                condition += script;
                break;
            case Jython:
                // Not implemented
                condition += script;
                break;
        }
        return condition;
    }

    public enum Engine {
        Jython, JavaScript;
    }
}
