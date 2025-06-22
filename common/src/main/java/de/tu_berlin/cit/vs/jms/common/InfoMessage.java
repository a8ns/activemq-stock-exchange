package de.tu_berlin.cit.vs.jms.common;

public class InfoMessage extends BrokerMessage{
    private Stock stock;

    public InfoMessage(Stock stock) {
        super(Type.STOCK_INFO);
        this.stock = stock;
    }

    public Stock getInfo() {
        return stock;
    }
}
