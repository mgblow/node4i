package org.eclipse.milo.platform.util;

import org.eclipse.milo.opcua.stack.core.types.structured.Argument;

public class ArrayUtils {
    public static int indexOf(Argument[] arguments , Argument element){
        int index = 0;
        for (Argument argument: arguments) {
            if(argument.getName().equals(element.getName())){
                return index;
            }
            index++;
        }
        return -1;
    }

    public static boolean isNullOrEmpty(String[] value) {
        return value == null || value.length == 0;
    }

    public static boolean exists(String name , Argument[] exceptList){
        for(Argument argument:exceptList){
            if(argument.getName().equalsIgnoreCase(name)){
                return true;
            }
        }
        return false;
    }

}
