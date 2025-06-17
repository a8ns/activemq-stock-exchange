package de.tu_berlin.cit.vs.jms.common;

import javax.jms.Queue;

public class RegisterAcknowledgmentMessage extends BrokerMessage {
    private String clientName;
    private Queue clientIncomingQueue;
    private Queue clientOutgoingQueue;

    public RegisterAcknowledgmentMessage(Queue clientIncomingQueue, Queue clientOutgoingQueue) {
        super(Type.SYSTEM_REGISTER);
        this.clientIncomingQueue = clientIncomingQueue;
        this.clientOutgoingQueue = clientOutgoingQueue;
    }

    public Queue getClientOutgoingQueue() {
        return clientOutgoingQueue;
    }

    public Queue getClientIncomingQueue() {
        return clientIncomingQueue;
    }

    public String getClientName() {
        return clientName;
    }
}
