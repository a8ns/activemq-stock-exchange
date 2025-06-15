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


public class SimpleBroker {
    private static final Logger logger = LoggingUtils.getLogger(SimpleBroker.class);


    Connection con;
    Session session;
    Queue incomingQueue;  // Receive from clients
    Queue outgoingQueue;  // Send to clients
    MessageProducer producer;
    MessageConsumer consumer;
    Topic topic;
    List<Stock> stockList;

    public SimpleBroker(List<Stock> stockList) throws JMSException {
        this.stockList = stockList;
        ActiveMQConnectionFactory conFactory = new ActiveMQConnectionFactory("tcp://localhost:61616");
        this.con = conFactory.createConnection();
        this.con.start();
        this.session = con.createSession(false, Session.AUTO_ACKNOWLEDGE);

        this.incomingQueue = session.createQueue("broker-incoming");
        this.outgoingQueue = session.createQueue("broker-outgoing");
        this.consumer= session.createConsumer(incomingQueue);
        this.producer = session.createProducer(outgoingQueue);

        MessageListener listener = new MessageListener() {
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
        consumer.setMessageListener(listener);



        for(Stock stock : stockList) {
            /* TODO: prepare stocks as topics */
            // topic = session.createTopic(″Topic Name″);
        }
    }
    
    public void stop() throws JMSException {
        if (this.con != null) this.con.close();
        if (this.producer != null) this.producer.close();
        if (this.session != null) this.session.close();
        if (this.consumer != null) this.consumer.close();
    }
    
    public synchronized int buy(String stockName, int amount) throws JMSException {
        //TODO
        return -1;
    }
    
    public synchronized int sell(String stockName, int amount) throws JMSException {
        //TODO
        return -1;
    }

    
    public synchronized List<Stock> getStockList() {
        // List<Stock> stockList = new ArrayList<>();

        /* TODO: populate stockList */

        return this.stockList;
    }
}
