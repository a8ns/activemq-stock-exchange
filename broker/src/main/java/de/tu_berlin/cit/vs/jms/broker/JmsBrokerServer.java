package de.tu_berlin.cit.vs.jms.broker;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.jms.JMSException;

import de.tu_berlin.cit.vs.jms.common.LoggingUtils;
import de.tu_berlin.cit.vs.jms.common.Stock;

public class JmsBrokerServer {
    private static final Logger logger = LoggingUtils.getLogger(JmsBrokerServer.class);
    private static Map<String, Stock> stocks = new HashMap<>();;
    public static void main(String[] args) {
        try {
            // Top 20 SP500 companies by market cap with current pricing and random share quantities
            stocks.put("MSFT", new Stock("MSFT", 150, BigDecimal.valueOf(474.96)));    // Microsoft - Software giant
            stocks.put("NVDA", new Stock("NVDA", 75, BigDecimal.valueOf(144.90)));     // NVIDIA - AI chip leader
            stocks.put("AAPL", new Stock("AAPL", 200, BigDecimal.valueOf(198.97)));    // Apple - iPhone maker
            stocks.put("GOOGL", new Stock("GOOGL", 125, BigDecimal.valueOf(176.80)));  // Alphabet/Google - Search giant
            stocks.put("AMZN", new Stock("AMZN", 85, BigDecimal.valueOf(213.03)));     // Amazon - E-commerce & cloud
            stocks.put("META", new Stock("META", 60, BigDecimal.valueOf(693.12)));     // Meta (Facebook) - Social media
            stocks.put("TSLA", new Stock("TSLA", 40, BigDecimal.valueOf(319.21)));     // Tesla - Electric vehicles
            stocks.put("BRK.A", new Stock("BRK.A", 1, BigDecimal.valueOf(738193.00))); // Berkshire Hathaway Class A
            stocks.put("TSM", new Stock("TSM", 300, BigDecimal.valueOf(210.50)));      // Taiwan Semiconductor            logger.log(Level.FINE, "STOCKS AVAILABLE: ");
            stocks.put("WMT", new Stock("WMT", 220, BigDecimal.valueOf(165.20)));      // Walmart - Retail giant
            logger.log(Level.FINE, "--------------------");
            for (String stock : stocks.keySet()) {
                logger.log(Level.FINE, stock);
            }

            SimpleBroker broker = new SimpleBroker(stocks);
            System.in.read();
            broker.stop();

        } catch (JMSException ex) {
            Logger.getLogger(JmsBrokerServer.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(JmsBrokerServer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
