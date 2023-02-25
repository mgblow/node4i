package org.eclipse.milo.platform.historian;

import org.eclipse.milo.opcua.sdk.core.Reference;
import org.eclipse.milo.opcua.sdk.server.AbstractLifecycle;
import org.eclipse.milo.opcua.sdk.server.nodes.UaFolderNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNodeContext;
import org.eclipse.milo.opcua.sdk.server.nodes.UaVariableNode;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.platform.historian.methods.SimpleArchiveMethod;
import org.eclipse.milo.platform.historian.observers.CompressedSimpleArchiveObserver;
import org.eclipse.milo.platform.historian.observers.SimpleArchiveObserver;
import org.eclipse.milo.platform.runtime.interfaces.UaInterface;
import org.eclipse.milo.platform.util.Props;
import org.eclipse.milo.platform.util.Utils;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;

public class Historian extends AbstractLifecycle {

    private static String ROOT_APP_NAME = null;
    private ScheduledThreadPoolExecutor executor = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(2);
    private Map<NodeId, SimpleArchiveObserver> simpleArchiveObserverMap = new ConcurrentHashMap<>();
    private static UaInterface uaInterface;
    UaNodeContext uaNodeContext;

    public Historian(UaNodeContext uaNodeContext) {
        this.uaNodeContext = uaNodeContext;
        this.uaInterface = UaInterface.getInstance(uaNodeContext);
        ROOT_APP_NAME = this.uaNodeContext.getServer().getConfig().getApplicationName().getText();
    }

    @Override
    protected void onStartup() {
        try {
            LoggerFactory.getLogger(getClass()).info("Starting [Historian] service, it might take a few seconds ...");
            // root folder node
            UaFolderNode rootNode = (UaFolderNode) uaNodeContext.getNodeManager().get(new NodeId(Integer.parseInt((String) Props.getProperty("app-namespace-index")), ROOT_APP_NAME));
            // add module folder
            UaFolderNode historianFolder = new UaFolderNode(uaNodeContext, Utils.newNodeId(ROOT_APP_NAME + "/Historian"), Utils.newQualifiedName("Historian"), new LocalizedText("Historian"));
            UaFolderNode simpleArchiveFolder = new UaFolderNode(uaNodeContext, Utils.newNodeId(ROOT_APP_NAME + "/Historian/SimpleArchiveFolder"), Utils.newQualifiedName(0, "SimpleArchive"), new LocalizedText("SimpleArchive"));
            uaNodeContext.getNodeManager().addNode(historianFolder);
            uaNodeContext.getNodeManager().addNode(simpleArchiveFolder);
            historianFolder.addOrganizes(simpleArchiveFolder);
            rootNode.addOrganizes(historianFolder);
            // bootstrap module methods
            injectMethods(historianFolder);
        } catch (Exception e) {
            LoggerFactory.getLogger(getClass()).error("[Historian] service, went through errors : {}", e.getMessage());
        }
    }

    @Override
    protected void onShutdown() {
        LoggerFactory.getLogger(getClass()).info("[Historian] service has been shutdown.");
    }

    public void addSimpleArchive(UaVariableNode uaNode) {
        SimpleArchiveObserver simpleArchiveObserver = new SimpleArchiveObserver(uaNodeContext.getServer());
        uaNode.addAttributeObserver(simpleArchiveObserver);
        this.simpleArchiveObserverMap.put(uaNode.getNodeId(), simpleArchiveObserver);
    }

    public void addCompressedArchive(UaVariableNode uaNode, Double tolerance) {
        CompressedSimpleArchiveObserver simpleArchiveObserver = new CompressedSimpleArchiveObserver(uaNodeContext.getServer(), tolerance);
        uaNode.addAttributeObserver(simpleArchiveObserver);
        this.simpleArchiveObserverMap.put(uaNode.getNodeId(), simpleArchiveObserver);
    }

    public void removeSimpleArchive(UaVariableNode uaNode) {
        SimpleArchiveObserver simpleArchiveObserver = this.simpleArchiveObserverMap.get(uaNode.getNodeId());
        uaNode.removeAttributeObserver(simpleArchiveObserver);
        this.simpleArchiveObserverMap.remove(simpleArchiveObserver);
    }

    private void injectMethods(UaFolderNode historianFolder) {
        UaMethodNode methodNode = UaMethodNode.builder(uaNodeContext).setNodeId(new NodeId(Integer.parseInt((String) Props.getProperty("app-namespace-index")), ROOT_APP_NAME + "/Historian/SimpleArchive")).setBrowseName(new QualifiedName(Integer.parseInt((String) Props.getProperty("app-namespace-index")), ROOT_APP_NAME + "/" + "Historian/SimpleArchive")).setDisplayName(new LocalizedText(null, "SimpleArchive")).setDescription(LocalizedText.english("SimpleArchive")).build();

        SimpleArchiveMethod simpleArchiveMethod = new SimpleArchiveMethod(methodNode, this);
        methodNode.setInputArguments(simpleArchiveMethod.getInputArguments());
        methodNode.setOutputArguments(simpleArchiveMethod.getOutputArguments());
        methodNode.setInvocationHandler(simpleArchiveMethod);
        uaNodeContext.getNodeManager().addNode(methodNode);
        methodNode.addReference(new Reference(methodNode.getNodeId(), Identifiers.HasComponent, historianFolder.getNodeId().expanded(), false));
    }
}
