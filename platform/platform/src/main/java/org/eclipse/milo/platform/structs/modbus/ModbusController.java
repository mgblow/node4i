package org.eclipse.milo.platform.structs.modbus;

public class ModbusController {
    private String host;
    private String connection_name;
    private String port;
    private boolean rtu;
    private String type;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getConnection_name() {
        return connection_name;
    }

    public void setConnection_name(String connection_name) {
        this.connection_name = connection_name;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isRtu() {
        return rtu;
    }

    public void setRtu(boolean rtu) {
        this.rtu = rtu;
    }
}