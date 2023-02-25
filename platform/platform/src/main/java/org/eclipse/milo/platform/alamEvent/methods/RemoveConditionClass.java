package org.eclipse.milo.platform.alamEvent.methods;

import org.eclipse.milo.opcua.sdk.core.ValueRanks;
import org.eclipse.milo.opcua.sdk.server.api.methods.AbstractMethodInvocationHandler;
import org.eclipse.milo.opcua.sdk.server.api.methods.InvalidArgumentException;
import org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNodeContext;
import org.eclipse.milo.opcua.sdk.server.nodes.UaObjectNode;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.structured.Argument;
import org.eclipse.milo.platform.validators.MethodInputValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public class RemoveConditionClass extends AbstractMethodInvocationHandler {
    public static final Argument name = new Argument("name", Identifiers.String, ValueRanks.Any, null, new LocalizedText("NAME"));
    UaNodeContext uaNodeContext;
    public static final Argument Result = new Argument("result", Identifiers.String, ValueRanks.Any, null, new LocalizedText("result"));

    public static String APP_NAME = null;
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private UaMethodNode uaMethodNode;

    public RemoveConditionClass(UaMethodNode uaMethodNode, UaNodeContext context) {
        super(uaMethodNode);
        APP_NAME = uaMethodNode.getNodeContext().getServer().getConfig().getApplicationName().getText();
        this.uaMethodNode = uaMethodNode;
        this.uaNodeContext = context;
    }

    @Override
    public Argument[] getInputArguments() {
        return new Argument[]{name};
    }

    @Override
    public Argument[] getOutputArguments() {
        return new Argument[]{Result};
    }

    @Override
    protected Variant[] invoke(AbstractMethodInvocationHandler.InvocationContext invocationContext, Variant[] inputValues) {
        try {
            logger.debug("Invoking RemoveConditionClass() method of objectId={}", invocationContext.getObjectId());
            String name = (String) inputValues[0].getValue();
            String conditionClassNodeId = APP_NAME + "/Alarm&Events/ConditionClasses/" + name;
            final NodeId nodeId = new NodeId(2, conditionClassNodeId);

            boolean hasAlarmAndEvent = !((UaObjectNode) Objects.requireNonNull(uaNodeContext.getNodeManager().get(nodeId))).getComponentNodes().isEmpty();

            if (hasAlarmAndEvent)
                throw new UaException(StatusCode.BAD, "It can not deleted because it has reference");

            Objects.requireNonNull(uaNodeContext.getNodeManager().get(nodeId.expanded(), invocationContext.getServer().getNamespaceTable())).delete();

            return new Variant[]{new Variant(Boolean.TRUE)};
        } catch (Exception e) {
            return new Variant[]{new Variant(Boolean.FALSE)};
        }
    }
    protected void validateInputArgumentValues(Variant[] inputArgumentValues) throws InvalidArgumentException {
        MethodInputValidator.forbiddenCharacter(this,name);
        MethodInputValidator.NotNull(this, name);
        MethodInputValidator.Exists(this,name,APP_NAME+"/Alarm&Events/ConditionClasses/");
        if (this.inputErrorMessages.size() != 0) {
            throw new InvalidArgumentException(null);
        }
    }


}
