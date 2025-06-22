package de.tu_berlin.cit.vs.jms.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.jms.*;

import de.tu_berlin.cit.vs.jms.common.*;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.slf4j.LoggerFactory;


public class JmsBrokerClient {
    private static final Logger logger = LoggingUtils.getLogger(JmsBrokerClient.class);
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(JmsBrokerClient.class);
    String registrationQueueName = "broker-registration";
    String clientName;
    Connection con;
    Session session;
    Queue incomingQueue;  // Receive from clients
    Queue outgoingQueue;  // Send to clients

    //rename to distinct message and topic Consumer
    MessageProducer messageProducer;
    MessageConsumer messageConsumer;

    Map<String, MessageConsumer> topicConsumer =new HashMap<>();

    public JmsBrokerClient(String clientName) throws JMSException {
        this.clientName = clientName;
        ActiveMQConnectionFactory conFactory = new ActiveMQConnectionFactory("tcp://localhost:61616");
        conFactory.setTrustedPackages(Arrays.asList(
                "de.tu_berlin.cit.vs.jms.common",
                "java.math",
                "java.util",
                "org.apache.activemq.command" ));
        this.con = conFactory.createConnection();
        this.con.start();
        this.session = con.createSession(false, Session.AUTO_ACKNOWLEDGE);

        RegisterAcknowledgementMessage response = registerWithBroker();

        if (response.getClientName().equals(this.clientName)) {
            if ( response.getClientIncomingQueue() instanceof  Queue &&
                    response.getClientOutgoingQueue() instanceof  Queue ) {
                // all further actions make sense if client gets registration
                this.incomingQueue = response.getClientIncomingQueue();
                this.outgoingQueue = response.getClientOutgoingQueue();
                this.messageConsumer = session.createConsumer(incomingQueue);
                this.messageProducer = session.createProducer(outgoingQueue);
                logger.log(Level.FINE, "Incoming Queue: " + incomingQueue);
                logger.log(Level.FINE, "Outgoing Queue: " + outgoingQueue);
                MessageListener messageListener = message -> {
                    try {
                        processMessages(message);
                    } catch (JMSException e) {
                        logger.log(Level.SEVERE, "Error processing JMS message", e);
                    }
                };
                this.messageConsumer.setMessageListener(messageListener);
                logger.log(Level.FINE, "Message listener registered");
            } else {
                throw new JMSException("Client " + this.clientName + " received a non-queue");
            }

        } else {
            throw new JMSException("Client " + this.clientName + " does not match expected client");
        }

    }

    private void processMessages(Message message) throws JMSException {
        if (message instanceof ObjectMessage) {
            ObjectMessage objReply = (ObjectMessage) message;
            Object responseData = objReply.getObject();
            if (responseData instanceof ListMessage) {
                ListMessage listResponse = (ListMessage) responseData;
                listResponse.getStocks().forEach(stock -> {
                    logger.log( Level.INFO,"Stock: " + stock.toString());
                });
            }

            //receive Topic-> subscribe to it
            if(responseData instanceof TopicMessage) {
                TopicMessage topicReply = (TopicMessage) responseData;
                String topicName = topicReply.getTopic().getTopicName();

                if(!topicConsumer.containsKey(topicName)) {
                    logger.log(Level.INFO, "Subscribing to " + topicReply.getTopic().getTopicName());
                    //create new consumer for each topic.
                    //MessageListener for each Topic. Limited to nly text messages
                    MessageConsumer consumer = session.createConsumer(topicReply.getTopic());
                    consumer.setMessageListener(topicMessage -> {
                        if (topicMessage instanceof TextMessage) {
                            TextMessage textMessage = (TextMessage) topicMessage;
                            try {
                                logger.log(Level.INFO, "Received TextMessage: " + textMessage.getText());
                            } catch (JMSException e) {
                                logger.log(Level.SEVERE, "Error processing TextMessage", e);
                            }
                        } else {
                            logger.log(Level.SEVERE, "Topic Message is limited to text only. Actual type: " + topicMessage.toString());
                        }
                    });
                    topicConsumer.put(topicReply.getTopic().getTopicName(), consumer);
                    logger.log(Level.FINE, "Subscribed to topic: " + topicReply.getTopic().getTopicName());
                } else {
                    logger.log(Level.INFO, "Already subscribed to " + topicName);
                }
            }
        } else if (message instanceof TextMessage) {
            TextMessage textMessage = (TextMessage) message;
            logger.log( Level.INFO,"Received TextMessage: " + textMessage.getText());
        }
    }

