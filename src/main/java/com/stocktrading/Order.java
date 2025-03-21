package com.stocktrading;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Represents a market order in the trading system.
 */
public class Order {
    public enum Type { BUY, SELL }

    // Static counter for generating unique IDs
    private static final AtomicLong ID_GENERATOR = new AtomicLong(0);
    
    private final long id;
    private final String symbol;
    private final Type type;
    private final double price;
    private final AtomicInteger quantity;
    private final long timestamp;
    
    /**
     * Creates a new order.
     * 
     * @param symbol The stock symbol
     * @param type The order type (BUY or SELL)
     * @param price The price per unit
     * @param quantity The quantity of units
     */
    public Order(String symbol, Type type, double price, int quantity) {
        this.id = ID_GENERATOR.incrementAndGet();
        this.symbol = symbol;
        this.type = type;
        this.price = price;
        this.quantity = new AtomicInteger(quantity);
        this.timestamp = System.currentTimeMillis();
    }

    public long getId() { return id; }
    public String getSymbol() { return symbol; }
    public Type getType() { return type; }
    public double getPrice() { return price; }
    public int getQuantity() { 
        return quantity.get(); 
    }
    public long getTimestamp() { return timestamp; }
    
    /**
     * Reduces the quantity of this order by the specified amount.
     * 
     * @param amount The amount to reduce by
     * @return true if the reduction was successful, false otherwise
     */
    public boolean reduceQuantity(int amount) {
        while (true) {
            int currentQuantity = quantity.get();
            if (amount > currentQuantity) {
                throw new IllegalArgumentException("Cannot reduce by more than the current quantity");
            }
            
            int newQuantity = currentQuantity - amount;
            if (quantity.compareAndSet(currentQuantity, newQuantity)) {
                return true;
            }
            // If we get here, someone else modified the quantity concurrently
            // We'll retry with the new current value
        }
    }
    
    @Override
    public String toString() {
        return String.format("Order[%d] %s %s %d@%.2f (time: %d)", 
                id, type, symbol, quantity.get(), price, timestamp);
    }
} 