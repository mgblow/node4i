package org.eclipse.milo.platform.gateway.methods;

import org.eclipse.milo.opcua.sdk.core.AccessLevel;
import org.eclipse.milo.opcua.sdk.core.Reference;
import org.eclipse.milo.opcua.sdk.core.ValueRanks;
import org.eclipse.milo.opcua.sdk.server.api.methods.AbstractMethodInvocationHandler;
import org.eclipse.milo.opcua.sdk.server.api.methods.InvalidArgumentException;
import org.eclipse.milo.opcua.sdk.server.nodes.*;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.types.builtin.*;
import org.eclipse.milo.opcua.stack.core.types.enumerated.NodeClass;
import org.eclipse.milo.opcua.stack.core.types.structured.Argument;
import org.eclipse.milo.platform.gateway.GatewayIdentifier;
import org.eclipse.milo.platform.util.Props;
import org.eclipse.milo.platform.validators.MethodInputValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

public class NoDeviceAttributeMethod extends AbstractMethodInvocationHandler {

    public static final Argument NAME = new Argument("name", Identifiers.String, ValueRanks.Any, null, new LocalizedText("name"));
    public static final Argument DISPLAY_NAME = new Argument("displayName", Identifiers.String, ValueRanks.Any, null, new LocalizedText("displayName"));
    public static final Argument LOCATION = new Argument("location", Identifiers.String, ValueRanks.Any, null, new LocalizedText("location"));
    public static final Argument NODE_CLASS = new Argument("nodeClass", Identifiers.String, ValueRanks.Any, null, new LocalizedText("nodeClass"));
    public static final Argument VALUE = new Argument("value", Identifiers.String, ValueRanks.Any, null, new LocalizedText("value"));
    public static final Argument DESCRIPTION = new Argument("description", Identifiers.String, ValueRanks.Any, null, new LocalizedText("description"));
    public static final Argument RESULT = new Argument("result", Identifiers.String, ValueRanks.Any, null, new LocalizedText("result"));
    public static String APP_NAME = null;
    private final Logger logger = LoggerFactory.getLogger(getClass());
    UaMethodNode uaMethodNode;
    private int appNamespaceIndex = Integer.parseInt(Props.getProperty("app-namespace-index").toString());

    public NoDeviceAttributeMethod(UaMethodNode uaMethodNode) {
        super(uaMethodNode);
        APP_NAME = uaMethodNode.getNodeContext().getServer().getConfig().getApplicationName().getText();
        this.uaMethodNode = uaMethodNode;
    }

    @Override
    public Argument[] getInputArguments() {
        return new Argument[]{NAME, DISPLAY_NAME, LOCATION, NODE_CLASS, VALUE, DESCRIPTION};
    }

    @Override
    public Argument[] getOutputArguments() {
        return new Argument[]{RESULT};
    }

    @Override
    protected Variant[] invoke(InvocationContext invocationContext, Variant[] inputValues) {
        try {
            logger.debug("Invoking tag method of objectId={}", invocationContext.getObjectId());
            String name = (String) inputValues[0].getValue();
            String displayName = (String) inputValues[1].getValue();
            displayName = displayName == null ? name : displayName;
            String location = (String) inputValues[2].getValue();
            String nodeClass = (String) inputValues[3].getValue();
            String value = (String) inputValues[4].getValue();
            String description = (String) inputValues[5].getValue();

            String internalIdentifier = GatewayIdentifier.INTERNAL_IDENTIFIER.getIdentifier().replace("{app-name}", APP_NAME);
            String tagId = GatewayIdentifier.INTERNAL_TAG.getIdentifier().replace("{app-name}", APP_NAME).replace("{node-class}", nodeClass).replace("{tag-name}", name);

            UaObjectNode internal = (UaObjectNode) uaMethodNode.getNodeManager().get(new NodeId(2, internalIdentifier));
            UaNode internalTag = invocationContext.getMethodNode().getNodeManager().get(new NodeId(appNamespaceIndex, tagId));
            if (internalTag == null) {
                switch (nodeClass) {
                    case "Folder":
                        internalTag = new UaFolderNode(uaMethodNode.getNodeContext(), new NodeId(appNamespaceIndex, tagId), new QualifiedName(appNamespaceIndex, tagId), new LocalizedText(displayName));
                        break;
                    case "Object":
                        internalTag = new UaObjectNode(uaMethodNode.getNodeContext(), new NodeId(appNamespaceIndex, tagId), new QualifiedName(appNamespaceIndex, tagId), new LocalizedText(displayName));
                        break;
                    case "Variable":
                        internalTag = new UaVariableNode.UaVariableNodeBuilder(uaMethodNode.getNodeContext()).setNodeId(new NodeId(appNamespaceIndex, tagId)).setAccessLevel(AccessLevel.READ_WRITE).setBrowseName(new QualifiedName(appNamespaceIndex, tagId)).setDisplayName(LocalizedText.english(displayName)).setDataType(Identifiers.String).setTypeDefinition(Identifiers.BaseDataVariableType).setValue(DataValue.valueOnly(new Variant(value))).build();
                        break;
                }
            } else {
                List<Reference> references = internalTag.getReferences().stream().filter(c -> c.getDirection() == Reference.Direction.INVERSE).collect(Collectors.toList());
                for (Reference reference : references) {
                    internalTag.removeReference(reference);
                }
                internalTag.setDisplayName(new LocalizedText(displayName));
                if (internalTag instanceof UaVariableNode) {
                    ((UaVariableNode) internalTag).setValue(new DataValue(new Variant(value)));
                }
            }
            internalTag.setDescription(LocalizedText.english(description));
            uaMethodNode.getNodeManager().addNode(internalTag);
            UaFolderNode root = (UaFolderNode) uaMethodNode.getNodeManager().get(new NodeId(2, location));
            root.addOrganizes(internalTag);
            internal.addComponent(internalTag);
            return new Variant[]{new Variant(true)};
        } catch (Exception e) {
            return new Variant[]{new Variant(false)};
        }
    }

    protected void validateInputArgumentValues(Variant[] inputArgumentValues) throws InvalidArgumentException {
        MethodInputValidator.forbiddenCharacter(this, NAME);
        MethodInputValidator.NotNull(this, NAME, LOCATION, NODE_CLASS);
        MethodInputValidator.Exists(this, LOCATION);
        MethodInputValidator.Equal(this, NODE_CLASS, "Object", "Variable", "Folder");
        MethodInputValidator.checkType(this, LOCATION, NodeClass.Object);

        if (this.inputErrorMessages.size() != 0) {
            throw new InvalidArgumentException(null);
        }
    }
}