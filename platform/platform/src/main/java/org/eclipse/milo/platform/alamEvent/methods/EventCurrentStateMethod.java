package org.eclipse.milo.platform.alamEvent.methods;

import org.eclipse.milo.opcua.sdk.core.ValueRanks;
import org.eclipse.milo.opcua.sdk.server.api.methods.AbstractMethodInvocationHandler;
import org.eclipse.milo.opcua.sdk.server.api.methods.InvalidArgumentException;
import org.eclipse.milo.opcua.sdk.server.model.nodes.objects.AlarmConditionTypeNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.structured.Argument;
import org.eclipse.milo.platform.alamEvent.AlarmEvent;
import org.eclipse.milo.platform.util.Props;
import org.eclipse.milo.platform.util.Utils;
import org.eclipse.milo.platform.validators.MethodInputValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventCurrentStateMethod extends AbstractMethodInvocationHandler {

    public static final Argument eventNodeId = new Argument("eventNodeId", Identifiers.String, ValueRanks.Any, null, new LocalizedText("", "alarm (AlarmConditionTypeNode) identifier"));
    public static final Argument Result = new Argument("result", Identifiers.String, ValueRanks.Any, null, new LocalizedText("result"));

    public static String APP_NAME = null;
    public static final int APP_NAMESPACE_INDEX = Integer.parseInt(Props.getProperty("app-namespace-index").toString());
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private UaMethodNode uaMethodNode;
    private AlarmEvent alarmEvent;

    public EventCurrentStateMethod(UaMethodNode uaMethodNode, AlarmEvent alarmEvent) {
        super(uaMethodNode);
        APP_NAME = uaMethodNode.getNodeContext().getServer().getConfig().getApplicationName().getText();
        this.uaMethodNode = uaMethodNode;
        this.alarmEvent = alarmEvent;
    }

    @Override
    public Argument[] getInputArguments() {
        return new Argument[]{eventNodeId};
    }

    @Override
    public Argument[] getOutputArguments() {
        return new Argument[]{Result};
    }

    @Override
    protected Variant[] invoke(InvocationContext invocationContext, Variant[] inputValues) {
        String eventNodeId = (String) inputValues[0].getValue();
        try {
            AlarmConditionTypeNode alarmConditionTypeNode = (AlarmConditionTypeNode) this.uaMethodNode.getNodeContext().getServer().getEventFactory().getNodeManager().get(Utils.newNodeId(eventNodeId));
            // should return current state
            if (alarmConditionTypeNode != null) {
                return new Variant[]{new Variant(this.uaMethodNode.getNodeContext().getServer().getEventFactory().serializedAlarmConditionTypeNodeEvent(alarmConditionTypeNode))};
            }
            return new Variant[]{new Variant(null)};
        } catch (Exception e) {
            logger.error("Error invoking FetchAlarmStateMethod method of objectId={} with message {}", invocationContext.getObjectId(), e.getMessage());
            return new Variant[]{new Variant(null)};
        }
    }
    protected void validateInputArgumentValues(Variant[] inputArgumentValues) throws InvalidArgumentException {
        MethodInputValidator.forbiddenCharacter(this,eventNodeId);
        MethodInputValidator.Exists(this,eventNodeId);
        MethodInputValidator.NotNull(this,eventNodeId);
        if (this.inputErrorMessages.size() != 0) {
            throw new InvalidArgumentException(null);
        }
    }
}
