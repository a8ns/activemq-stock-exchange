package de.tu_berlin.cit.vs.jms.common;


import javax.jms.Destination;
import java.math.BigDecimal;

public class RegisterMessage extends BrokerMessage {
    private String clientName;
    private BigDecimal initialAmount;
    
    public RegisterMessage(String clientName, BigDecimal initialAmount) {
        super(Type.SYSTEM_REGISTER);
        
        this.clientName = clientName;
        this.initialAmount = initialAmount;
    }
    
    public String getClientName() {
        return clientName;
    }
    public BigDecimal getInitialAmount() {
        return initialAmount;
    }
}
