package de.tu_berlin.cit.vs.jms.broker;

import de.tu_berlin.cit.vs.jms.common.*;

import javax.jms.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Client {
    private static final Logger logger = LoggingUtils.getLogger(Client.class);

    private String clientName;
    private Session session;
    private Queue incomingQueue;  // Receive from client
    private Queue outgoingQueue;  // Send to client
    private MessageProducer producer;
    private MessageConsumer consumer;
    private Map<String, Stock> stocks = new HashMap<>();
    private BigDecimal funds;
    private SimpleBroker broker;
    public Client(SimpleBroker broker, String clientName, Session session, BigDecimal funds) throws JMSException {
        this.broker = broker;
        this.clientName = clientName;
        this.funds = funds;
        this.session = session;

        this.incomingQueue = session.createQueue(clientName + "ToBroker");
        this.outgoingQueue = session.createQueue(clientName + "FromBroker");

        this.consumer= session.createConsumer(incomingQueue);
        this.producer = session.createProducer(outgoingQueue);
    }

    public Queue getIncomingQueue() {
        return incomingQueue;
    }
    public Queue getOutgoingQueue() {
        return outgoingQueue;
    }

    public void sendMessage(String message) throws JMSException {
        producer.send(session.createTextMessage(message));
    }

    public void sendObjectMessage(Serializable object) throws JMSException {
        ObjectMessage objectMessage = session.createObjectMessage(object);
        producer.send(objectMessage);
    }

    public void setMessageListener(MessageListener messageListener) throws JMSException {
        consumer.setMessageListener(messageListener);

    }

    public void removeMessageListener(MessageListener messageListener) throws JMSException {
        consumer.setMessageListener(null);
    }

    public String getClientName() {
        return clientName;
    }

    protected void handleClientMessage(Client client, Message msg) {
        try {
            if (msg instanceof ObjectMessage) {
                Object obj = ((ObjectMessage) msg).getObject();
                if (obj instanceof BrokerMessage) {
                    BrokerMessage brokerMessage = (BrokerMessage) obj;
                    BrokerMessage.Type msgType = brokerMessage.getType();
                    switch (msgType) {
                        case STOCK_LIST:
                            logger.log(Level.FINE, "Listing stocks for: " + client.getClientName());
                            ListMessage listMessage = new ListMessage(broker.getStockMap());
                            ObjectMessage request = session.createObjectMessage(listMessage);
                            logger.log(Level.FINE, "About to send list message");
                            producer.send(request);
                            break;
                        case SYSTEM_UNREGISTER:
                            broker.deregisterClient(client.getClientName());
                            break;
                        case STOCK_BUY:
                            logger.log(Level.FINE, "Buy stock request received from : " + client.getClientName());

                            if (brokerMessage instanceof BuyMessage) {
                                String buyConfirmationPayload;
                                try {
                                    Stock boughtStock = broker.buyStock(this, ((BuyMessage) brokerMessage).getStockName(),
                                            ((BuyMessage) brokerMessage).getAmount());
                                    addStock(boughtStock.getName(), boughtStock.getMaxStockCount(), boughtStock.getPrice());
                                    buyConfirmationPayload = "Confirmation: " + boughtStock.getMaxStockCount() +
                                            " stocks of " + boughtStock.getName() + " bought. Price: " + boughtStock.getPrice();
                                    } catch (Exception e) {
                                    buyConfirmationPayload = "error: " + e.getMessage();
                                }
                                    producer.send(session.createTextMessage(buyConfirmationPayload));
                            }

                            break;
                        case STOCK_SELL:
                            logger.log(Level.FINE, "Sell stock request received from : " + client.getClientName());
                            if (brokerMessage instanceof SellMessage) {
                                String stockNameForSell = ((SellMessage) brokerMessage).getStockName();
                                Integer amount = ((SellMessage) brokerMessage).getAmount();
                                try {
                                    broker.sellStock(this, stockNameForSell, amount);

                                String sellConfirmationPayload = "Confirmation: " + amount + " stocks of "
                                        + stockNameForSell + " sold. Price: TODO " ; // TODO: retrieve proper price
                                producer.send(session.createTextMessage(sellConfirmationPayload));
                                } catch (JMSException e) {
                                    String sellConfirmationPayload = "Refusal:  Stock count exceeded available stock count\n";
                                    producer.send(session.createTextMessage(sellConfirmationPayload));
                                    throw e;
                                }
                            }


                            break;
                        case STOCK_WATCH:
                            if (brokerMessage instanceof WatchMessage) {
                                String stockName = ((WatchMessage) brokerMessage).getStockName();
                                TopicMessage topicMessage = new TopicMessage(broker.topicMap.get(stockName));
                                ObjectMessage topicObject = session.createObjectMessage(topicMessage);
                                producer.send(topicObject);
                                logger.log(Level.FINE, "sent Information for subscriber : " + client.getClientName() + " to topic " + stockName);
                            }
                            break;
                        case STOCK_UNWATCH:
                            // TODO?
                            break;
                        default:

                    }
                }

            } else if (msg instanceof TextMessage) { // for the case of non-object messages (are we gonna have it at all?)
//                processClientCommand(client, ((TextMessage) msg).getText());
            }
        } catch (JMSException e) {
            logger.severe("Error from client " + client.getClientName() + ": " + e.getMessage());
        }
    }


    protected synchronized void addFunds(BigDecimal funds) {
        if (funds == null || funds.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("Invalid requested funds amount: " + funds);

        this.funds = this.funds.add(funds);
    }

    protected synchronized void removeFunds(BigDecimal funds) throws InsufficientFundsException {
        if (funds == null || funds.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("Invalid Requested funds amount");

        if (this.funds == null) throw new InsufficientFundsException("Account has no funds available");

        if (this.funds.compareTo(funds) < 0)
            throw new InsufficientFundsException("Insufficient funds: available: " + this.funds + ", requested: " + funds);

        this.funds = this.funds.subtract(funds);
    }


    protected BigDecimal getFunds() {
        return funds;
    }


    protected synchronized void addStock(String stockName, Integer quantity, BigDecimal price) throws JMSException {
        if (stocks.containsKey(stockName)) {
            Integer currentQuantity = stocks.get(stockName).getMaxStockCount();
            Integer newQuantity = currentQuantity + quantity;
            stocks.get(stockName).setMaxStockCount(newQuantity);
        } else {
            Stock newStock = new Stock(stockName, quantity, price);
            stocks.put(stockName, newStock);
        }
    }

    protected synchronized void removeStock(String stockName, Integer quantity) throws JMSException {
        if (stocks.containsKey(stockName)) {
            Stock stock = stocks.get(stockName);
            if (quantity <= stock.getMaxStockCount()) {
                Integer newQuantity = stock.getMaxStockCount() - quantity;
                if (newQuantity == 0) {
                    stocks.remove(stock);
                } else {
                    stock.setMaxStockCount(newQuantity);
                }
            } else {
                logger.log(Level.SEVERE, "Stock count exceeded available stock count");
            }
        }
        logger.log(Level.SEVERE, "Stock count exceeded available stock count");
        throw new JMSException("Stock count exceeded available stock count");


    }

    protected Map<String, Stock> getClientStocks() {
        return stocks;
    }

    public void cleanup() throws JMSException {
        try {
            if (producer != null) {
                producer.close();
            }
            if (consumer != null) {
                consumer.close();
            }

        } catch (JMSException e) {
            logger.log(Level.SEVERE, "Error cleaning up client", e);
        }
    }


    @Override
    public String toString() {
        return "Client{'name'='" + clientName + "'}" +
                ", incomingQueue=" + (incomingQueue == null ? "null" : incomingQueue.toString()) +
                ", outgoingQueue=" + (outgoingQueue == null ? "null" : outgoingQueue.toString()) +
                ", producer=" + (producer == null ? "null" : producer.toString()) +
                ", consumer=" + (consumer == null ? "null" : consumer.toString()) +
                "}" ;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Client client = (Client) o;
        return clientName.equals(client.clientName);
    }

    @Override
    public int hashCode() {
        return clientName.hashCode();
    }
}
