package de.tu_berlin.cit.vs.jms.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.jms.*;

import de.tu_berlin.cit.vs.jms.common.RegisterAcknowledgementMessage;
import de.tu_berlin.cit.vs.jms.common.RegisterMessage;
import org.apache.activemq.ActiveMQConnectionFactory;


public class JmsBrokerClient {
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
        this.con = conFactory.createConnection();
        this.con.start();
        this.session = con.createSession(false, Session.AUTO_ACKNOWLEDGE);

        RegisterAcknowledgementMessage response = registerWithBroker();
        if (response.getClientName().equals(this.clientName)) {
            if ( response.getClientIncomingQueue() instanceof  Queue &&
                    response.getClientOutgoingQueue() instanceof  Queue ) {
                this.incomingQueue = response.getClientIncomingQueue();
                this.outgoingQueue = response.getClientOutgoingQueue();
                this.consumer = session.createConsumer(incomingQueue);
                this.producer = session.createProducer(outgoingQueue);
                MessageListener messageListener = message -> processMessages(message);
                this.consumer.setMessageListener(messageListener);
            } else {
                throw new JMSException("Client " + this.clientName + " received a non-queue");
            }

        } else {
            throw new JMSException("Client " + this.clientName + " does not match expected client");
        }

    }

    private void processMessages(Message message) {
    }

    private RegisterAcknowledgementMessage registerWithBroker() throws JMSException {
        Integer timeout = 30000; // 30 seconds
        BigDecimal initialFunds = BigDecimal.valueOf(10000);
        RegisterMessage registerMessage = new RegisterMessage(clientName, initialFunds);

        Queue registrationQueue = session.createQueue(registrationQueueName);
        MessageProducer registrationProducer = session.createProducer(registrationQueue);
        TemporaryQueue replyQueue = session.createTemporaryQueue();
        MessageConsumer registrationConsumer = session.createConsumer(registrationQueue);


        ObjectMessage request = session.createObjectMessage(registerMessage);
        request.setJMSReplyTo(replyQueue);
        registrationProducer.send(request);
        Message reply = registrationConsumer.receive(timeout);
        if (reply == null) {
             throw new JMSException("Could not receive registration response");
        }

        if (reply instanceof RegisterAcknowledgementMessage) {
            RegisterAcknowledgementMessage response = (RegisterAcknowledgementMessage) reply;
            return response;
        } else {
            throw new JMSException("Could not receive registration response");
        }


    }
    public void requestList() throws JMSException {
        //TODO
    }
    
    public void buy(String stockName, int amount) throws JMSException {
        //TODO
    }
    
    public void sell(String stockName, int amount) throws JMSException {
        //TODO
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
                        default:
                            System.out.println("Unknown command. Try one of:");
                            System.out.println("quit, list, buy, sell, watch, unwatch");
                    }
                }
            }
            
        } catch (JMSException | IOException ex) {
            Logger.getLogger(JmsBrokerClient.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }
    
}
