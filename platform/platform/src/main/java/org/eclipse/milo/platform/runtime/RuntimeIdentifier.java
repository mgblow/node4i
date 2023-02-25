package org.eclipse.milo.platform.runtime;

public enum RuntimeIdentifier {
    WINDOWING_IDENTIFIER("{app-name}/Runtime/Windowing/{windowing-name}"),
    COMPONENT_IDENTIFIER("{app-name}/Runtime/General/{component-name}"),
    PROPERTY_BROWSE_NAME("Property/{property-name}"),
    RUNTIME_PROPERTY("{runtime-identifier}/Property/{property-name}");


    private final String identifier;

    RuntimeIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getIdentifier() {
        return identifier;
    }

}
