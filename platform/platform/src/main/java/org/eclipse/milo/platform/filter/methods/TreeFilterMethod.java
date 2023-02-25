

package org.eclipse.milo.platform.filter.methods;

import com.google.gson.Gson;
import io.netty.util.internal.StringUtil;
import org.eclipse.milo.opcua.sdk.core.ValueRanks;
import org.eclipse.milo.opcua.sdk.server.api.methods.AbstractMethodInvocationHandler;
import org.eclipse.milo.opcua.sdk.server.api.methods.InvalidArgumentException;
import org.eclipse.milo.opcua.sdk.server.api.persistence.RedisCommunication;
import org.eclipse.milo.opcua.sdk.server.api.persistence.caches.NodeFactoryCache;
import org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode;
import org.eclipse.milo.opcua.sdk.server.nodes.serializable.SerializableReference;
import org.eclipse.milo.opcua.sdk.server.nodes.serializable.SerializableUaNode;
import org.eclipse.milo.opcua.sdk.server.nodes.serializable.TreeNode;
import org.eclipse.milo.opcua.sdk.server.util.Props;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.types.builtin.*;
import org.eclipse.milo.opcua.stack.core.types.structured.Argument;
import org.eclipse.milo.platform.util.StringUtils;
import org.eclipse.milo.platform.validators.MethodInputValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;


public class TreeFilterMethod extends AbstractMethodInvocationHandler {

    public static final Argument query = new Argument("query", Identifiers.String, ValueRanks.Any, null, new LocalizedText("query"));
    public static final Argument tagFilter = new Argument("tagFilter", Identifiers.Boolean, ValueRanks.Any, null, new LocalizedText("tagFilter"));
    public static final Argument ioFilter = new Argument("ioFilter", Identifiers.Boolean, ValueRanks.Any, null, new LocalizedText("ioFilter"));
    public static final Argument conditionClassFilter = new Argument("conditionClassFilter", Identifiers.Boolean, ValueRanks.Any, null, new LocalizedText("conditionClassFilter"));
    public static final Argument eventFilter = new Argument("eventFilter", Identifiers.Boolean, ValueRanks.Any, null, new LocalizedText("eventFilter"));
    public static final Argument componentFilter = new Argument("componentFilter", Identifiers.Boolean, ValueRanks.Any, null, new LocalizedText("componentFilter"));
    public static final Argument monitoringGroupFilter = new Argument("monitoringGroupFilter", Identifiers.Boolean, ValueRanks.Any, null, new LocalizedText("monitoringGroupFilter"));
    public static final Argument offset = new Argument("offset", Identifiers.String, ValueRanks.Any,null , new LocalizedText("offset"));
    public static final Argument limit = new Argument("limit", Identifiers.String, ValueRanks.Any, null, new LocalizedText("limit"));

    public static final Argument Result = new Argument("result", Identifiers.String, ValueRanks.Any, null, new LocalizedText("result"));

    public static String APP_NAME = null;

    public static final int APP_NAMESPACE_INDEX = Integer.parseInt(Props.getProperty("app-namespace-index").toString());

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public UaMethodNode uaMethodNode;
    NodeFactoryCache nodeFactoryCache;

    public TreeFilterMethod(UaMethodNode uaMethodNode) {
        super(uaMethodNode);
        APP_NAME = uaMethodNode.getNodeContext().getServer().getConfig().getApplicationName().getText();
        nodeFactoryCache = new NodeFactoryCache(new RedisCommunication(uaMethodNode.getNodeContext().getServer()));
        this.uaMethodNode = uaMethodNode;
    }

    @Override
    public Argument[] getInputArguments() {
        return new Argument[]{query, tagFilter, ioFilter, conditionClassFilter, eventFilter, componentFilter, monitoringGroupFilter, offset, limit};
    }

    @Override
    public Argument[] getOutputArguments() {
        return new Argument[]{Result};
    }

