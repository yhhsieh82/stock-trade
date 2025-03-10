package com.stocktrading;

/**
 * Represents a completed trade between a buy and sell order.
 */
public class Trade {
    private final String symbol;
    private final double price;
    private final int quantity;
    private final long timestamp;
    
    /**
     * Creates a new trade.
     * 
     * @param symbol The stock symbol
     * @param price The price per unit
     * @param quantity The quantity of units
     */
    public Trade(String symbol, double price, int quantity) {
        this.symbol = symbol;
        this.price = price;
        this.quantity = quantity;
        this.timestamp = System.currentTimeMillis();
    }
    
    public String getSymbol() { return symbol; }
    public double getPrice() { return price; }
    public int getQuantity() { return quantity; }
    public long getTimestamp() { return timestamp; }
    
    @Override
    public String toString() {
        return String.format("TRADE: %s %d@%.2f (time: %d)", 
                symbol, quantity, price, timestamp);
    }
} 