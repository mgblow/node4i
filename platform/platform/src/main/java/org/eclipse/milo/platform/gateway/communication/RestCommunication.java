package org.eclipse.milo.platform.gateway.communication;

import org.eclipse.milo.opcua.sdk.server.nodes.UaNodeContext;
import org.eclipse.milo.opcua.sdk.server.nodes.UaVariableNode;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.platform.gateway.GatewayIdentifier;
import org.eclipse.milo.platform.util.Utils;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.stream.Collectors;

public class RestCommunication {
    private static String APP_NAME = null;
    private final UaNodeContext uaNodeContext;

    public RestCommunication(UaNodeContext uaNodeContext) {
        APP_NAME = uaNodeContext.getServer().getConfig().getApplicationName().getText();
        this.uaNodeContext = uaNodeContext;

    }

    public void receive(String deviceName, String inputUrl, String authorization, String name) throws Exception {
        final String tagId = GatewayIdentifier.REST_TAG.getIdentifier().replace("{app-name}", APP_NAME).replace("{io-name}", deviceName).replace("{tag-name}", name);
        final String valueId = GatewayIdentifier.VALUE_PROPERTY_IDENTIFIER.getIdentifier().replace("{tag-identifier}", tagId);
        NodeId nodeId = Utils.newNodeId(2, valueId);
        UaVariableNode node = (UaVariableNode) uaNodeContext.getNodeManager().get(nodeId);
        URL url = new URL(inputUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestProperty("accept", "application/json");
        connection.setRequestMethod("GET");
        if (!StringUtils.isEmpty(authorization)) {
            connection.setRequestProperty("Authorization", authorization);
        }
        InputStream responseStream = connection.getInputStream();

        String result = new BufferedReader(new InputStreamReader(responseStream))
                .lines().collect(Collectors.joining("\n"));
        DataValue nodeValue = new DataValue(new Variant((result)), StatusCode.GOOD);
        node.setValue(nodeValue);


    }

}
