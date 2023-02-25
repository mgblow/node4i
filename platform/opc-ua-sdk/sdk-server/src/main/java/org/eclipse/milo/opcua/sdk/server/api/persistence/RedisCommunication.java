package org.eclipse.milo.opcua.sdk.server.api.persistence;

import org.eclipse.milo.opcua.sdk.server.OpcUaServer;
import redis.clients.jedis.JedisPooled;

public class RedisCommunication {

    JedisPooled client;

    public RedisCommunication(OpcUaServer server) {
        this.client = new JedisPooled(server.getConfig().getRedisUri());
    }

    public JedisPooled getClient() {
        return this.client;
    }

    public Object exec(String command) {
        return this.client.eval("return redis.call(" + buildCommand(command) + ")");
    }

    private static String buildCommand(String query) {
        String[] q = query.split(" ");
        String cmd = '\'' + q[0] + '\'';
        for (int i = 1; i < q.length; i++) {
            cmd += ",\'" + q[i] + '\'';
        }
        return cmd;
    }


    public Object exec(String[] args) {
        return this.client.eval("return redis.call(" + buildCommand(args) + ")");
    }

    private static String buildCommand(String[] args) {
        StringBuilder cmd = new StringBuilder('\'' + args[0] + '\'');
        for (int i = 1; i < args.length; i++) {
            cmd.append(",\'").append(args[i]).append('\'');
        }
        return cmd.toString();
    }

}
