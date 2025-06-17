package de.tu_berlin.cit.vs.jms.broker;

import java.io.Serializable;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.jms.*;
import javax.jms.Queue;

import de.tu_berlin.cit.vs.jms.common.*;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.memory.list.MessageList;


public class SimpleBroker {
    private static final Logger logger = LoggingUtils.getLogger(SimpleBroker.class);

    Map<String, Client> clients = new HashMap<>();
    Connection con;
    Session session;
    Session replySession;
    Queue registrationQueue;
    Queue incomingQueue;  // Receive from clients
    Queue outgoingQueue;  // Send to clients
    MessageProducer producer;
    MessageConsumer consumer;
    MessageConsumer registrationConsumer;
    private MessageProducer replyOnceProducer;
    Map<String, Stock> stockList;
    List<MessageProducer> topicProducers = new ArrayList<>();

    public SimpleBroker(Map<String, Stock> stockList) throws JMSException {
        this.stockList = stockList;
        ActiveMQConnectionFactory conFactory = new ActiveMQConnectionFactory("tcp://localhost:61616");
        conFactory.setTrustedPackages(Arrays.asList("de.tu_berlin.cit.vs.jms.common", "java.math"));
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


        for(String stock : stockList.keySet()) {
            /* WIP: prepare stocks as topics */

            Topic topic = session.createTopic(stock);
            topicProducers.add(session.createProducer(topic));

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
        if (registerClient((registerMessage).getClientName(), session) == 0) {
            // get ReplyTo,  produce message and send out
            Destination replyTo = objMsg.getJMSReplyTo();
            logger.log(Level.FINE, "ReplyTo: " + replyTo.toString());
            if (replyTo != null) {
                Client newClient = clients.get(registerMessage.getClientName());
                logger.log(Level.FINE, "Registering client: " + newClient.getClientName());
                logger.log(Level.FINE, "Incoming Queue: " + newClient.getIncomingQueue());
                logger.log(Level.FINE, "Outgoing Queue: " + newClient.getOutgoingQueue());
                if (newClient != null) {
                    RegisterAcknowledgementMessage replyMessage =
                            new RegisterAcknowledgementMessage(registerMessage.getClientName(),
                                                                newClient.getIncomingQueue(),
                                                                newClient.getOutgoingQueue());
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




    public synchronized int registerClient(String clientName, Session session) throws JMSException {
        // check if client exists
        if (this.clients.containsKey(clientName)) {
            throw new IllegalArgumentException("Client " + clientName + " already registered");
        }

        Client newClient = new Client(this, clientName, session);
        newClient.setMessageListener(msg -> handleClientMessage(newClient, msg));
        this.clients.put(clientName, newClient);

        return 0;
    }

     protected void handleClientMessage(Client client, Message msg) {
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
                    logger.log(Level.FINE, "Listing stocks for: " + client.getClientName());
                    ListMessage listMessage = new ListMessage(getStockList());
                    ObjectMessage request = session.createObjectMessage(listMessage);
                    logger.log(Level.FINE, "About to send list message");
                    producer.send(request);
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
    public synchronized List<Stock> getStockList() {
        return new ArrayList<>(this.stockList.values());
    }
}