    @Override
    protected Variant[] invoke(InvocationContext invocationContext, Variant[] inputValues) {
        logger.debug("Invoking TreeFilterMethod() method of objectId={}", invocationContext.getObjectId());
        String query = (String) inputValues[0].getValue();
        Boolean tagFilter = (Boolean) inputValues[1].getValue();
        Boolean ioFilter = (Boolean) inputValues[2].getValue();
        Boolean conditionClassFilter = (Boolean) inputValues[3].getValue();
        Boolean eventFilter = (Boolean) inputValues[4].getValue();
        Boolean componentFilter = (Boolean) inputValues[5].getValue();
        Boolean monitoringGroupFilter = (Boolean) inputValues[6].getValue();
        Long offset =Long.valueOf(inputValues[7].getValue().toString());
        Long limit =Long.valueOf(inputValues[8].getValue().toString());
        Long begin = System.currentTimeMillis();
        TreeNode tree = filter(query, tagFilter, ioFilter, conditionClassFilter, eventFilter, componentFilter, monitoringGroupFilter, offset, limit);
        logger.info("Tree Filter method calculate response in  " + (System.currentTimeMillis() - begin) + " milis");
        Gson gson = new Gson();
        return new Variant[]{new Variant(gson.toJson(tree))};
    }

    protected void validateInputArgumentValues(Variant[] inputArgumentValues) throws InvalidArgumentException {
        MethodInputValidator.forbiddenCharacter(this, query);
        MethodInputValidator.NotNull(this, limit,offset);
        MethodInputValidator.isNumber(this, limit,offset);
        MethodInputValidator.checkRange(this, limit, 10000l, "<");
        if (this.inputErrorMessages.size() != 0) {
            throw new InvalidArgumentException(null);
        }
    }

    public TreeNode filter(String query,
                           Boolean tagFilter,
                           Boolean ioFilter,
                           Boolean conditionClassFilter,
                           Boolean eventFilter,
                           Boolean componentFilter,
                           Boolean monitoringGroupFilter,
                           Long offset,
                           Long limit) {
        List<TreeNode> searchResultNodes = new ArrayList<>();
        boolean removeUnWantedConditionClass=false;
        boolean removeUnWantedGroup=false;
        if(eventFilter){
            if(!conditionClassFilter)
                removeUnWantedConditionClass=true;
            if(!monitoringGroupFilter)
                removeUnWantedGroup=true;
        }
        List<String> filters = new ArrayList<>();

        if (ioFilter)
            filters.add(APP_NAME + "/IO/");
        if (tagFilter)
            filters.add("/TAG/");
        if (eventFilter)
            filters.add(APP_NAME + "/Alarm&Events/");
        if (conditionClassFilter)
            filters.add(APP_NAME + "/Alarm&Events/ConditionClasses/");
        if (monitoringGroupFilter)
            filters.add(APP_NAME + "/Alarm&Events/Groups/");
        if (componentFilter)
            filters.add(APP_NAME + "/Runtime/General/");
        if (filters.size() == 0) {
            query= StringUtil.isNullOrEmpty(query)?APP_NAME:query;
            searchResultNodes = nodeFactoryCache.findSearchResults(query, offset, limit);
        }
        else
            searchResultNodes = nodeFactoryCache.filterTreeNode(query, filters, offset, limit, removeUnWantedConditionClass , removeUnWantedGroup);
        return searchResultTreeFormation(searchResultNodes);
    }
    private TreeNode searchResultTreeFormation(List<TreeNode> searchResults) {
        TreeNode resultTree = new TreeNode();
        Set<String> treeResultIdentifiers = new HashSet<>();
        for (TreeNode resultNode : searchResults) {
            String searchResultIdentifier = resultNode.getNode().getNodeId().getIdentifier();
            List<SerializableReference> resultUpReferences = nodeFactoryCache.findInverseReference(resultNode.getNode().getNodeId().getIdentifier());
            if (!treeResultIdentifiers.contains(searchResultIdentifier) || resultUpReferences.size() > 1) {
                if (resultNode.getNode().getNodeId().getNamespaceIndex() == APP_NAMESPACE_INDEX) {
                    for (SerializableReference resultUpReference : resultUpReferences)
                        try {
                            resultTree = attachingParentToTree(resultNode, resultTree, resultUpReference, treeResultIdentifiers);
                        } catch (Exception e) {
                            LoggerFactory.getLogger(getClass()).error(e.getMessage());
                        }
                }
            }
        }
        return resultTree;
    }

