package org.eclipse.milo.platform.alamEvent.methods.uaInterface;

import org.eclipse.milo.opcua.sdk.core.ValueRanks;
import org.eclipse.milo.opcua.sdk.server.api.methods.AbstractMethodInvocationHandler;
import org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.structured.Argument;
import org.eclipse.milo.platform.alamEvent.interfaces.UaAlarmEventInterface;
import org.eclipse.milo.platform.util.Props;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FireEvent extends AbstractMethodInvocationHandler {

    public static final Argument location = new Argument("location", Identifiers.String, ValueRanks.Any, null, new LocalizedText("", "location"));
    public static final Argument name = new Argument("name", Identifiers.String, ValueRanks.Any, null, new LocalizedText("", "name"));
    public static final Argument message = new Argument("message", Identifiers.String, ValueRanks.Any, null, new LocalizedText("", "message"));
    public static final Argument severity = new Argument("severity", Identifiers.String, ValueRanks.Any, null, new LocalizedText("", "severity"));
    public static final Argument result = new Argument("result", Identifiers.String, ValueRanks.Any, null, new LocalizedText("result"));
    public static final String APP_NAME = Props.getProperty("app-name").toString();
    public static final int APP_NAMESPACE_INDEX = Integer.parseInt(Props.getProperty("app-namespace-index").toString());
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private UaMethodNode uaMethodNode;

    public FireEvent(UaMethodNode node) {
        super(node);
        this.uaMethodNode = node;
    }

    @Override
    public Argument[] getInputArguments() {
        return new Argument[]{location,name,message,severity};
    }

    @Override
    public Argument[] getOutputArguments() {
        return new Argument[]{result};
    }

    @Override
    protected Variant[] invoke(InvocationContext invocationContext, Variant[] inputValues) throws UaException {
        try {
            logger.debug("Invoking fireEvent method of objectId={}", invocationContext.getObjectId());
            String location =  inputValues[0].getValue().toString();
            String name =  inputValues[1].getValue().toString();
            String message =  inputValues[2].getValue().toString();
            String severity =  inputValues[3].getValue().toString();
            UaAlarmEventInterface.getInstance(this.uaMethodNode.getNodeContext()).fireEvent(location,name,message,severity);
            return new Variant[]{new Variant(true)};
        } catch (Exception e) {
            logger.error("error in invoking fireEvent method of objectId={}", invocationContext.getObjectId());
            return new Variant[]{new Variant(false)};
        }
    }
}
