package org.eclipse.milo.platform.runtime.methods.uaInterfaces;

import org.eclipse.milo.opcua.sdk.core.ValueRanks;
import org.eclipse.milo.opcua.sdk.server.api.methods.AbstractMethodInvocationHandler;
import org.eclipse.milo.opcua.sdk.server.api.methods.InvalidArgumentException;
import org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.structured.Argument;
import org.eclipse.milo.platform.runtime.interfaces.UaInterface;
import org.eclipse.milo.platform.util.Props;
import org.eclipse.milo.platform.validators.MethodInputValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Fire extends AbstractMethodInvocationHandler {

    public static final Argument eventDefinitionId = new Argument("eventDefinitionId", Identifiers.String, ValueRanks.Any, null, new LocalizedText("", "eventDefinitionId"));
    public static final Argument result = new Argument("result", Identifiers.String, ValueRanks.Any, null, new LocalizedText("result"));
    public static final String APP_NAME = Props.getProperty("app-name").toString();
    public static final int APP_NAMESPACE_INDEX = Integer.parseInt(Props.getProperty("app-namespace-index").toString());
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private UaMethodNode uaMethodNode;

    public Fire(UaMethodNode node) {
        super(node);
        this.uaMethodNode = node;
    }

    @Override
    public Argument[] getInputArguments() {
        return new Argument[]{eventDefinitionId};
    }

    @Override
    public Argument[] getOutputArguments() {
        return new Argument[]{result};
    }

    @Override
    protected Variant[] invoke(InvocationContext invocationContext, Variant[] inputValues) throws UaException {
        try {
            logger.debug("Invoking fire method of objectId={}", invocationContext.getObjectId());
            String eventDefinitionId =  inputValues[0].getValue().toString();
            UaInterface.getInstance(this.uaMethodNode.getNodeContext()).fire(eventDefinitionId);
            return new Variant[]{new Variant(result)};
        } catch (Exception e) {
            logger.error("error in fire alert method of objectId={}", invocationContext.getObjectId());
            return new Variant[]{new Variant(false)};
        }
    }
    protected void validateInputArgumentValues(Variant[] inputArgumentValues) throws InvalidArgumentException {
        MethodInputValidator.forbiddenCharacter(this,eventDefinitionId);
        MethodInputValidator.NotNull(this, eventDefinitionId);
        MethodInputValidator.Exists(this,eventDefinitionId);
        if (this.inputErrorMessages.size() != 0) {
            throw new InvalidArgumentException(null);
        }
    }
}
