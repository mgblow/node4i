package org.eclipse.milo.platform.util;

import org.eclipse.milo.opcua.sdk.server.nodes.UaNodeContext;
import org.slf4j.LoggerFactory;

public class LogUtil {
    private UaNodeContext uaNodeContext;
    private static LogUtil logUtil ;

    public static LogUtil getInstance() {
        if (logUtil == null) {
            logUtil = new LogUtil();
        }
        return logUtil;
    }
    private LogUtil(){
    }

    public UaNodeContext getUaNodeContext() {
        return uaNodeContext;
    }

    public void setUaNodeContext(UaNodeContext uaNodeContext) {
        this.uaNodeContext = uaNodeContext;
    }
    public void logAndFireEvent(Class clazz,String message , String location ){
        this.uaNodeContext.getServer().getEventFactory().sendSysErrorEvent(location , message);
        LoggerFactory.getLogger(clazz).error(message);
    }
}
