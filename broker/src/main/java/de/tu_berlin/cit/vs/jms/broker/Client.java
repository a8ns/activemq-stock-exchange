package de.tu_berlin.cit.vs.jms.broker;

import de.tu_berlin.cit.vs.jms.common.*;

import javax.jms.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
    public Client(SimpleBroker broker, String clientName, Connection connection, BigDecimal funds) throws JMSException {
        this.broker = broker;
        this.clientName = clientName;
        this.funds = funds;

        this.session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

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
                        case STOCK_INFO:
                            if (brokerMessage instanceof RequestInfoMessage) {
                                RequestInfoMessage rim = (RequestInfoMessage) brokerMessage;
                                ObjectMessage request;
                                if (broker.getStocks().get(rim.getStockName()) != null) {
                                    InfoMessage listMessage = new InfoMessage(broker.getStocks().get(rim.getStockName()));
                                    request = session.createObjectMessage(listMessage);
                                } else {
                                    TransactionRefusalMessage listMessage = new TransactionRefusalMessage("No such stock.");
                                    request = session.createObjectMessage(listMessage);
                                }
                                logger.log(Level.FINE, "About to send info message");
                                producer.send(request);
                            }
                            break;
                        case STOCK_PROFILE:
                            String clientName = client.getClientName();
                            BigDecimal funds = client.getFunds();
                            List<Stock> stocks = new ArrayList<>(client.getClientStocks().values());
                            ProfileMessage profileMessage = new ProfileMessage(clientName, funds, stocks);
                            logger.log(Level.FINE, "About to send profile message");
                            producer.send(session.createObjectMessage(profileMessage));
                            break;
                        case STOCK_LIST:
                            logger.log(Level.FINE, "Listing stocks for: " + client.getClientName());
                            ListMessage listMessage = new ListMessage(broker.getStockExchangeMap());
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
                                ObjectMessage transactionOM;
                                try {
                                    Stock boughtStock = broker.buyStock(
                                            this, ((BuyMessage) brokerMessage).getStockName(),
                                            ((BuyMessage) brokerMessage).getAmount());
                                    addStock(boughtStock.getName(), boughtStock.getMaxStockCount(), boughtStock.getPrice());
                                    String buyConfirmationPayload = "Confirmation: " + boughtStock.getMaxStockCount() +
                                            " stocks of " + boughtStock.getName() + " bought. Price: " +
                                            boughtStock.getPrice().setScale(2, RoundingMode.DOWN);
                                    TransactionConfirmationMessage transactionConfirmationMessage =
                                            new TransactionConfirmationMessage(buyConfirmationPayload);
                                    transactionOM = session.createObjectMessage(transactionConfirmationMessage);
                                } catch (Exception e) {
                                    String refusalPayload = "Transaction Refusal: " + e.getMessage();
                                    TransactionRefusalMessage transactionRefusalMessage =
                                            new TransactionRefusalMessage(refusalPayload);
                                    transactionOM = session.createObjectMessage(transactionRefusalMessage);
                                }

                                    producer.send(transactionOM);
                            }

                            break;
                        case STOCK_SELL:
                            logger.log(Level.FINE, "Sell stock request received from : " + client.getClientName());
                            if (brokerMessage instanceof SellMessage) {
                                String stockNameForSell = ((SellMessage) brokerMessage).getStockName();
                                Integer amount = ((SellMessage) brokerMessage).getAmount();
                                ObjectMessage transactionOM;
                                try {
                                    logger.log(Level.FINE, "Sending Sell Confirmation for : " + client.getClientName());
                                    BigDecimal price = broker.sellStock(this, stockNameForSell, amount);
                                    String sellConfirmationPayload = "Confirmation: " + amount + " stocks of "
                                        + stockNameForSell + " sold. Price: " + price.setScale(2, RoundingMode.DOWN);
                                    TransactionConfirmationMessage transactionConfirmationMessage =
                                            new TransactionConfirmationMessage(sellConfirmationPayload);
                                    transactionOM = session.createObjectMessage(transactionConfirmationMessage);
                                } catch (JMSException e) {
                                    logger.log(Level.FINE, "Sending Sell Refusal for : " + client.getClientName());
                                    String sellRefusalPayload = "Transaction Refusal: " + e.getMessage();
                                    TransactionRefusalMessage transactionRefusalMessage =
                                            new TransactionRefusalMessage(sellRefusalPayload);
                                    transactionOM = session.createObjectMessage(transactionRefusalMessage);
                                }
                                producer.send(transactionOM);
                            }

                            break;
                        case STOCK_WATCH:
                            if (brokerMessage instanceof WatchMessage) {
                                String stockName = ((WatchMessage) brokerMessage).getStockName();
                                TopicMessage topicMessage = new TopicMessage(broker.topicMap.get(stockName), true);
                                ObjectMessage topicObject = session.createObjectMessage(topicMessage);
                                producer.send(topicObject);
                                logger.log(Level.FINE, "sent Information for subscriber " + client.getClientName() + " to topic " + stockName);
                            }
                            break;
                        case STOCK_UNWATCH:
                            if (brokerMessage instanceof UnwatchMessage) {
                                String stockName = ((UnwatchMessage) brokerMessage).getStockName();
                                TopicMessage topicMessage = new TopicMessage(broker.topicMap.get(stockName), false);
                                ObjectMessage topicObject = session.createObjectMessage(topicMessage);
                                producer.send(topicObject);
                                logger.log(Level.FINE, "sent Information for subscriber " + client.getClientName() + " topic " + stockName);
                            }
                            break;
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

    protected synchronized void removeFunds(BigDecimal cost) throws InsufficientFundsException {
        if (cost == null || cost.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("Invalid Requested funds amount");

        if (this.funds == null) throw new InsufficientFundsException("Account has no funds available");

        if (this.funds.compareTo(cost) < 0)
            throw new InsufficientFundsException("Insufficient funds: available: " + this.funds + ", requested: " + cost);

        this.funds = this.funds.subtract(cost);
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
                    stocks.remove(stockName);
                } else {
                    stock.setMaxStockCount(newQuantity);
                }
            } else {
                logger.log(Level.SEVERE, "Stock count exceeded available stock count");
                throw new JMSException("Client has not enough stocks of this type");
            }
        } else {
            logger.log(Level.SEVERE, "Client has no stocks of this type");
            throw new JMSException("Client has no stocks of this type");
        }
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
            stocks.clear();
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
