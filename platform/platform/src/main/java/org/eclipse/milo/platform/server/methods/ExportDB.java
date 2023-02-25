package org.eclipse.milo.platform.server.methods;

import com.google.gson.Gson;
import org.eclipse.milo.opcua.sdk.core.ValueRanks;
import org.eclipse.milo.opcua.sdk.server.api.methods.AbstractMethodInvocationHandler;
import org.eclipse.milo.opcua.sdk.server.api.methods.InvalidArgumentException;
import org.eclipse.milo.opcua.sdk.server.api.methods.Out;
import org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode;
import org.eclipse.milo.opcua.sdk.server.nodes.serializable.SerializableNode;
import org.eclipse.milo.opcua.sdk.server.nodes.serializable.SerializableNodeId;
import org.eclipse.milo.opcua.sdk.server.nodes.serializable.SerializableReference;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.ByteString;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.structured.Argument;
import org.eclipse.milo.platform.structs.server.DBObject;
import org.eclipse.milo.platform.util.Props;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ExportDB extends AbstractMethodInvocationHandler {


    public static final Argument result = new Argument(
            "db export file",
            NodeId.parse("ns=0;i=15"),
            ValueRanks.Scalar,
            null,
            new LocalizedText("", "result")
    );
    public static String APP_NAME = null;

    public static final int APP_NAMESPACE_INDEX = Integer.parseInt(Props.getProperty("app-namespace-index").toString());
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private UaMethodNode uaMethodNode;

    public ExportDB(UaMethodNode node) {
        super(node);
        this.uaMethodNode = node;
        APP_NAME = uaMethodNode.getNodeContext().getServer().getConfig().getApplicationName().getText();
    }

    @Override
    public Argument[] getInputArguments() {
        return new Argument[]{};
    }

    @Override
    public Argument[] getOutputArguments() {
        return new Argument[]{result};
    }

    @Override
    protected Variant[] invoke(InvocationContext invocationContext, Variant[] inputValues) throws UaException {
        try {
            logger.debug("Invoking ExportDB method of objectId={}", invocationContext.getObjectId());
            Map<SerializableNodeId, SerializableNode> nodesMap = this.uaMethodNode.getNodeManager().cacheNodeManager().getNodeFactoryCache().findNodesByNamespaceIndex(APP_NAMESPACE_INDEX);
            List<SerializableNode> nodes = nodesMap.values().stream().collect(Collectors.toList());;
            Map<String, SerializableReference> references = this.uaMethodNode.getNodeManager().cacheNodeManager().getNodeFactoryCache().findReferencesByNamespaceIndex(APP_NAMESPACE_INDEX);
            DBObject DBObject = new DBObject(nodes, references);
            Gson gson = new Gson();
            String exportString= gson.toJson(DBObject);
            Out<ByteString> exportInBytes = new Out<ByteString>();
            ByteString jsonInBytes = ByteString.of(exportString.getBytes());
            exportInBytes.set(jsonInBytes);
            return new Variant[]{new Variant(exportInBytes.get())};
        } catch (Exception e) {
            logger.error("error in exportDB  method of objectId={}", invocationContext.getObjectId());
            return new Variant[]{new Variant(false)};
        }
    }

    protected void validateInputArgumentValues(Variant[] inputArgumentValues) throws InvalidArgumentException {

    }
}
