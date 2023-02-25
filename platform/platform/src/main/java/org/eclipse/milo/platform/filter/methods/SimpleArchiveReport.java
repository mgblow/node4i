package org.eclipse.milo.platform.filter.methods;

import com.google.gson.Gson;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.eclipse.milo.opcua.sdk.core.ValueRanks;
import org.eclipse.milo.opcua.sdk.server.api.methods.AbstractMethodInvocationHandler;
import org.eclipse.milo.opcua.sdk.server.api.methods.InvalidArgumentException;
import org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode;
import org.eclipse.milo.opcua.sdk.server.nodes.serializable.SerializableHistorian;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.*;
import org.eclipse.milo.opcua.stack.core.types.structured.Argument;
import org.eclipse.milo.platform.runtime.interfaces.UaInterface;
import org.eclipse.milo.platform.util.StringUtils;
import org.eclipse.milo.platform.validators.MethodInputValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.util.*;
import java.util.stream.Collectors;

public class SimpleArchiveReport extends AbstractMethodInvocationHandler {


    public static final Argument identifiers = new Argument("identifier", Identifiers.String, ValueRanks.OneOrMoreDimensions, null, new LocalizedText("event identifier list"));
    public static final Argument startTime = new Argument("startTime", Identifiers.String, ValueRanks.Any, null, new LocalizedText("start time"));
    public static final Argument endTime = new Argument("endTime", Identifiers.String, ValueRanks.Any, null, new LocalizedText("end time"));
    public static final Argument offset = new Argument("offset", Identifiers.String, ValueRanks.Any, null, new LocalizedText("offset"));
    public static final Argument limit = new Argument("limit", Identifiers.String, ValueRanks.Any, null, new LocalizedText("limit"));
    public static final Argument singleSheet = new Argument("single sheet", Identifiers.Boolean, ValueRanks.Any, null, new LocalizedText("single sheet"));

    public static final Argument result = new Argument("simple archive report", NodeId.parse("ns=0;i=15"), ValueRanks.Scalar, null, new LocalizedText("", "result"));
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final UaMethodNode uaMethodNode;

    public SimpleArchiveReport(UaMethodNode node) {
        super(node);
        this.uaMethodNode = node;
    }


    @Override
    public Argument[] getInputArguments() {
        return new Argument[]{identifiers, startTime, endTime, offset, limit, singleSheet};
    }

    @Override
    public Argument[] getOutputArguments() {
        return new Argument[]{result};
    }

    @Override
    protected Variant[] invoke(InvocationContext invocationContext, Variant[] inputValues) throws UaException {
        try {
            logger.debug("Invoking getArchivedValue method of objectId={}", invocationContext.getObjectId());
            String[] identifiers = (String[]) inputValues[0].getValue();
            Long startTime = Long.valueOf(inputValues[1].getValue().toString());
            Long endTime = Long.valueOf(inputValues[2].getValue().toString());
            Long offset = StringUtils.isNullOrEmpty(inputValues[3].getValue()) ? 0 : Long.valueOf(inputValues[3].getValue().toString());
            Long limit = StringUtils.isNullOrEmpty(inputValues[4].getValue()) ? 1000 : Long.valueOf(inputValues[4].getValue().toString());
            Boolean single = (Boolean) inputValues[5].getValue();
            String result = UaInterface.getInstance(this.uaMethodNode.getNodeContext()).getSimpleArchivedValue(Arrays.asList(identifiers), startTime, endTime, offset, limit);
            Gson gson = new Gson();
            SerializableHistorian[] historianArray = gson.fromJson(result, SerializableHistorian[].class);
            List<SerializableHistorian> historianList = Arrays.asList(historianArray);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            Workbook workbook = new XSSFWorkbook();
            if (single) {
                Sheet sheet = workbook.createSheet("identifiers");
                Row headerRow = sheet.createRow(0);
                headerRow = setHeader(headerRow);
                for (int i = 0; i < historianList.size(); i++) {
                    SerializableHistorian historian = historianList.get(i);
                    Row dataRow = sheet.createRow(i + 1);
                    dataRow = setValue(dataRow, historian);
                }
            } else {
                Map<String, List<SerializableHistorian>> map = historianListToMap(historianList);
                for (Map.Entry<String, List<SerializableHistorian>> entry : map.entrySet()) {
                    String identifier = entry.getKey();
                    String name = identifier.split("/")[identifier.split("/").length - 1];
                    Sheet sheet = workbook.createSheet(name);
                    Row headerRow = sheet.createRow(0);
                    headerRow = setHeader(headerRow);
                    List<SerializableHistorian> list = entry.getValue();
                    for (int i = 0; i < list.size(); i++) {
                        SerializableHistorian historian = list.get(i);
                        Row dataRow = sheet.createRow(i + 1);
                        dataRow = setValue(dataRow, historian);
                    }
                }
            }
            workbook.write(outputStream);
            ByteString output = ByteString.of(outputStream.toByteArray());
            return new Variant[]{new Variant(output)};

        } catch (Exception e) {
            logger.error("error in invoking getArchivedValue method of objectId={}", invocationContext.getObjectId());
            return new Variant[]{new Variant(false)};
        }
    }

    private Map<String, List<SerializableHistorian>> historianListToMap(List<SerializableHistorian> historianList) {
        Map<String, List<SerializableHistorian>> map = new HashMap<>();
        for (SerializableHistorian serializableHistorian : historianList) {
            List<SerializableHistorian> list = map.get(serializableHistorian.getIdentifier());
            if (list == null) {
                list = new ArrayList<>();
            }
            list.add(serializableHistorian);
            map.put(serializableHistorian.getIdentifier(), list);
        }
        return map;
    }

    private Row setHeader(Row headerRow) {
        headerRow.createCell(0).setCellValue("identifier");
        headerRow.createCell(1).setCellValue("time");
        headerRow.createCell(2).setCellValue("value");
        return headerRow;
    }

    private Row setValue(Row dataRow, SerializableHistorian historian) {
        dataRow.createCell(0).setCellValue(historian.getIdentifier());
        dataRow.createCell(1).setCellValue(new DateTime(historian.getTime()).getJavaDate().toString());
        dataRow.createCell(2).setCellValue(historian.getValue());
        return dataRow;
    }

    protected void validateInputArgumentValues(Variant[] inputArgumentValues) throws InvalidArgumentException {
        MethodInputValidator.forbiddenCharacter(this, identifiers);
        MethodInputValidator.NotNull(this, identifiers);
        MethodInputValidator.Exists(this, identifiers);
        MethodInputValidator.isNumber(this, startTime, endTime);
        MethodInputValidator.checkRange(this, limit, 10000l, "<");
        if (this.inputErrorMessages.size() != 0) {
            throw new InvalidArgumentException(null);
        }
    }
}

