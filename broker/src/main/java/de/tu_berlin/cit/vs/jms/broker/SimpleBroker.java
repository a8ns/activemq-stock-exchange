package de.tu_berlin.cit.vs.jms.broker;

import java.math.BigDecimal;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.jms.*;
import javax.jms.Queue;

import de.tu_berlin.cit.vs.jms.common.*;
import org.apache.activemq.ActiveMQConnectionFactory;


public class SimpleBroker {
    private static final Logger logger = LoggingUtils.getLogger(SimpleBroker.class);

    Map<String, Client> clients = new HashMap<>();
    Connection con;
    Session session;
    Session replySession;
    Queue registrationQueue;
    MessageConsumer registrationConsumer;
    private MessageProducer replyOnceProducer;
    StockExchange stockExchange;
    Map<String, Topic> topicMap = new HashMap<>();
    Map<String, MessageProducer> topicProducers = new HashMap<>();

    public SimpleBroker(StockExchange stockExchange) throws JMSException {
        this.stockExchange = stockExchange;
        ActiveMQConnectionFactory conFactory = new ActiveMQConnectionFactory("tcp://localhost:61616");
        conFactory.setTrustedPackages(Arrays.asList(
                "de.tu_berlin.cit.vs.jms.common",
                "java.math",
                "java.util",
                "org.apache.activemq.command"));
        this.con = conFactory.createConnection();
        this.con.start();
        this.session = con.createSession(false, Session.AUTO_ACKNOWLEDGE);
        this.replySession = con.createSession(false, Session.AUTO_ACKNOWLEDGE);


        this.registrationQueue = session.createQueue("broker-registration");

        this.registrationConsumer = session.createConsumer(registrationQueue);

        MessageListener registrationListener = message -> {
            try {
                logger.log(Level.INFO, "Received JMS Message ");
                processRegistration(message);
            } catch (JMSException e) {
                logger.log(Level.SEVERE, "Error processing registration", e);
            }
        };
        registrationConsumer.setMessageListener(registrationListener);


        for(String stockName : stockExchange.getStockMap().keySet()) {
            /* WIP: prepare stocks as topics */

            Topic topic = session.createTopic(stockName);
            topicMap.put(stockName, topic);
            topicProducers.put(stockName, session.createProducer(topic));

        }
    }

    private void updateStockTopic(Stock stock, StockEvent stockEvent) throws JMSException {
        String payload = "";
        switch(stockEvent) {
            case STOCK_PRICE_CHANGED:
                payload = "New price for " + stock.getName() + " is " + stock.getPrice();
                break;
            case STOCK_SOLD:
                payload = "some of " + stock.getName() + " stock is bought. Remaining: "  + stock.getAvailableCount();
                break;
            case STOCK_BOUGHT:
                payload = "some of " + stock.getName() + " stock is sold. Remaining: "  + stock.getAvailableCount();;
                break;
            default:
                break;
        }
        if (!payload.isEmpty()) {
            Message message = session.createTextMessage(payload);
            if (topicProducers.containsKey(stock.getName())) {
                topicProducers.get(stock.getName()).send(message);
            }
        }
    }

    private void updateStockTopic(String stockName, StockEvent stockEvent) throws JMSException {
        String payload = "";
        switch(stockEvent) {
            case STOCK_PRICE_CHANGED:
                if (stockExchange.getStockMap().containsKey(stockName)) {
                    payload = "New price for " + stockName + " is " + stockExchange.getStockMap().get(stockName).getPrice();
                }
                break;
            case STOCK_SOLD:
                payload = "some of " + stockName + " stock is sold by client. Remaining: "  + stockName;;
                break;
            case STOCK_BOUGHT:
                payload =  "some of " + stockName + " stock is bought by client. Remaining: "  + stockName;;
                break;
            default:
                break;
        }
        if (!payload.isEmpty()) {
            Message message = session.createTextMessage(payload);
            if (topicProducers.containsKey(stockName)) {
                topicProducers.get(stockName).send(message);
            }
        }
    }

