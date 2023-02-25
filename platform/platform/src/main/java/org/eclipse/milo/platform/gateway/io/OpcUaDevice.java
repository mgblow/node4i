package org.eclipse.milo.platform.gateway.io;

import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaMonitoredItem;
import org.eclipse.milo.opcua.sdk.client.subscriptions.ManagedDataItem;
import org.eclipse.milo.opcua.sdk.client.subscriptions.ManagedSubscription;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNodeContext;
import org.eclipse.milo.opcua.sdk.server.nodes.UaObjectNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaVariableNode;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.platform.gateway.GatewayIdentifier;
import org.eclipse.milo.platform.gateway.interfaces.Device;
import org.eclipse.milo.platform.gateway.interfaces.OpcUaDeviceClient;
import org.eclipse.milo.platform.util.LogUtil;
import org.eclipse.milo.platform.util.Utils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class OpcUaDevice implements Device, OpcUaDeviceClient {
    private static String APP_NAME = null;
    private UaNodeContext uaNodeContext;
    private Map<String, OpcUaClient> clients = new ConcurrentHashMap<>();
    private Map<String, OpcUaClient> writeClients = new ConcurrentHashMap<>();
    private Map<String, Thread> subscriptions = new HashMap<>();

    public OpcUaDevice(UaNodeContext uaNodeContext) {
        APP_NAME = uaNodeContext.getServer().getConfig().getApplicationName().getText();
        this.uaNodeContext = uaNodeContext;
    }


    @Override
    public void turnOnDevice(String ioName) {
        String deviceIdentifier = GatewayIdentifier.IO_IDENTIFIER.getIdentifier().replace("{app-name}", APP_NAME).replace("{io-name}", ioName);

        Thread thread = new Thread(new Runnable() {
            public void run() {
                try {
                    if (clients.get(ioName) == null) {
                        addDeviceClient(ioName);
                    }
                    ManagedSubscription subscription = ManagedSubscription.create(clients.get(ioName));
                    subscription.addDataChangeListener((items, values) -> {
                        for (int i = 0; i < items.size(); i++) {
                            logger.info("ManagedSubscription value received: for device : {} item={}, value={}", ioName, items.get(i).getNodeId(), values.get(i).getValue());
                            Variant value = values.get(i).getValue();
                            String nodeId = items.get(i).getNodeId().getIdentifier().toString();

                            final String tagId = GatewayIdentifier.OPC_TAG.getIdentifier().replace("{tag-name}", nodeId).replace("{app-name}",APP_NAME).replace("{io-name}",ioName);
                            final String valueId = GatewayIdentifier.VALUE_PROPERTY_IDENTIFIER.getIdentifier().replace("{tag-identifier}", tagId);

                            NodeId uaNode = new NodeId(2, valueId);
                            DataValue nodeValue = new DataValue(value, StatusCode.GOOD);
                            UaVariableNode node = (UaVariableNode) uaNodeContext.getNodeManager().get(uaNode);
                            if (node == null) {
                                LogUtil.getInstance().logAndFireEvent(getClass(), "ManagedSubscription failed to set value. no definition for io22Name : " + ioName + " in node : " + nodeId, "OPCDEVICE");
                            } else {
                                node.setValue(nodeValue);
                            }
                        }
                    });

                    try {

                        final String attributeProperty = GatewayIdentifier.IO_Property.getIdentifier().replace("{property-name}", "Attributes").replace("{io-identifier}", deviceIdentifier);
                        UaObjectNode deviceAttributesNode = (UaObjectNode) uaNodeContext.getNodeManager().get(new NodeId(2, attributeProperty));

                        deviceAttributesNode.getComponentNodes().forEach(node -> {
                            String nodeId = Utils.getPropertyValue((UaObjectNode) node, "Property/identifier");
                            int namespaceIndex = Integer.parseInt(Utils.getPropertyValue((UaObjectNode) node, "Property/namespaceIndex"));
                            // switch numeric nodeIds
                            NodeId reportingNodeId = new NodeId(namespaceIndex, nodeId);
                            if (Utils.isNumeric(nodeId)) {
                                reportingNodeId = new NodeId(namespaceIndex, Integer.parseInt(nodeId));
                            }
                            try {
                                ManagedDataItem dataItem = subscription.createDataItem(reportingNodeId);
                                if (dataItem.getStatusCode().isGood()) {
                                    logger.info("ManagedSubscription item created for nodeId={}", dataItem.getNodeId());
                                } else {
                                    LogUtil.getInstance().logAndFireEvent(getClass(), "ManagedSubscription failed to create item for nodeId=" + dataItem.getNodeId() + " (status=" + dataItem.getStatusCode() + ")", "OPCDEVICE");
                                }
                            } catch (UaException e) {
                                throw new RuntimeException(e);
                            }
                        });
                    } catch (Exception e) {
                        LogUtil.getInstance().logAndFireEvent(getClass(), "failed to create Managed Subscription for device : " + ioName + " with error : " + e.getMessage(), "OPCDEVICE");
                    }
                } catch (Exception e) {
                    LogUtil.getInstance().logAndFireEvent(getClass(), "failed to create subscription for device : " + ioName + " with error : " + e.getMessage(), "OPCDEVICE");
                }
            }

            private void onSubscriptionValue(UaMonitoredItem uaMonitoredItem, DataValue dataValue) {
                String deviceName = "";
                for (Map.Entry<String, OpcUaClient> client : clients.entrySet()) {
                    if (client.getValue().equals(uaMonitoredItem.getClient())) {
                        deviceName = client.getKey();
                    }
                }
                Object value = dataValue.getValue().getValue();
                String nodeId = uaMonitoredItem.getReadValueId().getNodeId().getIdentifier().toString();
                final String valueId = GatewayIdentifier.VALUE_PROPERTY_IDENTIFIER.getIdentifier().replace("{tag-identifier}", nodeId);

                NodeId uaNode = new NodeId(2, valueId);
                DataValue nodeValue = new DataValue(new Variant(value), StatusCode.GOOD);
                UaVariableNode node = (UaVariableNode) uaNodeContext.getNodeManager().get(uaNode);
                if (node == null) {
                    logger.error("no definition for deviceName : {} in node : {}", "topic", nodeId);
                } else {
                    node.setValue(nodeValue);
                }
            }
        });
        executor.submit(thread);
        this.subscriptions.put(ioName, thread);
    }


    @Override
    public void turnOffDevice(String ioName) {
        if (this.clients.get(ioName) != null) {
            if (this.subscriptions.get(ioName) != null) {
                this.subscriptions.get(ioName).interrupt();
                this.subscriptions.remove(ioName);
            }
            this.clients.get(ioName).disconnect();
            this.clients.remove(ioName);
        }
    }

    @Override
    public void restartDevice(String deviceName) {
        this.turnOffDevice(deviceName);
        this.turnOnDevice(deviceName);
    }

    public void addDeviceClient(String deviceName) {
        try {
            String deviceIdentifier = GatewayIdentifier.IO_IDENTIFIER.getIdentifier().replace("{app-name}", APP_NAME).replace("{io-name}", deviceName);
            final String configProperty = GatewayIdentifier.IO_Property.getIdentifier().replace("{property-name}", "Config").replace("{io-identifier}", deviceIdentifier);
            UaObjectNode deviceConfigNode = (UaObjectNode) uaNodeContext.getNodeManager().get(new NodeId(2, configProperty));

            if (clients.get(deviceName) == null) {
                OpcUaClient client = createOpcUaClient(Utils.getPropertyValue(deviceConfigNode, "Property/URL"), Utils.getPropertyValue(deviceConfigNode, "Property/POLICY"), Utils.getPropertyValue(deviceConfigNode, "Property/USERNAME"), Utils.getPropertyValue(deviceConfigNode, "Property/PASSWORD"));
                client.connect().get();
                this.clients.put(deviceName, client);
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
    }

    public OpcUaClient getClients(String deviceNAme) {
        return clients.get(deviceNAme);
    }

    public OpcUaClient getWriteClients(String deviceNAme) {
        return writeClients.get(deviceNAme);
    }

    public OpcUaDevice setWriteClients(Map<String, OpcUaClient> writeClients) {
        this.writeClients = writeClients;
        return this;
    }

    public void setWriteClients(String deviceName, OpcUaClient client) {
        this.writeClients.put(deviceName, client);
    }
}
