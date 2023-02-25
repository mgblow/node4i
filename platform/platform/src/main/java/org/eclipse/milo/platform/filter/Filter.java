package org.eclipse.milo.platform.filter;

import org.eclipse.milo.opcua.sdk.core.Reference;
import org.eclipse.milo.opcua.sdk.server.AbstractLifecycle;
import org.eclipse.milo.opcua.sdk.server.nodes.UaFolderNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNodeContext;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.platform.filter.methods.*;
import org.eclipse.milo.platform.util.Props;
import org.eclipse.milo.platform.util.Utils;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;

public class Filter extends AbstractLifecycle {

    private static String ROOT_APP_NAME = null;
    private ScheduledThreadPoolExecutor executor = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(2);
    UaNodeContext uaNodeContext;

    public Filter(UaNodeContext uaNodeContext) {
        this.uaNodeContext = uaNodeContext;
        ROOT_APP_NAME = this.uaNodeContext.getServer().getConfig().getApplicationName().getText();
    }

    @Override
    protected void onStartup() {
        try {
            LoggerFactory.getLogger(getClass()).info("Starting [Filter] service, it might take a few seconds ...");
            UaFolderNode rootNode = (UaFolderNode) uaNodeContext.getNodeManager().get(new NodeId(Integer.parseInt((String) Props.getProperty("app-namespace-index")), ROOT_APP_NAME));
            UaFolderNode filterFolder = new UaFolderNode(uaNodeContext, Utils.newNodeId(ROOT_APP_NAME + "/Filter"), Utils.newQualifiedName("Filter"), new LocalizedText("Filter"));
            uaNodeContext.getNodeManager().addNode(filterFolder);
            rootNode.addOrganizes(filterFolder);
            injectMethods(filterFolder);
        } catch (Exception e) {
            LoggerFactory.getLogger(getClass()).error("[Filter] service, went through errors : {}", e.getMessage());
        }
    }

    @Override
    protected void onShutdown() {
        LoggerFactory.getLogger(getClass()).info("[Filter] service has been shutdown.");
    }


