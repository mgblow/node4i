package org.eclipse.milo.platform.util;

import com.google.gson.Gson;
import org.eclipse.milo.opcua.sdk.server.nodes.UaFolderNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNodeContext;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;
import java.util.Random;

public class Utils {
    static Logger logger = LoggerFactory.getLogger(Utils.class);
    private static final int ROOT_APP_NAMESPACE_INDEX = Integer.parseInt(Props.getProperty("app-namespace-index").toString());

    public static String getPropertyValue(UaNode rootNode, String property) {
        try {
            return rootNode.getPropertyNode(property).get().getValue().getValue().getValue().toString();
        } catch (Exception e) {
            logger.info("the property [{}] value could not be found for : {}", property, rootNode.getNodeId());
            return null;
        }
    }
    public static String getPropertyValueDevice(UaNode rootNode, String property) {
        try {
            return rootNode.getPropertyNode(property).get().getValue().getValue().getValue().toString();
        } catch (Exception e) {
            logger.info("the property [{}] value could not be found for : {}", property, rootNode.getNodeId());
            return null;
        }
    }
    public static String getIdentifierValue(UaNode uaNode) {
        try {
            return uaNode.getNodeId().getIdentifier().toString();
        } catch (Exception e) {
            return null;
        }
    }

    public static <T> String toJson(T input) {
        Gson gson = new Gson();
        return gson.toJson(input);
    }

    public static Variant changeDataType(String input, String datatype) {
        switch (datatype.toLowerCase(Locale.ROOT)) {
            case "int32":
            case "int16":
            case "int":
            case "int64":
                return new Variant(Integer.valueOf(input));
            case "double":
                return new Variant(Double.valueOf(input));
            case "boolean":
                return new Variant(Boolean.valueOf(input));
            case "float":
                return new Variant(Float.valueOf(input));
            default:
                return new Variant(input);

        }
    }

    public static String[] fromJsonToArrayOfString(String input) {
        Gson gson = new Gson();
        return gson.fromJson(input, String[].class);
    }

    public static UaFolderNode buildAndAddFolder(UaNodeContext uaNodeContext, UaFolderNode rootNode, String identifier, String name) {
        String ROOT_APP_NAME = uaNodeContext.getServer().getConfig().getApplicationName().getText();
        UaFolderNode folder = new UaFolderNode(uaNodeContext, new NodeId(Integer.parseInt((String) Props.getProperty("app-namespace-index")), ROOT_APP_NAME + identifier), new QualifiedName(Integer.parseInt((String) Props.getProperty("app-namespace-index")), ROOT_APP_NAME + identifier), LocalizedText.english(name));
        uaNodeContext.getNodeManager().addNode(folder);
        rootNode.addOrganizes(folder);
        return folder;
    }

    public static UaMethodNode buildAndAddMethodNode(UaNodeContext uaNodeContext, String reference, String identifier, String displayName) {
        String ROOT_APP_NAME = uaNodeContext.getServer().getConfig().getApplicationName().getText();
        return UaMethodNode.builder(uaNodeContext).setNodeId(new NodeId(Integer.parseInt((String) Props.getProperty("app-namespace-index")), ROOT_APP_NAME + "/" + reference)).setBrowseName(new QualifiedName(Integer.parseInt((String) Props.getProperty("app-namespace-index")), ROOT_APP_NAME + "/" + identifier)).setDisplayName(new LocalizedText(null, displayName)).setDescription(LocalizedText.english(displayName)).build();
    }


    // This is a handy util that checks if a string is number or not
    public static boolean isNumeric(String str) {
        return str.matches("-?\\d+(\\.\\d+)?");
    }

    public static QualifiedName newQualifiedName(String qualifiedName) {
        return new QualifiedName(ROOT_APP_NAMESPACE_INDEX, qualifiedName);

    }

    public static QualifiedName newQualifiedName(int index, String qualifiedName) {
        return new QualifiedName(index, qualifiedName);
    }

    public static NodeId newNodeId(String identifier) {
        return new NodeId(ROOT_APP_NAMESPACE_INDEX, identifier);
    }

    public static NodeId newNodeId(int index, String identifier) {
        return new NodeId(index, identifier);
    }

    public static String generateRandomString() {
        int len = 32;
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz_";
        Random rnd = new Random();
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++)
            sb.append(chars.charAt(rnd.nextInt(chars.length())));
        return sb.toString();
    }

}
