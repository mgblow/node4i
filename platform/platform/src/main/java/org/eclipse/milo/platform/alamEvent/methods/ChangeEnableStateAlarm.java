package org.eclipse.milo.platform.alamEvent.methods;

import org.eclipse.milo.opcua.sdk.core.ValueRanks;
import org.eclipse.milo.opcua.sdk.server.api.methods.AbstractMethodInvocationHandler;
import org.eclipse.milo.opcua.sdk.server.api.methods.InvalidArgumentException;
import org.eclipse.milo.opcua.sdk.server.model.nodes.objects.AlarmConditionTypeNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.structured.Argument;
import org.eclipse.milo.platform.alamEvent.EventIdentifier;
import org.eclipse.milo.platform.util.Utils;
import org.eclipse.milo.platform.validators.MethodInputValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChangeEnableStateAlarm extends AbstractMethodInvocationHandler {

    public static final Argument Name = new Argument("name", Identifiers.String, ValueRanks.Any, null, new LocalizedText("Event name"));
    public static final Argument Result = new Argument("result", Identifiers.String, ValueRanks.Any, null, new LocalizedText("result"));

    public static String APP_NAME = null;
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private UaMethodNode uaMethodNode;
    private static LocalizedText aTrueState = new LocalizedText("true");
    private static LocalizedText aFalseState = new LocalizedText("false");

    public ChangeEnableStateAlarm(UaMethodNode uaMethodNode) {
        super(uaMethodNode);
        APP_NAME = uaMethodNode.getNodeContext().getServer().getConfig().getApplicationName().getText();
        this.uaMethodNode = uaMethodNode;
    }

    @Override
    public Argument[] getInputArguments() {
        return new Argument[]{Name};
    }

    @Override
    public Argument[] getOutputArguments() {
        return new Argument[]{Result};
    }

    @Override
    protected Variant[] invoke(InvocationContext invocationContext, Variant[] inputValues) {
        String name = (String) inputValues[0].getValue();
        String AlarmNodeID = APP_NAME + "/Alarm&Events/" + name;
        final AlarmConditionTypeNode alarmConditionTypeNode = (AlarmConditionTypeNode) uaMethodNode.getNodeContext().getServer().getEventFactory().getNodeManager().get(Utils.newNodeId(2, AlarmNodeID));
        if (alarmConditionTypeNode.getEnabledStateNode().getFalseState().equals(aTrueState)) {
            alarmConditionTypeNode.getEnabledStateNode().setFalseState(aFalseState);
            alarmConditionTypeNode.getEnabledStateNode().setTrueState(aTrueState);
            alarmConditionTypeNode.getPropertyNode(EventIdentifier.PROPERTY_BROWSE_NAME.getIdentifier().replace("{property-name}","enabled")).get().setValue(new DataValue(new Variant("true")));
        } else {
            alarmConditionTypeNode.getEnabledStateNode().setFalseState(aTrueState);
            alarmConditionTypeNode.getEnabledStateNode().setTrueState(aFalseState);
            alarmConditionTypeNode.getPropertyNode(EventIdentifier.PROPERTY_BROWSE_NAME.getIdentifier().replace("{property-name}","enabled")).get().setValue(new DataValue(new Variant("false")));
        }
        return new Variant[]{new Variant(Boolean.TRUE)};
    }
    protected void validateInputArgumentValues(Variant[] inputArgumentValues) throws InvalidArgumentException {
        MethodInputValidator.forbiddenCharacter(this,Name);
        MethodInputValidator.NotNull(this,Name );
        MethodInputValidator.Exists(this,Name,APP_NAME+"/Alarm&Events/");
        if (this.inputErrorMessages.size() != 0) {
            throw new InvalidArgumentException(null);
        }
    }
}
