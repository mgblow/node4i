package org.eclipse.milo.platform.gateway.interfaces;

import org.eclipse.milo.platform.util.Props;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public interface Device {
    ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(4);
    Logger logger = LoggerFactory.getLogger(Device.class);
    int APP_NAMESPACE_INDEX = Integer.parseInt(Props.getProperty("app-namespace-index").toString());


    void turnOnDevice(String ioName);

    void turnOffDevice(String ioName);

    default void restartDevice(String deviceName) {
        turnOffDevice(deviceName);
        turnOnDevice(deviceName);
    }
}