    private void injectMethods(UaFolderNode filterFolder) {
        UaMethodNode treeFilterMethodNode = UaMethodNode.builder(uaNodeContext).setNodeId(new NodeId(Integer.parseInt((String) Props.getProperty("app-namespace-index")), ROOT_APP_NAME + "/Filter/TreeFilterMethod")).setBrowseName(new QualifiedName(Integer.parseInt((String) Props.getProperty("app-namespace-index")), ROOT_APP_NAME + "/Filter/TreeFilterMethod")).setDisplayName(new LocalizedText(null, "TreeFilterMethod")).setDescription(LocalizedText.english("TreeFilterMethod")).build();
        TreeFilterMethod treeFilterMethod = new TreeFilterMethod(treeFilterMethodNode);
        treeFilterMethodNode.setInputArguments(treeFilterMethod.getInputArguments());
        treeFilterMethodNode.setOutputArguments(treeFilterMethod.getOutputArguments());
        treeFilterMethodNode.setInvocationHandler(treeFilterMethod);
        uaNodeContext.getNodeManager().addNode(treeFilterMethodNode);
        treeFilterMethodNode.addReference(new Reference(treeFilterMethodNode.getNodeId(), Identifiers.HasComponent, filterFolder.getNodeId().expanded(), false));

        UaMethodNode browseMethodNode = UaMethodNode.builder(uaNodeContext).setNodeId(new NodeId(Integer.parseInt((String) Props.getProperty("app-namespace-index")), ROOT_APP_NAME + "/Filter/BrowseMethod")).setBrowseName(new QualifiedName(Integer.parseInt((String) Props.getProperty("app-namespace-index")), ROOT_APP_NAME + "/Filter/BrowseMethod")).setDisplayName(new LocalizedText(null, "BrowseMethod")).setDescription(LocalizedText.english("BrowseMethod")).build();
        BrowseMethod browseMethod = new BrowseMethod(browseMethodNode);
        browseMethodNode.setInputArguments(browseMethod.getInputArguments());
        browseMethodNode.setOutputArguments(browseMethod.getOutputArguments());
        browseMethodNode.setInvocationHandler(browseMethod);
        uaNodeContext.getNodeManager().addNode(browseMethodNode);
        browseMethodNode.addReference(new Reference(browseMethodNode.getNodeId(), Identifiers.HasComponent, filterFolder.getNodeId().expanded(), false));

        UaMethodNode searchMethodNode = UaMethodNode.builder(uaNodeContext).setNodeId(new NodeId(Integer.parseInt((String) Props.getProperty("app-namespace-index")), ROOT_APP_NAME + "/Filter/SearchInLocationMethod")).setBrowseName(new QualifiedName(Integer.parseInt((String) Props.getProperty("app-namespace-index")), ROOT_APP_NAME + "/Filter/SearchInLocationMethod")).setDisplayName(new LocalizedText(null, "SearchInLocationMethod")).setDescription(LocalizedText.english("SearchInLocationMethod")).build();
        SearchInLocationMethod searchInLocationMethod = new SearchInLocationMethod(searchMethodNode);
        searchMethodNode.setInputArguments(searchInLocationMethod.getInputArguments());
        searchMethodNode.setOutputArguments(searchInLocationMethod.getOutputArguments());
        searchMethodNode.setInvocationHandler(searchInLocationMethod);
        uaNodeContext.getNodeManager().addNode(searchMethodNode);
        searchMethodNode.addReference(new Reference(searchMethodNode.getNodeId(), Identifiers.HasComponent, filterFolder.getNodeId().expanded(), false));


        UaMethodNode findByIdentifierMethodNode = UaMethodNode.builder(uaNodeContext).setNodeId(new NodeId(Integer.parseInt((String) Props.getProperty("app-namespace-index")), ROOT_APP_NAME + "/Filter/FindByIdentifierMethod")).setBrowseName(new QualifiedName(Integer.parseInt((String) Props.getProperty("app-namespace-index")), ROOT_APP_NAME + "/Filter/FindByIdentifierMethod")).setDisplayName(new LocalizedText(null, "FindByIdentifierMethod")).setDescription(LocalizedText.english("FindByIdentifierMethod")).build();
        FindByIdentifierMethod findByIdentifierMethod = new FindByIdentifierMethod(findByIdentifierMethodNode);
        findByIdentifierMethodNode.setInputArguments(findByIdentifierMethod.getInputArguments());
        findByIdentifierMethodNode.setOutputArguments(findByIdentifierMethod.getOutputArguments());
        findByIdentifierMethodNode.setInvocationHandler(findByIdentifierMethod);
        uaNodeContext.getNodeManager().addNode(findByIdentifierMethodNode);
        findByIdentifierMethodNode.addReference(new Reference(findByIdentifierMethodNode.getNodeId(), Identifiers.HasComponent, filterFolder.getNodeId().expanded(), false));

        UaMethodNode simpleArchiveReportMethodNode = UaMethodNode.builder(uaNodeContext).setNodeId(new NodeId(Integer.parseInt((String) Props.getProperty("app-namespace-index")), ROOT_APP_NAME + "/Filter/SimpleArchiveReport")).setBrowseName(new QualifiedName(Integer.parseInt((String) Props.getProperty("app-namespace-index")), ROOT_APP_NAME + "/Filter/SimpleArchiveReport")).setDisplayName(new LocalizedText(null, "SimpleArchiveReport")).setDescription(LocalizedText.english("SimpleArchiveReport")).build();
        SimpleArchiveReport simpleArchiveMethod = new SimpleArchiveReport(simpleArchiveReportMethodNode);
        simpleArchiveReportMethodNode.setInputArguments(simpleArchiveMethod.getInputArguments());
        simpleArchiveReportMethodNode.setOutputArguments(simpleArchiveMethod.getOutputArguments());
        simpleArchiveReportMethodNode.setInvocationHandler(simpleArchiveMethod);
        uaNodeContext.getNodeManager().addNode(simpleArchiveReportMethodNode);
        simpleArchiveReportMethodNode.addReference(new Reference(simpleArchiveReportMethodNode.getNodeId(), Identifiers.HasComponent, filterFolder.getNodeId().expanded(), false));

    }
}

