package org.eclipse.milo.platform.alamEvent.methods;

import com.google.gson.Gson;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.eclipse.milo.opcua.sdk.core.ValueRanks;
import org.eclipse.milo.opcua.sdk.server.api.methods.AbstractMethodInvocationHandler;
import org.eclipse.milo.opcua.sdk.server.api.methods.InvalidArgumentException;
import org.eclipse.milo.opcua.sdk.server.drivers.MongoDriver;
import org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode;
import org.eclipse.milo.opcua.sdk.server.nodes.serializable.SerializableAlarmConditionTypeNode;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.structured.Argument;
import org.eclipse.milo.platform.util.Props;
import org.eclipse.milo.platform.util.StringUtils;
import org.eclipse.milo.platform.validators.MethodInputValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class EventArchivedStateMethod extends AbstractMethodInvocationHandler {

    public static final Argument eventDefinitionNames = new Argument("eventDefinitionNames", Identifiers.String, ValueRanks.OneDimension, new UInteger[10], new LocalizedText("", "eventDefinitionNames"));
    public static final Argument eventIds = new Argument("eventIds", Identifiers.String, ValueRanks.OneDimension, new UInteger[10], new LocalizedText("", "eventIds"));
    public static final Argument severity = new Argument("severity", Identifiers.String, ValueRanks.Any, null, new LocalizedText("", "severity"));
    public static final Argument activeState = new Argument("activeState", Identifiers.String, ValueRanks.Any, null, new LocalizedText("", "activeState"));
    public static final Argument acknowledgeState = new Argument("acknowledgeState", Identifiers.String, ValueRanks.Any, null, new LocalizedText("", "acknowledgeState"));
    public static final Argument retain = new Argument("retain", Identifiers.String, ValueRanks.Any, null, new LocalizedText("", "retain"));
    public static final Argument from = new Argument("from", Identifiers.String, ValueRanks.Any, null, new LocalizedText("from"));
    public static final Argument to = new Argument("to", Identifiers.String, ValueRanks.Any, null, new LocalizedText("to"));
    public static final Argument offset = new Argument("offset", Identifiers.String, ValueRanks.Any, null, new LocalizedText("offset"));
    public static final Argument limit = new Argument("limit", Identifiers.String, ValueRanks.Any, null, new LocalizedText("limit"));
    public static final Argument Result = new Argument("result", Identifiers.String, ValueRanks.Any, null, new LocalizedText("result"));

    public static String APP_NAME = Props.getProperty("app-name").toString();
    public static final int APP_NAMESPACE_INDEX = Integer.parseInt(Props.getProperty("app-namespace-index").toString());
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private UaMethodNode uaMethodNode;
    private static String ALARM_EVENT_DOCUMENT = "events";

    public EventArchivedStateMethod(UaMethodNode uaMethodNode) {
        super(uaMethodNode);
        APP_NAME = uaMethodNode.getNodeContext().getServer().getConfig().getApplicationName().getText();
        this.uaMethodNode = uaMethodNode;
    }

    @Override
    public Argument[] getInputArguments() {
        return new Argument[]{eventDefinitionNames, eventIds, severity, activeState, acknowledgeState, retain, from, to, offset, limit};
    }

    @Override
    public Argument[] getOutputArguments() {
        return new Argument[]{Result};
    }

    @Override
    protected Variant[] invoke(InvocationContext invocationContext, Variant[] inputValues) {
        try {
            int index = -1;
            Map<String, List<String>> orMap = new HashMap<>();
            Map<String, String> andMap = new HashMap<>();
            for (Argument input : this.getInputArguments()) {
                index++;
                String name = input.getName();
                Object value = inputValues[index].getValue();
                if (name.equals("to"))
                    andMap.put("to", String.valueOf(value == null ? System.currentTimeMillis() : value));
                else if (name.equals("from"))
                    andMap.put("from", String.valueOf(value == null ? System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000) : value));
                else if (value != null && !value.equals("")) {
                    switch (name) {
                        case "eventDefinitionNames":
                            List<String> result = new ArrayList<>();
                            String[] nameArray = (String[]) value;
                            if (nameArray.length != 0) {
                                for (String string : Arrays.asList(nameArray)) {
                                    string = APP_NAME + "/Alarm&Events/" + string;
                                    result.add(string);
                                }
                                orMap.put("sourceNode", result);
                            }
                            break;
                        case "eventIds":
                            List<String> idResult = new ArrayList<>();
                            String[] idArray = (String[]) value;
                            if (idArray.length != 0) {
                                for (String string : Arrays.asList(idArray)) {
                                    idResult.add(string);
                                }
                                orMap.put("eventId", idResult);
                            }
                            break;
                        case "limit":
                        case "offset":
                            break;
                        default:
                            andMap.put(name, value.toString());
                    }
                }
            }

            Long offset = StringUtils.isNullOrEmpty(inputValues[8].getValue()) ? 0 : Long.valueOf(inputValues[8].getValue().toString());
            Long limit = StringUtils.isNullOrEmpty(inputValues[9].getValue()) ? 10000 : Long.valueOf(inputValues[9].getValue().toString());

            List<SerializableAlarmConditionTypeNode> events = this.uaMethodNode.getNodeContext().getServer().getEventFactory().extractArchive(orMap, andMap, offset, limit);
            Gson gson = new Gson();
            return new Variant[]{new Variant(gson.toJson(events))};
        } catch (Exception e) {
            logger.error("Error invoking EventArchivedStateMethod method of objectId={} with message {}", invocationContext.getObjectId(), e.getMessage());
            return new Variant[]{new Variant(null)};
        }
    }

    protected void validateInputArgumentValues(Variant[] inputArgumentValues) throws InvalidArgumentException {
        MethodInputValidator.checkExistenceEvent(this, eventIds);
        MethodInputValidator.NotNull(this, limit, offset);
        MethodInputValidator.Exists(this, eventDefinitionNames, APP_NAME + "/Alarm&Events/");
        MethodInputValidator.isNumber(this, severity, from, to, limit, offset);
        MethodInputValidator.isBoolean(this, activeState, acknowledgeState, retain);
        MethodInputValidator.checkRange(this, severity, 1000l, "<");
        MethodInputValidator.checkRange(this, limit, 10000l, "<");
        if (this.inputErrorMessages.size() != 0) {
            throw new InvalidArgumentException(null);
        }
    }
}
