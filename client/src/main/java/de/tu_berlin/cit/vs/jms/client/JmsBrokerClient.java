package de.tu_berlin.cit.vs.jms.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
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
    MessageProducer producer;
    MessageConsumer consumer;

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
                this.consumer = session.createConsumer(incomingQueue);
                this.producer = session.createProducer(outgoingQueue);
                logger.log(Level.FINE, "Incoming Queue: ", incomingQueue);
                logger.log(Level.FINE, "Outgoing Queue: ", outgoingQueue);
                MessageListener messageListener = message -> {
                    try {
                        processMessages(message);
                    } catch (JMSException e) {
                        logger.log(Level.SEVERE, "Error processing JMS message", e);
                    }
                };
                this.consumer.setMessageListener(messageListener);
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
                    logger.log(Level.INFO, "Stock: " + stock.toString());
                });
            }
            else if (responseData instanceof InfoMessage) {
                InfoMessage infoResponse = (InfoMessage) responseData;
                logger.log(Level.INFO, "Stock: " + infoResponse.getInfo().toString());
            }
            else if (responseData instanceof ProfileMessage) {
                ProfileMessage profileResponse = (ProfileMessage) responseData;
                logger.log(Level.INFO, "Client Name: " + profileResponse.getClientName());
                logger.log(Level.INFO, "Funds: " + profileResponse.getFunds());
                logger.log(Level.INFO, "Owned stocks:");
                List<Stock> stocks = profileResponse.getStocks();
                if (!stocks.isEmpty()) {
                    stocks.forEach(stock -> {
                        logger.log(Level.INFO, "Stock: " + stock.toString());
                    });
                } else {
                    logger.log(Level.INFO, "-- None --");
                }
            }
        } else if (message instanceof TextMessage) {
            TextMessage textMessage = (TextMessage) message;
            logger.log(Level.INFO,"Received TextMessage: " + textMessage.getText());
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
                return response;
            }
        }
        throw new JMSException("Could not receive registration response");

    }

    public void profile() throws JMSException {
        RequestProfileMessage profileMessage = new RequestProfileMessage();
        ObjectMessage request = session.createObjectMessage(profileMessage);
        producer.send(request);
        logger.log(Level.FINE,"Requesting profile sent");
    }

    public void requestList() throws JMSException {
        RequestListMessage listMessage = new RequestListMessage();
        ObjectMessage request = session.createObjectMessage(listMessage);
        producer.send(request);
        logger.log(Level.FINE,"Requesting list sent");
    }

    public void info(String stockName) throws JMSException {
        RequestInfoMessage infoMessage = new RequestInfoMessage(stockName);
        ObjectMessage request = session.createObjectMessage(infoMessage);
        producer.send(request);
        logger.log(Level.FINE,"Requesting info of " + stockName + " sent");
    }
    
    public void buy(String stockName, int amount) throws JMSException {
        BuyMessage buyMessage = new BuyMessage(stockName, amount);
        ObjectMessage request = session.createObjectMessage(buyMessage);
        producer.send(request);
        logger.log(Level.FINE,"Requesting buy sent: " + stockName + " , amount: " + amount);
    }
    
    public void sell(String stockName, int amount) throws JMSException {
        SellMessage sellMessage = new SellMessage(stockName, amount);
        ObjectMessage request = session.createObjectMessage(sellMessage);
        producer.send(request);
        logger.log(Level.FINE,"Requesting sell sent");
    }
    
    public void watch(String stockName) throws JMSException {
        //TODO
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
                        case "info":
                            if(task.length == 2) {
                                client.info(task[1]);
                            } else {
                                System.out.println("Correct usage: info [stock]");
                            }
                            break;
                        case "profile":
                            client.profile();
                            break;
                        case "help":
                            System.out.println(String.join("\n",
                                                           "Available Commands:",
                                                           "help - Shows this overview of commands",
                                                           "profile - Shows the name, amount of money and stocks this client has",
                                                           "list - Shows all available stocks, their max amount and price",
                                                           "info [stock] - Shows the max amount available of [stock]",
                                                           "buy [stock] [amount] - Buys [amount] of [stock]",
                                                           "sell [stock] [amount] - Sells [amount] of [stock]",
                                                           "watch [stock] - Watches the changes to [stock]",
                                                           "unwatch [stock] - Unwatches [stock]",
                                                           "quit - Terminates the client and unregisters it from the broker"));
                            break;
                        default:
                            System.out.println("Unknown command. Try 'help' to see available commands.");
                    }
                }
            }
            
        } catch (JMSException | IOException ex) {
            Logger.getLogger(JmsBrokerClient.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }
    
}
