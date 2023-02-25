package org.eclipse.milo.platform;

import org.eclipse.milo.opcua.sdk.core.AccessLevel;
import org.eclipse.milo.opcua.sdk.core.Reference;
import org.eclipse.milo.opcua.sdk.server.OpcUaServer;
import org.eclipse.milo.opcua.sdk.server.api.DataItem;
import org.eclipse.milo.opcua.sdk.server.api.ManagedNamespaceWithLifecycle;
import org.eclipse.milo.opcua.sdk.server.api.MonitoredItem;
import org.eclipse.milo.opcua.sdk.server.dtd.DataTypeDictionaryManager;
import org.eclipse.milo.opcua.sdk.server.nodes.UaFolderNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNodeContext;
import org.eclipse.milo.opcua.sdk.server.nodes.UaVariableNode;
import org.eclipse.milo.opcua.sdk.server.util.SubscriptionModel;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.StatusCodes;
import org.eclipse.milo.opcua.stack.core.types.builtin.*;
import org.eclipse.milo.opcua.stack.core.types.enumerated.NodeClass;
import org.eclipse.milo.opcua.stack.core.types.structured.AddNodesItem;
import org.eclipse.milo.opcua.stack.core.types.structured.AddNodesResult;
import org.eclipse.milo.opcua.stack.core.types.structured.DeleteNodesItem;
import org.eclipse.milo.opcua.stack.core.types.structured.WriteValue;
import org.eclipse.milo.platform.alamEvent.AlarmEvent;
import org.eclipse.milo.platform.boot.BootLoader;
import org.eclipse.milo.platform.server.Server;
import org.eclipse.milo.platform.gateway.Gateway;
import org.eclipse.milo.platform.historian.Historian;
import org.eclipse.milo.platform.runtime.RunTime;
import org.eclipse.milo.platform.filter.Filter;
import org.eclipse.milo.platform.util.LogUtil;
import org.eclipse.milo.platform.util.Props;
import org.eclipse.milo.platform.util.RestrictedAccessFilter;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class Namespace extends ManagedNamespaceWithLifecycle {

    public static final String NAMESPACE_URI = "urn:fanap:" + Props.getProperty("app-name") + ":server";
    public static String ROOT_APP_NAME = null;

    private final DataTypeDictionaryManager dictionaryManager;

    private final SubscriptionModel subscriptionModel;
    private final RunTime runTime;
    private final Historian historian;
    private final Filter filter;
    private final Server server;
    private final Gateway gateway;
    private final AlarmEvent alarmEvent;

    private final BootLoader bootLoader;

    Namespace(OpcUaServer server) {
        super(server, NAMESPACE_URI);
        ROOT_APP_NAME = this.getServer().getConfig().getApplicationName().getText();
        subscriptionModel = new SubscriptionModel(server, this);
        dictionaryManager = new DataTypeDictionaryManager(getNodeContext(), NAMESPACE_URI);
        runTime = new RunTime(getNodeContext());
        historian = new Historian(getNodeContext());
        filter = new Filter(getNodeContext());
        this.server = new Server(getNodeContext());
        alarmEvent = new AlarmEvent(getNodeContext());
        gateway = Gateway.getInstance(getNodeContext());
        bootLoader = new BootLoader(getNodeContext());
        LogUtil logUtil = LogUtil.getInstance();
        logUtil.setUaNodeContext(getNodeContext());
        getLifecycleManager().addLifecycle(dictionaryManager);
        getLifecycleManager().addLifecycle(subscriptionModel);
        getLifecycleManager().addLifecycle(runTime);
        getLifecycleManager().addLifecycle(alarmEvent);
        getLifecycleManager().addLifecycle(gateway);
        getLifecycleManager().addLifecycle(historian);
        getLifecycleManager().addLifecycle(filter);
        getLifecycleManager().addLifecycle(this.server);
        getLifecycleManager().addLifecycle(bootLoader);
        bootstrapNodes();
    }

    private void bootstrapNodes() {
        // create root folder for startup
        UaFolderNode rootFolder = new UaFolderNode(getNodeContext(), newNodeId(ROOT_APP_NAME), newQualifiedName(ROOT_APP_NAME), LocalizedText.english(ROOT_APP_NAME));
        getNodeManager().addNode(rootFolder);
        rootFolder.addReference(new Reference(rootFolder.getNodeId(), Identifiers.Organizes, Identifiers.RootFolder.expanded(), false));
        {
            String timeNodeName = "TIME";
            String scheduledNodeName = "Scheduled";
            NodeId typeId = Identifiers.String;
            Variant variant = new Variant(new DateTime());
            UaVariableNode node = new UaVariableNode.UaVariableNodeBuilder(getNodeContext()).setNodeId(newNodeId("TIME")).setAccessLevel(AccessLevel.READ_WRITE).setBrowseName(newQualifiedName(timeNodeName)).setDisplayName(LocalizedText.english(timeNodeName)).setDataType(typeId).setTypeDefinition(Identifiers.BaseDataVariableType).build();
            UaVariableNode scheduledNode = new UaVariableNode.UaVariableNodeBuilder(getNodeContext()).setNodeId(newNodeId(scheduledNodeName)).setAccessLevel(AccessLevel.READ_WRITE).setBrowseName(newQualifiedName(scheduledNodeName)).setDisplayName(LocalizedText.english(scheduledNodeName)).setDataType(typeId).setTypeDefinition(Identifiers.BaseDataVariableType).build();
            node.setValue(new DataValue(variant));
            scheduledNode.setValue(new DataValue(variant));
            getNodeManager().addNode(node);
            rootFolder.addComponent(node);
            getNodeManager().addNode(scheduledNode);
            rootFolder.addComponent(scheduledNode);
            this.getServer().getScheduledExecutorService().scheduleWithFixedDelay(new Runnable() {
                @Override
                public void run() {
                    ((UaVariableNode) getNodeManager().getNode(newNodeId("TIME")).get()).setValue(new DataValue(new Variant(new DateTime())));
                }
            }, 0, 1, TimeUnit.SECONDS);

            this.getServer().getScheduledExecutorService().scheduleWithFixedDelay(new Runnable() {
                @Override
                public void run() {
                    ((UaVariableNode) getNodeManager().getNode(newNodeId("Scheduled")).get()).setValue(new DataValue(new Variant(new DateTime())));
                }
            }, 0, 100, TimeUnit.MILLISECONDS);
        }
    }


    @Override
    public void onDataItemsCreated(List<DataItem> dataItems) {
        subscriptionModel.onDataItemsCreated(dataItems);
    }

    @Override
    public void onDataItemsModified(List<DataItem> dataItems) {
        subscriptionModel.onDataItemsModified(dataItems);
    }

    @Override
    public void onDataItemsDeleted(List<DataItem> dataItems) {
        subscriptionModel.onDataItemsDeleted(dataItems);
    }

    @Override
    public void onMonitoringModeChanged(List<MonitoredItem> monitoredItems) {
        subscriptionModel.onMonitoringModeChanged(monitoredItems);
    }


    @Override
    public void deleteNodes(DeleteNodesContext context, List<DeleteNodesItem> nodesToDelete) {
        StatusCode statusCode = new StatusCode(StatusCodes.Good_EntryInserted);
        try {
            getNodeManager().get(nodesToDelete.get(0).getNodeId().expanded(), context.getServer().getNamespaceTable()).delete();
        } catch (Exception e) {
            statusCode = new StatusCode(StatusCodes.Bad_NotSupported);
        }
        context.success(Collections.nCopies(nodesToDelete.size(), statusCode));
    }

    public AddNodesResult addNode(AddNodesItem nodeToAdd) {
        AddNodesResult result;
        try {
            UaNode node = null;
            AddNodesItem nodeItem = nodeToAdd;
            NodeId newNodeId = nodeItem.getRequestedNewNodeId().toNodeId(getServer().getNamespaceTable()).get();
            NodeId parentNodeId = nodeItem.getParentNodeId().toNodeId(getServer().getNamespaceTable()).get();
            QualifiedName browsName = nodeItem.getBrowseName();
            UaNode newNode = getNodeManager().get(newNodeId);
            if (newNode != null) {
                if (newNode.getNodeClass() != nodeItem.getNodeClass()) {
                    throw new Exception();
                }
                for (Reference reference : newNode.getReferences()) {
                    if (reference.getDirection().name().equals("INVERSE")) newNode.removeReference(reference);
                }
                newNode.addReference(new Reference(newNode.getNodeId(), Identifiers.Organizes, parentNodeId.expanded(), false));
            }
            LocalizedText displayName = LocalizedText.english(nodeItem.getBrowseName().getName());
            if (nodeItem.getNodeClass() == NodeClass.Object) {
                node = new UaFolderNode(getNodeContext(), newNodeId, browsName, displayName);
            } else if (nodeItem.getNodeClass() == NodeClass.Variable) {
                NodeId typeId = nodeItem.getTypeDefinition().toNodeId(getServer().getNamespaceTable()).get();
                Variant variant = new Variant(0);
                node = new UaVariableNode.UaVariableNodeBuilder(getNodeContext()).setNodeId(newNodeId).setAccessLevel(AccessLevel.READ_WRITE).setUserAccessLevel(AccessLevel.READ_WRITE).setBrowseName(browsName).setDisplayName(displayName).setDataType(typeId).setTypeDefinition(Identifiers.BaseDataVariableType).build();
                UaVariableNode uaVariableNode = ((UaVariableNode) node);
                uaVariableNode.setValue(new DataValue(variant));
                //0node.getFilterChain().addLast(new AttributeLoggingFilter(AttributeId.Value::equals));

                uaVariableNode.getFilterChain().addLast(new RestrictedAccessFilter(identity -> {
                    return AccessLevel.READ_WRITE;
//
//                    if ("admin".equals(identity)) {
//                        return AccessLevel.READ_WRITE;
//                    } else {
//                        return AccessLevel.NONE;
//                    }
                }));
            }


            getNodeManager().addNode(node);
            UaNode parentNode = getNodeManager().get(parentNodeId);
            if (parentNodeId.isNotNull()) ((UaFolderNode) parentNode).addOrganizes(node);
            return new AddNodesResult(new StatusCode(StatusCodes.Good_EntryInserted), newNodeId);
        } catch (Exception e) {
            return new AddNodesResult(new StatusCode(StatusCodes.Bad_NotSupported), NodeId.NULL_VALUE);
        }
    }

    @Override
    public void addNodes(AddNodesContext context, List<AddNodesItem> nodesToAdd) {
        AddNodesResult result = addNode(nodesToAdd.get(0));
        context.success(Collections.nCopies(nodesToAdd.size(), result));
    }

    @Override
    public void write(WriteContext context, List<WriteValue> writeValues) {
        super.write(context, writeValues);
    }

    public UaNodeContext returnNodeContext() {
        return getNodeContext();
    }


}
