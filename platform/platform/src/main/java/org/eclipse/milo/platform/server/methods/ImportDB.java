package org.eclipse.milo.platform.server.methods;

import org.eclipse.milo.opcua.sdk.core.ValueRanks;
import org.eclipse.milo.opcua.sdk.server.api.methods.AbstractMethodInvocationHandler;
import org.eclipse.milo.opcua.sdk.server.api.methods.InvalidArgumentException;
import org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.ByteString;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.structured.Argument;
import org.eclipse.milo.platform.util.Props;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Iterator;

public class ImportDB extends AbstractMethodInvocationHandler {

    public static final Argument JSON = new Argument(
            "db file",
            NodeId.parse("ns=0;i=15"),
            ValueRanks.Scalar,
            null,
            new LocalizedText("", "db file")
    );

    public static final Argument result = new Argument("result", Identifiers.String, ValueRanks.Any, null, new LocalizedText("result"));
    public static String APP_NAME = null;
    public static final int APP_NAMESPACE_INDEX = Integer.parseInt(Props.getProperty("app-namespace-index").toString());
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private UaMethodNode uaMethodNode;

    public ImportDB(UaMethodNode node) {
        super(node);
        this.uaMethodNode = node;
        APP_NAME = uaMethodNode.getNodeContext().getServer().getConfig().getApplicationName().getText();
    }

    @Override
    public Argument[] getInputArguments() {
        return new Argument[]{JSON};
    }

    @Override
    public Argument[] getOutputArguments() {
        return new Argument[]{result};
    }

    @Override
    protected Variant[] invoke(InvocationContext invocationContext, Variant[] inputValues) throws UaException {
        try {
            logger.debug("Invoking ImportDb method of objectId={}", invocationContext.getObjectId());
            ByteString jsonInBytes = (ByteString) inputValues[0].getValue();
            String value = new String(jsonInBytes.bytes(), StandardCharsets.UTF_8);
            JSONObject json = new JSONObject(value);
            String nodes = json.get("serializableNodes").toString();
            String references = json.get("referenceMap").toString();
            JSONArray array = new JSONArray(nodes);
            for (int i = 0; i < array.length(); i++) {
                JSONObject object = array.getJSONObject(i);
                String nodeId = object.get("nodeId").toString();
                String identifier = new JSONObject(nodeId).get("identifier").toString();
                String recordString = object.toString();
                this.uaMethodNode.getNodeManager().cacheNodeManager().getNodeFactoryCache().addNode(identifier, recordString);
            }
            logger.info("imported " + array.length() + " nodes");
            JSONObject refObj = new JSONObject(references);
            Iterator<String> keys = refObj.keys();
            int refscount = 0;
            while (keys.hasNext()) {
                String key = keys.next();
                String v = refObj.get(key).toString();
                this.uaMethodNode.getNodeManager().cacheNodeManager().getNodeFactoryCache().addReference(key, v);
                refscount++;
            }
            logger.info("imported " + refscount + " references");
            return new Variant[]{new Variant("ok")};
        } catch (Exception e) {
            logger.error("error in importDb  method of objectId={}", invocationContext.getObjectId());
            return new Variant[]{new Variant(false)};
        }
    }

    protected void validateInputArgumentValues(Variant[] inputArgumentValues) throws InvalidArgumentException {

    }
}
