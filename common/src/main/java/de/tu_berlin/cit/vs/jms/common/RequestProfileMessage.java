package de.tu_berlin.cit.vs.jms.common;

public class RequestProfileMessage extends BrokerMessage {
    
    public RequestProfileMessage() {
        super(Type.STOCK_PROFILE);
    }
}
