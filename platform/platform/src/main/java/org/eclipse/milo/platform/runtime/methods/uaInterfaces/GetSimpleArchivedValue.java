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
import org.eclipse.milo.platform.util.StringUtils;
import org.eclipse.milo.platform.validators.MethodInputValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;


public class GetSimpleArchivedValue extends AbstractMethodInvocationHandler {


    public static final Argument identifiers = new Argument("identifier", Identifiers.String, ValueRanks.OneOrMoreDimensions, null, new LocalizedText("event identifier list"));
    public static final Argument startTime = new Argument("startTime", Identifiers.String, ValueRanks.Any, null, new LocalizedText("start time"));
    public static final Argument endTime = new Argument("endTime", Identifiers.String, ValueRanks.Any, null, new LocalizedText("end time"));
    public static final Argument offset = new Argument("offset", Identifiers.String, ValueRanks.Any, null, new LocalizedText("offset"));
    public static final Argument limit = new Argument("limit", Identifiers.String, ValueRanks.Any, null, new LocalizedText("limit"));

    public static final Argument result = new Argument("result", Identifiers.String, ValueRanks.OneOrMoreDimensions, null, new LocalizedText("result"));
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final UaMethodNode uaMethodNode;

    public GetSimpleArchivedValue(UaMethodNode node) {
        super(node);
        this.uaMethodNode = node;
    }


    @Override
    public Argument[] getInputArguments() {
        return new Argument[]{identifiers, startTime, endTime, offset, limit};
    }

    @Override
    public Argument[] getOutputArguments() {
        return new Argument[]{result};
    }

    @Override
    protected Variant[] invoke(InvocationContext invocationContext, Variant[] inputValues) throws UaException {
        try {
            logger.debug("Invoking getArchivedValue method of objectId={}", invocationContext.getObjectId());
            String[] identifiers = (String[]) inputValues[0].getValue();
            Long startTime = Long.valueOf(inputValues[1].getValue().toString());
            Long endTime = Long.valueOf(inputValues[2].getValue().toString());
            Long offset = StringUtils.isNullOrEmpty(inputValues[3].getValue()) ? 0 : Long.valueOf(inputValues[3].getValue().toString());
            Long limit = StringUtils.isNullOrEmpty(inputValues[4].getValue()) ? 1000 : Long.valueOf(inputValues[4].getValue().toString());

            String result = UaInterface.getInstance(this.uaMethodNode.getNodeContext()).getSimpleArchivedValue(Arrays.asList(identifiers), startTime, endTime, offset, limit);
            return new Variant[]{new Variant(result)};
        } catch (Exception e) {
            logger.error("error in invoking getArchivedValue method of objectId={}", invocationContext.getObjectId());
            return new Variant[]{new Variant(false)};
        }
    }

    protected void validateInputArgumentValues(Variant[] inputArgumentValues) throws InvalidArgumentException {
        MethodInputValidator.forbiddenCharacter(this, identifiers);
        MethodInputValidator.NotNull(this, identifiers);
        MethodInputValidator.Exists(this, identifiers);
        MethodInputValidator.isNumber(this, startTime, endTime);
        MethodInputValidator.checkRange(this, limit, 10000l, "<");
        if (this.inputErrorMessages.size() != 0) {
            throw new InvalidArgumentException(null);
        }
    }
}

