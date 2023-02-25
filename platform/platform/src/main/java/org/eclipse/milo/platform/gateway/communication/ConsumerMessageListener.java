package org.eclipse.milo.platform.gateway.communication;

import org.eclipse.milo.opcua.sdk.server.nodes.UaVariableNode;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

public class ConsumerMessageListener implements MessageListener {
    private String consumerName;
    private UaVariableNode node;

    public ConsumerMessageListener(String consumerName, UaVariableNode node) {
        this.consumerName = consumerName;
        this.node = node;
    }

    public void onMessage(Message message) {
        try {
            DataValue nodeValue = new DataValue(new Variant(((TextMessage) message).getText()), StatusCode.GOOD);
            node.setValue(nodeValue);
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }

}
