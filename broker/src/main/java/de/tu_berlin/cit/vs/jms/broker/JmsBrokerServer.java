package de.tu_berlin.cit.vs.jms.broker;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.tu_berlin.cit.vs.jms.common.LoggingUtils;
import de.tu_berlin.cit.vs.jms.common.Stock;

public class JmsBrokerServer {
    private static final Logger logger = LoggingUtils.getLogger(JmsBrokerServer.class);
    private static Map<String, Stock> stocks = new HashMap<>();;
    public static void main(String[] args) {
        try {
            // Top 20 SP500 companies by market cap with current pricing and random share quantities
            stocks.put("MSFT", new Stock("MSFT", 150));    // Microsoft - Software giant
            stocks.put("NVDA", new Stock("NVDA", 75));     // NVIDIA - AI chip leader
            stocks.put("AAPL", new Stock("AAPL", 200));    // Apple - iPhone maker
            stocks.put("GOOGL", new Stock("GOOGL", 125));  // Alphabet/Google - Search giant
            stocks.put("AMZN", new Stock("AMZN", 85));     // Amazon - E-commerce & cloud
            stocks.put("META", new Stock("META", 60));     // Meta (Facebook) - Social media
            stocks.put("TSLA", new Stock("TSLA", 40));     // Tesla - Electric vehicles
            stocks.put("BRK-A", new Stock("BRK-A", 1)); // Berkshire Hathaway Class A
            stocks.put("TSM", new Stock("TSM", 300));      // Taiwan Semiconductor
            stocks.put("WMT", new Stock("WMT", 220));      // Walmart - Retail giant
            logger.log(Level.FINE, "STOCKS AVAILABLE: ");
            logger.log(Level.FINE, "--------------------");
            for (String stock : stocks.keySet()) {
                logger.log(Level.FINE, stock);
            }
            try {
                StockExchange stockExchange = new StockExchange(stocks,
                        "historical-prices/stock_prices_5yr.csv",
                        Optional.of(30000));
                SimpleBroker broker = new SimpleBroker(stockExchange);
                System.in.read();
                broker.stop();
            } catch (Exception e) {
                throw new Exception("Exception: " + e);
            }


        } catch (Exception ex) {
            Logger.getLogger(JmsBrokerServer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
