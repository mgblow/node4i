

package org.eclipse.milo.platform.filter.methods;

import com.google.gson.Gson;
import org.eclipse.milo.opcua.sdk.core.ValueRanks;
import org.eclipse.milo.opcua.sdk.server.api.methods.AbstractMethodInvocationHandler;
import org.eclipse.milo.opcua.sdk.server.api.methods.InvalidArgumentException;
import org.eclipse.milo.opcua.sdk.server.api.persistence.RedisCommunication;
import org.eclipse.milo.opcua.sdk.server.api.persistence.caches.NodeFactoryCache;
import org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode;
import org.eclipse.milo.opcua.sdk.server.nodes.serializable.SerializableReference;
import org.eclipse.milo.opcua.sdk.server.nodes.serializable.SerializableUaNode;
import org.eclipse.milo.opcua.sdk.server.util.Props;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.structured.Argument;
import org.eclipse.milo.platform.validators.MethodInputValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


public class SearchInLocationMethod extends AbstractMethodInvocationHandler {
    public static final Argument location = new Argument("location", Identifiers.String, ValueRanks.Any, null, new LocalizedText("location"));
    public static final Argument query = new Argument("query", Identifiers.String, ValueRanks.Any, null, new LocalizedText("query"));
    public static final Argument offset = new Argument("offset", Identifiers.String, ValueRanks.Any,null , new LocalizedText("offset"));
    public static final Argument limit = new Argument("limit", Identifiers.String, ValueRanks.Any, null, new LocalizedText("limit"));
    public static final Argument Result = new Argument("result", Identifiers.String, ValueRanks.Any, null, new LocalizedText("result"));
    public static String APP_NAME = null;
    public static final int APP_NAMESPACE_INDEX = Integer.parseInt(Props.getProperty("app-namespace-index").toString());
    private final Logger logger = LoggerFactory.getLogger(getClass());
    public UaMethodNode uaMethodNode;
    NodeFactoryCache nodeFactoryCache;

    public SearchInLocationMethod(UaMethodNode uaMethodNode) {
        super(uaMethodNode);
        APP_NAME = uaMethodNode.getNodeContext().getServer().getConfig().getApplicationName().getText();
        nodeFactoryCache = new NodeFactoryCache(new RedisCommunication(uaMethodNode.getNodeContext().getServer()));
        this.uaMethodNode = uaMethodNode;
    }

    @Override
    public Argument[] getInputArguments() {
        return new Argument[]{location,query,offset,limit};
    }

    @Override
    public Argument[] getOutputArguments() {
        return new Argument[]{Result};
    }

    @Override
    protected Variant[] invoke(InvocationContext invocationContext, Variant[] inputValues) {
        logger.debug("Invoking SearchInLocation() method of objectId={}", invocationContext.getObjectId());
        String location = (String) inputValues[0].getValue();
        String query = (String) inputValues[1].getValue();
        Long offset =  Long.valueOf(inputValues[2].getValue().toString());
        Long limit =Long.valueOf(inputValues[3].getValue().toString());
        Map<String, SerializableReference> map = nodeFactoryCache.searchInForwardReferencesBySourceNodeIdentifier(location,query,offset,limit);
        List<String> resultIdentifiers = map.values().stream().map(i -> i.getTargetNodeId().getIdentifier()).collect(Collectors.toList());
        List<SerializableUaNode> serializeResults = nodeFactoryCache.findSerializeUaNodes(resultIdentifiers, APP_NAMESPACE_INDEX,offset,limit );
        Gson gson = new Gson();
        return new Variant[]{new Variant(gson.toJson(serializeResults))};
    }

    protected void validateInputArgumentValues(Variant[] inputArgumentValues) throws InvalidArgumentException {
        MethodInputValidator.forbiddenCharacter(this, location,query);
        MethodInputValidator.NotNull(this, limit,offset,location,query);
        MethodInputValidator.isNumber(this, limit,offset);
        MethodInputValidator.checkRange(this, limit, 10000l, "<");
        if (this.inputErrorMessages.size() != 0) {
            throw new InvalidArgumentException(null);
        }
    }

}