    private RegisterAcknowledgementMessage registerWithBroker() throws JMSException {
        Integer timeout = 300000; // 300 seconds
        BigDecimal initialFunds = BigDecimal.valueOf(10000);
        RegisterMessage registerMessage = new RegisterMessage(clientName, initialFunds);

        Queue registrationQueue = session.createQueue(registrationQueueName);
        MessageProducer registrationProducer = session.createProducer(registrationQueue);
        Queue replyQueue = session.createQueue(clientName + "-registration-reply-queue");
        MessageConsumer registrationConsumer = session.createConsumer(replyQueue);

        ObjectMessage request = session.createObjectMessage(registerMessage);
        request.setJMSReplyTo(replyQueue);
        request.setJMSCorrelationID(clientName+"-"+System.currentTimeMillis());
        registrationProducer.send(request);
        Message reply = registrationConsumer.receive(timeout);
        if (reply == null) {
            throw new JMSException("Could not receive registration response");
        }
        if (reply instanceof ObjectMessage) {
            ObjectMessage objReply = (ObjectMessage) reply;
            Object replyObj = objReply.getObject();
            if (replyObj instanceof RegisterAcknowledgementMessage) {
                RegisterAcknowledgementMessage response = (RegisterAcknowledgementMessage) replyObj;

                //registrationProducer.close();
                return response;
            }
        }
        throw new JMSException("Could not receive registration response");



    }
    public void requestList() throws JMSException {
        RequestListMessage listMessage = new RequestListMessage();
        ObjectMessage request = session.createObjectMessage(listMessage);
        messageProducer.send(request);
        logger.log(Level.FINE,"Requesting list sent");
    }

    public void buy(String stockName, int amount) throws JMSException {
        BuyMessage buyMessage = new BuyMessage(stockName, amount);
        ObjectMessage request = session.createObjectMessage(buyMessage);
        messageProducer.send(request);
        logger.log(Level.FINE,"Requesting buy sent: " + stockName + " , amount: " + amount);
    }

    public void sell(String stockName, int amount) throws JMSException {
        SellMessage sellMessage = new SellMessage(stockName, amount);
        ObjectMessage request = session.createObjectMessage(sellMessage);
        messageProducer.send(request);
        logger.log(Level.FINE,"Requesting sell sent");
    }

    public void watch(String stockName) throws JMSException {
        WatchMessage watchMessage = new WatchMessage(stockName);
        ObjectMessage request = session.createObjectMessage(watchMessage);
        messageProducer.send(request);
        logger.log(Level.FINE,"Requesting to watch " + stockName + " sent ");
    }

    public void unwatch(String stockName) throws JMSException {
        //TODO
    }

    public void quit() throws JMSException {
        //TODO: deregister from Broker
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            System.out.println("Enter the client name:");
            String clientName = reader.readLine();

            JmsBrokerClient client = new JmsBrokerClient(clientName);
            logger.log(Level.INFO, "Client registration successful: " + clientName);
            boolean running = true;
            while(running) {
                System.out.println("Enter command:");
                String[] task = reader.readLine().split(" ");

                synchronized(client) {
                    switch(task[0].toLowerCase()) {
                        case "quit":
                            client.quit();
                            System.out.println("Bye bye");
                            running = false;
                            break;
                        case "list":
                            client.requestList();
                            break;
                        case "info":
                            //TODO
                            break;
                        case "buy":
                            if(task.length == 3) {
                                client.buy(task[1], Integer.parseInt(task[2]));
                            } else {
                                System.out.println("Correct usage: buy [stock] [amount]");
                            }
                            break;
                        case "sell":
                            if(task.length == 3) {
                                client.sell(task[1], Integer.parseInt(task[2]));
                            } else {
                                System.out.println("Correct usage: sell [stock] [amount]");
                            }
                            break;
                        case "watch":
                            if(task.length == 2) {
                                client.watch(task[1]);
                            } else {
                                System.out.println("Correct usage: watch [stock]");
                            }
                            break;
                        case "unwatch":
                            if(task.length == 2) {
                                client.unwatch(task[1]);
                            } else {
                                System.out.println("Correct usage: watch [stock]");
                            }
                            break;
                        default:
                            System.out.println("Unknown command. Try one of:");
                            System.out.println("quit, list, info, buy, sell, watch, unwatch");
                    }
                }
                Thread.sleep(500);
            }
        } catch (JMSException | IOException ex) {
            Logger.getLogger(JmsBrokerClient.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        System.exit(0);
    }

}
