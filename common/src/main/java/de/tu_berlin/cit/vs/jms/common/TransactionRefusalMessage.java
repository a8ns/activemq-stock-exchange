package de.tu_berlin.cit.vs.jms.common;

public class TransactionRefusalMessage extends BrokerMessage {
        String message;
    public TransactionRefusalMessage(String message) {
        super(Type.TRANSACTION_REFUSAL);
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
