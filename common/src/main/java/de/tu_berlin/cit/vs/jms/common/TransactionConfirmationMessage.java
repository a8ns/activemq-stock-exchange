package de.tu_berlin.cit.vs.jms.common;

public class TransactionConfirmationMessage extends BrokerMessage {
        String message;
    public TransactionConfirmationMessage(String message) {
        super(Type.TRANSACTION_CONFIRMATION);
        this.message = message;
    }
    public String getMessage() {
        return message;
    }
}
