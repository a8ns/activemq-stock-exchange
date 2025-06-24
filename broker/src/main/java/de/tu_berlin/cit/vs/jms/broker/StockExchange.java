package de.tu_berlin.cit.vs.jms.broker;

import de.tu_berlin.cit.vs.jms.common.LoggingUtils;
import de.tu_berlin.cit.vs.jms.common.Stock;
import org.apache.commons.csv.*;

import javax.jms.JMSException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.math.BigDecimal;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
/**
 * This class holds broker's stocks as well as generates stock prices every X seconds
 */
public class StockExchange {
    private static final Logger logger = LoggingUtils.getLogger(StockExchange.class);
    private Map<String, Stock> stockMap;
    private List<String> stockSymbols = new ArrayList<>();
    private List<SimpleBroker> brokers = new ArrayList<>();
    public StockExchange(Map<String, Stock> stockMap, String filePath) {
        this(stockMap, filePath, Optional.of(5000));
    }

    public StockExchange(Map<String, Stock> stockMap, String filePath, Optional<Integer> sleepMillis) {
        this.stockMap = stockMap;
        Thread priceThread = new Thread(() -> priceTickerGenerator(filePath, sleepMillis));
        priceThread.setDaemon(true);
        priceThread.setName("StockPriceGenerator");
        priceThread.start();
    }
    public Stock getStock(String symbol) {
        return stockMap.get(symbol);
    }

    public Map<String, Stock> getStockMap() {
        return stockMap;
    }
    private void processStockRecord(CSVRecord record) {

        // Process each stock price
        for (String symbol : stockSymbols) {
            try {
                String priceStr = record.get(symbol);
                if (priceStr != null && !priceStr.trim().isEmpty()) {
                    BigDecimal price = new BigDecimal(priceStr.trim());
                    stockMap.get(symbol).setPrice(price);
                    logger.log(Level.FINEST, symbol + ": " + price);
                }
            } catch (Exception e) {
                System.err.printf("Error parsing %s price '%s': %s%n",
                        symbol, record.get(symbol), e.getMessage());
            }
        }
    }

    public void priceTickerGenerator(String filePath, Optional<Integer> sleepMillis) {
        try (Reader reader = new FileReader(filePath)) {
            CSVFormat format = CSVFormat.DEFAULT.builder()
                    .setHeader()
                    .setIgnoreHeaderCase(true)
                    .setTrim(false)
                    .build();

            CSVParser parser = format.parse(reader);

            Map<String, Integer> headers = parser.getHeaderMap();
            stockSymbols = new ArrayList<>(headers.keySet());
            stockSymbols.remove("Date");

            for (CSVRecord record : parser) {
                processStockRecord(record);
                notifyBrokers();
                Thread.sleep(sleepMillis.orElse(10000)); // update price every 10 seconds
            }


        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Sleep interrupted: " + e.getMessage());
        }
    }

    private void notifyBrokers() {

        this.brokers.forEach(SimpleBroker -> {
            try {
                SimpleBroker.notifyPriceUpdate();
            } catch (JMSException e) {
                throw new RuntimeException(e);
            }
        });

    }
    public void registerBroker(SimpleBroker broker) {
        brokers.add(broker);
    }

    public List<SimpleBroker> getBrokers() {
        return brokers;
    }

    public void unregisterBroker(SimpleBroker broker) {
        brokers.remove(broker);
    }
}
