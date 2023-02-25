package org.eclipse.milo.platform.gateway.io;

import org.eclipse.milo.opcua.sdk.server.nodes.UaNodeContext;
import org.eclipse.milo.opcua.sdk.server.nodes.UaObjectNode;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.platform.gateway.GatewayIdentifier;
import org.eclipse.milo.platform.gateway.communication.RestCommunication;
import org.eclipse.milo.platform.gateway.interfaces.Device;
import org.eclipse.milo.platform.gateway.interfaces.OpcUaDeviceClient;
import org.eclipse.milo.platform.util.LogUtil;
import org.eclipse.milo.platform.util.Utils;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class RestDevice implements Device, OpcUaDeviceClient {
    private final UaNodeContext uaNodeContext;
    private final RestCommunication restCommunication;
    private static String APP_NAME = null;
    ScheduledExecutorService exec;

    public RestDevice(UaNodeContext uaNodeContext) {
        this.uaNodeContext = uaNodeContext;
        APP_NAME = uaNodeContext.getServer().getConfig().getApplicationName().getText();
        this.restCommunication = new RestCommunication(uaNodeContext);
    }

    @Override
    public void turnOnDevice(String ioName) {
        final String deviceIdentifier = GatewayIdentifier.IO_IDENTIFIER.getIdentifier().replace("{app-name}", APP_NAME).replace("{io-name}", ioName);

        final String attributeProperty = GatewayIdentifier.IO_Property.getIdentifier().replace("{property-name}", "Attributes").replace("{io-identifier}", deviceIdentifier);
        UaObjectNode deviceAttributesNode = (UaObjectNode) this.uaNodeContext.getNodeManager().get(new NodeId(2, attributeProperty));

        final String configProperty = GatewayIdentifier.IO_Property.getIdentifier().replace("{property-name}", "Config").replace("{io-identifier}", deviceIdentifier);
        UaObjectNode deviceConfig = (UaObjectNode) this.uaNodeContext.getNodeManager().get(Utils.newNodeId(2, configProperty));

        final String url = Utils.getPropertyValue(deviceConfig, "Property/URL");
        HashMap<String, List<String>> urlsAndRepeatTime = new HashMap<>();
        Objects.requireNonNull(deviceAttributesNode).getComponentNodes().forEach(node -> {
            String name = Utils.getPropertyValue(node, "Property/name");
            String path = Utils.getPropertyValue(node, "Property/path");
            String repeatTime = Utils.getPropertyValue(node, "Property/repeatTime");
            String authorization = Utils.getPropertyValue(node, "Property/authorization");

            List pathAndRepeatTimeAndAuthorization = new ArrayList();
            pathAndRepeatTimeAndAuthorization.add(path);
            pathAndRepeatTimeAndAuthorization.add(repeatTime);
            pathAndRepeatTimeAndAuthorization.add(authorization);
            urlsAndRepeatTime.put(name, pathAndRepeatTimeAndAuthorization);
        });


        Thread thread = new Thread(() -> {
            for (String name : urlsAndRepeatTime.keySet()) {

                final List<String> pathAndIntervals = urlsAndRepeatTime.get(name);
                try {
                    final String path = StringUtils.isEmpty(pathAndIntervals.get(0)) ?
                            url :
                            url.concat(pathAndIntervals.get(0));
                    int repeatTime = Integer.parseInt(pathAndIntervals.get(1));
                    String authorization = pathAndIntervals.get(2);
                    exec = Executors.newSingleThreadScheduledExecutor();
                    exec.scheduleAtFixedRate(() -> {
                        try {
                            restCommunication.receive(ioName, path, authorization, name);
                        } catch (Exception e) {
                            LogUtil.getInstance().logAndFireEvent(getClass(),"error occurred about rest device " + ioName,"RESETDEVICE");
                        }
                    }, 0, repeatTime, TimeUnit.MILLISECONDS);
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        });
        executor.submit(thread);
    }

    @Override
    public void turnOffDevice(String ioName) {
        try {
            exec.shutdown();
        }catch (Exception e){
            LogUtil.getInstance().logAndFireEvent(getClass(),"error occurred in turn off rest device " + ioName,"RESETDEVICE");
        }
    }

}
