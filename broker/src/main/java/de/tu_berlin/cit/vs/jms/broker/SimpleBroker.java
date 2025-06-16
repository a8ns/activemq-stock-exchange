package de.tu_berlin.cit.vs.jms.broker;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.jms.*;

import de.tu_berlin.cit.vs.jms.common.*;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.memory.list.MessageList;


public class SimpleBroker {
    private static final Logger logger = LoggingUtils.getLogger(SimpleBroker.class);

    Map<String, Client> clients = new HashMap<>();
    Connection con;
    Session session;
    Queue registrationQueue;
    Queue incomingQueue;  // Receive from clients
    Queue outgoingQueue;  // Send to clients
    MessageProducer producer;
    MessageConsumer consumer;
    MessageConsumer registrationConsumer;
    List<Stock> stockList;
    List<MessageProducer> topicProducers = new ArrayList<>();;

    public SimpleBroker(List<Stock> stockList) throws JMSException {
        this.stockList = stockList;
        ActiveMQConnectionFactory conFactory = new ActiveMQConnectionFactory("tcp://localhost:61616");
        this.con = conFactory.createConnection();
        this.con.start();
        this.session = con.createSession(false, Session.AUTO_ACKNOWLEDGE);

        this.incomingQueue = session.createQueue("broker-incoming");
        this.outgoingQueue = session.createQueue("broker-outgoing");
        this.registrationQueue = session.createQueue("broker-registration");

        this.consumer= session.createConsumer(incomingQueue);
        this.producer = session.createProducer(outgoingQueue);
        this.registrationConsumer = session.createConsumer(registrationQueue);

        MessageListener registrationListener = new MessageListener() {
            @Override
            public void onMessage(Message message) {
                try {
                    processRegistration(message);
                } catch (JMSException e) {
                    throw new RuntimeException(e);
                }
            }
        };

        MessageListener stockListener = new MessageListener() {
            @Override
            public void onMessage(Message msg) {
                String content = null;
                if (msg instanceof TextMessage) {
                    try {
                        content = ((TextMessage) msg).getText();
                    } catch (JMSException e) {
                        throw new RuntimeException(e);
                    }
                    logger.log(Level.FINE, "Received TextMessage: " + content);
                    switch (content) {
                        case "List":
                            List<Stock> stocks = getStockList();
                            ObjectMessage reply_msg = null;
                            try {
                                reply_msg = session.createObjectMessage((Serializable) stocks);
                                producer.send(reply_msg);
                            } catch (JMSException e) {
                                throw new RuntimeException(e);
                            }

                            break;
                        default:
                            break;
                    }
                }
                if(msg instanceof ObjectMessage) {
                    try {
                        content = (String)((ObjectMessage) msg).getObject();
                        if (content != null) {
                            logger.log(Level.FINE, "Received message from ActiveMQ: " );
                            logger.log(Level.FINE, "Received message from ActiveMQ: " + content);
                            switch (content) {
                                case "List":
                                    List<Stock> stocks = getStockList();
                                    ObjectMessage reply_msg = session.createObjectMessage((Serializable) stocks);
                                    producer.send(reply_msg);
                                    break;
                                default:
                                    break;
                            }
                        }
                    } catch (JMSException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        };
        consumer.setMessageListener(stockListener);



        for(Stock stock : stockList) {
            /* TODO: prepare stocks as topics */

            Topic topic = session.createTopic(stock.getName());
            topicProducers.add(session.createProducer(topic));

        }
    }

    private synchronized void processRegistration(Message msg) throws JMSException {
        if(msg instanceof ObjectMessage) {
            ObjectMessage objMsg = (ObjectMessage) msg;
            Object obj = objMsg.getObject();

            if (obj instanceof RegisterMessage) {
                registerClient(((RegisterMessage) obj).getClientName(), session);
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
    
    public synchronized int buy(String stockName, int amount) throws JMSException {
        if (stockName == null || stockName.isEmpty()) {
            throw new IllegalArgumentException("stockName is null or empty");
        } else if (amount < 0) {
            throw new IllegalArgumentException("amount is negative");
        } else if (amount > stockList.size()) {
            throw new IllegalArgumentException("amount is greater than the number of stocks");
        } else {

        }
        return -1;
    }
    
    public synchronized int sell(String stockName, int amount) throws JMSException {
        //TODO
        return -1;
    }

    public synchronized int registerClient(String clientName, Session session) throws JMSException {
        // case registerClient with 0 money
        // check if client exists
        if (this.clients.containsKey(clientName) == false) {
            Client newClient = new Client(clientName, session);
            this.clients.put(clientName, newClient);
            return 0;
        };
        logger.log(Level.WARNING, "client " + clientName + " already registered");

        return -1;
    }

    public synchronized int deregisterClient(String clientName) throws JMSException {
        if( this.clients.containsKey(clientName) ) {
            this.clients.remove(clientName);
            return 0;
        }
        return -1;
    }

    public synchronized int getInfoOnSingleStock(Stock stock) throws JMSException {
        return -1;
    }
    public synchronized List<Stock> getStockList() {
        // List<Stock> stockList = new ArrayList<>();

        /* TODO: populate stockList */

        return this.stockList;
    }
}
