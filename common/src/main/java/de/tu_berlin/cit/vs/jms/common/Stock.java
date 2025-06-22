package de.tu_berlin.cit.vs.jms.common;

import java.io.Serializable;
import java.math.BigDecimal;


public class Stock implements Serializable {
    private String name;
    private int maxStockCount;
    private int availableCount;
    private BigDecimal price;
    
    public Stock(String name, int maxStockCount, BigDecimal price) {
        this.maxStockCount = maxStockCount;
        this.availableCount = maxStockCount;
        this.name = name;
        this.price = price;
    }

    public Stock(String name, int maxStockCount) {
        this.maxStockCount = maxStockCount;
        this.availableCount = maxStockCount;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getMaxStockCount() {
        return maxStockCount;
    }

    public void setMaxStockCount(int maxStockCount) {
        this.maxStockCount = maxStockCount;
    }

    public int getAvailableCount() {
        return availableCount;
    }

    public void setAvailableCount(int availableCount) {
        this.availableCount = availableCount;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }
    
    @Override
    public String toString() {
        return "" + getName() +
                " -- price: " + getPrice() +
                " -- available: " + getAvailableCount() +
                " -- sum: " + getMaxStockCount();
    }
}