    private TreeNode attachingParentToTree(TreeNode resultNode, TreeNode resultTree, SerializableReference resultUpReference, Set<String> treeResultIdentifiers) throws Exception {
        String resultNodeIdentifier = resultNode.getNode().getNodeId().getIdentifier();
        treeResultIdentifiers.add(resultNodeIdentifier);
        String parentIdentifier = resultUpReference.getTargetNodeId().getIdentifier();
        TreeNode parent = findAvailableParent(parentIdentifier, resultTree);
        if (parent != null) {
            return attachToAvailableParent(resultNode, parent, resultTree, resultUpReference);
        } else {
            return attachToNotAvailableParent(resultNode, resultTree, resultUpReference, treeResultIdentifiers);
        }
    }

    private TreeNode attachToNotAvailableParent(TreeNode resultNode, TreeNode resultTree, SerializableReference resultUpReference, Set<String> treeResultIdentifiers) throws Exception {
        TreeNode parent = new TreeNode();
        SerializableUaNode parentSerializeUaNode = null;
        parentSerializeUaNode = nodeFactoryCache.getNode(new NodeId(resultUpReference.getTargetNodeId().getNamespaceIndex(), resultUpReference.getTargetNodeId().getIdentifier()));
        if (parentSerializeUaNode == null)
            throw new Exception("cant find parent for " + resultUpReference.getTargetNodeId().getIdentifier());
        parent.setNode(parentSerializeUaNode);
        treeResultIdentifiers.add(parentSerializeUaNode.getNodeId().getIdentifier());
        if (resultUpReference.getReferenceTypeId().getIdentifier().equals("35")) {
            resultNode.setLocation(parent.getNode().getNodeId().getIdentifier());
        }
        parent.addChildren(resultNode);

        if (parentSerializeUaNode.getNodeId().getIdentifier().equals(APP_NAME)) {
            return parent;
        } else {
            resultUpReference = nodeFactoryCache.findInverseReference(parent.getNode().getNodeId().getIdentifier()).get(0);
            return attachingParentToTree(parent, resultTree, resultUpReference, treeResultIdentifiers);
        }
    }

    private TreeNode attachToAvailableParent(TreeNode resultNode, TreeNode parent, TreeNode resultTree, SerializableReference serializableReference) {
        if (!isNodeOneOfChildren(resultNode.getNode().getNodeId().getIdentifier(), parent)) {
            if (serializableReference.getReferenceTypeId().getIdentifier().equals("35")) {
                resultNode.setLocation(parent.getNode().getNodeId().getIdentifier());
            }
            parent.addChildren(resultNode);

        }
        return resultTree;
    }

    private boolean isNodeOneOfChildren(String identifier, TreeNode parentLeaf) {
        for (TreeNode child : parentLeaf.getChildren()) {
            if (child.getNode().getNodeId().getIdentifier().equals(identifier)) {
                return true;
            }
        }
        return false;
    }

    private TreeNode findAvailableParent(String identifier, TreeNode treeNode) {
        if (treeNode.getNode() != null) {
            if (treeNode.getNode().getNodeId().getIdentifier().equals(identifier)) {
                return treeNode;
            }
            TreeNode result = null;
            for (TreeNode child : treeNode.getChildren()) {
                TreeNode temp = findAvailableParent(identifier, child);
                if (temp != null) {
                    result = temp;
                }
            }
            return result;
        } else {
            return null;
        }
    }

}
