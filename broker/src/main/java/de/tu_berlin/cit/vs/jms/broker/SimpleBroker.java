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
    Map<String, Stock> stockList;
    List<MessageProducer> topicProducers = new ArrayList<>();;

    public SimpleBroker(Map<String, Stock> stockList) throws JMSException {
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


        for(String stock : stockList.keySet()) {
            /* WIP: prepare stocks as topics */

            Topic topic = session.createTopic(stock);
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
    



    public synchronized int registerClient(String clientName, Session session) throws JMSException {
        // case registerClient with 0 money
        // check if client exists
        if (this.clients.containsKey(clientName) == false) {
            Client newClient = new Client(clientName, session);
            newClient.setMessageListener(msg -> handleClientMessage(newClient, msg));


            this.clients.put(clientName, newClient);
            return 0;
        };
        logger.log(Level.WARNING, "client " + clientName + " already registered");

        return -1;
    }

    private void handleClientMessage(Client client, Message msg) {
        // TODO handle correct message objects
        try {
            if (msg instanceof ObjectMessage) {
                Object obj = ((ObjectMessage) msg).getObject();

                if (obj instanceof String) {
                    processClientCommand(client, (String) obj);
                }
            } else if (msg instanceof TextMessage) {
                processClientCommand(client, ((TextMessage) msg).getText());
            }
        } catch (JMSException e) {
            logger.severe("Error from client " + client.getClientName() + ": " + e.getMessage());
        }
    }

    private void processClientCommand(Client client, String obj) throws JMSException {
        String[] commands = obj.split(" ");
        synchronized (client) {
            switch (commands[0].toLowerCase()) {
                case "list":
                    getStockList();
                    break;
                case "buy":
                    // TODO: Handle buy logic using client.addStock(), client.removeFunds()
                    break;
                case "sell":
                    // TODO: Handle sell logic using client.removeStock(), client.addFunds()
                    break;
                case "watch":
                    break;
                case "unwatch":
                    break;
                case "deregister":
                    deregisterClient(client.getClientName());
                    break;
                default:
            }
        }
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
    public synchronized Map<String, Stock> getStockList() {
        // List<Stock> stockList = new ArrayList<>();

        /* TODO: populate stockList */

        return this.stockList;
    }
}
