package de.tu_berlin.cit.vs.jms.broker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.jms.JMSException;

import de.tu_berlin.cit.vs.jms.common.LoggingUtils;
import de.tu_berlin.cit.vs.jms.common.Stock;

public class JmsBrokerServer {
    private static final Logger logger = LoggingUtils.getLogger(JmsBrokerServer.class);

    public static void main(String[] args) {
        try {
            List<Stock> stocks = new ArrayList<>();
            // Top 20 SP500 companies by market cap with current pricing and random share quantities
            stocks.add(new Stock("MSFT", 150, 474.96));    // Microsoft - ~$3.56T market cap
            stocks.add(new Stock("NVDA", 75, 144.90));     // NVIDIA - AI chip leader
            stocks.add(new Stock("AAPL", 200, 198.97));    // Apple - iPhone maker
            stocks.add(new Stock("GOOGL", 125, 176.80));   // Alphabet/Google - Search giant
            stocks.add(new Stock("AMZN", 85, 213.03));     // Amazon - E-commerce & cloud
            stocks.add(new Stock("META", 60, 693.12));     // Meta (Facebook) - Social media
            stocks.add(new Stock("TSLA", 40, 319.21));     // Tesla - Electric vehicles
            stocks.add(new Stock("BRK.A", 1, 738193.00));  // Berkshire Hathaway Class A - Warren Buffett's company
            stocks.add(new Stock("TSM", 300, 210.50));     // Taiwan Semiconductor - Chip manufacturing
            stocks.add(new Stock("AVGO", 25, 1875.00));    // Broadcom - Semiconductors & software
            stocks.add(new Stock("LLY", 95, 825.40));      // Eli Lilly - Pharmaceuticals
            stocks.add(new Stock("V", 180, 285.75));       // Visa - Payment processing
            stocks.add(new Stock("WMT", 220, 165.20));     // Walmart - Retail giant
            stocks.add(new Stock("JPM", 160, 198.45));     // JPMorgan Chase - Banking
            stocks.add(new Stock("UNH", 70, 575.30));      // UnitedHealth Group - Healthcare
            stocks.add(new Stock("XOM", 350, 115.80));     // ExxonMobil - Oil & gas
            stocks.add(new Stock("MA", 140, 475.60));      // Mastercard - Payment processing
            stocks.add(new Stock("PG", 190, 168.90));      // Procter & Gamble - Consumer goods
            stocks.add(new Stock("JNJ", 175, 155.25));     // Johnson & Johnson - Healthcare
            stocks.add(new Stock("HD", 120, 385.70));      // Home Depot - Home improvement retail

            for (Stock stock : stocks) {
                logger.log(Level.FINE, "STOCKS AVAILABLE: ");
                logger.log(Level.FINE, stock.toString());

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
