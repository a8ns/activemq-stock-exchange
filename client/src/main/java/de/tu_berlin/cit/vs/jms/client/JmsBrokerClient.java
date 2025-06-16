package de.tu_berlin.cit.vs.jms.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.jms.*;

import de.tu_berlin.cit.vs.jms.common.RegisterMessage;
import org.apache.activemq.ActiveMQConnectionFactory;


public class JmsBrokerClient {
    String clientName;
    Connection con;
    Session session;
    Queue registrationQueue;
    MessageProducer registrationProducer;
    public JmsBrokerClient(String clientName) throws JMSException {
        this.clientName = clientName;
        ActiveMQConnectionFactory conFactory = new ActiveMQConnectionFactory("tcp://localhost:61616");
        this.con = conFactory.createConnection();
        this.con.start();
        this.session = con.createSession(false, Session.AUTO_ACKNOWLEDGE);
        this.registrationQueue = session.createQueue("broker-registration");
        this.registrationProducer = session.createProducer(registrationQueue);
        registerWithBroker(); // TODO: might need refactoring with return queues
        // TODO: server might return the customer queues
        /* TODO: initialize consumer, producer, etc. */
    }

    private void registerWithBroker() throws JMSException {

        BigDecimal initialFunds = BigDecimal.valueOf(10000);
        RegisterMessage registerMessage = new RegisterMessage(clientName, initialFunds);
        //TODO : send out registerMessage
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
