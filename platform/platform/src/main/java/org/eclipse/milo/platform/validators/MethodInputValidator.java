package org.eclipse.milo.platform.validators;

import com.mongodb.client.model.Filters;
import org.apache.commons.lang3.EnumUtils;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.eclipse.milo.opcua.sdk.server.api.methods.AbstractMethodInvocationHandler;
import org.eclipse.milo.opcua.sdk.server.api.persistence.RedisCommunication;
import org.eclipse.milo.opcua.sdk.server.api.persistence.caches.EventFactoryCache;
import org.eclipse.milo.opcua.sdk.server.drivers.MongoDriver;
import org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNode;
import org.eclipse.milo.opcua.sdk.server.nodes.serializable.SerializableAlarmConditionTypeNode;
import org.eclipse.milo.opcua.sdk.server.nodes.serializable.SerializableTriggerParent;
import org.eclipse.milo.opcua.stack.core.types.enumerated.NodeClass;
import org.eclipse.milo.opcua.stack.core.types.structured.Argument;
import org.eclipse.milo.platform.util.ArrayUtils;
import org.eclipse.milo.platform.util.StringUtils;
import org.eclipse.milo.platform.util.Utils;
import org.eclipse.milo.platform.validators.scriptValidator.UaAlarmEventInterface;
import org.eclipse.milo.platform.validators.scriptValidator.UaInterface;
import org.eclipse.milo.platform.validators.scriptValidator.Window;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class MethodInputValidator {
    public static void NotNull(AbstractMethodInvocationHandler abstractMethodInvocationHandler, Argument... elements) {
        for (Argument argument : elements) {
            int index = ArrayUtils.indexOf(abstractMethodInvocationHandler.getInputArguments(), argument);
            if (index != -1) {
                String name = argument.getName();
                Object value = abstractMethodInvocationHandler.inputArgumentValues[index].getValue();
                if (value instanceof String[]) {
                    String[] array = (String[]) value;
                    if (array.length == 0) {
                        abstractMethodInvocationHandler.inputErrorMessages.add(name.toUpperCase() + "_IS_EMPTY");
                    }
                    for (int i = 0; i < array.length; i++) {
                        if (StringUtils.isNullOrEmpty(array[i]))
                            abstractMethodInvocationHandler.inputErrorMessages.add(name.toUpperCase() + "[" + i + "]" + "_ISNULL");
                    }
                } else {
                    if (StringUtils.isNullOrEmpty(value))
                        abstractMethodInvocationHandler.inputErrorMessages.add(name.toUpperCase() + "_ISNULL");
                }
            }
        }
    }

    public static void isNumber(AbstractMethodInvocationHandler abstractMethodInvocationHandler, Argument... elements) {
        for (Argument argument : elements) {
            int index = ArrayUtils.indexOf(abstractMethodInvocationHandler.getInputArguments(), argument);
            if (index != -1) {
                String name = argument.getName();
                Object value = abstractMethodInvocationHandler.inputArgumentValues[index].getValue();
                if (!StringUtils.isNullOrEmpty(value) && !StringUtils.isNumeric(value)) {
                    abstractMethodInvocationHandler.inputErrorMessages.add(name.toUpperCase() + "_IS_NOT_NUMERIC");
                }
            }
        }
    }

    public static void isBoolean(AbstractMethodInvocationHandler abstractMethodInvocationHandler, Argument... elements) {
        for (Argument argument : elements) {
            int index = ArrayUtils.indexOf(abstractMethodInvocationHandler.getInputArguments(), argument);
            if (index != -1) {
                String name = argument.getName();
                Object value = abstractMethodInvocationHandler.inputArgumentValues[index].getValue();
                if (!StringUtils.isNullOrEmpty(value) && !StringUtils.isBoolean(value)) {
                    abstractMethodInvocationHandler.inputErrorMessages.add(name.toUpperCase() + "_IS_NOT_BOOLEAN");
                }
            }
        }
    }

    public static void Exists(AbstractMethodInvocationHandler abstractMethodInvocationHandler, Argument... elements) {
        UaMethodNode uaMethodNode = abstractMethodInvocationHandler.getNode();
        String APP_NAME = uaMethodNode.getNodeContext().getServer().getConfig().getApplicationName().getText();
        for (Argument argument : elements) {
            int index = ArrayUtils.indexOf(abstractMethodInvocationHandler.getInputArguments(), argument);
            if (index != 1) {
                String name = argument.getName();
                Object value = abstractMethodInvocationHandler.inputArgumentValues[index].getValue();
                if (!StringUtils.isNullOrEmpty(value)) {
                    if (value instanceof String[]) {
                        String[] array = (String[]) value;
                        for (int i = 0; i < array.length; i++) {
                            UaNode uaNode = uaMethodNode.getNodeManager().get(Utils.newNodeId(array[i]));
                            if (uaNode == null)
                                abstractMethodInvocationHandler.inputErrorMessages.add(name.toUpperCase() + "[" + i + "]" + "_NOT_EXIST");
                        }
                    } else {
                        UaNode uaNode = uaMethodNode.getNodeManager().get(Utils.newNodeId(value.toString()));
                        if (uaNode == null)
                            abstractMethodInvocationHandler.inputErrorMessages.add(name.toUpperCase() + "_NOT_EXIST");
                    }
                }
            }
        }
    }

    public static void Exists(AbstractMethodInvocationHandler abstractMethodInvocationHandler, Argument argument, String prefix) {
        UaMethodNode uaMethodNode = abstractMethodInvocationHandler.getNode();
        int index = ArrayUtils.indexOf(abstractMethodInvocationHandler.getInputArguments(), argument);
        if (index != 1) {
            String name = argument.getName();
            Object value = abstractMethodInvocationHandler.inputArgumentValues[index].getValue();
            if (!StringUtils.isNullOrEmpty(value)) {
                if (value instanceof String[]) {
                    String[] array = (String[]) value;
                    for (int i = 0; i < array.length; i++) {
                        UaNode uaNode = uaMethodNode.getNodeManager().get(Utils.newNodeId(prefix + array[i]));
                        if (uaNode == null)
                            abstractMethodInvocationHandler.inputErrorMessages.add(name.toUpperCase() + "[" + i + "]" + "_NOT_EXIST");
                    }
                } else {
                    UaNode uaNode = uaMethodNode.getNodeManager().get(Utils.newNodeId(prefix + value));
                    if (uaNode == null)
                        abstractMethodInvocationHandler.inputErrorMessages.add(name.toUpperCase() + "_NOT_EXIST");
                }
            }
        }
    }

    public static void checkRange(AbstractMethodInvocationHandler abstractMethodInvocationHandler, Argument argument, Long range, String sign) {
        int index = ArrayUtils.indexOf(abstractMethodInvocationHandler.getInputArguments(), argument);
        if (index != -1) {
            String name = argument.getName();
            Object value = abstractMethodInvocationHandler.inputArgumentValues[index].getValue();
            if (!StringUtils.isNullOrEmpty(value) && StringUtils.isNumeric(value)) {
                boolean result = false;
                if (sign.equals(">")) {
                    result = Long.parseLong(value.toString()) >= range;
                } else if (sign.equals("<")) {
                    result = Long.parseLong(value.toString()) <= range;
                }
                if (!result)
                    abstractMethodInvocationHandler.inputErrorMessages.add(name.toUpperCase() + "_SHOULD_" + (sign.equals(">") ? "BIGGER" : "SMALLER") + "_THAN_" + range);
            }
        }
    }

    public static void validateIp(AbstractMethodInvocationHandler abstractMethodInvocationHandler, Argument argument) {
        int index = ArrayUtils.indexOf(abstractMethodInvocationHandler.getInputArguments(), argument);
        if (index != -1) {
            String name = argument.getName();
            Object value = abstractMethodInvocationHandler.inputArgumentValues[index].getValue();
            if (!StringUtils.isNullOrEmpty(value)) {
                String valueStr = value.toString();
                if (value.toString().startsWith("http://") ||
                        value.toString().startsWith("ssl://") ||
                        value.toString().startsWith("failover://tcp://"))
                    valueStr = valueStr.split("//")[valueStr.split("//").length - 1];
                String IPV4_PATTERN = "^(([0-9]|[1-9][0-9]|1[0-9][0-9]|2[0-4][0-9]|25[0-5])(\\.(?!$)|$)){4}$";
                Pattern pattern = Pattern.compile(IPV4_PATTERN);
                Matcher matcher = pattern.matcher(valueStr);
                if (!matcher.matches())
                    abstractMethodInvocationHandler.inputErrorMessages.add(name.toUpperCase() + "_IS_NOT_VALID");
            }
        }
    }

    public static void validateScript(AbstractMethodInvocationHandler abstractMethodInvocationHandler, Argument argument) {
        int index = ArrayUtils.indexOf(abstractMethodInvocationHandler.getInputArguments(), argument);
        if (index != -1) {
            String name = argument.getName();
            Object value = abstractMethodInvocationHandler.inputArgumentValues[index].getValue();
            if (!StringUtils.isNullOrEmpty(value)) {
                try {
                    String script = value.toString();
                    script = script.replace("return true", "console.log('return true')");
                    script = script.replace("return false", "console.log('return false')");
                    UaInterface uaInterface = new UaInterface();
                    Window window = new Window();
                    UaAlarmEventInterface uaAlarmEvent = new UaAlarmEventInterface();
                    Context engineContext = Context.newBuilder("js")
                            .allowAllAccess(true)
                            .allowIO(true)
                            .build();
                    engineContext.getBindings("js").putMember("uaInterface", uaInterface);
                    engineContext.getBindings("js").putMember("window", window);
                    engineContext.getBindings("js").putMember("uaAlarmEventInterface", uaAlarmEvent);
                    engineContext.eval(Source.create("js", script));
                } catch (Exception e) {
                    abstractMethodInvocationHandler.inputErrorMessages.add(name.toUpperCase() + "_IS_INCORRECT");

                }
            }
        }
    }

    public static void checkEventIdInMongo(AbstractMethodInvocationHandler abstractMethodInvocationHandler, Argument argument) {
        int index = ArrayUtils.indexOf(abstractMethodInvocationHandler.getInputArguments(), argument);
        if (index != -1) {
            String name = argument.getName();
            Object value = abstractMethodInvocationHandler.inputArgumentValues[index].getValue();
            if (!StringUtils.isNullOrEmpty(value)) {
                List<String> values = new ArrayList<>();
                if (value instanceof String[]) {
                    values = Arrays.asList((String[]) value);
                } else {
                    values.add(value.toString());
                }
                MongoDriver mongoDriver;
                mongoDriver = new MongoDriver("events");
                int i = 0;
                for (String string : values) {
                    List<Bson> filters = new ArrayList<>();
                    List<Document> documents = new ArrayList<>();
                    if (string != null) {
                        filters.add(Filters.eq("eventId", string));
                        documents.addAll(mongoDriver.findDocuments(filters, true));
                    }
                    if (documents.size() == 0) {
                        String message = "_NOT_EXISTS";
                        if (values.size() > 1)
                            message = String.format("[%s]%s", i, message);
                        abstractMethodInvocationHandler.inputErrorMessages.add(name.toUpperCase() + message);
                    }
                    i++;
                }
            }
        }
    }

    public static void checkExistenceEvent(AbstractMethodInvocationHandler abstractMethodInvocationHandler, Argument argument) {
        int index = ArrayUtils.indexOf(abstractMethodInvocationHandler.getInputArguments(), argument);
        if (index != -1) {
            String name = argument.getName();
            Object value = abstractMethodInvocationHandler.inputArgumentValues[index].getValue();
            if (!StringUtils.isNullOrEmpty(value)) {
                List<String> values = new ArrayList<>();
                if (value instanceof String[]) {
                    values = Arrays.asList((String[]) value);
                } else {
                    values.add(value.toString());
                }
                EventFactoryCache eventFactoryCache = new EventFactoryCache(new RedisCommunication(abstractMethodInvocationHandler.getNode().getNodeContext().getServer()));
                int i = 0;
                for (String string : values) {
                    if (string != null) {
                        List<SerializableAlarmConditionTypeNode> serializableAlarmConditionTypeNodes = eventFactoryCache.findByEventId(string);
                        if (serializableAlarmConditionTypeNodes.size() == 0) {
                            String message = "_NOT_EXISTS";
                            if (values.size() > 1)
                                message = String.format("[%s]%s", i, message);
                            abstractMethodInvocationHandler.inputErrorMessages.add(name.toUpperCase() + message);
                        }
                        i++;
                    }
                }
            }
        }
    }

    public static void onlyOne(AbstractMethodInvocationHandler abstractMethodInvocationHandler, Argument... arguments) {
        int counter = 0;
        String names = "";
        for (Argument argument : arguments) {
            int index = ArrayUtils.indexOf(abstractMethodInvocationHandler.getInputArguments(), argument);
            if (index != -1) {
                String name = argument.getName();
                names += name + "_";
                Object value = abstractMethodInvocationHandler.inputArgumentValues[index].getValue();
                if (value instanceof String[]) {
                    if (!ArrayUtils.isNullOrEmpty((String[]) value))
                        ++counter;
                } else if (value instanceof String) {
                    if (!StringUtils.isNullOrEmpty(value))
                        ++counter;
                }
            }
        }
        if (counter == 0) {
            abstractMethodInvocationHandler.inputErrorMessages.add("ONE_OF_" + names.toUpperCase() + "SHOULD_HAVE_VALUE");
        }
        if (counter > 1) {
            abstractMethodInvocationHandler.inputErrorMessages.add("ONLY_ONE OF_" + names.toUpperCase() + "CAN_HAVE_VALUE");
        }
    }

    public static void Equal(AbstractMethodInvocationHandler abstractMethodInvocationHandler, Argument argument, String... strings) {
        int index = ArrayUtils.indexOf(abstractMethodInvocationHandler.getInputArguments(), argument);
        if (index != 1) {
            String name = argument.getName();
            Object value = abstractMethodInvocationHandler.inputArgumentValues[index].getValue();
            if (!StringUtils.isNullOrEmpty(value)) {
                if (!Arrays.stream(strings).anyMatch(value::equals))
                    abstractMethodInvocationHandler.inputErrorMessages.add(name.toUpperCase() + "_SHOULD_BE_ONE_OF_" + String.join(",", strings).toUpperCase());
            }
        }
    }

    public static void validateAuthorization(AbstractMethodInvocationHandler abstractMethodInvocationHandler, Argument argument) {
        int index = ArrayUtils.indexOf(abstractMethodInvocationHandler.getInputArguments(), argument);
        if (index != 1) {
            String name = argument.getName();
            Object value = abstractMethodInvocationHandler.inputArgumentValues[index].getValue();
            if (!StringUtils.isNullOrEmpty(value)) {
                if (!value.toString().toLowerCase().startsWith("bearer ") && !value.toString().toLowerCase().startsWith("basic ")) {
                    abstractMethodInvocationHandler.inputErrorMessages.add(name.toUpperCase() + "_NOT_VALID");
                }
            }
        }
    }

    public static void validateMqttTopic(AbstractMethodInvocationHandler abstractMethodInvocationHandler, Argument argument) {
        int index = ArrayUtils.indexOf(abstractMethodInvocationHandler.getInputArguments(), argument);
        if (index != 1) {
            String name = argument.getName();
            Object value = abstractMethodInvocationHandler.inputArgumentValues[index].getValue();
            if (!StringUtils.isNullOrEmpty(value)) {
                if (value.toString().contains("+") || value.toString().contains("#")) {
                    abstractMethodInvocationHandler.inputErrorMessages.add(name.toUpperCase() + "_NOT_VALID");
                }
            }
        }
    }

    public static void forbiddenCharacter(AbstractMethodInvocationHandler abstractMethodInvocationHandler, Argument... elements) {
        for (Argument argument : elements) {
            int index = ArrayUtils.indexOf(abstractMethodInvocationHandler.getInputArguments(), argument);
            if (index != -1) {
                String name = argument.getName();
                Object value = abstractMethodInvocationHandler.inputArgumentValues[index].getValue();
                String str = "";
                if (value instanceof String[]) {
                    str = String.join("", (String[]) value);
                } else if (value instanceof String) {
                    str = value.toString();
                }
                if (StringUtils.includeSpecialCharacter(str)) {
                    abstractMethodInvocationHandler.inputErrorMessages.add(name.toUpperCase() + "_CONTAINS_SPECIAL_CHARACTER");
                }
            }
        }
    }

    public static void checkType(AbstractMethodInvocationHandler abstractMethodInvocationHandler, Argument argument, NodeClass nodeClass) {
        int index = ArrayUtils.indexOf(abstractMethodInvocationHandler.getInputArguments(), argument);
        if (index != 1) {
            try {
                String name = argument.getName();
                String value = abstractMethodInvocationHandler.inputArgumentValues[index].getValue().toString();
                UaNode uaNode = abstractMethodInvocationHandler.getNode().getNodeManager().get(Utils.newNodeId(value));
                if (uaNode.getNodeClass() != nodeClass) {
                    abstractMethodInvocationHandler.inputErrorMessages.add(name.toUpperCase() + "_MUST_BE_" + nodeClass.toString().toUpperCase());
                }
            }catch (Exception e){/*do nothing*/}
        }
    }

    public static void isTriggerNode(AbstractMethodInvocationHandler abstractMethodInvocationHandler, Argument argument) {
        int index = ArrayUtils.indexOf(abstractMethodInvocationHandler.getInputArguments(), argument);
        String name = argument.getName();
        Object identifier = abstractMethodInvocationHandler.inputArgumentValues[index].getValue();
        final SerializableTriggerParent trigger = abstractMethodInvocationHandler.getNode().getNodeContext().getNodeManager().cacheNodeManager().getTriggerParentFactoryCache().findTriggerParentsByTriggerIdentifier(identifier.toString());
        if (trigger.getParents() != null && trigger.getParents().size() >= 1) {
            abstractMethodInvocationHandler.inputErrorMessages.add(name.toUpperCase() + "_IS_A_TRIGGER");
        }
    }
    public static void validateKafkaTopic(AbstractMethodInvocationHandler abstractMethodInvocationHandler, Argument argument) {
        int index = ArrayUtils.indexOf(abstractMethodInvocationHandler.getInputArguments(), argument);
        if (index != -1) {
            String name = argument.getName();
            Object value = abstractMethodInvocationHandler.inputArgumentValues[index].getValue();
            if (!StringUtils.isNullOrEmpty(value)) {
                String topic = value.toString();
                //according to https://github.com/apache/kafka/blob/0.10.2/core/src/main/scala/kafka/common/Topic.scala#L24
                int maxNameLength = 249;
                if (!Pattern.compile("[a-zA-Z0-9\\._\\-]+").matcher(topic).matches()
                        ||topic.length()>maxNameLength
                        || topic.equals(".")
                        || topic.equals(".."))
                    abstractMethodInvocationHandler.inputErrorMessages.add(name.toUpperCase() + "_IS_NOT_VALID");
            }
        }
    }

    public static <E extends Enum<E>> void isValidEnum(AbstractMethodInvocationHandler abstractMethodInvocationHandler, Class<E> enumClass, Argument argument) {
        int index = ArrayUtils.indexOf(abstractMethodInvocationHandler.getInputArguments(), argument);
        if (index != 1) {
            String name = argument.getName();
            String value = abstractMethodInvocationHandler.inputArgumentValues[index].getValue().toString();

            if (!EnumUtils.isValidEnum(enumClass, value)) {
                abstractMethodInvocationHandler.inputErrorMessages.add(name.toUpperCase() + "_IS_NOT_VALID");
            }

        }
    }
}
