package org.eclipse.milo.platform.alamEvent;

import org.eclipse.milo.opcua.sdk.core.Reference;
import org.eclipse.milo.opcua.sdk.server.AbstractLifecycle;
import org.eclipse.milo.opcua.sdk.server.nodes.UaFolderNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNodeContext;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.platform.alamEvent.interfaces.UaAlarmEventInterface;
import org.eclipse.milo.platform.alamEvent.methods.*;
import org.eclipse.milo.platform.alamEvent.observers.AlarmConditionTypeObserver;
import org.eclipse.milo.platform.util.Props;
import org.eclipse.milo.platform.util.Utils;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AlarmEvent extends AbstractLifecycle {
    private static String ROOT_APP_NAME = null;
    private Map<NodeId, Map<NodeId, AlarmConditionTypeObserver>> baseEventTypeObserverHashMap = new ConcurrentHashMap<>();
    UaNodeContext uaNodeContext;

    private static UaAlarmEventInterface uaAlarmEventInterface;


    public AlarmEvent(UaNodeContext uaNodeContext) {
        this.uaNodeContext = uaNodeContext;
        this.uaAlarmEventInterface = UaAlarmEventInterface.getInstance(uaNodeContext);
        ROOT_APP_NAME = this.uaNodeContext.getServer().getConfig().getApplicationName().getText();
    }

    @Override
    protected void onStartup() {
        try {
            LoggerFactory.getLogger(getClass()).info("Starting [Alarm&Event] service, it might take a few seconds ...");
            // root folder node
            @Nullable UaFolderNode rootNode = (UaFolderNode) this.uaNodeContext.getNodeManager().get(new NodeId(Integer.parseInt((String) Props.getProperty("app-namespace-index")), ROOT_APP_NAME));
            // bootstrap module methods
            UaFolderNode alarmEventFolder = new UaFolderNode(uaNodeContext, Utils.newNodeId(ROOT_APP_NAME + "/Alarm&Events"), Utils.newQualifiedName("Alarm&Events"), new LocalizedText("Alarm&Events"));
            UaFolderNode alarmEventGroupsFolder = new UaFolderNode(uaNodeContext, Utils.newNodeId(ROOT_APP_NAME + "/Alarm&Events/Groups"), Utils.newQualifiedName("Groups"), new LocalizedText("Groups"));
            UaFolderNode conditionClassesFolder = new UaFolderNode(uaNodeContext, Utils.newNodeId(ROOT_APP_NAME + "/Alarm&Events/ConditionClasses"), Utils.newQualifiedName("ConditionClasses"), new LocalizedText("ConditionClasses"));
            UaFolderNode interfaceFolder = new UaFolderNode(uaNodeContext, Utils.newNodeId(ROOT_APP_NAME + "/Alarm&Events/Interfaces"), Utils.newQualifiedName("Interfaces"), new LocalizedText("Interfaces"));
            uaNodeContext.getNodeManager().addNode(alarmEventFolder);
            uaNodeContext.getNodeManager().addNode(interfaceFolder);
            uaNodeContext.getNodeManager().addNode(alarmEventGroupsFolder);
            uaNodeContext.getNodeManager().addNode(conditionClassesFolder);
            rootNode.addOrganizes(alarmEventFolder);
            alarmEventFolder.addOrganizes(alarmEventGroupsFolder);
            alarmEventFolder.addOrganizes(conditionClassesFolder);
            alarmEventFolder.addOrganizes(interfaceFolder);
            injectMethods(alarmEventFolder, conditionClassesFolder, alarmEventGroupsFolder);
        } catch (Exception e) {
            LoggerFactory.getLogger(getClass()).error("[Alarm&Event] service, went through errors : {}", e.getMessage());
        }
    }


    @Override
    protected void onShutdown() {
        LoggerFactory.getLogger(getClass()).info("[Alarm&Event] service has been shutdown.");
    }

    private void injectMethods(UaFolderNode alarmEventFolder, UaFolderNode conditionClassesFolder, UaFolderNode alarmEventGroupsFolder) {
        injectChangeAlarmEnabledMethod(alarmEventFolder);
        // add module folder
        UaMethodNode eventBootLoaderMethodNode = UaMethodNode.builder(uaNodeContext).setNodeId(new NodeId(Integer.parseInt((String) Props.getProperty("app-namespace-index")), ROOT_APP_NAME + "/Alarm&Events/BootLoader")).setBrowseName(new QualifiedName(Integer.parseInt((String) Props.getProperty("app-namespace-index")), ROOT_APP_NAME + "/Alarm&Events/BootLoader")).setDisplayName(new LocalizedText(null, "BootLoader")).setDescription(LocalizedText.english("BootLoader")).build();
        EventBootLoader eventBootLoader = new EventBootLoader(eventBootLoaderMethodNode, this);
        eventBootLoaderMethodNode.setInputArguments(eventBootLoader.getInputArguments());
        eventBootLoaderMethodNode.setOutputArguments(eventBootLoader.getOutputArguments());
        eventBootLoaderMethodNode.setInvocationHandler(eventBootLoader);

        UaMethodNode eventLoadMethodNode = UaMethodNode.builder(uaNodeContext).setNodeId(new NodeId(Integer.parseInt((String) Props.getProperty("app-namespace-index")), ROOT_APP_NAME + "/Alarm&Events/Load")).setBrowseName(new QualifiedName(Integer.parseInt((String) Props.getProperty("app-namespace-index")), ROOT_APP_NAME + "/Alarm&Events/Load")).setDisplayName(new LocalizedText(null, "Load")).setDescription(LocalizedText.english("Load")).build();
        EventLoadMethod eventLoadMethod = new EventLoadMethod(eventLoadMethodNode, this);
        eventLoadMethodNode.setInputArguments(eventLoadMethod.getInputArguments());
        eventLoadMethodNode.setOutputArguments(eventLoadMethod.getOutputArguments());
        eventLoadMethodNode.setInvocationHandler(eventLoadMethod);

        UaMethodNode eventUnLoadMethodNode = UaMethodNode.builder(uaNodeContext).setNodeId(new NodeId(Integer.parseInt((String) Props.getProperty("app-namespace-index")), ROOT_APP_NAME + "/Alarm&Events/UnLoad")).setBrowseName(new QualifiedName(Integer.parseInt((String) Props.getProperty("app-namespace-index")), ROOT_APP_NAME + "/Alarm&Events/UnLoad")).setDisplayName(new LocalizedText(null, "UnLoad")).setDescription(LocalizedText.english("UnLoad")).build();
        EventUnLoadMethod eventUnLoadMethod = new EventUnLoadMethod(eventUnLoadMethodNode, this);
        eventUnLoadMethodNode.setInputArguments(eventUnLoadMethod.getInputArguments());
        eventUnLoadMethodNode.setOutputArguments(eventUnLoadMethod.getOutputArguments());
        eventUnLoadMethodNode.setInvocationHandler(eventUnLoadMethod);


        UaMethodNode eventDeleteMethodNode = UaMethodNode.builder(uaNodeContext).setNodeId(new NodeId(Integer.parseInt((String) Props.getProperty("app-namespace-index")), ROOT_APP_NAME + "/Alarm&Events/Delete")).setBrowseName(new QualifiedName(Integer.parseInt((String) Props.getProperty("app-namespace-index")), ROOT_APP_NAME + "/Alarm&Events/Delete")).setDisplayName(new LocalizedText(null, "Delete")).setDescription(LocalizedText.english("Delete")).build();
        EventDeleteMethod eventDeleteMethod = new EventDeleteMethod(eventDeleteMethodNode, this);
        eventDeleteMethodNode.setInputArguments(eventDeleteMethod.getInputArguments());
        eventDeleteMethodNode.setOutputArguments(eventDeleteMethod.getOutputArguments());
        eventDeleteMethodNode.setInvocationHandler(eventDeleteMethod);

        UaMethodNode createEventsGroupMethod = UaMethodNode.builder(this.uaNodeContext).setNodeId(Utils.newNodeId(ROOT_APP_NAME + "/Alarm&Events/Groups/Create")).setBrowseName(Utils.newQualifiedName(ROOT_APP_NAME + "/Alarm&Events/Groups/Create")).setDisplayName(new LocalizedText(null, "Create")).setDescription(LocalizedText.english("Create a group of events")).build();
        CreateEventsGroup createEventsGroup = new CreateEventsGroup(createEventsGroupMethod);
        createEventsGroupMethod.setInputArguments(createEventsGroup.getInputArguments());
        createEventsGroupMethod.setOutputArguments(createEventsGroup.getOutputArguments());
        createEventsGroupMethod.setInvocationHandler(createEventsGroup);

        UaMethodNode eventAcknowledgeMethodNode = UaMethodNode.builder(uaNodeContext).setNodeId(new NodeId(Integer.parseInt((String) Props.getProperty("app-namespace-index")), ROOT_APP_NAME + "/Alarm&Events/Acknowledge")).setBrowseName(new QualifiedName(Integer.parseInt((String) Props.getProperty("app-namespace-index")), ROOT_APP_NAME + "/Alarm&Events/Acknowledge")).setDisplayName(new LocalizedText(null, "Acknowledge")).setDescription(LocalizedText.english("Acknowledge")).build();
        EventAcknowledgeMethod eventAcknowledgeMethod = new EventAcknowledgeMethod(eventAcknowledgeMethodNode, this);
        eventAcknowledgeMethodNode.setInputArguments(eventAcknowledgeMethod.getInputArguments());
        eventAcknowledgeMethodNode.setOutputArguments(eventAcknowledgeMethod.getOutputArguments());
        eventAcknowledgeMethodNode.setInvocationHandler(eventAcknowledgeMethod);

        UaMethodNode eventCurrentStateMethodNode = UaMethodNode.builder(uaNodeContext).setNodeId(new NodeId(Integer.parseInt((String) Props.getProperty("app-namespace-index")), ROOT_APP_NAME + "/Alarm&Events/EventCurrentState")).setBrowseName(new QualifiedName(Integer.parseInt((String) Props.getProperty("app-namespace-index")), ROOT_APP_NAME + "/Alarm&Events/EventCurrentState")).setDisplayName(new LocalizedText(null, "EventCurrentState")).setDescription(LocalizedText.english("EventCurrentState")).build();
        EventCurrentStateMethod eventCurrentStateMethod = new EventCurrentStateMethod(eventCurrentStateMethodNode, this);
        eventCurrentStateMethodNode.setInputArguments(eventCurrentStateMethod.getInputArguments());
        eventCurrentStateMethodNode.setOutputArguments(eventCurrentStateMethod.getOutputArguments());
        eventCurrentStateMethodNode.setInvocationHandler(eventCurrentStateMethod);

        UaMethodNode createConditionClassMethod = UaMethodNode.builder(this.uaNodeContext).setNodeId(Utils.newNodeId(ROOT_APP_NAME + "/Alarm&Events/ConditionClasses/Create")).setBrowseName(Utils.newQualifiedName(ROOT_APP_NAME + "/Alarm&Events/ConditionClasses/Create")).setDisplayName(new LocalizedText(null, "Create")).setDescription(LocalizedText.english("Create A Condition classes")).build();
        CreateConditionClass createConditionClass = new CreateConditionClass(createConditionClassMethod);
        createConditionClassMethod.setInputArguments(createConditionClass.getInputArguments());
        createConditionClassMethod.setOutputArguments(createConditionClass.getOutputArguments());
        createConditionClassMethod.setInvocationHandler(createConditionClass);

        UaMethodNode removeConditionClassMethod = UaMethodNode.builder(this.uaNodeContext).setNodeId(Utils.newNodeId(ROOT_APP_NAME + "/Alarm&Events/ConditionClasses/Delete")).setBrowseName(Utils.newQualifiedName(ROOT_APP_NAME + "/Alarm&Events/ConditionClasses/Delete")).setDisplayName(new LocalizedText(null, "Delete")).setDescription(LocalizedText.english("Delete A Condition classes")).build();
        RemoveConditionClass removeConditionClass = new RemoveConditionClass(removeConditionClassMethod, conditionClassesFolder.getNodeContext());
        removeConditionClassMethod.setInputArguments(removeConditionClass.getInputArguments());
        removeConditionClassMethod.setOutputArguments(removeConditionClass.getOutputArguments());
        removeConditionClassMethod.setInvocationHandler(removeConditionClass);

        UaMethodNode publishUncompletedEventLifeCycleMethodNode = UaMethodNode.builder(uaNodeContext).setNodeId(new NodeId(Integer.parseInt((String) Props.getProperty("app-namespace-index")), ROOT_APP_NAME + "/Alarm&Events/PublishUncompletedEventLifeCycle")).setBrowseName(new QualifiedName(Integer.parseInt((String) Props.getProperty("app-namespace-index")), ROOT_APP_NAME + "/Alarm&Events/PublishUncompletedEventLifeCycle")).setDisplayName(new LocalizedText(null, "PublishUncompletedEventLifeCycle")).setDescription(LocalizedText.english("PublishUncompletedEventLifeCycle")).build();
        PublishUncompletedEventLifeCycleMethod publishUncompletedEventLifeCycleMethod = new PublishUncompletedEventLifeCycleMethod(publishUncompletedEventLifeCycleMethodNode, this);
        publishUncompletedEventLifeCycleMethodNode.setInputArguments(publishUncompletedEventLifeCycleMethod.getInputArguments());
        publishUncompletedEventLifeCycleMethodNode.setOutputArguments(publishUncompletedEventLifeCycleMethod.getOutputArguments());
        publishUncompletedEventLifeCycleMethodNode.setInvocationHandler(publishUncompletedEventLifeCycleMethod);

        UaMethodNode eventArchivedStateMethodNode = UaMethodNode.builder(uaNodeContext).setNodeId(new NodeId(Integer.parseInt((String) Props.getProperty("app-namespace-index")), ROOT_APP_NAME + "/Alarm&Events/EventArchivedState")).setBrowseName(new QualifiedName(Integer.parseInt((String) Props.getProperty("app-namespace-index")), ROOT_APP_NAME + "/Alarm&Events/EventArchivedState")).setDisplayName(new LocalizedText(null, "EventArchivedState")).setDescription(LocalizedText.english("EventArchivedState")).build();
        EventArchivedStateMethod eventArchivedStateMethod = new EventArchivedStateMethod(eventArchivedStateMethodNode);
        eventArchivedStateMethodNode.setInputArguments(eventArchivedStateMethod.getInputArguments());
        eventArchivedStateMethodNode.setOutputArguments(eventArchivedStateMethod.getOutputArguments());
        eventArchivedStateMethodNode.setInvocationHandler(eventArchivedStateMethod);

        uaNodeContext.getNodeManager().addNode(eventBootLoaderMethodNode);
        uaNodeContext.getNodeManager().addNode(eventLoadMethodNode);
        uaNodeContext.getNodeManager().addNode(eventUnLoadMethodNode);
        uaNodeContext.getNodeManager().addNode(createConditionClassMethod);
        uaNodeContext.getNodeManager().addNode(createEventsGroupMethod);
        uaNodeContext.getNodeManager().addNode(removeConditionClassMethod);
        uaNodeContext.getNodeManager().addNode(eventAcknowledgeMethodNode);
        uaNodeContext.getNodeManager().addNode(eventCurrentStateMethodNode);
        uaNodeContext.getNodeManager().addNode(publishUncompletedEventLifeCycleMethodNode);
        uaNodeContext.getNodeManager().addNode(eventArchivedStateMethodNode);
        uaNodeContext.getNodeManager().addNode(eventDeleteMethodNode);

        eventUnLoadMethodNode.addReference(new Reference(eventBootLoaderMethodNode.getNodeId(), Identifiers.HasComponent, alarmEventFolder.getNodeId().expanded(), false));
        eventUnLoadMethodNode.addReference(new Reference(eventLoadMethodNode.getNodeId(), Identifiers.HasComponent, alarmEventFolder.getNodeId().expanded(), false));
        eventUnLoadMethodNode.addReference(new Reference(eventUnLoadMethodNode.getNodeId(), Identifiers.HasComponent, alarmEventFolder.getNodeId().expanded(), false));
        createConditionClassMethod.addReference(new Reference(createConditionClassMethod.getNodeId(), Identifiers.HasComponent, conditionClassesFolder.getNodeId().expanded(), false));
        createEventsGroupMethod.addReference(new Reference(createEventsGroupMethod.getNodeId(), Identifiers.HasComponent, alarmEventGroupsFolder.getNodeId().expanded(), false));
        removeConditionClassMethod.addReference(new Reference(removeConditionClassMethod.getNodeId(), Identifiers.HasComponent, conditionClassesFolder.getNodeId().expanded(), false));
        eventAcknowledgeMethodNode.addReference(new Reference(eventAcknowledgeMethodNode.getNodeId(), Identifiers.HasComponent, alarmEventFolder.getNodeId().expanded(), false));
        eventCurrentStateMethodNode.addReference(new Reference(eventCurrentStateMethodNode.getNodeId(), Identifiers.HasComponent, alarmEventFolder.getNodeId().expanded(), false));
        eventCurrentStateMethodNode.addReference(new Reference(publishUncompletedEventLifeCycleMethodNode.getNodeId(), Identifiers.HasComponent, alarmEventFolder.getNodeId().expanded(), false));
        eventArchivedStateMethodNode.addReference(new Reference(eventArchivedStateMethodNode.getNodeId(), Identifiers.HasComponent, alarmEventFolder.getNodeId().expanded(), false));
        eventDeleteMethodNode.addReference(new Reference(eventDeleteMethodNode.getNodeId(), Identifiers.HasComponent, alarmEventFolder.getNodeId().expanded(), false));
    }

    private void injectChangeAlarmEnabledMethod(UaFolderNode alarmEventFolder) {
        UaMethodNode changeEnabledMethodNode = UaMethodNode.builder(uaNodeContext).setNodeId(new NodeId(Integer.parseInt((String) Props.getProperty("app-namespace-index")), ROOT_APP_NAME + "/Alarm&Events/ChangeEnabled")).setBrowseName(new QualifiedName(Integer.parseInt((String) Props.getProperty("app-namespace-index")), ROOT_APP_NAME + "/Alarm&Events/ChangeEnabled")).setDisplayName(new LocalizedText(null, "ChangeEnabled")).setDescription(LocalizedText.english("ChangeEnabled")).build();
        ChangeEnableStateAlarm changeEnabledMethod = new ChangeEnableStateAlarm(changeEnabledMethodNode);
        changeEnabledMethodNode.setInputArguments(changeEnabledMethod.getInputArguments());
        changeEnabledMethodNode.setOutputArguments(changeEnabledMethod.getOutputArguments());
        changeEnabledMethodNode.setInvocationHandler(changeEnabledMethod);
        uaNodeContext.getNodeManager().addNode(changeEnabledMethodNode);
        changeEnabledMethodNode.addReference(new Reference(changeEnabledMethodNode.getNodeId(), Identifiers.HasComponent, alarmEventFolder.getNodeId().expanded(), false));
    }

    public void addEventToNode(UaNode uaNode, UaNode eventNode, String script) {
        AlarmConditionTypeObserver alarmConditionTypeObserver = new AlarmConditionTypeObserver(uaNodeContext, script);
        uaNode.addAttributeObserver(alarmConditionTypeObserver);
        if (this.baseEventTypeObserverHashMap.get(eventNode.getNodeId()) == null) {
            this.baseEventTypeObserverHashMap.put(eventNode.getNodeId(), new HashMap<>());
        }
        this.baseEventTypeObserverHashMap.get(eventNode.getNodeId()).put(uaNode.getNodeId(), alarmConditionTypeObserver);
    }

    public void unload(UaNode eventNode) throws Exception {
        LoggerFactory.getLogger(getClass()).info("removed event for event : {}", eventNode.getNodeId());
        if (this.baseEventTypeObserverHashMap.get(eventNode.getNodeId()) != null) {
            this.baseEventTypeObserverHashMap.get(eventNode.getNodeId()).forEach((nodeId, alarmConditionTypeObserver) -> {
                uaNodeContext.getNodeManager().get(nodeId).removeAttributeObserver(alarmConditionTypeObserver);
            });
        }
        getUaNodeContext().getNodeManager().cacheNodeManager().getTriggerParentFactoryCache().deleteParent(eventNode.getNodeId().getIdentifier().toString());
    }

    public void removeEvent(UaNode eventNode) {
        try {
            LoggerFactory.getLogger(getClass()).info("removed event for event : {}", eventNode.getNodeId());
            unload(eventNode);
            eventNode.delete();
        } catch (Exception e) {
            LoggerFactory.getLogger(getClass()).error("error in: removeEvent() for nodeId : {} and eventId : {}", e.getMessage(), eventNode.getNodeId());
        }
    }

    public UaNodeContext getUaNodeContext() {
        return uaNodeContext;
    }
}
