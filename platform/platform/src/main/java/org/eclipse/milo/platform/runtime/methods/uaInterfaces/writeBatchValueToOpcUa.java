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
import org.eclipse.milo.platform.validators.MethodInputValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class writeBatchValueToOpcUa extends AbstractMethodInvocationHandler {

    public static final Argument ioName = new Argument("ioName", Identifiers.String, ValueRanks.Any, null, new LocalizedText("ioName"));
    public static final Argument tags = new Argument("tags", Identifiers.String, ValueRanks.OneOrMoreDimensions, null, new LocalizedText("tags"));
    public static final Argument values = new Argument("values", Identifiers.String, ValueRanks.OneOrMoreDimensions, null, new LocalizedText("values"));

    public static final Argument result = new Argument("result", Identifiers.String, ValueRanks.Any, null, new LocalizedText("result"));
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private UaMethodNode uaMethodNode;
    public static String APP_NAME = null;

    public writeBatchValueToOpcUa(UaMethodNode node) {
        super(node);
        this.uaMethodNode = node;
        APP_NAME = uaMethodNode.getNodeContext().getServer().getConfig().getApplicationName().getText();

    }

    @Override
    public Argument[] getInputArguments() {
        return new Argument[]{ioName, tags, values};
    }

    @Override
    public Argument[] getOutputArguments() {
        return new Argument[]{result};
    }

    @Override
    protected Variant[] invoke(InvocationContext invocationContext, Variant[] inputValues) throws UaException {
        try {
            logger.debug("Invoking WriteValueToOpcUaDevice method of objectId={}", invocationContext.getObjectId());
            String ioName = inputValues[0].getValue().toString();
            String[] tags = (String[]) inputValues[1].getValue();
            String[] values = (String[]) inputValues[2].getValue();
//            UaInterface.getInstance(this.uaMethodNode.getNodeContext()).writeValueToOpcUaDevice(ioName, tags, values);
            return new Variant[]{new Variant(true)};
        } catch (Exception e) {
            logger.error("error in invoking WriteValueToOpcUaDevice method of objectId={}", invocationContext.getObjectId());
            return new Variant[]{new Variant(false)};
        }
    }
    protected void validateInputArgumentValues(Variant[] inputArgumentValues) throws InvalidArgumentException {
        MethodInputValidator.NotNull(this, ioName,tags);
        MethodInputValidator.Exists(this, ioName,APP_NAME+"/Devices/");
        if (this.inputErrorMessages.size() != 0) {
            throw new InvalidArgumentException(null);
        }
    }
}

