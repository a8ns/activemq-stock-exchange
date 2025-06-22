package de.tu_berlin.cit.vs.jms.broker;

import de.tu_berlin.cit.vs.jms.common.Stock;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;

public class StockExchange {
    private Map<String, Stock> stockMap;
    private Map<Integer, String> stockPositionMap = new HashMap<>();

    public StockExchange(Map<String, Stock> stockMap, String filePath) {
        this(stockMap, filePath, Optional.of(5000)); // Call the main constructor with default
    }

    public StockExchange(Map<String, Stock> stockMap, String filePath, Optional<Integer> sleepMillis) {
        this.stockMap = stockMap;
        priceTickerGenerator(filePath, sleepMillis);
    }
    public Stock getStock(String symbol) {
        return stockMap.get(symbol);
    }

    public Map<String, Stock> getStockMap() {
        return stockMap;
    }


    public void priceTickerGenerator(String filePath, Optional<Integer> sleepMillis) {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            line = reader.readLine();
            if (line != null) {
                String[] tokens = line.split(",");
                int position = 0;
                for (String token : tokens) {
                    stockPositionMap.put(position++, token);
                }
            }
            // starting from the second line:

            while ((line = reader.readLine()) != null) {
                String[] tokens = line.split(",");
                IntStream.range(0, tokens.length).forEach(position -> {
                    if (position > 0) {
                        BigDecimal currentPrice = new BigDecimal(tokens[position]);
                        stockMap.get(stockPositionMap.get(position))
                                .setPrice(currentPrice);}

                });

                // Sleep between lines
                Thread.sleep(sleepMillis.orElse(5000));

            }
        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Sleep interrupted: " + e.getMessage());
        }
    }


}
