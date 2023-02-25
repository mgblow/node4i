package org.eclipse.milo.platform.runtime.methods;

import org.eclipse.milo.opcua.sdk.core.ValueRanks;
import org.eclipse.milo.opcua.sdk.server.api.methods.AbstractMethodInvocationHandler;
import org.eclipse.milo.opcua.sdk.server.api.methods.InvalidArgumentException;
import org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaObjectNode;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.structured.Argument;
import org.eclipse.milo.platform.runtime.RunTime;
import org.eclipse.milo.platform.util.Props;
import org.eclipse.milo.platform.validators.MethodInputValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JavaScriptProgramDeleteMethod extends AbstractMethodInvocationHandler {

    public static final Argument COMPONENT_Identifier = new Argument("Identifier", Identifiers.String, ValueRanks.Any, null, new LocalizedText("Component Identifier"));

    public static final Argument Result = new Argument("result", Identifiers.String, ValueRanks.Any, null, new LocalizedText("result"));

    public static String APP_NAME = null;
    public static final int APP_NAMESPACE_INDEX = Integer.parseInt(Props.getProperty("app-namespace-index").toString());
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private UaMethodNode uaMethodNode;
    private RunTime runTime;

    public JavaScriptProgramDeleteMethod(UaMethodNode uaMethodNode, RunTime runTime) {
        super(uaMethodNode);
        APP_NAME = uaMethodNode.getNodeContext().getServer().getConfig().getApplicationName().getText();
        this.uaMethodNode = uaMethodNode;
        this.runTime = runTime;
    }

    @Override
    public Argument[] getInputArguments() {
        return new Argument[]{COMPONENT_Identifier};
    }

    @Override
    public Argument[] getOutputArguments() {
        return new Argument[]{Result};
    }

    @Override
    protected Variant[] invoke(AbstractMethodInvocationHandler.InvocationContext invocationContext, Variant[] inputValues) {
        logger.debug("Invoking program() method of objectId={}", invocationContext.getObjectId());
        try {
            String componentIdentifier = (String) inputValues[0].getValue();
            // check existence
            UaObjectNode componentNode = (UaObjectNode) uaMethodNode.getNodeManager().get(new NodeId(APP_NAMESPACE_INDEX, componentIdentifier));
            if (componentNode == null) {
                logger.error("error invoking JavaScriptProgramUnLoadMethod , program with same component node ID [{}] does not exists", componentIdentifier);
                throw new UaException(StatusCode.BAD);
            }
            this.runTime.delete(componentNode);

            return new Variant[]{new Variant(true)};
        } catch (Exception e) {
            logger.error("Error invoking ProgramMethod of objectId={} with message {}", invocationContext.getObjectId(), e.getMessage());
            return new Variant[]{new Variant(false)};
        }
    }

    protected void validateInputArgumentValues(Variant[] inputArgumentValues) throws InvalidArgumentException {
        MethodInputValidator.NotNull(this, COMPONENT_Identifier);
        MethodInputValidator.Exists(this, COMPONENT_Identifier);
        if (this.inputErrorMessages.size() != 0) {
            throw new InvalidArgumentException(null);
        }
    }
}

