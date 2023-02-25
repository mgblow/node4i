package org.eclipse.milo.platform.alamEvent.methods;

import org.eclipse.milo.opcua.sdk.core.ValueRanks;
import org.eclipse.milo.opcua.sdk.server.api.methods.AbstractMethodInvocationHandler;
import org.eclipse.milo.opcua.sdk.server.api.methods.InvalidArgumentException;
import org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.*;
import org.eclipse.milo.opcua.stack.core.types.structured.Argument;
import org.eclipse.milo.platform.alamEvent.AlarmEvent;
import org.eclipse.milo.platform.util.Props;
import org.eclipse.milo.platform.util.Utils;
import org.eclipse.milo.platform.validators.MethodInputValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventAcknowledgeMethod extends AbstractMethodInvocationHandler {

    public static final Argument eventId = new Argument("eventId", Identifiers.String, ValueRanks.Any, null, new LocalizedText("", "The identifier for the event to acknowledge."));
    public static final Argument comment = new Argument("comment", NodeId.parse("ns=0;i=21"), ValueRanks.Scalar, null, new LocalizedText("", "The comment to add to the condition."));
    public static final Argument result = new Argument("result", Identifiers.String, ValueRanks.Any, null, new LocalizedText("result"));

    public static String APP_NAME = null;
    public static final int APP_NAMESPACE_INDEX = Integer.parseInt(Props.getProperty("app-namespace-index").toString());
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private UaMethodNode uaMethodNode;
    private AlarmEvent alarmEvent;

    public EventAcknowledgeMethod(UaMethodNode uaMethodNode, AlarmEvent alarmEvent) {
        super(uaMethodNode);
        APP_NAME = uaMethodNode.getNodeContext().getServer().getConfig().getApplicationName().getText();
        this.uaMethodNode = uaMethodNode;
        this.alarmEvent = alarmEvent;
    }

    @Override
    public Argument[] getInputArguments() {
        return new Argument[]{eventId, comment};
    }

    @Override
    public Argument[] getOutputArguments() {
        return new Argument[]{result};
    }

    @Override
    protected Variant[] invoke(InvocationContext invocationContext, Variant[] inputValues) throws UaException {
        logger.debug("Invoking program() method of objectId={}", invocationContext.getObjectId());
        String eventId = (String) inputValues[0].getValue();
        LocalizedText comment = (LocalizedText) inputValues[1].getValue();
        try {
            this.uaMethodNode.getNodeContext().getServer().getEventFactory().acknowledge(eventId, comment);
            return new Variant[]{new Variant(true)};
        } catch (Exception e) {
            logger.error("Error invoking EventAcknowledgeMethod method of objectId={} with message {}", invocationContext.getObjectId(), e.getMessage());
            return new Variant[]{new Variant(false)};
        }
    }
    protected void validateInputArgumentValues(Variant[] inputArgumentValues) throws InvalidArgumentException {
        MethodInputValidator.NotNull(this, eventId);
        MethodInputValidator.checkExistenceEvent(this, eventId);
        if (this.inputErrorMessages.size() != 0) {
            throw new InvalidArgumentException(null);
        }
    }
}
