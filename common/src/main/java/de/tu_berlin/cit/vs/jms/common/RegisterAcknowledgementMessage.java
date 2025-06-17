package de.tu_berlin.cit.vs.jms.common;

import javax.jms.Queue;

public class RegisterAcknowledgementMessage extends BrokerMessage {
    private String clientName;
    private Queue clientIncomingQueue;
    private Queue clientOutgoingQueue;

    public RegisterAcknowledgementMessage(String clientName, Queue clientIncomingQueue, Queue clientOutgoingQueue) {
        super(Type.SYSTEM_REGISTER);
        this.clientName = clientName;
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
