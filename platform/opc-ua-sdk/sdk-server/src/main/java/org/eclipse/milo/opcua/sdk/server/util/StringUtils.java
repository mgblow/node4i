package org.eclipse.milo.opcua.sdk.server.util;

public class StringUtils {
    public static String redisCliParameter(String parameter) {
        return parameter.replaceAll("-", "\\\\\\\\-")
                .replaceAll("&", "\\\\\\\\&")
                .replaceAll("\\.", "\\\\\\\\.")
                .replaceAll("\\_", "\\\\\\\\_")
                .replaceAll("/", "\\\\\\\\/");
    }
}