package org.eclipse.milo.platform.alamEvent;

public enum EventIdentifier {
    ALARM_IDENTIFIER("{app-name}/Alarm&Events/{alarm-name}"),
    CONDITION_CLASS_IDENTIFIER("{app-name}/Alarm&Events/ConditionClasses/{class-name}"),
    ALARM_PROPERTY_IDENTIFIER("{alarm-identifier}/Property/{property-name}"),
    CONDITION_CLASS_PROPERTY_IDENTIFIER("{condition-class-identifier}/Property/{property-name}"),
    ALARM_EVENT_FOLDER("{app-name}/Alarm&Events"),
    CONDITION_CLASS_FOLDER("{app-name}/Alarm&Events/ConditionClasses"),
    PROPERTY_BROWSE_NAME("Property/{property-name}");


    private final String identifier;

    EventIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getIdentifier() {
        return identifier;
    }
}
