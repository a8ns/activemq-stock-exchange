package de.tu_berlin.cit.vs.jms.common;

import java.math.BigDecimal;
import java.util.List;

public class ProfileMessage extends BrokerMessage {
    private String clientName;
    private BigDecimal funds;
    private List<Stock> stocks;

    public ProfileMessage(String clientName, BigDecimal funds, List<Stock> stocks) {
        super(Type.STOCK_PROFILE);
        this.clientName = clientName;
        this.funds = funds;
        this.stocks = stocks;
    }

    public String getClientName() {
        return clientName;
    }

    public BigDecimal getFunds() {
        return funds;
    }

    public List<Stock> getStocks() {
        return stocks;
    }
}