    private synchronized void processRegistration(Message msg) throws JMSException {
        if (!(msg instanceof ObjectMessage)) {
            throw new IllegalArgumentException("Expected ObjectMessage");
        }

        ObjectMessage objMsg = (ObjectMessage) msg;
        Object obj = objMsg.getObject();
        if (!(obj instanceof RegisterMessage)) {
            throw new IllegalArgumentException("Expected RegisterMessage");
        }
        RegisterMessage registerMessage = (RegisterMessage) obj;
        if (registerClient(registerMessage.getClientName(), session, registerMessage.getInitialAmount()) == 0) {
            // get ReplyTo,  produce message and send out
            Destination replyTo = objMsg.getJMSReplyTo();
            logger.log(Level.FINE, "ReplyTo: " + replyTo.toString());
            if (replyTo != null) {
                Client newClient = clients.get(registerMessage.getClientName());
                logger.log(Level.FINE, "Registering client: " + newClient.getClientName());
                logger.log(Level.FINE, "Incoming Queue: " + newClient.getIncomingQueue());
                logger.log(Level.FINE, "Outgoing Queue: " + newClient.getOutgoingQueue());
                if (newClient != null) {
                    // reversing incoming to outgoing and vise versa:
                    RegisterAcknowledgementMessage replyMessage =
                            new RegisterAcknowledgementMessage(registerMessage.getClientName(),
                                                                newClient.getOutgoingQueue(),
                                                                newClient.getIncomingQueue());
                    ObjectMessage reply = replySession.createObjectMessage(replyMessage);
                    reply.setJMSCorrelationID(objMsg.getJMSCorrelationID());
                    reply.setJMSDeliveryMode(DeliveryMode.NON_PERSISTENT);
                    reply.setJMSReplyTo(replyTo);
                    replyOnceProducer = replySession.createProducer(null);
                    replyOnceProducer.setTimeToLive(5000);
                    logger.log(Level.FINE, "ReplyTo destination: " + replyTo);
                    logger.log(Level.FINE, "Reply: " + reply);
                    if (replyTo instanceof TemporaryQueue) {
                        logger.log(Level.FINE, "Temp queue confirmed");
                    } else {
                        logger.log(Level.WARNING, "ReplyTo is not a temp queue: " + replyTo.getClass());
                    }
                    replyOnceProducer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
                    replyOnceProducer.send(replyTo, reply);
                    replyOnceProducer.close();
                    logger.log(Level.FINE, "replyTo sent out, replyOnceProducer closed");
                }
            }
        }
    }

    public void stop() throws JMSException {
        for(Client client : clients.values()) {
            client.cleanup();
        }
        clients.clear();
        if (this.session != null) this.session.close();
        if (this.con != null) this.con.close();
    }

    public synchronized int registerClient(String clientName, Session session, BigDecimal funds) throws JMSException {
        // check if client exists
        if (this.clients.containsKey(clientName)) {
            throw new IllegalArgumentException("Client " + clientName + " already registered");
        }

        Client newClient = new Client(this, clientName, session, funds);
        newClient.setMessageListener(msg -> newClient.handleClientMessage(newClient, msg));
        this.clients.put(clientName, newClient);

        return 0;
    }

    public synchronized void sellStock(Client client, String stockName, Integer quantity) throws JMSException {
        try {
            if (stockExchange.getStockMap().containsKey(stockName)) {
                client.removeStock(stockName, quantity);
                Stock stock = stockExchange.getStockMap().get(stockName);
                Integer newQuantity = quantity + stock.getAvailableCount();
                stock.setAvailableCount(newQuantity);
            }
            updateStockTopic(stockName, StockEvent.STOCK_SOLD);
            client.addFunds(
                    this.getCurrentStockPrice(stockName).multiply(BigDecimal.valueOf(quantity))
            );
        } catch (JMSException e) {
            logger.log(Level.SEVERE, "Error processing sell stock", e);
            throw e;
        }
    }

    public synchronized Stock buyStock(Client client, String stockName, Integer quantity) throws JMSException, InsufficientFundsException {
        if (stockExchange.getStockMap().containsKey(stockName)) {
            Stock stock = stockExchange.getStockMap().get(stockName);
            if (quantity <= stock.getAvailableCount()) {
                BigDecimal cost = BigDecimal.valueOf(quantity).multiply(this.getCurrentStockPrice(stockName));
                if (client.getFunds().compareTo(cost) >= 0) { // check if enough funds with client
                    try {
                        client.removeFunds(cost);
                        Integer newQuantity = stock.getAvailableCount() - quantity;
                        stock.setAvailableCount(newQuantity);
                        Stock boughtStock = new Stock(stockName, quantity, this.getCurrentStockPrice(stockName));
                        updateStockTopic(boughtStock, StockEvent.STOCK_BOUGHT);
                        return boughtStock;
                    } catch (InsufficientFundsException e) {
                        throw new InsufficientFundsException("Not enough funds to buy " + quantity + " stocks of " + stockName);
                    }
                }
                throw new InsufficientFundsException("Not enough funds to buy " + quantity + " stocks of " + stockName);
            }
            throw new IllegalArgumentException("Requested stock quantity for " + stockName + " is not available. (Available: " + stock.getAvailableCount() + ")");
        }
        throw new IllegalArgumentException("Stock " + stockName + " does not exist");
    }

    public BigDecimal getCurrentStockPrice(String stockName) {
        return stockExchange.getStockMap().get(stockName).getPrice();
    }

    public synchronized int deregisterClient(String clientName) throws JMSException {
        if( this.clients.containsKey(clientName) ) {
            this.clients.get(clientName).cleanup();
            this.clients.remove(clientName);
            return 0;
        }
        return -1;
    }

    public synchronized String getInfoOnSingleStock(Stock stock) throws JMSException {
        return stock.toString();
    }
    public synchronized List<Stock> getStockExchangeMap() {
        return new ArrayList<>(this.stockExchange.getStockMap().values());
    }

    public synchronized Map<String, Stock> getStocks() {
        return this.stockExchange.getStockMap();
    }


}
