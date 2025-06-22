package de.tu_berlin.cit.vs.jms.common;

public class WatchMessage extends BrokerMessage {
    private String stockName;

    public WatchMessage(String stockName) {
        super(Type.STOCK_WATCH);
        this.stockName = stockName;
    }

    public String getStockName() {
        return stockName;
    }
}