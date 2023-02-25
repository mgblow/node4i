package org.eclipse.milo.platform.util;

import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringUtils {
    public static boolean isNullOrEmpty(Object string) {
        if (string instanceof LocalizedText)
            string = ((LocalizedText) string).getText();
        return string == null || string.toString().trim().equals("");
    }

    public static boolean isNumeric(Object string) {
        Pattern pattern = Pattern.compile("-?\\d+(\\.\\d+)?");
        return pattern.matcher(string.toString()).matches();
    }

    public static boolean isBoolean(Object string) {
        return string.toString().equalsIgnoreCase("true") || string.toString().equalsIgnoreCase("false");
    }

    public static boolean includeSpecialCharacter(Object string) {
        Pattern special = Pattern.compile("[!@'$%*^()+=|<>?{}\\[\\]~]");
        Matcher hasSpecial = special.matcher(string.toString());
        return (hasSpecial.find() || string.toString().contains("\\") || string.toString().contains("\""));
    }

    public static String extractProgramId(String scriptValue) {
        String location = scriptValue.split("uaInterface.saveNode")[1].split("'")[1];
        String identifier = scriptValue.split("uaInterface.saveNode")[1].split("'")[3];
        return location+"/"+identifier;
    }
}
