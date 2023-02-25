package org.eclipse.milo.platform.server;

import org.eclipse.milo.opcua.sdk.core.Reference;
import org.eclipse.milo.opcua.sdk.server.AbstractLifecycle;
import org.eclipse.milo.opcua.sdk.server.nodes.UaFolderNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNodeContext;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.platform.server.methods.ExportDB;
import org.eclipse.milo.platform.server.methods.ImportDB;
import org.eclipse.milo.platform.util.Props;
import org.eclipse.milo.platform.util.Utils;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;

public class Server extends AbstractLifecycle {

    private static String ROOT_APP_NAME = null;
    private ScheduledThreadPoolExecutor executor = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(2);
    UaNodeContext uaNodeContext;

    public Server(UaNodeContext uaNodeContext) {
        this.uaNodeContext = uaNodeContext;
        ROOT_APP_NAME = this.uaNodeContext.getServer().getConfig().getApplicationName().getText();
    }

    @Override
    protected void onStartup() {
        try {
            LoggerFactory.getLogger(getClass()).info("Starting [Server] service, it might take a few seconds ...");
            UaFolderNode rootNode = (UaFolderNode) uaNodeContext.getNodeManager().get(new NodeId(Integer.parseInt((String) Props.getProperty("app-namespace-index")), ROOT_APP_NAME));
            UaFolderNode serverFolder = new UaFolderNode(uaNodeContext, Utils.newNodeId(ROOT_APP_NAME + "/Server"), Utils.newQualifiedName("Server"), new LocalizedText("Server"));
            uaNodeContext.getNodeManager().addNode(serverFolder);
            rootNode.addOrganizes(serverFolder);
            injectMethods(serverFolder);
        } catch (Exception e) {
            LoggerFactory.getLogger(getClass()).error("[Server] service, went through errors : {}", e.getMessage());
        }
    }

    @Override
    protected void onShutdown() {
        LoggerFactory.getLogger(getClass()).info("[Server] service has been shutdown.");
    }


    private void injectMethods(UaFolderNode exportFolder) {
        UaMethodNode importMethodNode = UaMethodNode.builder(uaNodeContext).setNodeId(new NodeId(Integer.parseInt((String) Props.getProperty("app-namespace-index")), ROOT_APP_NAME + "/Server/Import")).setBrowseName(new QualifiedName(Integer.parseInt((String) Props.getProperty("app-namespace-index")), ROOT_APP_NAME + "/Server/Import")).setDisplayName(new LocalizedText(null, "Import")).setDescription(LocalizedText.english("Import")).build();
        ImportDB importDb = new ImportDB(importMethodNode);
        importMethodNode.setInputArguments(importDb.getInputArguments());
        importMethodNode.setOutputArguments(importDb.getOutputArguments());
        importMethodNode.setInvocationHandler(importDb);
        uaNodeContext.getNodeManager().addNode(importMethodNode);
        importMethodNode.addReference(new Reference(importMethodNode.getNodeId(), Identifiers.HasComponent, exportFolder.getNodeId().expanded(), false));


        UaMethodNode exportMethodeNode = UaMethodNode.builder(uaNodeContext).setNodeId(new NodeId(Integer.parseInt((String) Props.getProperty("app-namespace-index")), ROOT_APP_NAME + "/Server/Export")).setBrowseName(new QualifiedName(Integer.parseInt((String) Props.getProperty("app-namespace-index")), ROOT_APP_NAME + "/Server/Export")).setDisplayName(new LocalizedText(null, "Export")).setDescription(LocalizedText.english("Export")).build();
        ExportDB exportDB = new ExportDB(exportMethodeNode);
        exportMethodeNode.setInputArguments(exportDB.getInputArguments());
        exportMethodeNode.setOutputArguments(exportDB.getOutputArguments());
        exportMethodeNode.setInvocationHandler(exportDB);
        uaNodeContext.getNodeManager().addNode(exportMethodeNode);
        exportMethodeNode.addReference(new Reference(exportMethodeNode.getNodeId(), Identifiers.HasComponent, exportFolder.getNodeId().expanded(), false));
    }
}

