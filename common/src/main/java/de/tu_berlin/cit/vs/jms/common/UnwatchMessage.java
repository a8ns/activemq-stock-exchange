package de.tu_berlin.cit.vs.jms.common;

public class UnwatchMessage extends BrokerMessage {
    private String stockName;

    public UnwatchMessage(String stockName) {
        super(Type.STOCK_UNWATCH);
        this.stockName = stockName;
    }

    public String getStockName() {
        return stockName;
    }
}