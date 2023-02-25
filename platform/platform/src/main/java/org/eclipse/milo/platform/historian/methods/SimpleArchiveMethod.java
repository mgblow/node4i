package org.eclipse.milo.platform.historian.methods;

import org.eclipse.milo.opcua.sdk.core.ValueRanks;
import org.eclipse.milo.opcua.sdk.server.api.methods.AbstractMethodInvocationHandler;
import org.eclipse.milo.opcua.sdk.server.api.methods.InvalidArgumentException;
import org.eclipse.milo.opcua.sdk.server.nodes.UaFolderNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaVariableNode;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.structured.Argument;
import org.eclipse.milo.platform.historian.Historian;
import org.eclipse.milo.platform.util.ArrayUtils;
import org.eclipse.milo.platform.util.Props;
import org.eclipse.milo.platform.validators.MethodInputValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimpleArchiveMethod extends AbstractMethodInvocationHandler {

    public static final Argument Identifier = new Argument("identifier", Identifiers.String, ValueRanks.Any, null, new LocalizedText("Identifier"));
    public static final Argument link = new Argument("link", Identifiers.Boolean, ValueRanks.Any, null, new LocalizedText("link"));
    public static final Argument swingDoorCompression = new Argument("swingDoorCompression", Identifiers.Boolean, ValueRanks.Any, null, new LocalizedText("swingDoorCompression"));
    public static final Argument tolerance = new Argument("tolerance", Identifiers.String, ValueRanks.Any, null, new LocalizedText("tolerance"));
    public static final Argument Result = new Argument("result", Identifiers.Boolean, ValueRanks.Any, null, new LocalizedText("result"));

    public static final int APP_NAMESPACE_INDEX = Integer.parseInt(Props.getProperty("app-namespace-index").toString());

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private UaMethodNode uaMethodNode;

    private Historian historian;


    public SimpleArchiveMethod(UaMethodNode uaMethodNode, Historian historian) {
        super(uaMethodNode);
        APP_NAME = uaMethodNode.getNodeContext().getServer().getConfig().getApplicationName().getText();
        this.uaMethodNode = uaMethodNode;
        this.historian = historian;

    }

    public static String APP_NAME = null;

    @Override
    public Argument[] getInputArguments() {
        return new Argument[]{Identifier, link, swingDoorCompression, tolerance};
    }

    @Override
    public Argument[] getOutputArguments() {
        return new Argument[]{Result};
    }

    @Override
    protected Variant[] invoke(InvocationContext invocationContext, Variant[] inputValues) {
        try {
            logger.debug("Invoking SimpleArchive method of objectId={}", invocationContext.getObjectId());

            String identifier = (String) inputValues[0].getValue();
            UaVariableNode uaNodeToArchive = (UaVariableNode) this.uaMethodNode.getNodeManager().get(new NodeId(APP_NAMESPACE_INDEX, identifier));
            UaFolderNode simpleArchiveFolder = (UaFolderNode) this.uaMethodNode.getNodeManager().get(new NodeId(APP_NAMESPACE_INDEX, APP_NAME + "/Historian/SimpleArchiveFolder"));
            if (simpleArchiveFolder == null) {
                throw new Exception("SimpleArchive folder is not defined!");
            }
            if (uaNodeToArchive.equals(null)) {
                throw new Exception("NodeId not found!");
            }
            Boolean link = (Boolean) inputValues[1].getValue();
            Boolean compressed = (Boolean) inputValues[2].getValue();
            if (link.equals(true)) {
                if (compressed) {
                    final Double tolerance = Double.valueOf(inputValues[3].getValue().toString());
                    this.historian.addCompressedArchive(uaNodeToArchive, tolerance);
                } else {
                    this.historian.addSimpleArchive(uaNodeToArchive);
                }
                simpleArchiveFolder.addOrganizes(uaNodeToArchive);
            } else {
                this.historian.removeSimpleArchive(uaNodeToArchive);
                simpleArchiveFolder.removeOrganizes(uaNodeToArchive);
            }
            return new Variant[]{new Variant(true)};
        } catch (Exception e) {
            logger.error("error invoking SimpleArchive method, error: {}", e.getMessage());
            return new Variant[]{new Variant(false)};
        }
    }

    protected void validateInputArgumentValues(Variant[] inputArgumentValues) throws InvalidArgumentException {
        MethodInputValidator.forbiddenCharacter(this, Identifier);
        MethodInputValidator.NotNull(this, Identifier, link);
        MethodInputValidator.Exists(this, Identifier);
        MethodInputValidator.isBoolean(this, link, swingDoorCompression);

        int index = ArrayUtils.indexOf(this.getInputArguments(), swingDoorCompression);
        final Boolean isCompressed = Boolean.valueOf((Boolean) this.inputArgumentValues[index].getValue());
        if (isCompressed) {
            MethodInputValidator.NotNull(this, tolerance);
            MethodInputValidator.isNumber(this, tolerance);
        }

        if (this.inputErrorMessages.size() != 0) {
            throw new InvalidArgumentException(null);
        }
    }


}
