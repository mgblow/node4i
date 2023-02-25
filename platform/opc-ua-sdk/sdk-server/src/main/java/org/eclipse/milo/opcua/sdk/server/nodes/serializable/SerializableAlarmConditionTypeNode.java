package org.eclipse.milo.opcua.sdk.server.nodes.serializable;

import org.eclipse.milo.opcua.sdk.server.model.nodes.objects.AlarmConditionTypeNode;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;

public class SerializableAlarmConditionTypeNode implements Serializable {
    private String eventId;
    private String message;
    private Integer severity;
    private String sourceNode;
    private String sourceName;
    private Long activeTime;
    private String activeState;
    private String enabledState;
    private String acknowledgeState;
    private Long acknowledgeTime;
    private String comment;
    private String conditionName;
    private String retain;
    private String conditionClassName;
    private Long time;

    public SerializableAlarmConditionTypeNode(AlarmConditionTypeNode event) {
        this.eventId = new String(event.getEventId().bytes(), StandardCharsets.UTF_8);
        this.message = event.getMessage().getText();
        this.severity = event.getSeverity().intValue();
        this.sourceNode = event.getConditionName();
        this.sourceName = event.getSourceName();
        this.activeTime = event.getActiveStateNode().getTransitionTime().getJavaTime();
        this.activeState = event.getActiveStateNode().getTrueState().getText();
        this.enabledState = event.getEnabledStateNode().getTrueState().getText();
        this.acknowledgeState = event.getAckedStateNode().getTrueState().getText();
        this.acknowledgeTime = event.getAckedStateNode().getTransitionTime() != null ?event.getAckedStateNode().getTransitionTime().getJavaTime() : null;
        this.comment = (event.getComment() != null) ? event.getComment().getText() : null;
        this.conditionName = event.getConditionName();
        this.retain = event.getRetain() ? "true" : "false";
        this.conditionClassName = event.getConditionClassName().getText();
        this.time = event.getTime().getJavaTime();
    }

    public Long getActiveTime() {
        return activeTime;
    }

    public void setActiveTime(Long activeTime) {
        this.activeTime = activeTime;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }


    public String getSourceNode() {
        return sourceNode;
    }

    public void setSourceNode(String sourceNode) {
        this.sourceNode = sourceNode;
    }

    public String getSourceName() {
        return sourceName;
    }

    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getActiveState() {
        return activeState;
    }

    public void setActiveState(String activeState) {
        this.activeState = activeState;
    }

    public String getEnabledState() {
        return enabledState;
    }

    public void setEnabledState(String enabledState) {
        this.enabledState = enabledState;
    }

    public String getAcknowledgeState() {
        return acknowledgeState;
    }

    public void setAcknowledgeState(String acknowledgeState) {
        this.acknowledgeState = acknowledgeState;
    }

    public String getConditionName() {
        return conditionName;
    }

    public void setConditionName(String conditionName) {
        this.conditionName = conditionName;
    }

    public String getRetain() {
        return retain;
    }

    public void setRetain(String retain) {
        this.retain = retain;
    }

    public String getConditionClassName() {
        return conditionClassName;
    }

    public void setConditionClassName(String conditionClassName) {
        this.conditionClassName = conditionClassName;
    }

    public Integer getSeverity() {
        return severity;
    }

    public void setSeverity(Integer severity) {
        this.severity = severity;
    }

    public Long getAcknowledgeTime() {
        return acknowledgeTime;
    }

    public void setAcknowledgeTime(Long acknowledgeTime) {
        this.acknowledgeTime = acknowledgeTime;
    }

    public Long getTime() {
        return time;
    }

    public void setTime(Long time) {
        this.time = time;
    }
}
