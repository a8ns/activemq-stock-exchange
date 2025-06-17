package de.tu_berlin.cit.vs.jms.broker;

import de.tu_berlin.cit.vs.jms.common.BrokerMessage;
import de.tu_berlin.cit.vs.jms.common.InsufficientFundsException;
import de.tu_berlin.cit.vs.jms.common.LoggingUtils;
import de.tu_berlin.cit.vs.jms.common.Stock;

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
    private Map<Stock, Integer> stocks = new HashMap<>();
    private BigDecimal funds;

    public Client(String clientName, Session session) throws JMSException {
        this.clientName = clientName;
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


    protected synchronized void addStock(Stock stock, Integer quantity) throws JMSException {
        if (stocks.containsKey(stock)) {
            Integer currentQuantity = stocks.get(stock);
                Integer newQuantity = currentQuantity + quantity;
                stocks.put(stock, newQuantity);
        } else {
            stocks.put(stock, quantity);
        }
    }

    protected synchronized void removeStock(Stock stock, Integer quantity) throws JMSException {
        if (stocks.containsKey(stock)) {
            Integer currentQuantity = stocks.get(stock);
            if (quantity <= currentQuantity) {
                Integer newQuantity = currentQuantity - quantity;
                if (newQuantity == 0) {
                    stocks.remove(stock);
                } else {
                    stocks.put(stock, newQuantity);
                }
            }
            stocks.remove(stock);
        }


    }

    protected Map<Stock, Integer> getClientStocks() {
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

    MessageListener stockListener = new MessageListener() {
        @Override
        public void onMessage(Message message) {
            String content = null;
            if (message instanceof TextMessage) {

            } else if (message instanceof BrokerMessage) {
                BrokerMessage brokerMessage = (BrokerMessage) message;

                if (brokerMessage.getType() == BrokerMessage.Type.STOCK_BUY) {

                }
            }
        }
    };

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
