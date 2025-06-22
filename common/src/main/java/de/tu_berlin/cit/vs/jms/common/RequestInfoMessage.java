package de.tu_berlin.cit.vs.jms.common;

public class RequestInfoMessage extends BrokerMessage {
    private String stockName;

    public RequestInfoMessage(String stockName) {
        super(Type.STOCK_INFO);
        this.stockName = stockName;
    }

    public String getStockName() {
        return stockName;
    }
}
