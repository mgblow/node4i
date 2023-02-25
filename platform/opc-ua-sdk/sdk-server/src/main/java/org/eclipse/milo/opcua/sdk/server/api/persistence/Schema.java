package org.eclipse.milo.opcua.sdk.server.api.persistence;

import org.slf4j.LoggerFactory;

import java.util.ArrayList;

public class Schema {

    private RedisCommunication redisCommunication = null;
    ArrayList<String> indexes;

    public Schema(RedisCommunication redisCommunication) {
        this.redisCommunication = redisCommunication;
        this.indexes = (ArrayList<String>) this.redisCommunication.exec("FT._LIST");
    }

    public void build() {
        this.uaNodeSchemaBuilder();
        this.uaReferenceSchemaBuilder();
        this.uaEventNodeSchemaBuilder();
        this.simpleArchiveSchemaBuilder();
        this.simpleArchiveSwingDoorSchemaBuilder();
        this.triggerParentSchemaBuilder();
        this.parentTriggerSchemaBuilder();
    }

    private void uaNodeSchemaBuilder() {
        try {
            if (!this.indexes.contains("UaNodeIndex")) {
                String uaNodeSchemaBuilderCommand =
                        "FT.CREATE UaNodeIndex ON JSON PREFIX 1 UaNode: SCHEMA " +
                                "$.nodeId.namespaceIndex AS nodeIdNamespaceIndex NUMERIC " +
                                "$.nodeId.identifier AS nodeIdIdentifier TAG " +
                                "$.displayName AS displayName TAG " +
                                "$.browseName AS browseName TAG " +
                                "$.description AS description TAG " +
                                "$.nodeClass as nodeClass NUMERIC";
                this.redisCommunication.exec(uaNodeSchemaBuilderCommand);
            }
        } catch (Exception e) {
            LoggerFactory.getLogger(getClass()).error("error creating UaNodeIndex on redis hash.");
        }
    }


    private void uaEventNodeSchemaBuilder() {
        try {
            if (!this.indexes.contains("UaEventNodeIndex")) {
                String uaNodeSchemaBuilderCommand =
                        "FT.CREATE UaEventNodeIndex ON JSON PREFIX 1 AlarmConditionTypeNodeEvent: SCHEMA " +
                                "$.eventId AS eventId TAG " +
                                "$.sourceNode AS sourceNode TAG " +
                                "$.severity AS severity NUMERIC " +
                                "$.activeState AS activeState TAG " +
                                "$.acknowledgeState AS acknowledgeState TAG " +
                                "$.retain AS retain TAG " +
                                "$.activeTime AS activeTime NUMERIC " +
                                "$.time AS time NUMERIC " +
                                "$.acknowledgeTime AS acknowledgeTime NUMERIC " +
                                "$.message AS message TEXT ";
                this.redisCommunication.exec(uaNodeSchemaBuilderCommand);
            }
        } catch (Exception e) {
            LoggerFactory.getLogger(getClass()).error("error creating UaEventNodeIndex on redis hash.");
        }
    }

    private void uaReferenceSchemaBuilder() {
        try {
            if (!this.indexes.contains("UaReferenceIndex")) {
                String uaReferenceSchemaBuilderCommand =
                                "FT.CREATE UaReferenceIndex ON JSON PREFIX 1 UaReference: SCHEMA " +
                                "$.sourceNodeId.namespaceIndex AS sourceNodeIdNamespaceIndex NUMERIC " +
                                "$.sourceNodeId.identifier AS sourceNodeIdIdIdentifier TAG " +
                                "$.targetNodeId.namespaceIndex AS targetNodeIdNamespaceIndex NUMERIC " +
                                "$.targetNodeId.identifier AS targetNodeIdIdIdentifier TAG "+
                                "$.direction AS direction TAG ";
                this.redisCommunication.exec(uaReferenceSchemaBuilderCommand);
            }
        } catch (Exception e) {
            LoggerFactory.getLogger(getClass()).error("error creating UaReferenceIndex on redis hash.");
        }
    }

    private void triggerParentSchemaBuilder() {
        try {
            if (!this.indexes.contains("TriggerParentIndex")) {
                String triggerParentSchemaBuilderCommand =
                        "FT.CREATE TriggerParentIndex ON JSON PREFIX 1 TriggerParent: SCHEMA " +
                                "$.parents AS parents TEXT " +
                                "$.identifier AS identifier TAG ";
                this.redisCommunication.exec(triggerParentSchemaBuilderCommand);
            }
        } catch (Exception e) {
            LoggerFactory.getLogger(getClass()).error("error creating TriggerParentIndex on redis hash.");
        }
    }

    private void parentTriggerSchemaBuilder() {
        try {
            if (!this.indexes.contains("ParentTriggerIndex")) {
                String triggerParentSchemaBuilderCommand =
                        "FT.CREATE ParentTriggerIndex ON JSON PREFIX 1 ParentTrigger: SCHEMA " +
                                "$.identifier AS identifier TAG " +
                                "$.triggers AS triggers TEXT ";
                this.redisCommunication.exec(triggerParentSchemaBuilderCommand);
            }
        } catch (Exception e) {
            LoggerFactory.getLogger(getClass()).error("error creating TriggerParentIndex on redis hash.");
        }
    }

    private void simpleArchiveSchemaBuilder() {
        try {
            if (!this.indexes.contains("simpleArchiveIndex")) {
                String simpleArchiveSchemaBuilderCommand =
                        "FT.CREATE simpleArchiveIndex ON JSON PREFIX 1 simpleArchive: SCHEMA " +
                                "$.identifier AS identifier TAG " +
                                "$.value as value TEXT " +
                                "$.time AS time NUMERIC ";
                this.redisCommunication.exec(simpleArchiveSchemaBuilderCommand);
            }
        } catch (Exception e) {
            LoggerFactory.getLogger(getClass()).error("error creating simpleArchiveIndex on redis hash.");
        }
    }

    private void simpleArchiveSwingDoorSchemaBuilder() {
        try {
            if (!this.indexes.contains("simpleArchiveSwingDoorIndex")) {
                String simpleArchiveSwingDoorSchemaBuilderCommand =
                        "FT.CREATE simpleArchiveSwingDoorIndex ON JSON PREFIX 1 simpleArchiveSwingDoor: SCHEMA " +
                                "$.identifier AS identifier TAG " +
                                "$.value as value TEXT " +
                                "$.time AS time NUMERIC ";
                this.redisCommunication.exec(simpleArchiveSwingDoorSchemaBuilderCommand);
            }
        } catch (Exception e) {
            LoggerFactory.getLogger(getClass()).error("error creating simpleArchiveIndex on redis hash.");
        }
    }

}
