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

import java.util.Collection;
import java.util.Map;

public class GetNodeValues extends AbstractMethodInvocationHandler {

    public static final Argument identifiers = new Argument("identifiers", Identifiers.String, ValueRanks.OneOrMoreDimensions, null, new LocalizedText("identifiers"));
    public static final Argument result = new Argument("result", Identifiers.String, ValueRanks.OneOrMoreDimensions, null, new LocalizedText("result"));
    public static final String APP_NAME = Props.getProperty("app-name").toString();
    public static final int APP_NAMESPACE_INDEX = Integer.parseInt(Props.getProperty("app-namespace-index").toString());
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private UaMethodNode uaMethodNode;
    public GetNodeValues(UaMethodNode node) {
        super(node);
        this.uaMethodNode = node;
    }

    @Override
    public Argument[] getInputArguments() {
        return new Argument[]{identifiers};
    }

    @Override
    public Argument[] getOutputArguments() {
        return new Argument[]{result};
    }

    @Override
    protected Variant[] invoke(InvocationContext invocationContext, Variant[] inputValues) throws UaException {
        try {
            logger.debug("Invoking GetNodeValues method of objectId={}", invocationContext.getObjectId());
            String[] identifiers = (String[]) inputValues[0].getValue();
            Map<String,String> interfaceResult = UaInterface.getInstance(this.uaMethodNode.getNodeContext()).getNodeValues(identifiers);
            Collection<String> values = interfaceResult.values();
            String[] arrayValues = values.toArray(new String[0]);
            return new Variant[]{new Variant(arrayValues)};
        } catch (Exception e) {
            logger.error("error in invoking GetNodeValues method of objectId={}", invocationContext.getObjectId());
            return new Variant[]{new Variant(false)};
        }
    }
    protected void validateInputArgumentValues(Variant[] inputArgumentValues) throws InvalidArgumentException {
        MethodInputValidator.forbiddenCharacter(this,identifiers);
        MethodInputValidator.NotNull(this, identifiers);
        MethodInputValidator.Exists(this,identifiers);
        if (this.inputErrorMessages.size() != 0) {
            throw new InvalidArgumentException(null);
        }
    }
}
